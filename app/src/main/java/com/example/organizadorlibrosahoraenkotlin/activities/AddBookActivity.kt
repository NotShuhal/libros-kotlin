package com.example.organizadorlibrosahoraenkotlin.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.example.organizadorlibrosahoraenkotlin.models.Book
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.organizadorlibrosahoraenkotlin.models.FirebaseBook

class AddBookActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var etTitle: EditText
    private lateinit var etAuthor: EditText
    private lateinit var etPublisher: EditText
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var db: BookDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_book)

        // Inicializar Room
        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        etTitle = findViewById(R.id.etTitle)
        etAuthor = findViewById(R.id.etAuthor)
        etPublisher = findViewById(R.id.etPublisher)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        btnSave.setOnClickListener {
            saveBook()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveBook() {
        val title = etTitle.text.toString().trim()
        val author = etAuthor.text.toString().trim()
        val publisher = etPublisher.text.toString().trim()
        val note = etNote.text.toString().trim()

        if (title.isEmpty() || author.isEmpty() || publisher.isEmpty()) {
            Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val localBook = Book(title = title, author = author, publisher = publisher, note = note)
        val firebaseBook = FirebaseBook(title, author, publisher, note)

        val uid = auth.currentUser?.uid ?: return
        val bookId = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("books")
            .push()
            .key ?: return

        lifecycleScope.launch {
            // Guardar en Room
            db.bookDao().insert(localBook)

            // Guardar en Firebase
            FirebaseDatabase.getInstance()
                .reference
                .child("users")
                .child(uid)
                .child("books")
                .child(bookId)
                .setValue(firebaseBook)

            runOnUiThread {
                Toast.makeText(this@AddBookActivity, "Libro agregado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
