package com.example.taskreminderapp

class Task(
    var id: Int,
    var content: String,
    var date: String,
    var time: String,
    var createdAt: String,
    var selected: Boolean = false
) {
    fun shortenContentText(n: Int): String {
        if (content.length > n) {
            return "${content.substring(0, n)}..."
        } else {
            return content
        }
    }
}
