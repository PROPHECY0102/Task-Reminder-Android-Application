package com.example.taskreminderapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TaskRecyclerAdapter(
    private var tasksList: List<Task>,
    private var selectionState: Boolean,
    private val onItemLongClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskRecyclerAdapter.ViewHolder>() {

    // ViewHolder class definition
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardTaskBackground = itemView.findViewById<CardView>(R.id.cardTaskBackground)
        val radioButton = itemView.findViewById<ImageButton>(R.id.btnTaskRadio)
        val textViewTaskContent = itemView.findViewById<TextView>(R.id.textViewTaskContent)
        val textViewTaskDateTime = itemView.findViewById<TextView>(R.id.textViewTaskDateTime)

        fun bind(task: Task, onItemLongClick: (Task) -> Unit, onItemClick: (Task) -> Unit) {
            itemView.setOnLongClickListener() {
                onItemLongClick(task)
                true
            }

            itemView.setOnClickListener() {
                onItemClick(task)
            }

            if (!selectionState) {
                cardTaskBackground.setCardBackgroundColor(Color.parseColor("#FF334155"))
                if (task.due > System.currentTimeMillis()) {
                    radioButton.setImageResource(R.drawable.notification_active_icon)
                } else {
                    radioButton.setImageResource(R.drawable.baseline_check_circle_24)
                }
            } else {
                if (task.selected == true) {
                    cardTaskBackground.setCardBackgroundColor(Color.parseColor("#FFF43F5E"))
                    radioButton.setImageResource(R.drawable.radio_checked_icon)
                } else {
                    cardTaskBackground.setCardBackgroundColor(Color.parseColor("#FF334155"))
                    radioButton.setImageResource(R.drawable.radio_unchecked_icon)
                }
            }
        }
    }

    //Update the items manually
    fun updateAllItems(newTasksList: List<Task>, newSelectionState: Boolean) {
        selectionState = newSelectionState
        tasksList = newTasksList
        notifyDataSetChanged()
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
        holder.textViewTaskContent.setText(task.shortenContentText(80))
        holder.textViewTaskDateTime.setText("${task.date} ${task.time}")
        holder.bind(task, onItemLongClick, onItemClick)
    }

    // Return the size of your dataset
    override fun getItemCount() = tasksList.size
}