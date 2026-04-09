package com.JoseRosas.registrador.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.JoseRosas.registrador.MainActivity
import com.JoseRosas.registrador.R

class BridgeForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "bridge_service_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Bridge activo"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = androidx.core.app.PendingIntentCompat.getActivity(
            this,
            0,
            openIntent,
            0,
            false
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Registrador activo")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bridge Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Servicio activo del bridge"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}