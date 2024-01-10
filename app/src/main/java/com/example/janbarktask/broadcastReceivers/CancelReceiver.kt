package com.example.janbarktask.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.janbarktask.utils.MyForegroundService

class CancelReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Implement your cancel logic here
        // For example, stop the foreground service
        val serviceIntent = Intent(context, MyForegroundService::class.java)
        context.stopService(serviceIntent)
    }
}