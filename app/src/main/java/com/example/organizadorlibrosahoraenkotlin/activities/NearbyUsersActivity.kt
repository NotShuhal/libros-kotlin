package com.example.organizadorlibrosahoraenkotlin.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.adapters.NearbyUsersAdapter
import com.example.organizadorlibrosahoraenkotlin.models.UserNearby
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.*

class NearbyUsersActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val users = mutableListOf<UserNearby>()

    // Coordenadas mock para pruebas
    private val MOCK_LAT = -33.45
    private val MOCK_LNG = -70.66

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_users)

        val recycler = findViewById<RecyclerView>(R.id.recyclerNearbyUsers)
        recycler.layoutManager = LinearLayoutManager(this)

        recycler.adapter = NearbyUsersAdapter(users) { user ->
            openChat(user)
        }

        findViewById<android.widget.Button>(R.id.btnBackToMain).setOnClickListener {
            finish()
        }

        loadNearbyUsers()
    }

    private fun loadNearbyUsers() {
        val currentUid = auth.currentUser?.uid ?: return

        database.reference.child("users").get().addOnSuccessListener { snapshot ->
            users.clear()

            for (child in snapshot.children) {
                val uid = child.key ?: continue
                if (uid == currentUid) continue

                val username = child.child("username").getValue(String::class.java) ?: continue
                val lat = child.child("lat").getValue(Double::class.java) ?: continue
                val lng = child.child("lng").getValue(Double::class.java) ?: continue

                val distance = distanceInMeters(MOCK_LAT, MOCK_LNG, lat, lng)

                if (distance <= 1000) {
                    users.add(UserNearby(uid, username, lat, lng))
                }
            }

            findViewById<RecyclerView>(R.id.recyclerNearbyUsers)
                .adapter?.notifyDataSetChanged()
        }
    }

    private fun openChat(user: UserNearby) {
        val currentUid = auth.currentUser?.uid ?: return
        val chatId = generateChatId(currentUid, user.uid)

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("otherUid", user.uid)
            putExtra("username", user.username)
        }

        startActivity(intent)
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    private fun distanceInMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
