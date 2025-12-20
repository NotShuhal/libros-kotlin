package com.example.organizadorlibrosahoraenkotlin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import com.example.organizadorlibrosahoraenkotlin.activities.NearbyUsersActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: BookDatabase
    private lateinit var adapter: BookAdapter
    private lateinit var auth: FirebaseAuth
    private val PROFILE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fabSearch.setOnClickListener {
            startActivity(Intent(this, SearchBookActivity::class.java))
        }
        binding.btnNearby.setOnClickListener {
            startActivity(Intent(this, NearbyUsersActivity::class.java))
        }

        auth = FirebaseAuth.getInstance()

        // Si NO hay usuario logueado en Firebase → enviamos al LoginActivity
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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
        updateUserLocation()
    }

    private fun updateUserLocation() {
        val uid = auth.currentUser?.uid ?: return

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val updates = mapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude
                )

                FirebaseDatabase.getInstance()
                    .reference
                    .child("users")
                    .child(uid)
                    .updateChildren(updates)
            }
        }
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadUserInfo()
        }
    }
}
