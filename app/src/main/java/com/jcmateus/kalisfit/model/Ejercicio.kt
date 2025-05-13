package com.jcmateus.kalisfit.model

// Define los lugares de entrenamiento como un enum class para mayor claridad
enum class LugarEntrenamiento {
    CASA, GIMNASIO, EXTERIOR, CALISTENIA
}

// Define los grupos musculares, incluyendo los del JSON
enum class GrupoMuscular {
    PECHO, ESPALDA, PIERNAS, BRAZOS, ABDOMEN, HOMBROS, FULL_BODY,
    GLUTEOS, TRICEPS, FEMORALES, ESPALDA_BAJA, CUADRICEPS // Añadidos
}

// Estructura de datos para un ejercicio (mapea documentos de la subcolección)
data class Ejercicio(
    // Mapeará el campo 'id' que escribimos nosotros en el documento
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val videoUrl: String? = null,
    val duracionSegundos: Int = 0,
    val repeticiones: Int = 0,
    val series: Int = 0,
    // Mapeará la lista de strings a tu enum (asegúrate de que los nombres coincidan)
    val grupoMuscular: List<GrupoMuscular> = emptyList(),
    val equipamientoNecesario: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(),
    val orden: Int = 0
)

// Estructura de datos para una rutina (mapea SOLO el documento principal)
data class Rutina(
    // Mapeará el campo 'id' que escribimos nosotros en el documento
    // Si quieres mapear el ID autogenerado de Firestore, usa @DocumentId
    val id: String = "",
    // ¡Añadir el campo slug!
    val slug: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val nivelRecomendado: List<String> = emptyList(),
    val objetivos: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(),
    // ¡Añadir el campo imagenUrl si existe en el documento principal!
    val imagenUrl: String? = null
    // NO incluyas la lista de ejercicios aquí, se carga por separado de la subcolección.
)