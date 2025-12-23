package com.example.organizadorlibrosahoraenkotlin.activities

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.adapters.WishlistAdapter
import com.example.organizadorlibrosahoraenkotlin.models.FirebaseWishlistBook
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WishlistActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var wishlistRef: DatabaseReference
    private lateinit var adapter: WishlistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        wishlistRef = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("wishlist")

        // RecyclerView
        val recycler = findViewById<RecyclerView>(R.id.recyclerWishlist)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = WishlistAdapter(mutableListOf())
        recycler.adapter = adapter

        // FAB
        findViewById<FloatingActionButton>(R.id.fabAddWishlist)
            .setOnClickListener {
                showAddWishlistDialog()
            }

        loadWishlist()
    }

    // ===== PASO 4: CARGAR WISHLIST =====
    private fun loadWishlist() {
        wishlistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FirebaseWishlistBook>()

                for (child in snapshot.children) {
                    val book = child.getValue(FirebaseWishlistBook::class.java)
                    if (book != null) list.add(book)
                }

                adapter.update(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ===== PASO 3: AGREGAR A WISHLIST =====
    private fun showAddWishlistDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_wishlist, null)

        val title = view.findViewById<android.widget.EditText>(R.id.etTitle)
        val author = view.findViewById<android.widget.EditText>(R.id.etAuthor)
        val publisher = view.findViewById<android.widget.EditText>(R.id.etPublisher)

        AlertDialog.Builder(this)
            .setTitle("Agregar a wishlist")
            .setView(view)
            .setPositiveButton("Agregar") { _, _ ->
                val book = FirebaseWishlistBook(
                    title.text.toString(),
                    author.text.toString(),
                    publisher.text.toString()
                )

                wishlistRef.push().setValue(book)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
