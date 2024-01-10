package com.example.janbarktask.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.janbarktask.ui.MainActivity
import com.example.janbarktask.R
import com.example.janbarktask.broadcastReceivers.CancelReceiver

class MyForegroundService : Service() {

    companion object {
        private const val YOUR_NOTIFICATION_ID = 1
        private const val YOUR_CHANNEL_ID = "myChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel() // Create the notification channel
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(YOUR_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val remoteViews = RemoteViews(packageName, R.layout.custom_notification_layout)
        val expandedRemoteViews = RemoteViews(packageName, R.layout.expanded_custom_layout)

        // Intent and PendingIntent for the screenshot action
        val screenshotIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "take_screenshot")
        }
        val screenshotPendingIntent = PendingIntent.getActivity(
            this, 0, screenshotIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent and PendingIntent for the cancel action
        val cancelIntent = Intent(this, CancelReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Assign the pending intents to the click events of the views in the custom layout
        remoteViews.setOnClickPendingIntent(R.id.screenshotImage, screenshotPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.cancelImage, cancelPendingIntent)

        // Build the notification
        return NotificationCompat.Builder(this, YOUR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_camera)
            .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            var description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(YOUR_CHANNEL_ID, name, importance).apply {
                description = description
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}