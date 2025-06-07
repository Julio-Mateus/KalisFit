package com.jcmateus.kalisfit.data



import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.viewmodel.UserProfile
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.text.uppercase

// Estructura de datos para un ejercicio TAL COMO SE GUARDARÁ EN FIRESTORE
data class EjercicioFirestore(
    var id: String = "", // Hacerlo var para poder asignarle el ID del documento
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val videoUrl: String? = null,
    val duracionSegundos: Int = 0,
    val repeticiones: Int = 0,
    val series: Int = 0, // ESTE ES EL NÚMERO DE SERIES PLANIFICADAS
    val descansoEntreSeriesSegundos: Int = 0, // Añadido para mapear
    val grupoMuscular: List<String> = emptyList(),
    val equipamientoNecesario: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(), // GUARDADO COMO LISTA DE STRINGS
    val orden: Int = 0
)

// Representa cómo se guardará un ejercicio individual dentro del progreso de una rutina
data class EjercicioProgresoFirestore(
    val ejercicioIdOriginal: String = "", // ID del Ejercicio original
    val nombre: String = "",
    // Valores objetivo por serie del ejercicio original
    val duracionPorSerieSegundos: Int = 0,
    val repeticionesPorSerie: Int = 0,
    // Lo que realmente se hizo
    val seriesRealizadas: Int = 0, // Cuántas series de este ejercicio se completaron
    val orden: Int = 0 // Para mantener el orden de los ejercicios tal como se hicieron
)

// Representa cómo se guardará el progreso completo de una sesión de rutina
data class ProgresoRutinaFirestore(
    val userId: String = "", // ID del usuario que realizó la rutina
    val rutinaIdOriginal: String = "", // ID de la Rutina base que se realizó
    val nombreRutina: String = "",
    val fecha: Timestamp = Timestamp.now(), // Usar Timestamp de Firestore para facilitar consultas y ordenación
    val nivelUsuarioAlCompletar: String = "",
    val objetivosUsuarioAlCompletar: List<String> = emptyList(),
    val ejerciciosCompletados: List<EjercicioProgresoFirestore> = emptyList(),
    val rondasRealizadas: Int = 0,
    val tiempoTotalSesionSegundos: Int = 0
    // Considera añadir 'version: Int = 1' para futuras migraciones de datos si la estructura cambia mucho
)

// Estructura de datos para una rutina TAL COMO SE GUARDARÁ EN FIRESTORE (sin la lista de ejercicios)
data class RutinaFirestore(
    var id: String = "", // Hacerlo var para poder asignarle el ID del documento
    val slug: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val nivelRecomendado: List<String> = emptyList(),
    val objetivos: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(), // GUARDADO COMO LISTA DE STRINGS
    val numeroDeRondas: Int = 1, // Añadido para que coincida con tu modelo Rutina
    val descansoEntreRondasSegundos: Int = 0 // Añadido
    // No incluir la lista de ejercicios aquí para esta función específica
)
// Estructura para un nivel de ejercicio de calistenia TAL COMO SE GUARDARÁ EN FIRESTORE
data class CalisthenicsLevelFirestore(
    val id: String = "", // ID único del nivel
    val name: String = "",
    val description: String = "",
    val targetReps: String? = null,
    val targetSets: String? = null,
    val targetHoldTime: String? = null,
    val videoUrl: String? = null,
    val notes: String? = null,
    val imageUrl: String? = null, // URL de la imagen del nivel
    val orden: Int = 0 // Para mantener el orden de los niveles
)

// Estructura para una progresión de calistenia TAL COMO SE GUARDARÁ EN FIRESTORE
data class CalisthenicsProgressionFirestore(
    val id: String = "", // ID del documento en la colección 'calisthenicsProgressions'
    val name: String = "",
    val iconUrl: String? = null, // URL del icono de la progresión
    val description: String? = null,
    val levels: List<CalisthenicsLevelFirestore> = emptyList()
    // Los niveles se guardarán en una subcolección "levels"
)

