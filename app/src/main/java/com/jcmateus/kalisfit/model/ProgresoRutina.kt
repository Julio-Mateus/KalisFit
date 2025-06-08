package com.jcmateus.kalisfit.model

import com.google.firebase.Timestamp


data class ProgresoRutina(
    val fecha: Timestamp = Timestamp.now(), // O considera usar Long para milisegundos desde epoch, o Timestamp si Firestore lo maneja bien al convertir
    val rutinaIdOriginal: String = "", // ID de la rutina base que se realizó
    val nombreRutina: String = "",
    val nivelUsuarioAlCompletar: String = "", // Nivel del usuario en ese momento
    val objetivosUsuarioAlCompletar: List<String> = emptyList(), // Objetivos del usuario en ese momento
    val ejerciciosCompletados: List<EjercicioProgreso> = emptyList(), // Cambiado el nombre para mayor claridad
    val rondasRealizadas: Int = 0, // Cuántas rondas se completaron
    val tiempoTotalSesionSegundos: Int = 0 // Tiempo total real de la sesión de entrenamiento
)

data class EjercicioProgreso( // Renombrado de EjercicioSimple para mayor claridad en este contexto
    val ejercicioIdOriginal: String = "", // ID del ejercicio base
    val nombre: String = "",
    // Estos serían los *objetivos por serie* del ejercicio original
    val duracionPorSerieSegundos: Int = 0,
    val repeticionesPorSerie: Int = 0,
    val seriesRealizadas: Int = 0 // Cuántas series de este ejercicio se completaron
    // podrías añadir aquí los descansos configurados si es relevante para el historial
)