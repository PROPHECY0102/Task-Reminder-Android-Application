package com.example.taskreminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskRecyclerAdapter(private val tasksList: List<Task>) : RecyclerView.Adapter<TaskRecyclerAdapter.ViewHolder>() {

    // ViewHolder class definition
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTaskContent = itemView.findViewById<TextView>(R.id.textViewTaskContent)
        val textViewTaskDateTime = itemView.findViewById<TextView>(R.id.textViewTaskDateTime)
    }

    // Create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_row_card, parent, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasksList[position]
        holder.textViewTaskContent.setText(task.shortenContentText(35))
        holder.textViewTaskDateTime.setText("${task.date} ${task.time}")
    }

    // Return the size of your dataset
    override fun getItemCount() = tasksList.size
}