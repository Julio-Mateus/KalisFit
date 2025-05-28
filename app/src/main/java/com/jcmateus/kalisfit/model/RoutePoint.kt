package com.jcmateus.kalisfit.model

data class RoutePoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L
) {
    // Constructor sin argumentos requerido por Firestore para deserialización
    constructor() : this(0.0, 0.0, 0L)
}
