package com.example.organizadorlibrosahoraenkotlin.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.example.organizadorlibrosahoraenkotlin.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SearchBookActivity : AppCompatActivity() {

    private lateinit var tvNoResults: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var recyclerResults: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: BookSearchAdapter
    private lateinit var db: BookDatabase

    private val books = mutableListOf<BookResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_book)

        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        recyclerResults = findViewById(R.id.recyclerResults)
        progressBar = findViewById(R.id.progressBar)
        tvNoResults = findViewById(R.id.tvNoResults)

        recyclerResults.layoutManager = LinearLayoutManager(this)
        adapter = BookSearchAdapter(books) { result ->
            saveBook(result)
        }
        recyclerResults.adapter = adapter

        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        ).build()

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Ingrese un término de búsqueda", Toast.LENGTH_SHORT).show()
            } else {
                searchBooks(query)
            }
        }

        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // vuelve a MainActivity
        }

    }

    private fun searchBooks(query: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            books.clear()
            val results = withContext(Dispatchers.IO) {
                val apiUrl = "https://www.googleapis.com/books/v1/volumes?q=${query.replace(" ", "+")}&maxResults=15"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val data = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val json = JSONObject(data)
                val items = json.optJSONArray("items")
                val tempList = mutableListOf<BookResult>()

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val volumeInfo = item.getJSONObject("volumeInfo")
                        val title = volumeInfo.optString("title", "Sin título")
                        val authorsArray = volumeInfo.optJSONArray("authors")
                        val author = authorsArray?.join(", ") ?: "Desconocido"
                        val publisher = volumeInfo.optString("publisher", "Sin editorial")
                        val imageLinks = volumeInfo.optJSONObject("imageLinks")
                        val thumbnail = imageLinks?.optString("thumbnail")

                        tempList.add(BookResult(title, author, publisher, thumbnail))
                    }
                }
                tempList
            }

            books.addAll(results)
            adapter.notifyDataSetChanged()

            tvNoResults.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            progressBar.visibility = View.GONE

        }
    }

    private fun saveBook(result: BookResult) {
        val book = Book(
            title = result.title,
            author = result.author,
            publisher = result.publisher,
            note = ""
        )

        lifecycleScope.launch {
            db.bookDao().insert(book)
            runOnUiThread {
                Toast.makeText(this@SearchBookActivity, "Libro agregado a tu lista", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class BookResult(
    val title: String,
    val author: String,
    val publisher: String,
    val imageUrl: String?
)

class BookSearchAdapter(
    private val books: List<BookResult>,
    private val onAddClicked: (BookResult) -> Unit
) : RecyclerView.Adapter<BookSearchAdapter.BookViewHolder>() {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvPublisher: TextView = view.findViewById(R.id.tvPublisher)
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BookViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = "Autor: ${book.author}"
        holder.tvPublisher.text = "Editorial: ${book.publisher}"

        val imageUrl = book.imageUrl?.replace("http://", "https://")
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.btnAdd.setOnClickListener {
            onAddClicked(book)
        }
    }


    override fun getItemCount(): Int = books.size
}
