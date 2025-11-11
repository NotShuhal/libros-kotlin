package com.example.organizadorlibrosahoraenkotlin.activities

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.adapters.BookAdapter
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.example.organizadorlibrosahoraenkotlin.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: BookDatabase
    private lateinit var adapter: BookAdapter
    private val PROFILE_REQUEST_CODE = 1001

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
                    runOnUiThread { adapter.updateBooks(updatedBooks) }
                }
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        binding.recyclerViewBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBooks.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddBookActivity::class.java))
        }

        binding.fabSearch.setOnClickListener {
            startActivity(Intent(this, SearchBookActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivityForResult(intent, PROFILE_REQUEST_CODE)
        }

        loadBooks()
        loadUserInfo()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
        loadUserInfo()
    }

    private fun loadBooks() {
        lifecycleScope.launch {
            val books = db.bookDao().getAll()
            runOnUiThread { adapter.updateBooks(books) }
        }
    }

    private fun loadUserInfo() {
        val user = SharedPrefManager.getUser(this)
        binding.tvWelcome.text = if (!user?.username.isNullOrEmpty()) {
            "Bienvenido, ${user?.username}"
        } else "Bienvenido, Usuario"

        val imagePath = user?.profileImageUri
        if (!imagePath.isNullOrEmpty()) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.btnProfile.setImageBitmap(bitmap)
                return
            }
        }
        binding.btnProfile.setImageResource(R.drawable.ic_person)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadUserInfo()
        }
    }
}
