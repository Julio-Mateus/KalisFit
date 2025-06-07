package com.jcmateus.kalisfit.model

// Define los lugares de entrenamiento como un enum class para mayor claridad
enum class LugarEntrenamiento {
    CASA, GIMNASIO, EXTERIOR, CALISTENIA
}

// Define los grupos musculares, incluyendo los del JSON
enum class GrupoMuscular {
    PECHO, ESPALDA, PIERNAS, BRAZOS, ABDOMEN, HOMBROS, FULL_BODY,
    GLUTEOS, TRICEPS, FEMORALES, ESPALDA_BAJA, CUADRICEPS, BICEPS, CORE // Manteniendo los añadidos
}

// Estructura de datos para un ejercicio
data class Ejercicio(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val videoUrl: String? = null,
    val duracionSegundos: Int = 0,       // Duración si el ejercicio es por tiempo (por serie)
    val repeticiones: Int = 0,          // Repeticiones si el ejercicio es por cantidad (por serie)

    // NUEVOS CAMPOS PARA SERIES POR EJERCICIO
    val numeroDeSeries: Int = 1,        // Cuántas series de ESTE ejercicio se realizarán
    // Si es 1, se comporta como antes dentro de una ronda de rutina.
    val descansoEntreSeriesSegundos: Int = 0, // Descanso después de cada serie de ESTE ejercicio
    // (excepto la última, antes de pasar al siguiente ejercicio o ronda)

    val grupoMuscular: List<GrupoMuscular> = emptyList(),
    val equipamientoNecesario: List<String> = emptyList(),
    // lugarEntrenamiento en Ejercicio podría ser útil si un ejercicio específico de una rutina
    // SOLO se puede hacer en un lugar, aunque la rutina sea más general.
    // Si la rutina define el lugar, quizás no lo necesitas aquí o lo usas como override.
    val lugarEntrenamiento: List<LugarEntrenamiento> = emptyList(), // Cambiado a List<LugarEntrenamiento> para consistencia y flexibilidad
    val orden: Int = 0                  // Orden del ejercicio dentro de la secuencia de la rutina/ronda
)

// Estructura de datos para una rutina
data class Rutina(
    val id: String = "",
    val slug: String = "", // Campo slug añadido
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null, // Campo imagenUrl añadido para la rutina en general

    // NUEVOS CAMPOS PARA RONDAS DE LA RUTINA COMPLETA (CIRCUITOS)
    val numeroDeRondas: Int = 1,        // Cuántas veces se repetirá la secuencia completa de ejercicios.
    // Si es 1, la rutina se hace una vez (cada ejercicio con sus series).
    // Si los ejercicios tienen numeroDeSeries > 1, esto crea un efecto de circuito de series.
    val descansoEntreRondasSegundos: Int = 0, // Descanso después de completar una ronda completa de todos los ejercicios

    val nivelRecomendado: List<String> = emptyList(), // Puedes considerar un enum NivelDificultad si tienes niveles fijos
    val objetivos: List<String> = emptyList(),      // Puedes considerar un enum ObjetivoEntrenamiento
    val lugarEntrenamiento: List<LugarEntrenamiento> = emptyList(), // Cambiado a List<LugarEntrenamiento>
    // Representa dónde se puede realizar la rutina en general.
    // Los ejercicios se cargarán como una subcolección en Firestore,
    // pero para la lógica de la pantalla de rutina, los tendremos aquí una vez cargados.
    val ejercicios: List<Ejercicio> = emptyList()
    // Nota: Cuando cargas desde Firestore, típicamente cargas el documento Rutina
    // y LUEGO haces una consulta separada para la subcolección de 'ejercicios'.
    // El campo 'ejercicios' aquí sería poblado en tu ViewModel después de ambas cargas.
)