package com.example.taskreminderapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

// This class is used for configuring the RecyclerView for rendering all tasks into CardViews
class TaskRecyclerAdapter(
    private var tasksList: List<Task>,
    private var selectionState: Boolean,
    private val onItemLongClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskRecyclerAdapter.ViewHolder>() {

    // ViewHolder, for configuring how each cardView regardless of content should be rendered
    // External data and state can be passed into this inner class from MainActivity
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Get necessary views elements from task_row_card
        val cardTaskBackground = itemView.findViewById<CardView>(R.id.cardTaskBackground)
        val radioButton = itemView.findViewById<ImageButton>(R.id.btnTaskRadio)
        val textViewTaskContent = itemView.findViewById<TextView>(R.id.textViewTaskContent)
        val textViewTaskDateTime = itemView.findViewById<TextView>(R.id.textViewTaskDateTime)

        // Bind event listeners like on click or on long click (hold)
        fun bind(task: Task, onItemLongClick: (Task) -> Unit, onItemClick: (Task) -> Unit) {
            itemView.setOnLongClickListener() {
                // Binding Events, specified from MainActivity
                onItemLongClick(task)
                true
            }

            itemView.setOnClickListener() {
                // Binding Events, specified from MainActivity
                onItemClick(task)
            }

            // Controlling how each card item is rendered based on its current state
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

    //Update the items manually used for refreshing RecyclerView
    fun updateAllItems(newTasksList: List<Task>, newSelectionState: Boolean) {
        selectionState = newSelectionState
        tasksList = newTasksList
        notifyDataSetChanged()
    }

    // Create new views, Used for inflating the CardView then pass it to ViewHolder for each card configuration
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_row_card, parent, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view, this method is called whenever a task card item gets into view of the user
    // Tracks the position and fills details according to each task items
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasksList[position]
        holder.textViewTaskContent.setText(task.shortenContentText(80))
        holder.textViewTaskDateTime.setText("${task.date} ${task.time}")
        holder.bind(task, onItemLongClick, onItemClick)
    }

    // Return the size of your dataset
    override fun getItemCount() = tasksList.size
}