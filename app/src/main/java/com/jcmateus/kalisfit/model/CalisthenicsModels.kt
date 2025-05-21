package com.jcmateus.kalisfit.model

data class ExerciseLevel(
    val id: String,
    val name: String,
    val description: String,
    val targetReps: String? = null, // Ej: "3-5", "8-12"
    val targetSets: String? = null, // Ej: "3"
    val targetHoldTime: String? = null, // Ej: "30s", "1 min"
    val videoUrl: String? = null, // O un ID de recurso para una imagen
    val notes: String? = null,
    val imageUrl: String? = null
)

data class Progression(
    val id: String,
    val name: String,
    val iconUrl: String? = null, // Opcional: para un icono representativo de la progresión
    val description: String? = null, // Breve descripción de la progresión
    val levels: List<ExerciseLevel>
)
