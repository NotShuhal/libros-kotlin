package com.example.organizadorlibrosahoraenkotlin.activities

import android.app.Activity
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
import com.example.organizadorlibrosahoraenkotlin.User
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var db: BookDatabase
    private lateinit var auth: FirebaseAuth

    private val PICK_IMAGE_REQUEST = 1
    private var savedImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivProfile = findViewById(R.id.ivProfile)
        etName = findViewById(R.id.etName)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)

        auth = FirebaseAuth.getInstance()

        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        ).build()

        val user = SharedPrefManager.getUser(this)
        etName.setText(user?.username ?: "Usuario")

        user?.profileImageUri?.let {
            val file = File(it)
            if (file.exists()) {
                ivProfile.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                savedImagePath = it
            }
        }

        ivProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedUser = user?.copy(
                username = name,
                profileImageUri = savedImagePath
            ) ?: User(email = "", username = name, country = "", profileImageUri = savedImagePath)

            SharedPrefManager.saveUser(this, updatedUser)
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                db.bookDao().deleteAll()
                SharedPrefManager.clearSession(this@ProfileActivity)

                auth.signOut() // ← Cierre de sesión REAL en Firebase

                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data ?: return
            val copiedPath = copyImageToInternalStorage(imageUri)
            if (copiedPath != null) {
                savedImagePath = copiedPath
                ivProfile.setImageBitmap(BitmapFactory.decodeFile(copiedPath))
            } else {
                Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                inputStream?.copyTo(output)
            }
            inputStream?.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
