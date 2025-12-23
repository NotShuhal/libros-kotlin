package com.example.organizadorlibrosahoraenkotlin.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.adapters.NearbyUsersAdapter
import com.example.organizadorlibrosahoraenkotlin.models.FirebaseBook
import com.example.organizadorlibrosahoraenkotlin.models.UserNearby
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class NearbyUsersActivity : AppCompatActivity() {

    private lateinit var adapter: NearbyUsersAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_users)

        auth = FirebaseAuth.getInstance()

        adapter = NearbyUsersAdapter(emptyList())

        val recycler = findViewById<RecyclerView>(R.id.recyclerNearbyUsers)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Usuarios cercanos"


        loadNearbyUsers()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private suspend fun getMyWishlist(): Set<String> {
        val uid = FirebaseAuth.getInstance().uid ?: return emptySet()

        val snapshot = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("books")
            .get()
            .await()

        return snapshot.children
            .filter { it.child("wishlist").getValue(Boolean::class.java) == true }
            .mapNotNull { it.child("title").getValue(String::class.java) }
            .toSet()
    }

    private fun distanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun loadNearbyUsers() {
        val myUid = FirebaseAuth.getInstance().uid ?: return
        val usersRef = FirebaseDatabase.getInstance().reference.child("users")

        lifecycleScope.launch {
            val myWishlist = getMyWishlist()

            usersRef.get().addOnSuccessListener { snapshot ->
                val results = mutableListOf<UserNearby>()

                val myLat = snapshot.child(myUid).child("lat").getValue(Double::class.java) ?: return@addOnSuccessListener
                val myLng = snapshot.child(myUid).child("lng").getValue(Double::class.java) ?: return@addOnSuccessListener

                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    if (uid == myUid) continue

                    val lat = userSnap.child("lat").getValue(Double::class.java) ?: continue
                    val lng = userSnap.child("lng").getValue(Double::class.java) ?: continue

                    val distance = distanceKm(myLat, myLng, lat, lng)

                    val matchedBooks = userSnap.child("books").children
                        .filter {
                            it.child("forTrade").getValue(Boolean::class.java) == true &&
                                    myWishlist.contains(it.child("title").getValue(String::class.java))
                        }
                        .mapNotNull { it.child("title").getValue(String::class.java) }

                    if (matchedBooks.isNotEmpty()) {
                        results.add(
                            UserNearby(
                                uid = uid,
                                username = userSnap.child("username").getValue(String::class.java) ?: "Usuario",
                                distanceKm = distance,
                                matchedBooks = matchedBooks
                            )
                        )
                    }
                }

                val ordered = results.sortedWith(
                    compareBy<UserNearby> { it.distanceKm }
                        .thenByDescending { it.matchedBooks.size }
                )

                adapter.update(ordered)
            }
        }
    }
}