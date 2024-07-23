package com.example.myfirstapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val users: List<UserModel>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvUserName)
        val usernameTextView: TextView = itemView.findViewById(R.id.tvUsername)
        val emailTextView: TextView = itemView.findViewById(R.id.tvEmail)
        val addressTextView: TextView = itemView.findViewById(R.id.tvAddress)
        val companyTextView: TextView = itemView.findViewById(R.id.tvCompany)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.nameTextView.text = user.name
        holder.usernameTextView.text = user.username
        holder.emailTextView.text = user.email
        holder.addressTextView.text = "${user.address.street}, ${user.address.city}, ${user.address.zipcode}"
        holder.companyTextView.text = user.company.name
    }

    override fun getItemCount(): Int {
        return users.size
    }
}
