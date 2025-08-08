package com.jcmateus.kalisfit.model

import com.google.firebase.Timestamp

data class UserCustomRoutine(
    var id: String = "", // ID único de esta rutina personalizada
    val userId: String = "", // ID del usuario
    var nombrePersonalizado: String = "",
    var descripcion: String = "",
    var imagenUrl: String? = null,

    // El usuario personaliza una lista de Ejercicio, igual que en tu modelo Rutina original.
    // Todos los campos dentro de cada Ejercicio pueden ser modificados por el usuario para su rutina.
    var ejercicios: List<Ejercicio> = emptyList(),

    var numeroDeRondas: Int = 1, // El usuario puede ajustar las rondas para su versión
    var descansoEntreRondasSegundos: Int = 0, // Y el descanso entre ellas

    // Campos que el usuario podría querer ajustar para su versión
    var nivelRecomendado: List<String> = emptyList(),
    var objetivos: List<String> = emptyList(),
    // Usamos el enum LugarEntrenamiento para consistencia
    var lugarEntrenamiento: List<LugarEntrenamiento> = emptyList(),

    // Metadatos de la rutina personalizada
    val fechaCreacion: Timestamp = Timestamp.now(),
    var fechaUltimaModificacion: Timestamp = Timestamp.now(),
    val originalTemplateId: String? = null, // ID de la Rutina plantilla original, si aplica
    // Slug podría no ser necesario para rutinas personalizadas a menos que quieras URLs únicas para ellas
    // var slugPersonalizado: String? = null
)
