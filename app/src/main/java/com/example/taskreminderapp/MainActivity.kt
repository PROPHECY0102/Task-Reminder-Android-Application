package com.example.taskreminderapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private var tasksList: MutableList<Task> = mutableListOf()
    private var selectionState = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private lateinit var tasksRecyclerAdapter: TaskRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fillDummyTasks()
        updateSubText()
        createDeleteButtonFunction()
        buildTaskRecyclerContainer(selectionState)
    }

    private fun refreshTaskRecyclerContainer() {
        if (tasksRecyclerAdapter != null) {
            tasksRecyclerAdapter.updateAllItems(tasksList, selectionState)
        }
    }

    private fun buildTaskRecyclerContainer(localSelectionState: Boolean) {
        val tasksRecyclerContainer = findViewById<RecyclerView>(R.id.RecyclerTasksContainer)
        this.tasksRecyclerAdapter = TaskRecyclerAdapter(tasksList, localSelectionState,
            onItemLongClick = {
            task: Task ->
            selectionState = !selectionState
            if (selectionState) {
                task.selected = true
            } else {
                refreshSelectionTasksList()
            }
            updateSubText()
            refreshTaskRecyclerContainer()
        }, onItemClick = {
            task: Task ->
            if (selectionState) {
                task.selected = !task.selected
                refreshTaskRecyclerContainer()
                updateSubText()
            }
        })
        tasksRecyclerContainer.adapter = tasksRecyclerAdapter
        tasksRecyclerContainer.layoutManager = LinearLayoutManager(this)
    }

    private fun createDeleteButtonFunction() {
        val deleteButton = findViewById<ImageButton>(R.id.btnDeleteTasks)
        deleteButton.setOnClickListener {
            if (selectionState) {
                tasksList = tasksList.filter { task: Task ->
                    !task.selected
                }.toMutableList()
                refreshTaskRecyclerContainer()
                if (tasksList.size == 0) {
                    selectionState = false
                    updateSubText()
                }
            }
        }
    }

    private fun updateSubText() {
        val subText = findViewById<TextView>(R.id.textViewTaskAmount)
        val deleteButton = findViewById<ImageButton>(R.id.btnDeleteTasks)
        if (!selectionState)  {
            deleteButton.setImageResource(R.drawable.delete_icon)
            subText.setTextColor(getColor(R.color.foreground))
            subText.setText("Showing ${tasksList.size} Tasks")
        } else {
            val currentlySelectedTasks = tasksList.filter {
                task ->
                task.selected
            }
            deleteButton.setImageResource(R.drawable.delete_icon_active)
            subText.setTextColor(getColor(R.color.accent_red))
            subText.setText("Currently Selecting ${currentlySelectedTasks.size} Tasks")
        }
    }

    private fun refreshSelectionTasksList() {
        tasksList.forEach() {
            task: Task ->
            task.selected = false
        }
    }

    private fun fillDummyTasks() {
        val contents = listOf("Discuss Plans of World Domination with my War Secretary"
            , "Have tea with the Major General"
            , "Ensure the Resupply of Front Line Troops"
            , "Co-ordinate Structural Plans for a Ground Invasion"
            , "Declare War on the Trade Federation"
            , "Reassure Alliances with Vulnerable Countries"
            , "Execute Order 66"
            , "Declare an Emergency in the Senate!"
            , "Invade Coruscant with a Grand Army of Clones"
            , "Capture the Jedi Temple")
        val dates = listOf("12/08/2024"
            , "12/08/2024"
            , "15/08/2024"
            , "18/08/2024"
            , "20/08/2024"
            , "21/08/2024"
            , "30/08/2024"
            , "31/08/2024"
            , "05/09/2024"
            , "05/09/2024")
        val times = listOf("10:30 A.M"
            , "12:30 P.M"
            , "9:45 A.M"
            , "2:00 P.M"
            , "1:30 P.M"
            , "8:45 A.M"
            , "11:30 A.M"
            , "3:45 P.M"
            , "9:15 A.M"
            , "6:30 P.M")
        val currentDateTime = dateFormat.format(Date(System.currentTimeMillis()))
        contents.forEachIndexed {
            index, content ->
            val task = Task(index + 1, content, dates[index], times[index], currentDateTime)
            tasksList.add(task)
        }
    }
}