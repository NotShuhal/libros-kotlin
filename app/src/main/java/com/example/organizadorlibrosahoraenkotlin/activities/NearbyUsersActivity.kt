package com.example.organizadorlibrosahoraenkotlin.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.adapters.NearbyUsersAdapter
import com.example.organizadorlibrosahoraenkotlin.models.UserNearby
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.*

class NearbyUsersActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_CODE = 2001
        private const val MAX_DISTANCE_METERS = 5_000 // 5 km
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val users = mutableListOf<UserNearby>()

    private var currentLat: Double? = null
    private var currentLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_users)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerNearbyUsers)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = NearbyUsersAdapter(users) { user ->
            openChat(user)
        }

        findViewById<Button>(R.id.btnBackToMain).setOnClickListener {
            finish()
        }

        checkLocationPermission()
    }

    // =========================
    // PERMISOS
    // =========================

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================
    // UBICACIÓN
    // =========================

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude

                saveLocationToFirebase()
                loadNearbyUsers()
            } else {
                Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLocationToFirebase() {
        val uid = auth.currentUser?.uid ?: return

        val locationData = mapOf(
            "lat" to currentLat,
            "lng" to currentLng,
            "updatedAt" to System.currentTimeMillis()
        )

        database.reference
            .child("users")
            .child(uid)
            .child("location")
            .setValue(locationData)
    }

    // =========================
    // USUARIOS CERCANOS
    // =========================

    private fun loadNearbyUsers() {
        val myLat = currentLat ?: return
        val myLng = currentLng ?: return
        val currentUid = auth.currentUser?.uid ?: return

        database.reference.child("users").get().addOnSuccessListener { snapshot ->
            users.clear()

            for (child in snapshot.children) {
                val uid = child.key ?: continue
                if (uid == currentUid) continue

                val username = child.child("username").getValue(String::class.java) ?: continue
                val locationSnap = child.child("location")

                val lat = locationSnap.child("lat").getValue(Double::class.java) ?: continue
                val lng = locationSnap.child("lng").getValue(Double::class.java) ?: continue

                val distance = distanceInMeters(myLat, myLng, lat, lng)

                if (distance <= MAX_DISTANCE_METERS) {
                    users.add(
                        UserNearby(
                            uid = uid,
                            username = username,
                            lat = lat,
                            lng = lng,
                            distance = distance
                        )
                    )
                }
            }

            users.sortBy { it.distance }

            findViewById<RecyclerView>(R.id.recyclerNearbyUsers)
                .adapter?.notifyDataSetChanged()
        }
    }

    // =========================
    // CHAT
    // =========================

    private fun openChat(user: UserNearby) {
        val currentUid = auth.currentUser?.uid ?: return
        val chatId = listOf(currentUid, user.uid).sorted().joinToString("_")

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("otherUid", user.uid)
            putExtra("username", user.username)
        }

        startActivity(intent)
    }

    // =========================
    // DISTANCIA
    // =========================

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
