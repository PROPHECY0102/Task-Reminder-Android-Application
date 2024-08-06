package com.example.taskreminderapp

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taskreminderapp.MainActivity.Companion.scheduleNotification
import com.example.taskreminderapp.MainActivity.Companion.taskReminderNotificationTitle

// This class inherits from Service as startForeground() requires a service object
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

    // Once the foreground service has started all pending task's notifications will be reschedule
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        scheduleTasksNotifications()

        return START_STICKY
    }

    // Method to schedule notifications from tasks list, tasks that are past due are ignored
    private fun scheduleTasksNotifications() {
        MainActivity.tasksList.forEach {
            task: Task ->
            if (task.due > System.currentTimeMillis()) {
                Log.d("Time", "${task.due} | ${System.currentTimeMillis()}")
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

    // Notification indicating that the foreground service has been started and is currently active
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

    // Debugging method to test notifications, not used in production
    private fun scheduleTestNotification() {
        val testTime = System.currentTimeMillis() + 3 * 60 * 1000 // 3 minutes from now
        scheduleNotification(
            this,
            testTime,
            "Test Content",
            "Test Notification (3 minutes)",
            Int.MAX_VALUE // Use a unique ID for the test notification
        )
    }
}