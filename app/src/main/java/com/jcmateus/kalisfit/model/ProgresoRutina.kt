package com.jcmateus.kalisfit.model



data class ProgresoRutina(
    val fecha: String = "",
    val nivel: String = "",
    val objetivos: List<String> = emptyList(),
    val ejercicios: List<EjercicioSimple> = emptyList(),
    val tiempoTotal: Int = 0
)

data class EjercicioSimple(
    val nombre: String = "",
    val duracion: Int = 0
)

