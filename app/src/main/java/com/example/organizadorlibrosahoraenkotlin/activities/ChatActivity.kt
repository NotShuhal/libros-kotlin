package com.example.organizadorlibrosahoraenkotlin.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.adapters.ChatAdapter
import com.example.organizadorlibrosahoraenkotlin.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

private const val CAMERA_REQUEST_CODE = 101
private const val CAMERA_PERMISSION_CODE = 102

class ChatActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var ivUser: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var btnCamera: ImageButton

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var storage: FirebaseStorage   // ✅ AQUÍ VA

    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Firebase Storage
        storage = FirebaseStorage.getInstance()

        // Header
        btnBack = findViewById(R.id.btnBack)
        ivUser = findViewById(R.id.ivUser)
        tvUsername = findViewById(R.id.tvUsername)
        btnCamera = findViewById(R.id.btnCamera)

        btnBack.setOnClickListener { finish() }

        chatId = intent.getStringExtra("chatId") ?: return
        tvUsername.text = intent.getStringExtra("username") ?: "Usuario"

        // Chat
        recyclerView = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }
        btnCamera.setOnClickListener { openCamera() }

        listenMessages()
    }

    // 📸 Cámara
    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            bitmap?.let { uploadImageToFirebase(it) }
        }
    }

    // ☁️ Subir imagen
    private fun uploadImageToFirebase(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageData = baos.toByteArray()

        val ref = storage.reference
            .child("chat_images/$chatId/${System.currentTimeMillis()}.jpg")

        ref.putBytes(imageData)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    sendImageMessage(uri.toString())
                }
            }
    }

    // 💬 Texto
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val message = ChatMessage(
            senderId = auth.currentUser!!.uid,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        database.reference.child("messages").child(chatId).push().setValue(message)
        etMessage.text.clear()
    }

    // 🖼 Imagen
    private fun sendImageMessage(imageUrl: String) {
        val message = ChatMessage(
            senderId = auth.currentUser!!.uid,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis()
        )

        database.reference.child("messages").child(chatId).push().setValue(message)
    }

    private fun listenMessages() {
        database.reference.child("messages").child(chatId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.getValue(ChatMessage::class.java)?.let {
                        messages.add(it)
                        adapter.notifyItemInserted(messages.size - 1)
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
