package com.jcmateus.kalisfit.model

import com.google.firebase.Timestamp

enum class TipoDiaEntrenamiento { ENTRENAMIENTO, DESCANSO }
data class ProgresoRutina(
    val fecha: Timestamp = Timestamp.now(), // O considera usar Long para milisegundos desde epoch, o Timestamp si Firestore lo maneja bien al convertir
    val rutinaIdOriginal: String = "", // ID de la rutina base que se realizó
    val customRoutineIdRealizado: String? = null,
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
data class DiaDeEntrenamientoPlanificado(
    val fecha: Timestamp = Timestamp.now(),
    val diaDeLaSemana: String = "",
    var rutinaIdAsignada: String? = null,
    var nombreRutinaAsignada: String? = null,
    var tipoRutina: String? = null, // Evalúa si aún lo necesitas
    var esRutinaPersonalizada: Boolean? = false,
    var completada: Boolean = false,
    var tipoDeDia: String = TipoDiaEntrenamiento.ENTRENAMIENTO.name,
    var progresoRutinaIdCompletada: String? = null
) {
    // Constructor sin argumentos para Firestore (si no usas valores por defecto para todos los campos)
    constructor() : this(Timestamp.now(), "", null, null, null, false, false, TipoDiaEntrenamiento.ENTRENAMIENTO.name, null)
}

data class PlanSemanalUsuario(
    val id: String = "",
    val userId: String = "",
    val fechaInicioSemana: Timestamp = Timestamp.now(),
    val fechaFinSemana: Timestamp = Timestamp.now(),
    // CAMBIO SUGERIDO AQUÍ:
    val diasPlanificados: MutableList<DiaDeEntrenamientoPlanificado> = mutableListOf(),
    val frecuenciaObjetivoOriginal: Int = 0,
    // Podrías añadir un campo para saber cuándo fue la última vez que este plan se actualizó en Firestore
    val ultimaActualizacion: Timestamp = Timestamp.now()
) {
    // Constructor sin argumentos para Firestore
    constructor() : this("", "", Timestamp.now(), Timestamp.now(), mutableListOf(), 0, Timestamp.now())
}