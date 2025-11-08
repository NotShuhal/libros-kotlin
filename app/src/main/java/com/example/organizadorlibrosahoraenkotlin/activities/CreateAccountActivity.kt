package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organizadorlibrosahoraenkotlin.activities.MainActivity
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.User

class CreateAccountActivity : AppCompatActivity() {


    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerCountry: Spinner
    private lateinit var btnCreate: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)


        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spinnerCountry = findViewById(R.id.spinnerCountry)
        btnCreate = findViewById(R.id.btnCreate)


// Poblamos spinner de países simple (puedes ampliar)
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


        if (email.isEmpty()) { etEmail.error = "Ingresa un correo"; etEmail.requestFocus(); return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Correo inválido"; etEmail.requestFocus(); return }
        if (username.isEmpty()) { etUsername.error = "Ingresa un nombre de usuario"; etUsername.requestFocus(); return }
        if (password.length < 6) { etPassword.error = "La contraseña debe tener al menos 6 caracteres"; etPassword.requestFocus(); return }
        if (password != confirm) { etConfirmPassword.error = "Las contraseñas no coinciden"; etConfirmPassword.requestFocus(); return }


// Guardar usuario (ejemplo simple)
        val user = User(email = email, username = username, country = country)
        SharedPrefManager.saveUser(this, user)
        SharedPrefManager.setLoggedIn(this, true)


        Toast.makeText(this, "Cuenta creada. Bienvenido, $username", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}