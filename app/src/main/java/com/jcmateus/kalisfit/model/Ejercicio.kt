package com.jcmateus.kalisfit.model

// Define los lugares de entrenamiento como un enum class para mayor claridad
enum class LugarEntrenamiento(val displayName: String) {
    CASA("Casa"),
    GIMNASIO("Gimnasio"),
    EXTERIOR("Exterior"),
    CALISTENIA("Calistenia"); // Añade un valor 'OTRO' por si acaso

    companion object {
        fun fromString(value: String): LugarEntrenamiento? {
            return values().find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            } ?: values().find { it.name.equals(value.replace(" ", "_"), ignoreCase = true) } // Intenta con underscores
        }
    }
}

// Define los grupos musculares, incluyendo los del JSON
enum class GrupoMuscular(val displayName: String) {
    PECHO("Pecho"),
    ESPALDA("Espalda"),
    PIERNAS("Piernas"),
    BRAZOS("Brazos"), // Podrías ser más específico si quieres (Bíceps, Tríceps)
    ABDOMEN("Abdomen"),
    HOMBROS("Hombros"),
    FULL_BODY("Cuerpo Completo"),
    GLUTEOS("Glúteos"),
    TRICEPS("Tríceps"),
    FEMORALES("Femorales"), // Podrías usar ISQUIOTIBIALES para consistencia con tu JSON de EjercicioTodos
    ESPALDA_BAJA("Espalda Baja"),
    CUADRICEPS("Cuádriceps"),
    BICEPS("Bíceps"),
    CORE("Core"),
    ANTEBRAZOS("Antebrazos"),
    CARDIO("Cardio"),
    // Nuevos valores de tu JSON de EjercicioTodos (asegúrate de que estén aquí o en displayName)
    ISQUIOTIBIALES("Isquiotibiales"), // Si quieres usar este término
    ESPALDA_ALTA("Espalda Alta"),    // Si lo necesitas
    ABDUCTORES("Abductores"),        // Si lo necesitas
    OBLICUOS("Oblicuos"),            // Si lo necesitas
    CUERPO_COMPLETO("Cuerpo Completo"), // Ya estaba como FULL_BODY, unifica
    OTRO("Otro"); // Para futuros valores no mapeados


    companion object {
        fun fromString(value: String): GrupoMuscular? {
            val upperValue = value.uppercase().replace(" ", "_")
            return values().find {
                it.name.equals(upperValue, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true) // Compara con displayName original
            }
        }
    }
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
    var id: String = "",
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