package com.jcmateus.kalisfit.model

import java.util.UUID

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
fun Ejercicio.esTipoComplejo(): Boolean {
    return when (this.tipoEjercicio) {
        TipoDeEjercicio.SUPERSET_SEQUENCIAL,
        TipoDeEjercicio.POR_LADO_ALTERNADO,
        TipoDeEjercicio.COMBINADO_TEMPORIZADO,
        TipoDeEjercicio.CIRCUITO_TEMPORIZADO -> true
        // TipoDeEjercicio.SIMPLE y TipoDeEjercicio.CON_TEMPO se consideran no complejos
        // en este contexto (no tienen una lista explícita de "sub-componentes" de la misma manera)
        else -> false
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
enum class TipoDeEjercicio(val displayName: String) {
    SIMPLE("Simple"),
    SUPERSET_SEQUENCIAL("Superset"),
    POR_LADO_ALTERNADO("Por Lado"),
    COMBINADO_TEMPORIZADO("Combinado"),
    CIRCUITO_TEMPORIZADO("Circuito"),
    CON_TEMPO("Con Tempo"); // Ejercicio simple pero con una cadencia específica.
    fun esComplejo(): Boolean {
        return when (this) {
            SUPERSET_SEQUENCIAL, COMBINADO_TEMPORIZADO, CIRCUITO_TEMPORIZADO -> true
            else -> false
        }
    }
}
// NUEVO: Define un componente de un ejercicio complejo (parte de un superset, circuito, etc.)
data class ComponenteEjercicio(
    val id: String = UUID.randomUUID().toString(),
    val nombreEspecifico: String? = null, // Ej: "Curl Martillo", "Curl con Banda", "Wall Sit"
    val repeticiones: String? = null,     // Ej: "12", "25". String para mantener "AMRAP" o similares si fuera necesario.
    val duracionSegundos: Int? = null,    // Ej: 30 (para "30s")
    val imagenUrl: String? = null,        // Si este componente tiene una imagen específica.
    val orden: Int = 0,                    // Orden del componente dentro del ejercicio padre.
    val notaTempo: String? = null,      // <--- AÑADIR SI FALTA
    val esUnilateral: Boolean = false, // <--- AÑADIR SI FALTA
    val descansoPostComponenteSegundos: Int = 0
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