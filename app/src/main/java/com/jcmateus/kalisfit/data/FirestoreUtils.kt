package com.jcmateus.kalisfit.data



import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.EjercicioSimple
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.sequences.ifEmpty
import kotlin.text.map
import kotlin.text.uppercase

// Estructura de datos para un ejercicio TAL COMO SE GUARDARÁ EN FIRESTORE
data class EjercicioFirestore(
    val id: String = "", // ID del documento en la subcolección
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val videoUrl: String? = null,
    val duracionSegundos: Int = 0,
    val repeticiones: Int = 0,
    val series: Int = 0,
    val grupoMuscular: List<String> = emptyList(), // Guardado como lista de Strings
    val equipamientoNecesario: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(), // Guardado como lista de Strings
    val orden: Int = 0 // Para mantener el orden de los ejercicios
)

// Estructura de datos para una rutina TAL COMO SE GUARDARÁ EN FIRESTORE (sin la lista de ejercicios)
data class RutinaFirestore(
    val id: String = "", // ID del documento en la colección 'rutinas'
    val nombre: String = "",
    val descripcion: String = "",
    val nivelRecomendado: List<String> = emptyList(),
    val objetivos: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList() // Guardado como lista de Strings
    // No incluir la lista de ejercicios aquí
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
    userId: String,
    nivel: String,
    objetivos: List<String>,
    rutina: List<Ejercicio>, // Asumo que Ejercicio tiene duracionSegundos y repeticiones como Int
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    // Mapea de tu Ejercicio de la lógica de la app a EjercicioSimple para guardar progreso
    // usando los valores Int directamente
    val ejerciciosSimples = rutina.map { ejercicio ->
        EjercicioSimple(
            id = ejercicio.id, // Si EjercicioSimple necesita ID, úsalo desde Ejercicio
            nombre = ejercicio.nombre,
            duracionSegundos = ejercicio.duracionSegundos, // Pasa el Int directamente
            repeticiones = ejercicio.repeticiones // Pasa el Int directamente
        )
    }

    val progreso = ProgresoRutina(
        fecha = Timestamp.now().toDate().toInstant().toString(),
        nivel = nivel,
        objetivos = objetivos,
        ejercicios = ejerciciosSimples, // Usa la lista de EjercicioSimple que acabas de crear
        tiempoTotal = rutina.sumOf { it.duracionSegundos } // Suma los Int directamente
    )

    db.collection("users")
        .document(userId)
        .collection("progreso")
        .add(progreso)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it.message ?: "Error desconocido") }
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

