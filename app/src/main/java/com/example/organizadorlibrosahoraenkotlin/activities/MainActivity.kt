package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.adapters.BookAdapter
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.example.organizadorlibrosahoraenkotlin.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: BookDatabase
    private lateinit var adapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        ).build()

        adapter = BookAdapter(listOf()) { bookToDelete ->
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Eliminar libro")
            builder.setMessage("¿Seguro que deseas eliminar \"${bookToDelete.title}\"?")
            builder.setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    db.bookDao().delete(bookToDelete)
                    val updatedBooks = db.bookDao().getAll()
                    runOnUiThread {
                        adapter.updateBooks(updatedBooks)
                    }
                }
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        binding.recyclerViewBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBooks.adapter = adapter

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddBookActivity::class.java)
            startActivity(intent)
        }

        binding.fabSearch.setOnClickListener {
            val intent = Intent(this, SearchBookActivity::class.java)
            startActivity(intent)
        }

        // Cargar libros al iniciar
        loadBooks()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar la lista automáticamente al volver a esta Activity
        loadBooks()
    }

    private fun loadBooks() {
        lifecycleScope.launch {
            val books = db.bookDao().getAll()
            runOnUiThread {
                adapter.updateBooks(books)
            }
        }
    }
}
