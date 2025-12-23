package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.userProfileChangeRequest

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerCountry: Spinner
    private lateinit var btnCreate: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spinnerCountry = findViewById(R.id.spinnerCountry)
        btnCreate = findViewById(R.id.btnCreate)

        val countries = listOf("Chile", "Argentina", "Perú", "México", "España", "Otro")
        spinnerCountry.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, countries)

        btnCreate.setOnClickListener {
            createAccount()
        }
    }

    private fun createAccount() {
        val email = etEmail.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        val country = spinnerCountry.selectedItem.toString()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo inválido"; return
        }
        if (username.isEmpty()) {
            etUsername.error = "Ingresa un nombre"; return
        }
        if (password.length < 6) {
            etPassword.error = "Mínimo 6 caracteres"; return
        }
        if (password != confirm) {
            etConfirmPassword.error = "No coinciden"; return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val uid = auth.currentUser!!.uid

                    val userMap = mapOf(
                        "email" to email,
                        "username" to username,
                        "country" to country,
                        "profileImageUrl" to ""
                    )

                    FirebaseDatabase.getInstance()
                        .reference
                        .child("users")
                        .child(uid)
                        .setValue(userMap)
                        .addOnSuccessListener {

                            // Guardado local (cache)
                            SharedPrefManager.saveUser(
                                this,
                                User(
                                    email = email,
                                    username = username,
                                    country = country,
                                    profileImageUri = null
                                )
                            )

                            Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()

                            startActivity(
                                Intent(this, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                            finish()
                        }

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Error al crear cuenta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}