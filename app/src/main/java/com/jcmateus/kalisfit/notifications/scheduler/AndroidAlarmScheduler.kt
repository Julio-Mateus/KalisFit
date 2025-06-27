package com.jcmateus.kalisfit.notifications.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.jcmateus.kalisfit.data.AlarmRepository
import com.jcmateus.kalisfit.model.AlarmItem
import com.jcmateus.kalisfit.notifications.NotificationReceiver
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context,
    private val alarmRepository: AlarmRepository // Inyecta el repositorio
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        private const val TAG = "AndroidAlarmScheduler"
    }

    override fun schedule(item: AlarmItem) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, item.id)
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_TITLE, item.title)
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_MESSAGE, item.message)
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_CHANNEL_ID, item.channelId)
            // Aquí podrías añadir item.dataPayload si lo necesitas en el receiver
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id, // Usamos el ID del AlarmItem como requestCode para poder cancelarlo
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Comprobar si el permiso SCHEDULE_EXACT_ALARM está concedido (API 31+)
        // En una app real, manejarías el caso de que no esté concedido.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "No se puede programar alarma exacta, permiso SCHEDULE_EXACT_ALARM no concedido.")
                // Aquí deberías guiar al usuario a la configuración para conceder el permiso
                // o usar alarmas inexactas como fallback.
                // Por ahora, solo logueamos y no programamos si no se puede.
                // Opcionalmente, podrías guardar el item para programarlo más tarde si el permiso se concede.
                alarmRepository.saveAlarm(item) // Guardamos incluso si no se puede programar ahora, para reprogramar al arranque.
                return
            }
        }

        if (item.isRepeating && item.intervalMillis != null && item.intervalMillis > 0) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                item.timeMillis,
                item.intervalMillis,
                pendingIntent
            )
            Log.d(TAG, "Alarma repetitiva programada: ID ${item.id} a las ${formatTime(item.timeMillis)} cada ${item.intervalMillis / 1000}s")
        } else {
            // Para alarmas no repetitivas o si el intervalo no es válido
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                item.timeMillis,
                pendingIntent
            )
            Log.d(TAG, "Alarma exacta programada: ID ${item.id} a las ${formatTime(item.timeMillis)}")
        }

        // Guardar la alarma para poder reprogramarla después de un reinicio
        alarmRepository.saveAlarm(item)
    }

    override fun cancel(item: AlarmItem) { // O fun cancel(alarmId: Int)
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id, // Usar el mismo requestCode que al programar
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel() // Es buena práctica cancelar también el PendingIntent
        alarmRepository.deleteAlarm(item.id)
        Log.d(TAG, "Alarma cancelada: ID ${item.id}")
    }


    override fun rescheduleAllPersistentAlarms() {
        val persistentAlarms = alarmRepository.getAllAlarms()
        if (persistentAlarms.isEmpty()) {
            Log.d(TAG, "No hay alarmas persistentes para reprogramar.")
            return
        }
        Log.d(TAG, "Reprogramando ${persistentAlarms.size} alarmas persistentes...")
        persistentAlarms.forEach { alarmItem ->
            // Si la alarma ya pasó y no es repetitiva, no la reprogrames
            // (a menos que tengas una lógica específica para "ponerse al día")
            if (!alarmItem.isRepeating && alarmItem.timeMillis < System.currentTimeMillis()) {
                Log.d(TAG, "Alarma ID ${alarmItem.id} ya pasó y no es repetitiva. Omitiendo reprogramación.")
                alarmRepository.deleteAlarm(alarmItem.id) // Limpiar la alarma pasada
                return@forEach // Continúa con la siguiente alarma
            }

            // Si es repetitiva y la hora original ya pasó, necesitamos recalcular la próxima ocurrencia
            // o simplemente reprogramarla con su hora original (AlarmManager maneja esto para setRepeating)
            // Para setExactAndAllowWhileIdle con repetición manual, necesitarías calcular la siguiente timeMillis.

            Log.d(TAG, "Reprogramando alarma ID ${alarmItem.id} para ${formatTime(alarmItem.timeMillis)}")
            // Llamamos a schedule, que ya tiene la lógica de setExact/setRepeating
            // y también guarda la alarma de nuevo (aunque en este caso ya estaba guardada).
            // Podríamos tener un método interno "private fun doSchedule(item: AlarmItem)"
            // para evitar la doble escritura en el repo, pero por simplicidad lo dejamos así.
            schedule(alarmItem) // Esto reprogramará y volverá a guardar (podría optimizarse)
        }
        Log.d(TAG, "Reprogramación completada.")
    }

    private fun formatTime(timeMillis: Long): String {
        val calendar = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
        }
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", calendar).toString()
    }
}