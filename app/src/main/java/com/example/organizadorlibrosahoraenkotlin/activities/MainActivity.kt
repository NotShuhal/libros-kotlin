package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.organizadorlibrosahoraenkotlin.adapters.BookAdapter
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.example.organizadorlibrosahoraenkotlin.models.Book
import kotlinx.coroutines.launch
import com.example.organizadorlibrosahoraenkotlin.R


class MainActivity : AppCompatActivity() {

    private lateinit var bookAdapter: BookAdapter
    private lateinit var db: BookDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        val recycler: RecyclerView = findViewById(R.id.recyclerBooks)
        val btnAdd: Button = findViewById(R.id.btnAddBook)
        val btnSearch: Button = findViewById(R.id.btnSearchBook)
        val btnProfile: ImageButton = findViewById(R.id.btnProfile)

        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        ).build()

        val books = mutableListOf<Book>()
        bookAdapter = BookAdapter(books) { book ->
            lifecycleScope.launch {
                db.bookDao().delete(book)
                books.remove(book)
                bookAdapter.notifyDataSetChanged()
            }
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = bookAdapter

        lifecycleScope.launch {
            val savedBooks = db.bookDao().getAll()
            books.addAll(savedBooks)
            bookAdapter.notifyDataSetChanged()
        }

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddBookActivity::class.java))
        }

        btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchBookActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        tvWelcome.text = "Bienvenido, ${SharedPrefManager.getUsername(this)}"
    }
}