private const val TAG = "FirestoreUtils"
@RequiresApi(Build.VERSION_CODES.O)
fun guardarProgresoRutina(
    userIdAuth: String, // ID del usuario autenticado
    rutinaRealizada: Rutina, // El objeto Rutina de tu app que se completó
    perfilUsuarioActual: UserProfile, // El perfil del usuario en el momento de completar la rutina
    rondasCompletadasEnSesion: Int,
    tiempoTotalDeLaSesionSegundos: Int,
    // Aquí podrías necesitar un mapa o una lista especial si las series completadas
    // por ejercicio no siempre son 'ejercicio.numeroDeSeries'.
    // Por ahora, asumiremos que si un ejercicio está en 'rutinaRealizada.ejercicios'
    // y la rutina se completa, todas sus series planificadas se hicieron.
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    // Mapea de tu Ejercicio (modelo de app) a EjercicioProgresoFirestore
    val ejerciciosParaProgresoFirestore = rutinaRealizada.ejercicios.mapIndexed { index, ejercicioApp ->
        EjercicioProgresoFirestore(
            ejercicioIdOriginal = ejercicioApp.id,
            nombre = ejercicioApp.nombre,
            duracionPorSerieSegundos = ejercicioApp.duracionSegundos, // Duración objetivo por serie
            repeticionesPorSerie = ejercicioApp.repeticiones,    // Reps objetivo por serie
            seriesRealizadas = ejercicioApp.numeroDeSeries, // Asume que se completaron todas las series planificadas para este ejercicio
            orden = index // Mantenemos el orden en que se presentaron
        )
    }

    val progresoFirestore = ProgresoRutinaFirestore(
        userId = userIdAuth,
        rutinaIdOriginal = rutinaRealizada.id,
        nombreRutina = rutinaRealizada.nombre,
        fecha = Timestamp.now(), // Firestore Timestamp para mejor manejo de fechas
        nivelUsuarioAlCompletar = perfilUsuarioActual.nivel,
        objetivosUsuarioAlCompletar = perfilUsuarioActual.objetivos,
        ejerciciosCompletados = ejerciciosParaProgresoFirestore,
        rondasRealizadas = rondasCompletadasEnSesion,
        tiempoTotalSesionSegundos = tiempoTotalDeLaSesionSegundos
    )

    db.collection("users")
        .document(userIdAuth)
        .collection("progresoRutinas") // Usar un nombre de subcolección específico
        .add(progresoFirestore)
        .addOnSuccessListener { documentReference ->
            Log.d(TAG, "Progreso de rutina guardado con ID: ${documentReference.id}")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error al guardar progreso de rutina", e)
            onError(e.message ?: "Error desconocido al guardar progreso")
        }
}

