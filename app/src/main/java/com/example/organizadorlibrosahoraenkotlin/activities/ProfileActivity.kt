package com.example.organizadorlibrosahoraenkotlin.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    private val PREFS_NAME = "user_prefs"
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var db: BookDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivProfile = findViewById(R.id.ivProfile)
        etName = findViewById(R.id.etName)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Inicializar base de datos
        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        ).build()

        // Cargar datos guardados
        val imageUri = prefs.getString("profile_image", null)
        val userName = prefs.getString("user_name", "")
        etName.setText(userName)

        if (imageUri != null) {
            try {
                val uri = Uri.parse(imageUri)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ivProfile.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                prefs.edit().remove("profile_image").apply()
                ivProfile.setImageResource(R.drawable.ic_person)
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_person)
        }

        // Cambiar imagen
        ivProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Guardar nombre e imagen
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            prefs.edit().apply {
                putString("user_name", name)
                apply()
            }
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
        }

        // Cerrar sesión
        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                db.bookDao().deleteAll()

                SharedPrefManager.clearSession(this@ProfileActivity)
                prefs.edit().clear().apply()

                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Sesión cerrada", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                ivProfile.setImageURI(imageUri)
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("profile_image", imageUri.toString())
                    apply()
                }
            }
        }
    }
}
