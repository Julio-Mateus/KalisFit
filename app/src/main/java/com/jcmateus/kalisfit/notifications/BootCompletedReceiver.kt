package com.jcmateus.kalisfit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jcmateus.kalisfit.data.SharedPreferencesAlarmRepository
import com.jcmateus.kalisfit.notifications.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.i(TAG, "El dispositivo ha terminado de arrancar. Iniciando reprogramación de alarmas...")

            // Es importante que el trabajo en onReceive sea rápido.
            // Si la reprogramación es compleja o requiere I/O pesada,
            // deberías delegarla a un WorkManager Worker o un servicio en primer plano.
            // Para este ejemplo, si la carga del repo y la reprogramación son rápidas,
            // podemos hacerlo directamente, pero en una corrutina para no bloquear el hilo principal.

            // NO inyectes dependencias directamente en un BroadcastReceiver si usas Hilt,
            // ya que son instanciados por el sistema.
            // Deberías obtener las instancias de otra manera o usar WorkManager con Hilt.
            // Para este ejemplo sin Hilt en el Receiver, instanciamos directamente.
            val alarmRepository = SharedPreferencesAlarmRepository(context.applicationContext)
            val alarmScheduler = AndroidAlarmScheduler(context.applicationContext, alarmRepository)

            // Usamos una corrutina para la I/O del repositorio, aunque onReceive ya está en un hilo de fondo.
            // goAsync() es otra opción para extender la vida del onReceive si es necesario.
            val pendingResult = goAsync() // Opcional, pero bueno para tareas asíncronas

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    alarmScheduler.rescheduleAllPersistentAlarms()
                    Log.i(TAG, "Reprogramación de alarmas intentada desde corrutina.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error durante la reprogramación de alarmas: ${e.message}", e)
                } finally {
                    pendingResult.finish() // Asegúrate de llamar a finish
                }
            }
        } else {
            Log.w(TAG, "Recibido un intent inesperado: ${intent.action}")
        }
    }
}