fun obtenerHistorialProgreso(
    userId: String,
    onResult: (List<ProgresoRutina>) -> Unit,
    onError: (String) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(userId)
        .collection("progreso")
        .orderBy("fecha", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { result ->
            val lista = result.documents.mapNotNull { it.toObject(ProgresoRutina::class.java) }
            onResult(lista)
        }
        .addOnFailureListener {
            onError(it.message ?: "Error al obtener historial")
        }
}

suspend fun getAllCalisthenicsProgressions(): List<Progression> {
    val db = FirebaseFirestore.getInstance()
    val progressionsList = mutableListOf<Progression>()

    try {
        val progressionsSnapshot = db.collection("calisthenicsProgressions")
            .get()
            .await()

        for (progressionDoc in progressionsSnapshot.documents) {
            // Ahora CalisthenicsProgressionFirestore espera un campo "levels"
            val progressionFirestore = progressionDoc.toObject(CalisthenicsProgressionFirestore::class.java)

            if (progressionFirestore != null) {
                // Los niveles ya vienen dentro de progressionFirestore.levels
                // No necesitas hacer otra consulta a Firestore para una subcolección

                val exerciseLevels = progressionFirestore.levels.map { levelFirestore ->
                    // Mapear de CalisthenicsLevelFirestore (que vino del array) a ExerciseLevel (UI Model)
                    ExerciseLevel(
                        id = levelFirestore.id.ifEmpty { /* Podrías necesitar un ID único aquí si el del JSON es vacío */ "" },
                        name = levelFirestore.name,
                        description = levelFirestore.description,
                        targetReps = levelFirestore.targetReps,
                        targetSets = levelFirestore.targetSets,
                        targetHoldTime = levelFirestore.targetHoldTime,
                        videoUrl = levelFirestore.videoUrl,
                        notes = levelFirestore.notes,
                        imageUrl = levelFirestore.imageUrl,
                        order = levelFirestore.orden // Mapea 'orden' a 'order'
                    )
                }.sortedBy { it.order } // Ordena aquí si los niveles en el array no vienen ordenados
                // o si quieres asegurarte del orden

                progressionsList.add(
                    Progression(
                        id = progressionFirestore.id.ifEmpty { progressionDoc.id },
                        name = progressionFirestore.name,
                        iconUrl = progressionFirestore.iconUrl,
                        description = progressionFirestore.description,
                        levels = exerciseLevels // Asigna la lista de niveles mapeada
                    )
                )
            } else {
                Log.w(TAG, "No se pudo mapear el documento de progresión ${progressionDoc.id} a CalisthenicsProgressionFirestore.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener progresiones de calistenia desde Firestore.", e)
    }
    return progressionsList
}
suspend fun getCalisthenicsExerciseLevel(progressionId: String, levelId: String): ExerciseLevel? {
    val db = FirebaseFirestore.getInstance()
    return try {
        // 1. Obtener el documento completo de la progresión
        val progressionDocSnapshot = db.collection("calisthenicsProgressions")
            .document(progressionId)
            .get()
            .await()

        if (progressionDocSnapshot.exists()) {
            val progressionFirestore = progressionDocSnapshot.toObject(CalisthenicsProgressionFirestore::class.java)
            if (progressionFirestore != null) {
                // 2. Buscar el nivel específico por su ID dentro de la lista de niveles
                val levelFirestore = progressionFirestore.levels.firstOrNull { it.id == levelId }

                if (levelFirestore != null) {
                    // Mapear de CalisthenicsLevelFirestore a ExerciseLevel (UI Model)
                    ExerciseLevel(
                        id = levelFirestore.id.ifEmpty { levelId }, // Usar levelId como fallback si el id en el objeto está vacío
                        name = levelFirestore.name,
                        description = levelFirestore.description,
                        targetReps = levelFirestore.targetReps,
                        targetSets = levelFirestore.targetSets,
                        targetHoldTime = levelFirestore.targetHoldTime,
                        videoUrl = levelFirestore.videoUrl,
                        notes = levelFirestore.notes,
                        imageUrl = levelFirestore.imageUrl
                        // 'order' no está en tu ExerciseLevel UI model, pero 'orden' sí en CalisthenicsLevelFirestore
                        // Si ExerciseLevel necesita 'order', deberías añadirlo y mapear levelFirestore.orden
                    )
                } else {
                    Log.w(TAG, "Nivel con id '$levelId' no encontrado DENTRO de la progresión '$progressionId'")
                    null
                }
            } else {
                Log.e(TAG, "Error al mapear documento de progresión '$progressionId' a CalisthenicsProgressionFirestore.")
                null
            }
        } else {
            Log.w(TAG, "Documento de progresión no encontrado en Firestore: progressionId='$progressionId'")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener nivel de calistenia: progressionId='$progressionId', levelId='$levelId'", e)
        null
    }
}

@RequiresApi(Build.VERSION_CODES.O) // Para Instant
fun calcularResumenSemanal(historialProgreso: List<ProgresoRutinaFirestore>): ResumenSemanal {
    val ahora = Instant.now()
    val hace7Dias = ahora.minus(7, ChronoUnit.DAYS)

    val recientes = historialProgreso.filter { progreso ->
        // Convertir Timestamp de Firestore a Instant para comparar
        val fechaProgresoInstant = progreso.fecha.toDate().toInstant()
        fechaProgresoInstant.isAfter(hace7Dias)
    }

    if (recientes.isEmpty()) {
        return ResumenSemanal() // Devuelve resumen vacío con valores por defecto
    }

    val totalRutinas = recientes.size
    // El tiempo total ahora viene de ProgresoRutinaFirestore.tiempoTotalSesionSegundos
    val tiempoTotalSegundos = recientes.sumOf { it.tiempoTotalSesionSegundos }

    val objetivos = recientes.flatMap { it.objetivosUsuarioAlCompletar } // Usar los objetivos guardados en el progreso
    val objetivosRepetidos = objetivos
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key }
        .take(3) // Quizás tomar los 3 más frecuentes

    var totalEjerciciosCompletados = 0 // Suma de todos los ejercicios en todas las series y rondas
    var ejerciciosContadosPorTiempo = 0
    var ejerciciosContadosPorRepeticiones = 0

    recientes.forEach { rutinaProgreso ->
        rutinaProgreso.ejerciciosCompletados.forEach { ejercicioProgreso ->
            // Cada 'ejercicioProgreso' representa un tipo de ejercicio que se hizo.
            // Y 'seriesRealizadas' nos dice cuántas veces se hizo ese bloque.
            totalEjerciciosCompletados += ejercicioProgreso.seriesRealizadas // Un ejercicio se cuenta por cada serie realizada

            // Para clasificar si el ejercicio fue por tiempo o por repeticiones,
            // miramos sus valores objetivo (duracionPorSerieSegundos vs repeticionesPorSerie)
            if (ejercicioProgreso.repeticionesPorSerie > 0) {
                ejerciciosContadosPorRepeticiones += ejercicioProgreso.seriesRealizadas
            } else if (ejercicioProgreso.duracionPorSerieSegundos > 0) {
                ejerciciosContadosPorTiempo += ejercicioProgreso.seriesRealizadas
            }
        }
    }

    return ResumenSemanal(
        rutinas = totalRutinas,
        tiempoTotal = tiempoTotalSegundos,
        objetivosRecurrentes = objetivosRepetidos,
        totalEjercicios = totalEjerciciosCompletados, // Este es el total de *series de ejercicios* completadas
        ejerciciosPorTiempo = ejerciciosContadosPorTiempo,
        ejerciciosPorRepeticiones = ejerciciosContadosPorRepeticiones
    )
}
// En FirestoreUtils.kt - Asumiendo que el usuario puede seleccionar VARIOS lugares

fun obtenerRutinas(
    nivel: String? = null,
    objetivos: List<String>? = null,
    lugaresEntrenamiento: List<LugarEntrenamiento>? = null, // Lista de Enum de lugares del usuario
    onResult: (List<Rutina>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var query: Query = db.collection("rutinas")
    var filtroServidorAplicadoParaNivel = false

    if (nivel != null) {
        query = query.whereArrayContains("nivelRecomendado", nivel)
        filtroServidorAplicadoParaNivel = true
        Log.d(TAG, "Filtrando en servidor por nivel: $nivel")
    }

    // Convertir List<LugarEntrenamiento> (enum) a List<String> para la consulta
    val nombresLugaresUsuarioComoString = lugaresEntrenamiento?.map { it.name } ?: emptyList()

    if (!filtroServidorAplicadoParaNivel && nombresLugaresUsuarioComoString.isNotEmpty()) {
        // Firestore 'array-contains' solo funciona con un elemento para arrays.
        // Si quieres buscar rutinas que contengan CUALQUIERA de los lugares del usuario,
        // tendrías que usar 'whereArrayContainsAny' (si los lugares del usuario son pocos)
        // o hacer el filtrado en cliente si la lista de lugares del usuario es grande
        // o si los documentos de rutina pueden tener muchos lugares.
        // Por ahora, se mantiene la lógica original de filtrar por el primero.
        query = query.whereArrayContains("lugarEntrenamiento", nombresLugaresUsuarioComoString.first())
        Log.d(TAG, "Filtrando en servidor por el primer lugarEntrenamiento: ${nombresLugaresUsuarioComoString.first()}")
    } else if (!filtroServidorAplicadoParaNivel && (objetivos != null && objetivos.isNotEmpty())) {
        query = query.whereArrayContains("objetivos", objetivos.first())
        Log.d(TAG, "Filtrando en servidor por primer objetivo: ${objetivos.first()}")
    }

    query.get()
        .addOnSuccessListener { result ->
            val rutinasDesdeFirestore = result.documents.mapNotNull { document ->
                document.toObject(RutinaFirestore::class.java)?.apply { id = document.id } // Asignar ID
            }

            val rutinasFiltradasCliente = rutinasDesdeFirestore.filter { rutinaFirestore ->
                val pasaFiltroNivel = nivel == null ||
                        rutinaFirestore.nivelRecomendado.any { rn -> rn.equals(nivel, ignoreCase = true) }

                // Filtrado en cliente para LugarEntrenamiento (String en Firestore vs Enum en App)
                val pasaFiltroLugar = nombresLugaresUsuarioComoString.isEmpty() ||
                        rutinaFirestore.lugarEntrenamiento.any { lugarRutinaStr -> // lugarRutinaStr es String
                            nombresLugaresUsuarioComoString.any { lugarUsuarioStr -> // lugarUsuarioStr es String
                                lugarUsuarioStr.equals(lugarRutinaStr, ignoreCase = true)
                            }
                        }

                val pasaFiltroObjetivos = objetivos == null || objetivos.isEmpty() ||
                        rutinaFirestore.objetivos.any { objetivoRutina ->
                            objetivos.any { objetivoUsuario ->
                                objetivoUsuario.equals(objetivoRutina, ignoreCase = true)
                            }
                        }
                pasaFiltroNivel && pasaFiltroLugar && pasaFiltroObjetivos
            }

            val rutinasModeloApp = rutinasFiltradasCliente.map { rf ->
                // Mapeo de List<String> a List<LugarEntrenamiento> (enum)
                val lugaresEnum = rf.lugarEntrenamiento.mapNotNull { lugarStr ->
                    try {
                        LugarEntrenamiento.valueOf(lugarStr.uppercase())
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Lugar de entrenamiento desconocido en Firestore: $lugarStr para rutina ${rf.id}")
                        null // O manejar el error de otra forma
                    }
                }
                Rutina(
                    id = rf.id,
                    slug = rf.slug,
                    nombre = rf.nombre,
                    descripcion = rf.descripcion,
                    imagenUrl = rf.imagenUrl,
                    nivelRecomendado = rf.nivelRecomendado,
                    objetivos = rf.objetivos,
                    lugarEntrenamiento = lugaresEnum, // Usar la lista de Enums mapeada
                    ejercicios = emptyList(), // Los ejercicios se cargan por separado
                    numeroDeRondas = rf.numeroDeRondas, // Mapear desde RutinaFirestore
                    descansoEntreRondasSegundos = rf.descansoEntreRondasSegundos // Mapear
                )
            }
            onResult(rutinasModeloApp)
        }
        .addOnFailureListener {
            Log.e(TAG, "Error al obtener rutinas Firestore: ${it.message}", it)
            onError(it.message ?: "Error al obtener rutinas filtradas")
        }
}
suspend fun getRutinaByIdFromFirestore(rutinaId: String): Rutina? {
    val db = FirebaseFirestore.getInstance()
    return try {
        val rutinaDocumentSnapshot = db.collection("rutinas")
            .document(rutinaId)
            .get()
            .await()

        if (!rutinaDocumentSnapshot.exists()) {
            Log.w(TAG, "Rutina con ID $rutinaId no encontrada en Firestore.")
            return null
        }

        val rutinaFirestore = rutinaDocumentSnapshot.toObject(RutinaFirestore::class.java)
            ?.apply { id = rutinaDocumentSnapshot.id } // Asignar ID
            ?: run {
                Log.e(TAG, "Error al mapear documento de rutina $rutinaId a RutinaFirestore.")
                return null
            }

        val ejerciciosSnapshot = db.collection("rutinas")
            .document(rutinaId)
            .collection("ejercicios")
            .orderBy("orden", Query.Direction.ASCENDING)
            .get()
            .await()

        val ejerciciosFirestoreList = ejerciciosSnapshot.documents.mapNotNull { doc ->
            doc.toObject(EjercicioFirestore::class.java)?.apply { id = doc.id } // Asignar ID al EjercicioFirestore
        }

        val ejerciciosAppModel = ejerciciosFirestoreList.map { ef ->
            // Mapeo de List<String> a List<GrupoMuscular> (enum)
            val gruposMuscularesEnum = ef.grupoMuscular.mapNotNull { str ->
                try {
                    GrupoMuscular.valueOf(str.uppercase()) // Asume que los strings en Firestore coinciden con los nombres del enum
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Grupo muscular desconocido en Firestore: $str para ejercicio ${ef.id}")
                    null
                }
            }
            // Mapeo de List<String> a List<LugarEntrenamiento> (enum)
            val lugaresEntrenamientoEnum = ef.lugarEntrenamiento.mapNotNull { str ->
                try {
                    LugarEntrenamiento.valueOf(str.uppercase())
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Lugar de entrenamiento desconocido en Firestore: $str para ejercicio ${ef.id}")
                    null
                }
            }

            Ejercicio( // Modelo de tu app
                id = ef.id,
                nombre = ef.nombre,
                descripcion = ef.descripcion,
                imagenUrl = ef.imagenUrl,
                videoUrl = ef.videoUrl,
                duracionSegundos = ef.duracionSegundos,
                repeticiones = ef.repeticiones,
                numeroDeSeries = ef.series, // Mapear 'series' de Firestore a 'numeroDeSeries'
                descansoEntreSeriesSegundos = ef.descansoEntreSeriesSegundos, // Mapear desde EjercicioFirestore
                grupoMuscular = gruposMuscularesEnum,
                equipamientoNecesario = ef.equipamientoNecesario,
                lugarEntrenamiento = lugaresEntrenamientoEnum, // Usar la lista de Enums mapeada
                orden = ef.orden
            )
        }

        // Mapeo de List<String> a List<LugarEntrenamiento> (enum) para la Rutina principal
        val lugaresRutinaEnum = rutinaFirestore.lugarEntrenamiento.mapNotNull { lugarStr ->
            try {
                LugarEntrenamiento.valueOf(lugarStr.uppercase())
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Lugar de entrenamiento desconocido en Firestore: $lugarStr para rutina ${rutinaFirestore.id}")
                null
            }
        }

        Rutina( // Modelo de tu app
            id = rutinaFirestore.id,
            slug = rutinaFirestore.slug,
            nombre = rutinaFirestore.nombre,
            descripcion = rutinaFirestore.descripcion,
            imagenUrl = rutinaFirestore.imagenUrl,
            nivelRecomendado = rutinaFirestore.nivelRecomendado,
            objetivos = rutinaFirestore.objetivos,
            lugarEntrenamiento = lugaresRutinaEnum, // Usar la lista de Enums mapeada
            ejercicios = ejerciciosAppModel,
            numeroDeRondas = rutinaFirestore.numeroDeRondas, // Mapear desde RutinaFirestore
            descansoEntreRondasSegundos = rutinaFirestore.descansoEntreRondasSegundos // Mapear
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener rutina con ID $rutinaId desde Firestore con ejercicios.", e)
        throw e // O return null
    }
}

data class ResumenSemanal(
    val rutinas: Int = 0, // Es buena práctica añadir valores por defecto
    val tiempoTotal: Int = 0, // en segundos
    val objetivosRecurrentes: List<String> = emptyList(),
    val totalEjercicios: Int = 0,
    val ejerciciosPorTiempo: Int = 0,
    val ejerciciosPorRepeticiones: Int = 0
)




