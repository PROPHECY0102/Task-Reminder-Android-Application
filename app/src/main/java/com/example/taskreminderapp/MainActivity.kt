package com.example.taskreminderapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    //Static Properties and Methods Called from other classes by MainActivity.staticProp
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        var tasksList = mutableListOf<Task>()
        val taskReminderNotificationTitle = "Reminded On:"
        val hoursInMillisOffset = 14400000

        @RequiresApi(Build.VERSION_CODES.S)
        fun canScheduleExactAlarms(context: Context): Boolean {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }

        fun scheduleNotification(
            context: Context,
            dateTimeLong: Long,
            title: String,
            message: String,
            notificationID: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!canScheduleExactAlarms(context)) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                    return
                }
            }

            val intent = Intent(context, Notification::class.java).apply {
                putExtra("title", title)
                putExtra("message", message)
                putExtra("notificationID", notificationID)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dateTimeLong,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Handle the exception (e.g., fallback to inexact alarm or notify the user)
                Log.e("AlarmScheduler", "Failed to schedule exact alarm", e)
                // Fallback to inexact alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    dateTimeLong,
                    pendingIntent
                )
            }
        }
    }

    //Instance Properties and Methods
    private var selectionState = false
    private var sortMode = "descending"
    private lateinit var tasksRecyclerAdapter: TaskRecyclerAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var calendar = Calendar.getInstance()
    private var currentYear = calendar.get(Calendar.YEAR)
    private var currentMonth = calendar.get(Calendar.MONTH)
    private var currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    private var currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    private var currentMinute = calendar.get(Calendar.MINUTE)

    private var newTaskContent = ""
    private var newTaskDate = ""
    private var newTaskTimeNotForDisplay = ""
    private var newTaskTime = ""

    private lateinit var currentEditingTask: Task

    private var editTaskContent = ""
    private var editTaskDate = ""
    private var editTaskTimeNotForDisplay = ""
    private var editTaskTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        fillDummyTasks() only used for testing/debugging RecyclerView
        onLaunchInitialise()
    }

    private fun onLaunchInitialise() {
        checkNotificationPermission()
        tasksList = initTasksDataPersistent().toMutableList()
        updateSubText()
        createDeleteButtonFunctionality()
        buildTaskRecyclerContainer(selectionState)
        createAddTaskFunctionality()
        createDropdownMenuFunctionality()
        startTaskReminderService()
    }

    private fun startTaskReminderService() {
        val serviceIntent = Intent(this, TaskReminderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopTaskReminderService() {
        val serviceIntent = Intent(this, TaskReminderService::class.java)
        stopService(serviceIntent)
    }

    private fun restartTaskReminderService() {
        stopTaskReminderService()
        startTaskReminderService()
    }

    private fun createAddTaskFunctionality() {
        val addTaskButton = findViewById<FloatingActionButton>(R.id.btnAddTask)
        addTaskButton.setOnClickListener {
            resetNewTaskInputs()
            val bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.add_new_task_modal, null)

            bottomSheetDialog.setContentView(bottomSheetView)
            val newTaskEditText = bottomSheetView.findViewById<EditText>(R.id.editTextNewTask)
            bottomSheetDialog.setOnShowListener {
                newTaskEditText.requestFocus()
                showKeyboard(newTaskEditText)
            }
            bottomSheetDialog.show()

            val cancelButton = bottomSheetView.findViewById<Button>(R.id.btnCancelTask)
            cancelButton.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            val setDateButton = bottomSheetView.findViewById<Button>(R.id.btnSetDate)
            setDateButton.setOnClickListener {
                val datePickerView = DatePickerDialog(
                    this,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        updateCurrentTime()
                        val paddedDay = selectedDay.toString().padStart(2, '0')
                        val paddedMonth = (selectedMonth + 1).toString().padStart(2, '0')
                        newTaskDate = "$paddedDay/$paddedMonth/$selectedYear"
                        updateDateText(setDateButton, newTaskDate)
                    },
                    currentYear, currentMonth, currentDay
                )
                datePickerView.show()
            }

            val setTimeButton = bottomSheetView.findViewById<Button>(R.id.btnSetTime)
            setTimeButton.setOnClickListener {
                val timePickerView = TimePickerDialog(
                    this,
                    {
                        _, selectedHour, selectedMinute ->
                        updateCurrentTime()
                        val amPm = if (selectedHour < 12) "A.M" else "P.M"
                        val hour = if (selectedHour > 12) selectedHour - 12 else if (selectedHour == 0) 12 else selectedHour
                        val paddedHour = hour.toString().padStart(2, '0')
                        val paddedMinute = selectedMinute.toString().padStart(2, '0')
                        newTaskTimeNotForDisplay = "$paddedHour:$paddedMinute:00"
                        newTaskTime = "${paddedHour}:${paddedMinute} $amPm"
                        updateTimeText(setTimeButton, newTaskTime)
                    },
                    currentHour, currentMinute, false
                )
                timePickerView.show()
            }

            val saveButton = bottomSheetView.findViewById<Button>(R.id.btnCreateSave)
            saveButton.setOnClickListener {
                newTaskContent = newTaskEditText.text.toString()
                updateCurrentTime()
                if (newTaskContent == "") {
                    val alertView = AlertDialog.Builder(this)
                    alertView.setTitle("Task Content cannot be Empty!")
                    alertView.setMessage("Please fill in the task content input for this reminder before saving it.")
                    alertView.setPositiveButton(android.R.string.ok) { dialog, which ->
                        Toast.makeText(applicationContext,
                            android.R.string.ok, Toast.LENGTH_SHORT).show()
                    }
                    alertView.show()
                    return@setOnClickListener
                }
                if (newTaskDate != "" || newTaskTime != "") {
                    val taskID = tasksList.size + 1
                    val dateTimeLongLocal = convertStringToMillis("$newTaskDate $newTaskTimeNotForDisplay")
                    val dateTimeLong = convertToUtcMillis(dateTimeLongLocal)
                    tasksList.add(Task(taskID, newTaskContent, newTaskDate, newTaskTime, dateFormat.format(System.currentTimeMillis()), dateTimeLong))
                    restartTaskReminderService()
                    saveTasksDataPersistent()
                    refreshTaskRecyclerContainer()
                    bottomSheetDialog.dismiss()
                    resetNewTaskInputs()
                } else {
                    val alertView = AlertDialog.Builder(this)
                    alertView.setTitle("No Date or Time has been set!")
                    alertView.setMessage("Please set a date and time for this reminder before saving it.")
                    alertView.setPositiveButton(android.R.string.ok) { dialog, which ->
                        Toast.makeText(applicationContext,
                            android.R.string.ok, Toast.LENGTH_SHORT).show()
                    }
                    alertView.show()
                }
            }
        }
    }

    private fun editCurrentTaskFunctionality() {
        if (currentEditingTask == null) return
        resetEditTaskInputs()
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.edit_existing_task_modal, null)

        bottomSheetDialog.setContentView(bottomSheetView)
        val editingTaskEditText = bottomSheetView.findViewById<EditText>(R.id.editTextExistingTask)
        bottomSheetDialog.setOnShowListener {
            editingTaskEditText.requestFocus()
            showKeyboard(editingTaskEditText)
        }
        bottomSheetDialog.show()

        editingTaskEditText.setText(currentEditingTask.content)

        // Convert date string
        val dateString = currentEditingTask.date
        val (day, month, year) = dateString.split("/").map { it.toInt() }

        // Convert time string
        val timeString = currentEditingTask.time
        val timeParts = timeString.replace(".", "").split(":")
        val hourMinute = timeParts[0].toInt()
        val minute = timeParts[1].substring(0, 2).toInt()
        val period = timeParts[1].substring(3)

        val hour = when {
            period.uppercase() == "PM" && hourMinute != 12 -> hourMinute + 12
            period.uppercase() == "AM" && hourMinute == 12 -> 0
            else -> hourMinute
        }

        val cancelButton = bottomSheetView.findViewById<Button>(R.id.btnCancelEditTask)
        cancelButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        val setDateButton = bottomSheetView.findViewById<Button>(R.id.btnSetEditDate)
        editTaskDate = currentEditingTask.date
        setDateButton.setText(currentEditingTask.date)
        setDateButton.setOnClickListener {
            val datePickerView = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val paddedDay = selectedDay.toString().padStart(2, '0')
                    val paddedMonth = (selectedMonth + 1).toString().padStart(2, '0')
                    editTaskDate = "$paddedDay/$paddedMonth/$selectedYear"
                    updateDateText(setDateButton, editTaskDate)
                },
                year, month - 1, day
            )
            datePickerView.show()
        }

        val setTimeButton = bottomSheetView.findViewById<Button>(R.id.btnSetEditTime)
        editTaskTime = currentEditingTask.time
        editTaskTimeNotForDisplay = "${currentEditingTask.time.substring(0, 2)}:${currentEditingTask.time.substring(3, 5)}:00"
        setTimeButton.setText(currentEditingTask.time)
        setTimeButton.setOnClickListener {
            val timePickerView = TimePickerDialog(
                this,
                {
                        _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "A.M" else "P.M"
                    val hour = if (selectedHour > 12) selectedHour - 12 else if (selectedHour == 0) 12 else selectedHour
                    val paddedHour = hour.toString().padStart(2, '0')
                    val paddedMinute = selectedMinute.toString().padStart(2, '0')
                    editTaskTimeNotForDisplay = "$paddedHour:$paddedMinute:00"
                    editTaskTime = "${paddedHour}:${paddedMinute} $amPm"
                    updateTimeText(setTimeButton, editTaskTime)
                },
                hour, minute, false
            )
            timePickerView.show()
        }

        val saveButton = bottomSheetView.findViewById<Button>(R.id.btnCreateEdit)
        saveButton.setOnClickListener {
            editTaskContent = editingTaskEditText.text.toString()
            if (editTaskContent == "") {
                val alertView = AlertDialog.Builder(this)
                alertView.setTitle("Task Content cannot be Empty!")
                alertView.setMessage("Please fill in the task content input for this reminder before editing it.")
                alertView.setPositiveButton(android.R.string.ok) { dialog, which ->
                    Toast.makeText(applicationContext,
                        android.R.string.ok, Toast.LENGTH_SHORT).show()
                }
                alertView.show()
                return@setOnClickListener
            }
            if (editTaskDate != "" || editTaskTime != "") {
                val dateTimeLongLocal = convertStringToMillis("$editTaskDate $editTaskTimeNotForDisplay")
                val dateTimeLong = convertToUtcMillis(dateTimeLongLocal)
                tasksList.forEach {
                    task: Task ->
                    if (task.id == currentEditingTask.id) {
                        task.content = editTaskContent
                        task.date = editTaskDate
                        task.time = editTaskTime
                        task.due = dateTimeLong
                    }
                }
                restartTaskReminderService()
                saveTasksDataPersistent()
                refreshTaskRecyclerContainer()
                bottomSheetDialog.dismiss()
                resetEditTaskInputs()
            } else {
                val alertView = AlertDialog.Builder(this)
                alertView.setTitle("No Date or Time has been set!")
                alertView.setMessage("Please set a date and time for this reminder before editing it.")
                alertView.setPositiveButton(android.R.string.ok) { dialog, which ->
                    Toast.makeText(applicationContext,
                        android.R.string.ok, Toast.LENGTH_SHORT).show()
                }
                alertView.show()
            }
        }

    }

    private fun resetNewTaskInputs() {
        newTaskContent = ""
        newTaskTime = ""
        newTaskDate = ""
    }

    private fun resetEditTaskInputs() {
        editTaskContent = ""
        editTaskDate = ""
        editTaskTime = ""
    }

    private fun updateCurrentTime() {
        calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)
    }

    private fun updateDateText(button: Button, date: String) {
        button.setText(date)
    }

    private fun updateTimeText(button: Button, time: String) {
        button.setText(time)
    }

    private fun buildTaskRecyclerContainer(localSelectionState: Boolean) {
        val sortedList = sortList()
        val tasksRecyclerContainer = findViewById<RecyclerView>(R.id.RecyclerTasksContainer)
        this.tasksRecyclerAdapter = TaskRecyclerAdapter(sortedList, localSelectionState,
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
            } else {
                currentEditingTask = task
                editCurrentTaskFunctionality()
            }
        })
        tasksRecyclerContainer.adapter = tasksRecyclerAdapter
        tasksRecyclerContainer.layoutManager = LinearLayoutManager(this)
    }

    private fun refreshTaskRecyclerContainer() {
        updateSubText()
        val sortedList = sortList()
        if (tasksRecyclerAdapter != null) {
            tasksRecyclerAdapter.updateAllItems(sortedList, selectionState)
        }
    }

    private fun createDeleteButtonFunctionality() {
        val deleteButton = findViewById<ImageButton>(R.id.btnDeleteTasks)
        deleteButton.setOnClickListener {
            if (selectionState) {
                tasksList = tasksList.filter { task: Task ->
                    !task.selected
                }.toMutableList()
                restartTaskReminderService()
                saveTasksDataPersistent()
                refreshTaskRecyclerContainer()
                if (tasksList.size == 0) {
                    selectionState = false
                }
                updateSubText()
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
        tasksList.forEach {
            task: Task ->
            task.selected = false
        }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveTasksDataPersistent() {
        val json = Gson().toJson(TasksListStructure(tasksList))
        openFileOutput("tasks.json", Context.MODE_PRIVATE).use {
            outputStream ->
            outputStream.write(json.toByteArray())
        }
    }

    private fun initTasksDataPersistent(): List<Task> {
        return try {
            val fileContents = openFileInput("tasks.json").bufferedReader().use { it.readText() }
            val taskListType = object : TypeToken<TasksListStructure>() {}.type
            Gson().fromJson<TasksListStructure>(fileContents, taskListType).tasks
        } catch (e: FileNotFoundException) {
            emptyList()
        }
    }

    private fun convertStringToMillis(dateString: String): Long {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        try {
            val date = format.parse(dateString)
            return date?.time ?: throw IllegalArgumentException("Invalid date string")
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing date string", e)
        }
    }

    private fun convertToUtcMillis(localTimeInMillis: Long): Long {
        val localDate = Date(localTimeInMillis)
        val localCalendar = Calendar.getInstance().apply {
            time = localDate
        }

        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                localCalendar.get(Calendar.YEAR),
                localCalendar.get(Calendar.MONTH),
                localCalendar.get(Calendar.DAY_OF_MONTH),
                localCalendar.get(Calendar.HOUR_OF_DAY),
                localCalendar.get(Calendar.MINUTE),
                localCalendar.get(Calendar.SECOND)
            )
            set(Calendar.MILLISECOND, localCalendar.get(Calendar.MILLISECOND))
        }

        return utcCalendar.timeInMillis + hoursInMillisOffset
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted, proceed with notification functionality
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) -> {
                    // Show in-app explanation
                    showNotificationImportanceDialog()
                }
                else -> {
                    // Request the permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf("android.permission.POST_NOTIFICATIONS"),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            // For Android versions below 13, notifications are enabled by default
            // You might want to check if notifications are enabled in app settings
            if (!areNotificationsEnabled()) {
                showNotificationImportanceDialog()
            }
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    private fun showNotificationImportanceDialog() {
        // Implement a dialog or in-app UI to explain why notifications are important
        // You can use AlertDialog, a custom dialog, or navigate to a specific fragment/activity
        AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("Notifications are important for this app because...")
            .setPositiveButton("Enable") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun createDropdownMenuFunctionality() {
        val dropdownButton = findViewById<ImageButton>(R.id.btnMenuDropdown)
        dropdownButton.setOnClickListener {
            view: View ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.dropdown_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.btnSortDescending -> {
                        sortMode = "descending"
                        refreshTaskRecyclerContainer()
                        true
                    }
                    R.id.btnSortAscending -> {
                        sortMode = "ascending"
                        refreshTaskRecyclerContainer()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun sortList(): MutableList<Task> {
        if (sortMode == "descending") {
            return tasksList.sortedByDescending { it.id }.toMutableList()
        }
        return tasksList.sortedBy { it.id }.toMutableList()
    }

    // Unused method
    // Was used to fill the application with dummy tasks to test RecyclerView functionality
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
            val task = Task(index + 1, content, dates[index], times[index], currentDateTime, 0)
            tasksList.add(task)
        }
    }
}