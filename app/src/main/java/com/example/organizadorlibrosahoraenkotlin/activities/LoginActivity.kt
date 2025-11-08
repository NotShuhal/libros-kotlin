package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organizadorlibrosahoraenkotlin.activities.MainActivity
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager

class LoginActivity : AppCompatActivity() {


    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvCreateAccount: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// Si ya está logueado, saltar al Main
        if (SharedPrefManager.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }


        setContentView(R.layout.activity_login)


        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)


        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            performLogin(email, password)
        }


        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }


    private fun performLogin(email: String, password: String) {
        if (email.isEmpty()) {
            etEmail.error = "Ingresa un correo"
            etEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo inválido"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Ingresa la contraseña"
            etPassword.requestFocus()
            return
        }


// Para este ejemplo simple guardamos un único usuario en SharedPreferences al crear cuenta.
        val storedUser = SharedPrefManager.getUser(this)
        if (storedUser == null) {
            Toast.makeText(this, "No existe cuenta. Crea una cuenta primero.", Toast.LENGTH_LONG).show()
            return
        }


// En esta implementación de ejemplo no guardamos la contraseña en claro.
// Para la tarea universitaria podemos simular que cualquier contraseña es válida si el email coincide.
        if (storedUser.email.equals(email, ignoreCase = true)) {
// Login exitoso
            SharedPrefManager.setLoggedIn(this, true)
            Toast.makeText(this, "Bienvenido, ${storedUser.username}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_LONG).show()
        }
    }
}