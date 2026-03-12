package com.jcmateus.kalisfit.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jcmateus.kalisfit.MainActivity
import com.jcmateus.kalisfit.R

class RunningForegroundService : Service() {

    companion object {
        const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
        const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "ACTION_UPDATE_NOTIFICATION"

        const val NOTIFICATION_CHANNEL_ID = "running_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Running Tracking"
        const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    startForegroundService()
                }
                ACTION_PAUSE_SERVICE -> {
                    // Lógica para pausar si es necesario desde la notificación
                }
                ACTION_STOP_SERVICE -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                ACTION_UPDATE_NOTIFICATION -> {
                    val time = it.getStringExtra("elapsedTime") ?: "00:00"
                    val distance = it.getStringExtra("distanceKm") ?: "0.00 km"
                    updateNotification(time, distance)
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        startForeground(NOTIFICATION_ID, createNotification("00:00", "0.00 km"))
    }

    private fun updateNotification(time: String, distance: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(time, distance))
    }

    private fun createNotification(time: String, distance: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este icono existe o cámbialo
            .setContentTitle("Entrenamiento en curso")
            .setContentText("Tiempo: $time | Distancia: $distance")
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
