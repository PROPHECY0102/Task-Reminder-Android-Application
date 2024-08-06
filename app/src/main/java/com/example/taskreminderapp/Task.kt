package com.example.taskreminderapp

// This class is used for modelling Task objects in order to better organised properties of individual tasks
class Task(
    var id: Int, // auto-increment unique integer
    var content: String, // task content example: "Meeting at 5pm with George"
    var date: String, // format dd/MM/yyyy
    var time: String, // format hh:mm a
    var createdAt: String, // required but not actually used
    var due: Long, // date/time following universal standards of UTC milliseconds offset by 8 Hours (GMT+8)
    var selected: Boolean = false, // for selection mode indicate which tasks are selected
) {
    // Helper method to shorten text in card view
    fun shortenContentText(n: Int): String {
        if (content.length > n) {
            return "${content.substring(0, n)}..."
        } else {
            return content
        }
    }
}

data class TasksListStructure(val tasks: List<Task>)
