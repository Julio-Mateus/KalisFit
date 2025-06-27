package com.jcmateus.kalisfit.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jcmateus.kalisfit.KalisFitApplication
import com.jcmateus.kalisfit.MainActivity
import com.jcmateus.kalisfit.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTIFICATION_ID = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_ID"
        const val EXTRA_NOTIFICATION_TITLE = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_TITLE"
        const val EXTRA_NOTIFICATION_MESSAGE = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_MESSAGE"
        const val EXTRA_NOTIFICATION_CHANNEL_ID = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_CHANNEL_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Alarma recibida en .notifications. Path correcto. Intent: ${intent.action}")

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt()) // Usar timestamp para ID único por defecto
        val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: context.getString(R.string.default_notification_title) // Usa recursos de strings
        val message = intent.getStringExtra(EXTRA_NOTIFICATION_MESSAGE) ?: context.getString(R.string.default_notification_message) // Usa recursos de strings
        val channelId = intent.getStringExtra(EXTRA_NOTIFICATION_CHANNEL_ID)
            ?: KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID


        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Puedes añadir extras aquí si quieres pasar datos específicos a MainActivity
            // por ejemplo, para navegar a una pantalla específica:
            // putExtra("NAVIGATE_TO", "specific_screen_route")
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Usar notificationId como requestCode
            mainActivityIntent,
            pendingIntentFlag
        )

        // Asegúrate de tener este ícono en tus drawables
        val smallIconResId = R.drawable.ic_kalis_notification_mono
        // He cambiado el nombre del ícono, ajústalo al tuyo.
        // Debe ser un ícono monocromático.

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconResId)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)) // Opcional: ícono grande
        // .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Opcional: para texto largo

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("NotificationReceiver", "Permiso POST_NOTIFICATIONS no concedido. No se puede mostrar la notificación.")
                    return
                }
            }
            Log.d("NotificationReceiver", "Mostrando notificación ID: $notificationId, Título: '$title', Canal: '$channelId'")
            try {
                notify(notificationId, builder.build())
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error al mostrar notificación: ${e.localizedMessage}")
                // Podrías intentar con un ícono por defecto si el error es por el ícono
            }
        }
    }
}