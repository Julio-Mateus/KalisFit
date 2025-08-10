package com.jcmateus.kalisfit.data



import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.viewmodel.UserProfile
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.text.uppercase

// Estructura de datos para un ejercicio TAL COMO SE GUARDARÁ EN FIRESTORE
data class EjercicioFirestore(
    var id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    // AÑADE imagenUrl1 y imagenUrl2 si existen en tu Firestore/JSON
    val imagenUrl1: String? = null,
    val imagenUrl2: String? = null,
    val videoUrl: String? = null,
    val duracionSegundos: Int = 0, // Este es el que usaremos como duracionSegundosOriginal
    val repeticiones: String = "0", // CAMBIADO A STRING
    val numeroDeSeries: Int = 0,
    val descansoEntreSeriesSegundos: Int = 0,
    val descansoDespuesEjercicioSegundos: Int = 0,
    val grupoMuscular: List<String> = emptyList(),
    val equipamientoNecesario: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(),
    val orden: Double = 0.0 // CAMBIADO A DOUBLE para consistencia con el modelo de app Ejercicio
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
private const val TAG_CUSTOM_ROUTINE = "FirestoreUtils_CustomRoutine" // Nuevo TAG para claridad
private const val TAG_GET_CUSTOM_ROUTINES = "FirestoreUtils_GetCustom"
@RequiresApi(Build.VERSION_CODES.O)
fun guardarProgresoRutina(
    userIdAuth: String,
    rutinaRealizada: Rutina, // Tu modelo de app Rutina
    perfilUsuarioActual: UserProfile,
    rondasCompletadasEnSesion: Int,
    tiempoTotalDeLaSesionSegundos: Int,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val ejerciciosParaProgresoFirestore = rutinaRealizada.ejercicios.mapIndexed { index, ejercicioApp ->
        // ejercicioApp es de tipo com.jcmateus.kalisfit.model.Ejercicio

        // Lógica para convertir repeticionesOriginal (String) a un Int para el progreso
        // Esto toma la primera secuencia de dígitos. Si es "12 + 15", toma 12. Si "AMRAP", toma 0.
        val repeticionesNumericas = ejercicioApp.repeticionesOriginal
            .substringBefore(" ") // Toma la parte antes del primer espacio (si hay)
            .filter { it.isDigit() } // Toma solo los dígitos
            .toIntOrNull() ?: 0 // Convierte a Int, o 0 si falla

        EjercicioProgresoFirestore(
            ejercicioIdOriginal = ejercicioApp.id,
            nombre = ejercicioApp.nombre,
            // Usar los campos renombrados de tu modelo de app Ejercicio
            duracionPorSerieSegundos = ejercicioApp.duracionSegundosOriginal, // CORREGIDO
            repeticionesPorSerie = repeticionesNumericas,                  // CORREGIDO y CONVERTIDO
            seriesRealizadas = ejercicioApp.numeroDeSeries,
            orden = index // Aquí usas el índice, pero ejercicioApp.orden (que es Double) también existe
            // Decide cuál es más apropiado para el orden en Progreso.
            // Si ejercicioApp.orden es fiable, usa ejercicioApp.orden.toInt() o similar.
        )
    }

    val progresoFirestore = ProgresoRutinaFirestore(
        userId = userIdAuth,
        rutinaIdOriginal = rutinaRealizada.id,
        nombreRutina = rutinaRealizada.nombre,
        fecha = Timestamp.now(),
        nivelUsuarioAlCompletar = perfilUsuarioActual.nivel, // Asegúrate que UserProfile tiene 'nivel'
        objetivosUsuarioAlCompletar = perfilUsuarioActual.objetivos, // Asegúrate que UserProfile tiene 'objetivos'
        ejerciciosCompletados = ejerciciosParaProgresoFirestore,
        rondasRealizadas = rondasCompletadasEnSesion,
        tiempoTotalSesionSegundos = tiempoTotalDeLaSesionSegundos
    )

    db.collection("users")
        .document(userIdAuth)
        .collection("progresoRutinas")
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
        .collection("progresoRutinas")
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
suspend fun saveOrUpdateUserCustomRoutine(userId: String, routine: UserCustomRoutine) {
    if (userId.isBlank()) {
        Log.e(TAG_CUSTOM_ROUTINE, "El ID de usuario no puede estar vacío al guardar UserCustomRoutine.")
        throw IllegalArgumentException("El ID de usuario no puede estar vacío.")
    }
    if (routine.id.isBlank()) {
        Log.e(TAG_CUSTOM_ROUTINE, "El ID de la UserCustomRoutine no puede estar vacío.")
        throw IllegalArgumentException("El ID de la UserCustomRoutine no puede estar vacío.")
    }

    val db = FirebaseFirestore.getInstance()
    val customRoutineRef = db.collection("users")
        .document(userId)
        .collection("customRoutines") // Asegúrate que este es el nombre de tu subcolección
        .document(routine.id) // Usamos el ID de la rutina para crear/actualizar

    try {
        // Firestore guardará los objetos UserCustomRoutine, Ejercicio y ComponenteEjercicio
        // tal como están definidos en tus data classes.
        // Los Enums se guardarán como Strings (ej: "CASA", "PECHO", "SIMPLE").
        customRoutineRef.set(routine).await() // .set() crea o sobrescribe el documento.
        Log.d(TAG_CUSTOM_ROUTINE, "UserCustomRoutine guardada/actualizada con ID: ${routine.id} para usuario: $userId")
    } catch (e: Exception) {
        Log.e(TAG_CUSTOM_ROUTINE, "Error al guardar/actualizar UserCustomRoutine con ID: ${routine.id} para usuario: $userId", e)
        throw e // Relanzar la excepción para que el ViewModel la maneje
    }
}
private fun parsearEjercicioFirestore(
    ef: EjercicioFirestore,
    gruposMuscularesEnum: List<GrupoMuscular>,
    lugaresEntrenamientoEnum: List<LugarEntrenamiento>
): Ejercicio {

    var tipo = TipoDeEjercicio.SIMPLE
    val componentes = mutableListOf<ComponenteEjercicio>()
    var notaTempoDetectada: String? = null
    var esEjercicioUnilateral = false

    val nombreLimpio = ef.nombre // Guardar una copia por si modificas ef.nombre
    val repeticionesOriginalString = ef.repeticiones.trim()

    // 1. Detectar Tempo en el nombre
    val tempoRegex = """\(Tempo\s*([\d-]+)\)""".toRegex(RegexOption.IGNORE_CASE)
    tempoRegex.find(nombreLimpio)?.let { matchResult ->
        notaTempoDetectada = matchResult.groupValues[1]
        tipo = TipoDeEjercicio.CON_TEMPO
        // ef.nombre = nombreLimpio.replace(tempoRegex, "").trim() // Opcional: limpiar el nombre en el objeto 'ef' si es var
    }

    // 2. Analizar repeticionesOriginalString
    if (repeticionesOriginalString.contains(" + ")) {
        tipo = TipoDeEjercicio.SUPERSET_SEQUENCIAL
        val partesRepeticiones = repeticionesOriginalString.split("+").map { it.trim() }
        val nombresComponentesSugeridos = if (nombreLimpio.contains(" + ")) {
            nombreLimpio.split(" + ").map { it.trim() }
        } else if (nombreLimpio.contains(" / ")) {
            nombreLimpio.split(" / ").map { it.trim() }
        } else {
            emptyList()
        }

        partesRepeticiones.forEachIndexed { index, parteRep ->
            var duracionComp: Int? = null
            var repComp: String? = parteRep

            if (parteRep.endsWith("s", ignoreCase = true)) {
                duracionComp = parteRep.dropLast(1).toIntOrNull()
                repComp = null
            }

            val nombreEspecificoComp = nombresComponentesSugeridos.getOrNull(index)
                ?: ef.descripcion.split("\n").getOrNull(index)?.trim()
                ?: "Parte ${index + 1}"

            componentes.add(
                ComponenteEjercicio(
                    nombreEspecifico = nombreEspecificoComp,
                    repeticiones = repComp,
                    duracionSegundos = duracionComp, // CORREGIDO
                    orden = index
                )
            )
        }

        if (componentes.all { it.duracionSegundos != null && it.duracionSegundos!! > 0 } && ef.duracionSegundos > 0) { // CORREGIDO y añadido !! para non-null
            tipo = TipoDeEjercicio.CIRCUITO_TEMPORIZADO
        } else if (ef.duracionSegundos > 0 && componentes.isNotEmpty() && componentes.first().duracionSegundos != null) { // CORREGIDO
            tipo = TipoDeEjercicio.COMBINADO_TEMPORIZADO
        }

    } else if (repeticionesOriginalString.contains(" por pierna", ignoreCase = true) ||
        repeticionesOriginalString.contains(" por lado", ignoreCase = true) ||
        ef.descripcion.contains(" cada lado", ignoreCase = true) ||
        repeticionesOriginalString.contains(" unilateral", ignoreCase = true)
    ) {
        esEjercicioUnilateral = true
        tipo = TipoDeEjercicio.POR_LADO_ALTERNADO
    } else if (repeticionesOriginalString.matches("""\d+\s*x\s*\d+s.*""".toRegex(RegexOption.IGNORE_CASE))) {
        if (ef.duracionSegundos > 0 && (nombreLimpio.contains("+") || nombreLimpio.contains("/"))) {
            val partesNombre = nombreLimpio.split(Regex("[+/]")).map { it.trim() }
            val matchRep = """(\d+)\s*x\s*(\d+)s.*""".toRegex(RegexOption.IGNORE_CASE).find(repeticionesOriginalString)
            if (matchRep != null && partesNombre.isNotEmpty()) { // Cambiado de partesNombre.size >=1 a isNotEmpty
                val duracionPorComponente = matchRep.groupValues[2].toIntOrNull()

                if (duracionPorComponente != null) {
                    tipo = TipoDeEjercicio.CIRCUITO_TEMPORIZADO
                    componentes.clear()
                    partesNombre.forEachIndexed { index, nombreParte ->
                        componentes.add(ComponenteEjercicio(
                            nombreEspecifico = nombreParte,
                            duracionSegundos = duracionPorComponente, // CORREGIDO
                            orden = index
                        ))
                    }
                }
            }
        }
    }

    return Ejercicio(
        id = ef.id,
        nombre = nombreLimpio.replace(tempoRegex, "").trim(), // Limpiar el nombre al final si se detectó tempo
        descripcion = ef.descripcion,
        imagenUrl = ef.imagenUrl,
        imagenUrl1 = ef.imagenUrl1,
        imagenUrl2 = ef.imagenUrl2,
        videoUrl = ef.videoUrl,
        duracionSegundosOriginal = ef.duracionSegundos,
        repeticionesOriginal = repeticionesOriginalString,
        numeroDeSeries = ef.numeroDeSeries,
        descansoEntreSeriesSegundos = ef.descansoEntreSeriesSegundos,
        descansoDespuesEjercicioSegundos = ef.descansoDespuesEjercicioSegundos,
        grupoMuscular = gruposMuscularesEnum,
        equipamientoNecesario = ef.equipamientoNecesario,
        lugarEntrenamiento = lugaresEntrenamientoEnum,
        orden = ef.orden, // Asumiendo que ef.orden ya es Double según tu EjercicioFirestore

        tipoEjercicio = tipo,
        componentes = componentes,
        notaTempo = notaTempoDetectada,
        esUnilateral = esEjercicioUnilateral
    )
}
suspend fun getRutinaByIdFromFirestore(rutinaId: String): Rutina? { // Asegúrate que devuelve tu modelo de app
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
            ?.apply { id = rutinaDocumentSnapshot.id }
            ?: run {
                Log.e(TAG, "Error al mapear documento de rutina $rutinaId a RutinaFirestore.")
                return null
            }

        val ejerciciosSnapshot = db.collection("rutinas")
            .document(rutinaId)
            .collection("ejercicios")
            .orderBy("orden", Query.Direction.ASCENDING) // Ordenar por Double ahora
            .get()
            .await()

        val ejerciciosFirestoreList = ejerciciosSnapshot.documents.mapNotNull { doc ->
            // Asegúrate que EjercicioFirestore tiene imagenUrl1, imagenUrl2, repeticiones como String y orden como Double
            doc.toObject(EjercicioFirestore::class.java)?.apply { id = doc.id }
        }

        val ejerciciosAppModel = ejerciciosFirestoreList.map { ef -> // ef es EjercicioFirestore
            // Mapeo de List<String> a List<GrupoMuscular> (enum)
            val gruposMuscularesEnum = ef.grupoMuscular.mapNotNull { str ->
                try {
                    GrupoMuscular.valueOf(str.trim().uppercase().replace(" ", "_")) // Manejar espacios y asegurar mayúsculas
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Grupo muscular desconocido en Firestore: '$str' para ejercicio ${ef.id}")
                    null
                }
            }
            // Mapeo de List<String> a List<LugarEntrenamiento> (enum)
            val lugaresEntrenamientoEnum = ef.lugarEntrenamiento.mapNotNull { str ->
                try {
                    LugarEntrenamiento.valueOf(str.trim().uppercase())
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Lugar de entrenamiento desconocido en Firestore: '$str' para ejercicio ${ef.id}")
                    null
                }
            }

            // AQUÍ LA LLAMADA A LA NUEVA FUNCIÓN DE PARSEO
            parsearEjercicioFirestore(ef, gruposMuscularesEnum, lugaresEntrenamientoEnum)
        }

        // Mapeo de List<String> a List<LugarEntrenamiento> (enum) para la Rutina principal
        val lugaresRutinaEnum = rutinaFirestore.lugarEntrenamiento.mapNotNull { lugarStr ->
            try {
                LugarEntrenamiento.valueOf(lugarStr.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Lugar de entrenamiento desconocido en Firestore: '$lugarStr' para rutina ${rutinaFirestore.id}")
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
            lugarEntrenamiento = lugaresRutinaEnum,
            ejercicios = ejerciciosAppModel, // ¡Lista de ejercicios procesados!
            numeroDeRondas = rutinaFirestore.numeroDeRondas,
            descansoEntreRondasSegundos = rutinaFirestore.descansoEntreRondasSegundos
        )

    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener rutina con ID $rutinaId desde Firestore con ejercicios.", e)
        throw e // O return null
    }
}
suspend fun getUserCustomRoutineById(userId: String, customRoutineId: String): UserCustomRoutine? {
    if (userId.isBlank() || customRoutineId.isBlank()) {
        Log.w(TAG, "IDs de usuario o rutina personalizada están vacíos. userId: $userId, customRoutineId: $customRoutineId")
        return null
    }
    val db = FirebaseFirestore.getInstance()
    return try {
        val documentSnapshot = db.collection("users")
            .document(userId)
            .collection("customRoutines") // O como llames a tu subcolección
            .document(customRoutineId)
            .get()
            .await()

        if (documentSnapshot.exists()) {
            // Aquí es CRUCIAL que UserCustomRoutine sea tu data class que tiene originalTemplateId
            val customRoutine = documentSnapshot.toObject(UserCustomRoutine::class.java)
            // Firestore no siempre mete el ID del documento en el objeto, así que lo asignamos:
            customRoutine?.id = documentSnapshot.id
            // Si también quieres el userId en el objeto (aunque ya lo tienes como parámetro),
            // podrías hacer: customRoutine?.userId = userId (si 'userId' es var en UserCustomRoutine)
            // o asegurarte de que se guarde correctamente.
            customRoutine
        } else {
            Log.w(TAG, "No se encontró UserCustomRoutine con ID: $customRoutineId para el usuario: $userId")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener UserCustomRoutine: $customRoutineId para el usuario: $userId", e)
        null // O re-lanza la excepción si quieres manejarla más arriba
    }
}
suspend fun getUserCustomRoutines(userId: String): List<UserCustomRoutine> {
    if (userId.isBlank()) {
        Log.e(TAG_GET_CUSTOM_ROUTINES, "El ID de usuario no puede estar vacío al obtener UserCustomRoutines.")
        return emptyList() // O lanzar una excepción, según prefieras
    }

    val db = FirebaseFirestore.getInstance()
    return try {
        val snapshot = db.collection("users")
            .document(userId)
            .collection("customRoutines") // Asegúrate que este es el nombre de tu subcolección
            .orderBy("fechaUltimaModificacion", Query.Direction.DESCENDING) // Opcional: ordenar por fecha
            .get()
            .await()

        // Mapear los documentos a objetos UserCustomRoutine
        // Usamos toObject con la clase correcta y manejamos el caso donde la conversión podría fallar
        // o un documento no existe (aunque get() sin where() no debería dar documentos inexistentes en la lista).
        val routines = snapshot.documents.mapNotNull { document ->
            try {
                document.toObject(UserCustomRoutine::class.java)?.apply {
                    // Si el ID del documento de Firestore no está guardado dentro del objeto UserCustomRoutine,
                    // (por ejemplo, si 'id' en UserCustomRoutine no es un campo directo del documento,
                    // sino que es el ID del documento mismo), necesitarías asignarlo aquí.
                    // Pero como lo estableces al guardar, debería estar ya en el objeto.
                    // Si 'id' en tu UserCustomRoutine ES el id del documento y no un campo, harías:
                    // this.id = document.id // Asegúrate que 'id' es var en tu data class
                }
            } catch (e: Exception) {
                Log.e(TAG_GET_CUSTOM_ROUTINES, "Error al convertir documento ${document.id} a UserCustomRoutine", e)
                null // Ignorar este documento si hay un error de conversión
            }
        }
        Log.d(TAG_GET_CUSTOM_ROUTINES, "Se encontraron ${routines.size} rutinas personalizadas para el usuario $userId.")
        routines
    } catch (e: Exception) {
        Log.e(TAG_GET_CUSTOM_ROUTINES, "Error al obtener rutinas personalizadas para el usuario: $userId", e)
        throw e // Relanzar para que el ViewModel lo maneje
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




