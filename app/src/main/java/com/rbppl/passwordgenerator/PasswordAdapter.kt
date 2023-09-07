package com.rbppl.passwordgenerator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PasswordAdapter(
    private val passwords: List<String>,
    private val copyClickListener: (position: Int) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder>() {

    inner class PasswordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val passwordTextView: TextView = itemView.findViewById(R.id.passwordTextView)
        val copyButton: ImageButton = itemView.findViewById(R.id.copyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_generated_password, parent, false)
        return PasswordViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        val password = passwords[position]
        holder.passwordTextView.text = password
        holder.copyButton.setOnClickListener { copyClickListener.invoke(position) }
    }

    override fun getItemCount(): Int = passwords.size
}
