package com.jcmateus.kalisfit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmItem(
    val id: Int, // ID único para la notificación y para el PendingIntent. Debe ser estable.
    val timeMillis: Long, // Hora de disparo en milisegundos
    val title: String,
    val message: String,
    val channelId: String,
    val isRepeating: Boolean = false, // ¿Es una alarma repetitiva?
    val intervalMillis: Long? = null, // Intervalo de repetición en milisegundos si isRepeating es true
    val dataPayload: String? = null, // Opcional: para datos adicionales que necesites
    val largeIconResId: Int? = null, // <--- CAMPO IMPORTANTE
) : Parcelable {
    // Es importante que 'id' sea lo suficientemente único para diferenciar PendingIntents
    // si varias alarmas pueden tener el mismo 'id' de notificación pero diferentes 'requestCode'
    // para el PendingIntent, considera añadir un campo 'requestCode' explícito o derivarlo del id.
    // Para este ejemplo, asumiremos que 'id' es usado como requestCode para el PendingIntent.
}
