package com.example.organizadorlibrosahoraenkotlin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.bumptech.glide.Glide
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.SharedPrefManager
import com.example.organizadorlibrosahoraenkotlin.adapters.BookAdapter
import com.example.organizadorlibrosahoraenkotlin.data.BookDatabase
import com.example.organizadorlibrosahoraenkotlin.databinding.ActivityMainBinding
import com.example.organizadorlibrosahoraenkotlin.models.Book
import com.example.organizadorlibrosahoraenkotlin.models.FirebaseBook
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: BookDatabase
    private lateinit var adapter: BookAdapter
    private lateinit var auth: FirebaseAuth

    private lateinit var booksRef: DatabaseReference
    private var booksListener: ValueEventListener? = null

    private val PROFILE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = auth.currentUser!!.uid

        booksRef = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("books")

        db = Room.databaseBuilder(
            applicationContext,
            BookDatabase::class.java,
            "books-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        adapter = BookAdapter(
            books = emptyList(),

            onDeleteClick = { book ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar libro")
                    .setMessage("¿Seguro que deseas eliminar \"${book.title}\"?")
                    .setPositiveButton("Sí") { _, _ ->
                        lifecycleScope.launch {
                            db.bookDao().delete(book)
                            deleteBookFromFirebase(book)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },

            onTradeToggle = { book, isForTrade ->
                lifecycleScope.launch {
                    val updated = book.copy(forTrade = isForTrade)
                    db.bookDao().update(updated)
                    updateBookField(book.title, "forTrade", isForTrade)
                }
            },

            onWishlistToggle = { book, isWishlist ->
                lifecycleScope.launch {
                    val updated = book.copy(wishlist = isWishlist)
                    db.bookDao().update(updated)
                    updateBookField(book.title, "wishlist", isWishlist)
                }
            }
        )

        binding.recyclerViewBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBooks.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddBookActivity::class.java))
        }

        binding.fabSearch.setOnClickListener {
            startActivity(Intent(this, SearchBookActivity::class.java))
        }

        binding.btnNearby.setOnClickListener {
            startActivity(Intent(this, NearbyUsersActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            startActivityForResult(
                Intent(this, ProfileActivity::class.java),
                PROFILE_REQUEST_CODE
            )
        }

        loadBooksFromFirebase()
        loadUserInfo()
        updateUserLocation()
    }

    // ---------------------------
    // Firebase → Room sync
    // ---------------------------

    private fun loadBooksFromFirebase() {

        booksListener?.let { booksRef.removeEventListener(it) }

        booksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                lifecycleScope.launch {
                    val firebaseBooks = mutableListOf<Book>()

                    for (bookSnap in snapshot.children) {
                        val fbBook = bookSnap.getValue(FirebaseBook::class.java) ?: continue

                        val book = Book(
                            title = fbBook.title,
                            author = fbBook.author,
                            publisher = fbBook.publisher,
                            note = fbBook.note,
                            forTrade = fbBook.forTrade,
                            wishlist = fbBook.wishlist
                        )

                        firebaseBooks.add(book)
                    }

                    // SOLO actualizar lista visual
                    adapter.updateBooks(firebaseBooks)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar libros",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        booksRef.addValueEventListener(booksListener!!)
    }

    // ---------------------------
    // Firebase helpers
    // ---------------------------

    private fun updateBookField(bookTitle: String, field: String, value: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("books")
            .orderByChild("title")
            .equalTo(bookTitle)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach {
                    it.ref.child(field).setValue(value)
                }
            }
    }

    private fun deleteBookFromFirebase(book: Book) {
        val uid = auth.currentUser?.uid ?: return

        FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("books")
            .orderByChild("title")
            .equalTo(book.title)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { it.ref.removeValue() }
            }
    }

    // ---------------------------
    // User info & GPS
    // ---------------------------

    private fun loadUserInfo() {
        val user = SharedPrefManager.getUser(this) ?: return

        binding.tvWelcome.text = "Bienvenido, ${user.username}"

        if (!user.profileImageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.profileImageUri)
                .placeholder(R.drawable.ic_person)
                .into(binding.btnProfile)
        } else {
            binding.btnProfile.setImageResource(R.drawable.ic_person)
        }
    }

    private fun updateUserLocation() {
        val uid = auth.currentUser?.uid ?: return

        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                location ?: return@addOnSuccessListener

                FirebaseDatabase.getInstance()
                    .reference
                    .child("users")
                    .child(uid)
                    .updateChildren(
                        mapOf(
                            "lat" to location.latitude,
                            "lng" to location.longitude
                        )
                    )
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadUserInfo()
        }
    }
    override fun onResume() {
        super.onResume()
        updateUserLocation()
    }

}