@RequiresApi(Build.VERSION_CODES.O)
fun calcularResumenSemanal(progreso: List<ProgresoRutina>): ResumenSemanal {
    val ahora = Instant.now()
    val hace7Dias = ahora.minus(7, ChronoUnit.DAYS)

    val recientes = progreso.filter {
        try {
            // Asegúrate de que ProgresoRutina.fecha es un String parseable a Instant
            // Si es un Timestamp de Firestore, ya lo manejas bien en guardarProgresoRutina
            // Si ya es un String ISO, está bien.
            val fecha = Instant.parse(it.fecha)
            fecha.isAfter(hace7Dias)
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear fecha del progreso: ${it.fecha}", e)
            false
        }
    }

    if (recientes.isEmpty()) {
        // Si no hay rutinas recientes, devuelve un resumen vacío o con ceros.
        return ResumenSemanal() // Gracias a los valores por defecto en el data class
    }

    val totalRutinas = recientes.size
    val tiempoTotalSegundos = recientes.sumOf { it.tiempoTotal } // Ya lo tenías como totalTiempo

    val objetivos = recientes.flatMap { it.objetivos }
    val objetivosRepetidos = objetivos
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key }
        .take(2) // Tomar los 2 más frecuentes

    // --- NUEVA LÓGICA PARA CONTAR EJERCICIOS ---
    var totalEjercicios = 0
    var ejerciciosPorTiempo = 0
    var ejerciciosPorRepeticiones = 0

    recientes.forEach { rutinaProgreso ->
        // Sumar la cantidad de ejercicios en esta rutina al total
        totalEjercicios += rutinaProgreso.ejercicios.size

        // Iterar sobre cada ejercicio simple en la rutina actual
        rutinaProgreso.ejercicios.forEach { ejercicioSimple ->
            if (ejercicioSimple.repeticiones > 0) {
                ejerciciosPorRepeticiones++
            } else if (ejercicioSimple.duracionSegundos > 0) {
                // Considerar como "por tiempo" si tiene duración y no repeticiones (o repeticiones es 0)
                ejerciciosPorTiempo++
            }
            // Puedes añadir un 'else' aquí si quieres manejar ejercicios
            // que no tienen ni repeticiones ni duración (aunque no debería ocurrir
            // con EjercicioSimple tal como está definido)
        }
    }
    // --- FIN DE LA NUEVA LÓGICA ---

    return ResumenSemanal(
        rutinas = totalRutinas,
        tiempoTotal = tiempoTotalSegundos,
        objetivosRecurrentes = objetivosRepetidos,
        // --- NUEVOS CAMPOS ---
        totalEjercicios = totalEjercicios,
        ejerciciosPorTiempo = ejerciciosPorTiempo,
        ejerciciosPorRepeticiones = ejerciciosPorRepeticiones
    )
}
// En FirestoreUtils.kt - Asumiendo que el usuario puede seleccionar VARIOS lugares
fun obtenerRutinas(
    nivel: String? = null, // Nivel del usuario (String)
    objetivos: List<String>? = null, // Objetivos del usuario (List<String>)
    lugaresEntrenamiento: List<LugarEntrenamiento>? = null, // Lista de Enum de lugares del usuario
    onResult: (List<Rutina>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var query: Query = db.collection("rutinas")
    var filtroServidorAplicadoParaNivel = false
    // var filtroServidorAplicadoParaLugar = false // Ya tienes filtroLugarEnServidor
    // var filtroServidorAplicadoParaObjetivo = false

    // --- Filtrado en Servidor (Optimización) ---
    if (nivel != null) {
        query = query.whereArrayContains("nivelRecomendado", nivel)
        filtroServidorAplicadoParaNivel = true
        Log.d(TAG, "Filtrando en servidor por nivel: $nivel")
    }

    val nombresLugaresUsuario = lugaresEntrenamiento?.map { it.name } ?: emptyList()

    // Solo podemos usar un 'array-contains' en la consulta.
    // Prioridad: Nivel, luego Lugar, luego Objetivo para el filtro de servidor.
    if (!filtroServidorAplicadoParaNivel && nombresLugaresUsuario.isNotEmpty()) {
        query = query.whereArrayContains("lugarEntrenamiento", nombresLugaresUsuario.first())
        // filtroServidorAplicadoParaLugar = true; // No necesitas esta variable si solo la usas aquí
        Log.d(TAG, "Filtrando en servidor por el primer lugarEntrenamiento: ${nombresLugaresUsuario.first()}")
    } else if (!filtroServidorAplicadoParaNivel && (objetivos != null && objetivos.isNotEmpty())) { // else if para asegurar solo un filtro de array
        query = query.whereArrayContains("objetivos", objetivos.first())
        // filtroServidorAplicadoParaObjetivo = true;
        Log.d(TAG, "Filtrando en servidor por primer objetivo: ${objetivos.first()}")
    }

    query.get()
        .addOnSuccessListener { result ->
            val rutinasDesdeFirestore = result.documents.mapNotNull { document ->
                document.toObject(RutinaFirestore::class.java)?.copy(id = document.id)
            }

            val rutinasFiltradasFinal = rutinasDesdeFirestore.filter { rutinaFirestore ->
                // --- Filtrado en Cliente (Lógica Final y Correcta) ---

                // Nivel: La rutina debe incluir el nivel del usuario (si se especifica)
                val pasaFiltroNivel = nivel == null ||
                        rutinaFirestore.nivelRecomendado.any { rn -> rn.equals(nivel, ignoreCase = true) }

                // Lugar: La rutina debe ser compatible con al menos uno de los lugares del usuario (si se especifica)
                val pasaFiltroLugar = nombresLugaresUsuario.isEmpty() ||
                        rutinaFirestore.lugarEntrenamiento.any { lugarRutina ->
                            nombresLugaresUsuario.any { lugarUsuario ->
                                lugarUsuario.equals(lugarRutina, ignoreCase = true)
                            }
                        }

                // Objetivos: La rutina debe abordar al menos uno de los objetivos del usuario (si se especifican)
                val pasaFiltroObjetivos = objetivos == null || objetivos.isEmpty() ||
                        rutinaFirestore.objetivos.any { objetivoRutina ->
                            objetivos.any { objetivoUsuario ->
                                objetivoUsuario.equals(objetivoRutina, ignoreCase = true)
                            }
                        }

                Log.d(TAG, "Evaluando rutina: ${rutinaFirestore.nombre} (ID: ${rutinaFirestore.id})")
                Log.d(TAG, "    Nivel Rutina: ${rutinaFirestore.nivelRecomendado}, Filtro Nivel Usuario: $nivel, Pasa Nivel: $pasaFiltroNivel")
                Log.d(TAG, "    Lugar Rutina: ${rutinaFirestore.lugarEntrenamiento}, Filtro Lugar Usuario: $nombresLugaresUsuario, Pasa Lugar: $pasaFiltroLugar")
                Log.d(TAG, "    Objetivos Rutina: ${rutinaFirestore.objetivos}, Filtro Objetivos Usuario: $objetivos, Pasa Objetivos: $pasaFiltroObjetivos")

                pasaFiltroNivel && pasaFiltroLugar && pasaFiltroObjetivos
            }

            Log.d(TAG, "Rutinas obtenidas desde Firestore: ${rutinasDesdeFirestore.size}, Rutinas filtradas final en cliente: ${rutinasFiltradasFinal.size}")

            val rutinasModeloFinal = rutinasFiltradasFinal.map { rf ->
                Rutina(
                    id = rf.id,
                    nombre = rf.nombre,
                    descripcion = rf.descripcion,
                    nivelRecomendado = rf.nivelRecomendado,
                    objetivos = rf.objetivos,
                    lugarEntrenamiento = rf.lugarEntrenamiento,
                    ejercicios = emptyList()
                )
            }
            onResult(rutinasModeloFinal)
        }
        .addOnFailureListener {
            Log.e(TAG, "Error al obtener rutinas Firestore: ${it.message}", it)
            onError(it.message ?: "Error al obtener rutinas filtradas")
        }
}
suspend fun getRutinaByIdFromFirestore(rutinaId: String): Rutina? {
    val db = FirebaseFirestore.getInstance()
    return try {
        // 1. Obtener el documento principal de la rutina
        val rutinaDocumentSnapshot = db.collection("rutinas")
            .document(rutinaId)
            .get()
            .await()

        if (!rutinaDocumentSnapshot.exists()) {
            Log.w(TAG, "Rutina con ID $rutinaId no encontrada en Firestore.")
            return null
        }

        // Mapear el documento principal a RutinaFirestore usando .toObject(Clase::class.java)
        val rutinaFirestore = rutinaDocumentSnapshot.toObject(RutinaFirestore::class.java)
            ?: run {
                Log.e(TAG, "Error al mapear documento de rutina $rutinaId a RutinaFirestore.")
                return null
            }

        // 2. Obtener los ejercicios de la subcolección
        val ejerciciosSnapshot = db.collection("rutinas")
            .document(rutinaId)
            .collection("ejercicios")
            // Opcional: Ordenar los ejercicios si tienes un campo 'orden'
            .orderBy("orden", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .await()

        // Mapear los documentos de ejercicios a EjercicioFirestore usando .toObjects(Clase::class.java)
        val ejerciciosFirestoreList = ejerciciosSnapshot.toObjects(EjercicioFirestore::class.java)

        // Mapear la lista de EjercicioFirestore a Ejercicio del modelo
        val ejercicios = ejerciciosFirestoreList.map { ejercicioFirestore ->
            // Mapear EjercicioFirestore a Ejercicio del modelo
            Ejercicio(
                id = ejercicioFirestore.id,
                nombre = ejercicioFirestore.nombre,
                descripcion = ejercicioFirestore.descripcion,
                imagenUrl = ejercicioFirestore.imagenUrl,
                videoUrl = ejercicioFirestore.videoUrl,
                duracionSegundos = ejercicioFirestore.duracionSegundos,
                repeticiones = ejercicioFirestore.repeticiones,
                series = ejercicioFirestore.series,
                // Mapea List<String> (Firestore) a List<Enum> (Modelo Ejercicio). Esto ya lo tenías y está bien.
                grupoMuscular = ejercicioFirestore.grupoMuscular.mapNotNull { try { GrupoMuscular.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { Log.w(TAG, "Grupo muscular desconocido: $it"); null } },
                equipamientoNecesario = ejercicioFirestore.equipamientoNecesario, // List<String> a List<String> (OK)
                // Mapea List<String> (Firestore) a List<String> (Modelo Ejercicio). Esto ya lo teníamos y está bien.
                lugarEntrenamiento = ejercicioFirestore.lugarEntrenamiento, // Asigna directamente la lista de Strings
                // Asegúrate de que tu data class Ejercicio tenga el campo 'orden'
                orden = ejercicioFirestore.orden
            )
        }

        // 3. Construir el objeto Rutina completo con los ejercicios cargados
        val rutinaCompleta = Rutina(
            id = rutinaFirestore.id,
            nombre = rutinaFirestore.nombre,
            descripcion = rutinaFirestore.descripcion,
            nivelRecomendado = rutinaFirestore.nivelRecomendado,
            objetivos = rutinaFirestore.objetivos,
            // Mapea List<String> (Firestore) a List<String> (Modelo Rutina). Esto ya lo teníamos y está bien.
            lugarEntrenamiento = rutinaFirestore.lugarEntrenamiento, // Asigna directamente la lista de Strings
            ejercicios = ejercicios // Asignar la lista de ejercicios cargados
        )

        rutinaCompleta

    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener rutina con ID $rutinaId desde Firestore con ejercicios.", e)
        // Decide si quieres propagar la excepción o devolver null
        throw e
        // O return null
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




