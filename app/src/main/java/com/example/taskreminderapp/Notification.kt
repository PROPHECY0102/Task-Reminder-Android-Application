package com.example.taskreminderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

// This class inherits from BroadcastReceiver, Once the AlarmManager reach the specify date/time it will send
// an intent to the BroadcastReceiver which builds and then send a notification to the user device
class Notification: BroadcastReceiver() {

    // Get contents pass over from MainActivity.scheduleNotification()
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val title = intent.getStringExtra("title") ?: "Scheduled Notification"
            val message = intent.getStringExtra("message") ?: "This is your scheduled notification."
            val notificationID = intent.getIntExtra("notificationID", 1)
            showNotification(context, title, message, notificationID)
        }
    }

    // Method used to build the notification here notification specifications can be configured
    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "default")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.notification_active_icon)
            .setContentIntent(createPendingIntent(context))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // When the user clicks the notification it will launch the task reminder application
    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}