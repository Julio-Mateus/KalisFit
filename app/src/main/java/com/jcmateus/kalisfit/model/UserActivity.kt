package com.jcmateus.kalisfit.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Define la estructura de un Split individual para Firestore
data class SplitData(
    val km: Int = 0,
    val timeSeconds: Long = 0L,
    val pace: String = ""
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(0, 0L, "")
}

data class UserActivity(
    var id: String? = null, // ID del documento, Firestore lo puede autogenerar o tú lo asignas
    val userId: String? = null, // ID del usuario de Firebase Auth
    @ServerTimestamp // Firestore llenará esto automáticamente con la hora del servidor
    var timestamp: Date? = null,
    val elapsedTimeSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val avgPace: String = "",
    val caloriesBurned: Int = 0,
    val routePoints: List<RoutePoint> = emptyList(), // Lista de nuestros RoutePoint
    var mapImageUrl: String? = null, // Opcional: para la imagen estática del mapa
    val splits: List<SplitData> = emptyList() // <--- NUEVO CAMPO PARA LOS SPLITS
) {
    // Constructor sin argumentos requerido por Firestore para deserialización
    constructor() : this(
        id = null,
        userId = null,
        timestamp = null,
        elapsedTimeSeconds = 0L,
        distanceKm = 0.0,
        avgPace = "",
        caloriesBurned = 0,
        routePoints = emptyList(),
        mapImageUrl = null,
        splits = emptyList() // <--- Inicializar el nuevo campo
    )
}
