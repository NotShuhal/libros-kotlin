package com.example.organizadorlibrosahoraenkotlin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.models.UserNearby

class NearbyUsersAdapter(
    private var users: List<UserNearby>,
    private val onUserClick: (UserNearby) -> Unit
) : RecyclerView.Adapter<NearbyUsersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val matches: TextView = itemView.findViewById(R.id.tvMatches)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        holder.username.text = user.username
        holder.matches.text =
            "Coincide con ${user.matchedBooks.size} libros:\n" +
                    user.matchedBooks.joinToString(", ")

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }


    override fun getItemCount(): Int = users.size

    fun update(newUsers: List<UserNearby>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
