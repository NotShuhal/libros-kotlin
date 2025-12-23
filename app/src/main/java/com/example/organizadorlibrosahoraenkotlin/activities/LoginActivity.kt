package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.User
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN = 1001

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvCreateAccount: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Si el usuario ya está autenticado en Firebase
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            performLogin(email, password)
        }

        findViewById<Button>(R.id.btnGoogleLogin).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
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

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    saveUserIfNeeded(email)
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    Toast.makeText(
                        this,
                        "Correo o contraseña incorrectos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserIfNeeded(email: String) {
        val storedUser = SharedPrefManager.getUser(this)
        if (storedUser == null) {
            val firebaseUser = auth.currentUser
            SharedPrefManager.saveUser(
                this,
                User(
                    email = email,
                    username = firebaseUser?.displayName ?: "Usuario",
                    country = "",
                    profileImageUri = null
                )
            )
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val firebaseUser = auth.currentUser

                    SharedPrefManager.saveUser(
                        this,
                        User(
                            email = firebaseUser?.email ?: "",
                            username = firebaseUser?.displayName ?: "Usuario",
                            country = "",
                            profileImageUri = firebaseUser?.photoUrl?.toString()
                        )
                    )

                    Toast.makeText(this, "Sesión iniciada con Google", Toast.LENGTH_SHORT).show()
                    goToMain()

                } else {
                    Toast.makeText(this, "Falló autenticación con Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error al iniciar con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
