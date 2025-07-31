package com.jcmateus.kalisfit.model

// Define los lugares de entrenamiento como un enum class para mayor claridad
enum class LugarEntrenamiento {
    CASA, GIMNASIO, EXTERIOR, CALISTENIA
}

// Define los grupos musculares, incluyendo los del JSON
enum class GrupoMuscular {
    PECHO, ESPALDA, PIERNAS, BRAZOS, ABDOMEN, HOMBROS, FULL_BODY,
    GLUTEOS, TRICEPS, FEMORALES, ESPALDA_BAJA, CUADRICEPS, BICEPS, CORE,
    ANTEBRAZOS, // Añadido de tu JSON
    CARDIO // Añadido de tu JSON
}

// NUEVO: Define los tipos de ejecución de los ejercicios
enum class TipoDeEjercicio {
    SIMPLE,                 // Ejercicio estándar: una métrica de tiempo o repeticiones.
    SUPERSET_SEQUENCIAL,    // Múltiples movimientos uno tras otro sin descanso entre ellos. (Ej: "12 + 15")
    POR_LADO_ALTERNADO,     // Se realiza para un lado y luego para el otro. (Ej: "10 por pierna")
    COMBINADO_TEMPORIZADO,  // Múltiples acciones dentro de un mismo bloque de tiempo. (Ej: "30s wall sit + 25 talones")
    CIRCUITO_TEMPORIZADO,   // Componentes por tiempo, con duración total definida. (Ej: "2 x 30s", total 60s)
    CON_TEMPO               // Ejercicio simple pero con una cadencia específica.
}

// NUEVO: Define un componente de un ejercicio complejo (parte de un superset, circuito, etc.)
data class ComponenteEjercicio(
    val nombreEspecifico: String? = null, // Ej: "Curl Martillo", "Curl con Banda", "Wall Sit"
    val repeticiones: String? = null,     // Ej: "12", "25". String para mantener "AMRAP" o similares si fuera necesario.
    val duracionSegundos: Int? = null,    // Ej: 30 (para "30s")
    val imagenUrl: String? = null,        // Si este componente tiene una imagen específica.
    val orden: Int = 0                    // Orden del componente dentro del ejercicio padre.
)

// Estructura de datos para un ejercicio (ACTUALIZADA)
data class Ejercicio(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val imagenUrl1: String? = null, // Para soportar múltiples imágenes como en tu JSON
    val imagenUrl2: String? = null, // Para soportar múltiples imágenes como en tu JSON
    val videoUrl: String? = null,

    // Campos originales de duración y repeticiones del JSON.
    // Serán la base para el parseo o para ejercicios simples.
    val duracionSegundosOriginal: Int = 0,    // Renombrado para claridad (antes duracionSegundos)
    val repeticionesOriginal: String = "0", // Renombrado y cambiado a String para parsear "6-8", "12 + 15", "10 por pierna" (antes repeticiones: Int)

    val numeroDeSeries: Int = 1,
    val descansoEntreSeriesSegundos: Int = 0,
    val descansoDespuesEjercicioSegundos: Int = 0,

    val grupoMuscular: List<GrupoMuscular> = emptyList(),
    val equipamientoNecesario: List<String> = emptyList(),
    val lugarEntrenamiento: List<LugarEntrenamiento> = emptyList(),
    val orden: Double = 0.0, // Cambiado a Double para permitir sub-órdenes decimales si es necesario para el parseador o lógica futura

    // NUEVOS CAMPOS PROCESADOS/DERIVADOS
    val tipoEjercicio: TipoDeEjercicio = TipoDeEjercicio.SIMPLE,
    val componentes: List<ComponenteEjercicio> = emptyList(),
    val notaTempo: String? = null,      // Ej: "3-1-2"
    val esUnilateral: Boolean = false,  // True si es "por pierna", "por brazo", etc.

    // Podrías añadir campos para repeticiones/duración calculadas para el ejercicio simple o como guía:
    // val repeticionesMin: Int? = null,
    // val repeticionesMax: Int? = null, // Para rangos como "6-8"
    // val duracionCalculadaSegundos: Int? = null // Podría ser igual a duracionSegundosOriginal o la suma de componentes
)

// Estructura de datos para una rutina (sin cambios directos aquí, pero los Ejercicio internos se actualizarán)
data class Rutina(
    val id: String = "",
    val slug: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,

    val numeroDeRondas: Int = 1,
    val descansoEntreRondasSegundos: Int = 0,

    val nivelRecomendado: List<String> = emptyList(),
    val objetivos: List<String> = emptyList(),
    val lugarEntrenamiento: List<LugarEntrenamiento> = emptyList(),
    val ejercicios: List<Ejercicio> = emptyList() // Esta lista contendrá los objetos Ejercicio actualizados
)