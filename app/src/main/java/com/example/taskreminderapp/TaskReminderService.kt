package com.example.taskreminderapp

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taskreminderapp.MainActivity.Companion.hoursInMillisOffset
import com.example.taskreminderapp.MainActivity.Companion.scheduleNotification
import com.example.taskreminderapp.MainActivity.Companion.taskReminderNotificationTitle

class TaskReminderService : Service() {

    companion object {
        const val CHANNEL_ID = "TaskReminderServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        scheduleTasksNotifications()

        return START_STICKY
    }

    private fun scheduleTasksNotifications() {
        MainActivity.tasksList.forEach {
            task: Task ->
            if (task.due > System.currentTimeMillis()) {
                scheduleNotification(this, task.due, task.content, "${taskReminderNotificationTitle} ${task.date} ${task.time}", task.id)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminder Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Task Reminder is Active")
            .setContentText("This is required for Corinth to send notifications even after the application is closed.")
            .setSmallIcon(R.drawable.task_reminder_ic_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
}