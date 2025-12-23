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
import com.bumptech.glide.Glide
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.User
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var db: BookDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private var savedImagePath: String? = null

    private fun saveUserProfile(
        uid: String,
        username: String,
        imageUrl: String?
    ) {
        val userMap = mapOf(
            "email" to auth.currentUser?.email,
            "username" to username,
            "country" to "",
            "profileImageUrl" to imageUrl
        )

        // 1. Guardar en Realtime Database
        database.reference
            .child("users")
            .child(uid)
            .setValue(userMap)
            .addOnSuccessListener {

                // 2. Actualizar Firebase Auth
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = username
                    imageUrl?.let { photoUri = Uri.parse(it) }
                }

                auth.currentUser?.updateProfile(profileUpdates)

                // 3. 🔥 ACTUALIZAR SharedPreferences (CLAVE)
                SharedPrefManager.saveUser(
                    this,
                    User(
                        email = auth.currentUser?.email ?: "",
                        username = username,
                        country = "",
                        description = "",
                        profileImageUri = imageUrl
                    )
                )

                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar perfil", Toast.LENGTH_SHORT).show()
            }
    }



    private fun uploadProfileImage(uid: String, username: String) {
        val ref = storage.reference.child("profile_images/$uid.jpg")

        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveUserProfile(uid, username, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivProfile = findViewById(R.id.ivProfile)
        etName = findViewById(R.id.etName)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        auth = FirebaseAuth.getInstance()

        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val user = SharedPrefManager.getUser(this)
        etName.setText(user?.username ?: "Usuario")

        user?.profileImageUri?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_person)
                .into(ivProfile)
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

            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            if (selectedImageUri != null) {
                uploadProfileImage(uid, name)
            } else {
                saveUserProfile(uid, name, null)
            }
        }



        btnLogout.setOnClickListener {

            FirebaseAuth.getInstance().signOut()
            SharedPrefManager.clearSession(this)

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }


        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data ?: return
            val copiedPath = copyImageToInternalStorage(imageUri)
            if (copiedPath != null) {
                selectedImageUri = imageUri
                ivProfile.setImageURI(imageUri)
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
