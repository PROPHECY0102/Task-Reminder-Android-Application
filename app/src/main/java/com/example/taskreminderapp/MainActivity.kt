package com.example.taskreminderapp

import android.os.Bundle
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
    var tasksList: MutableList<Task> = mutableListOf()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
        findViewById<TextView>(R.id.textViewTaskAmount).setText("Showing ${tasksList.size} Tasks")
        val tasksRecyclerContainer = findViewById<RecyclerView>(R.id.RecyclerTasksContainer)
        val tasksRecyclerAdapter = TaskRecyclerAdapter(tasksList)
        tasksRecyclerContainer.adapter = tasksRecyclerAdapter
        tasksRecyclerContainer.layoutManager = LinearLayoutManager(this)
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