package com.jcmateus.kalisfit.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
        // Mantén tus nombres de extras si ya los usas así en AndroidAlarmScheduler
        const val EXTRA_NOTIFICATION_ID = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_ID"
        const val EXTRA_NOTIFICATION_TITLE = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_TITLE"
        const val EXTRA_NOTIFICATION_MESSAGE = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_MESSAGE"
        const val EXTRA_NOTIFICATION_CHANNEL_ID = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_CHANNEL_ID"
        // --- NUEVAS CONSTANTES (o renombra las que te di antes si prefieres estos nombres) ---
        const val EXTRA_SMALL_ICON_RES_ID = "com.jcmateus.kalisfit.EXTRA_SMALL_ICON_RES_ID"
        const val EXTRA_LARGE_ICON_RES_ID = "com.jcmateus.kalisfit.EXTRA_LARGE_ICON_RES_ID"
        const val EXTRA_NOTIFICATION_PAYLOAD = "com.jcmateus.kalisfit.EXTRA_NOTIFICATION_PAYLOAD" // Si lo usas

        // --- ICONO PEQUEÑO DE FALLBACK (ASEGÚRATE QUE ESTE DRAWABLE EXISTA Y SEA CORRECTO) ---
        // Este debe ser un icono BLANCO y TRANSPARENTE para la barra de estado.
        val DEFAULT_SMALL_ICON_FALLBACK = R.drawable.ic_stat_kalisfit_notification
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Alarma recibida. Intent Action: ${intent.action}")

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: context.getString(R.string.default_notification_title)
        val message = intent.getStringExtra(EXTRA_NOTIFICATION_MESSAGE) ?: context.getString(R.string.default_notification_message)
        val channelId = intent.getStringExtra(EXTRA_NOTIFICATION_CHANNEL_ID) ?: KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID
        val payload = intent.getStringExtra(EXTRA_NOTIFICATION_PAYLOAD) // Opcional, para el PendingIntent

        // --- OBTENER LOS IDs DE LOS ICONOS DEL INTENT ---
        val receivedSmallIconResId = intent.getIntExtra(EXTRA_SMALL_ICON_RES_ID, 0) // 0 si no se encuentra
        val receivedLargeIconResId = intent.getIntExtra(EXTRA_LARGE_ICON_RES_ID, 0) // 0 si no se encuentra

        Log.d("NotificationReceiver", "Recibido smallIconResId: $receivedSmallIconResId, largeIconResId: $receivedLargeIconResId")


        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            payload?.let { putExtra("ROUTINE_ID_PAYLOAD", it) } // Ejemplo de cómo usar el payload
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainActivityIntent,
            pendingIntentFlag
        )

        // --- USAR EL ICONO PEQUEÑO RECIBIDO O EL DE FALLBACK ---
        val finalSmallIconResId = if (receivedSmallIconResId != 0) {
            receivedSmallIconResId
        } else {
            Log.w("NotificationReceiver", "No se recibió smallIconResId del Intent, usando fallback.")
            DEFAULT_SMALL_ICON_FALLBACK
        }
        // Asegúrate que `DEFAULT_SMALL_ICON_FALLBACK` (ej. R.drawable.ic_stat_kalisfit_default_fallback)
        // sea un icono blanco y transparente adecuado para la barra de estado.

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(finalSmallIconResId) // <<< --- CAMBIO CLAVE AQUÍ ---
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // --- AÑADIR ICONO GRANDE SI SE PROPORCIONÓ ---
        if (receivedLargeIconResId != 0) {
            try {
                val largeIconBitmap = BitmapFactory.decodeResource(context.resources, receivedLargeIconResId)
                builder.setLargeIcon(largeIconBitmap)
                Log.d("NotificationReceiver", "Icono grande establecido desde ResId: $receivedLargeIconResId")
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error al decodificar el icono grande desde ResId: $receivedLargeIconResId", e)
            }
        } else {
            Log.d("NotificationReceiver", "No se proporcionó ResId para icono grande.")
            // Opcional: Podrías poner un .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)) aquí
            // como un fallback general si lo deseas, pero asegúrate que R.mipmap.ic_launcher es adecuado.
        }

        // .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Opcional

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
            Log.d("NotificationReceiver", "Mostrando notificación ID: $notificationId, Título: '$title', Canal: '$channelId', SmallIcon: $finalSmallIconResId")
            try {
                notify(notificationId, builder.build())
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error al mostrar notificación: ${e.localizedMessage}")
                // Si el error es por el ícono (aunque ahora tenemos fallback), podrías intentar
                // mostrar una notificación ultra-básica sin ícono o con uno garantizado.
            }
        }
    }
}