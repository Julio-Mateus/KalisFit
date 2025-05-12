package com.jcmateus.kalisfit.model

// Define los lugares de entrenamiento como un enum class para mayor claridad
enum class LugarEntrenamiento {
    CASA, GIMNASIO, EXTERIOR, CALISTENIA
}

// Define los grupos musculares como un enum class (o una lista de Strings si prefieres más flexibilidad)
enum class GrupoMuscular {
    PECHO, ESPALDA, PIERNAS, BRAZOS, ABDOMEN, HOMBROS, FULL_BODY
}

// Estructura de datos para un ejercicio
data class Ejercicio(
    val id: String = "", // Un ID único para el ejercicio
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null, // Usaremos URL para imágenes más flexibles (Firebase Storage, por ejemplo)
    val videoUrl: String? = null, // URL a un video tutorial
    val duracionSegundos: Int = 0, // Para ejercicios basados en tiempo
    val repeticiones: Int = 0, // Para ejercicios basados en repeticiones (puedes usar uno u otro)
    val series: Int = 0,
    val grupoMuscular: List<GrupoMuscular> = emptyList(), // Un ejercicio puede trabajar múltiples grupos
    val equipamientoNecesario: List<String> = emptyList(), // Ej: "mancuernas", "barra fija", "bandas de resistencia"
    val lugarEntrenamiento: List<String> = emptyList(), // Dónde se puede realizar el ejercicio
    val orden: Int = 0
)

// Estructura de datos para una rutina
data class Rutina(
    val id: String = "", // Un ID único para la rutina
    val nombre: String = "",
    val descripcion: String = "",
    val nivelRecomendado: List<String> = emptyList(), // "Principiante", "Intermedio", "Avanzado"
    val objetivos: List<String> = emptyList(), // "Fuerza", "Resistencia", etc.
    val ejercicios: List<Ejercicio> = emptyList(), // Lista de ejercicios en la rutina
    val lugarEntrenamiento: List<String> = emptyList() // Lugares donde se puede hacer esta rutina
)