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
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.sequences.ifEmpty
import kotlin.text.get
import kotlin.text.map
import kotlin.text.mapNotNull
import kotlin.text.set
import kotlin.text.uppercase
import kotlin.toString

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

@RequiresApi(Build.VERSION_CODES.O)
fun calcularResumenSemanal(progreso: List<ProgresoRutina>): ResumenSemanal {
    val ahora = Instant.now()
    val hace7Dias = ahora.minus(7, ChronoUnit.DAYS)

    val recientes = progreso.filter {
        try {
            val fecha = Instant.parse(it.fecha)
            fecha.isAfter(hace7Dias)
        } catch (e: Exception) {
            false
        }
    }

    val totalRutinas = recientes.size
    val totalTiempo = recientes.sumOf { it.tiempoTotal }
    val objetivos = recientes.flatMap { it.objetivos }

    val objetivosRepetidos = objetivos
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key }

    return ResumenSemanal(
        rutinas = totalRutinas,
        tiempoTotal = totalTiempo,
        objetivosRecurrentes = objetivosRepetidos.take(2)
    )
}
fun obtenerRutinas(
    nivel: String? = null,
    objetivos: List<String>? = null,
    lugarEntrenamiento: LugarEntrenamiento? = null, // Usamos el enum del modelo
    onResult: (List<Rutina>) -> Unit, // Devuelve List<Rutina> del modelo
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var query: Query = db.collection("rutinas")

    // Aplicar filtros si se proporcionan
    if (nivel != null) {
        query = query.whereArrayContains("nivelRecomendado", nivel)
    }

    if (objetivos != null && objetivos.isNotEmpty()) {
        // Filtrar por el primer objetivo en el servidor
        // Si necesitas filtrar por MÚLTIPLES objetivos estrictamente en el servidor,
        // considera índices compuestos o ajusta la lógica aquí.
        query = query.whereArrayContains("objetivos", objetivos.first())
    }

    if (lugarEntrenamiento != null) {
        // Para filtrar por un valor dentro de una lista (lugarEntrenamiento)
        query = query.whereArrayContains("lugarEntrenamiento", lugarEntrenamiento.name) // Firestore almacena en String
    }

    query.get()
        .addOnSuccessListener { result ->
            // Mapear los documentos a objetos Rutina (usando tu modelo)
            // IMPORTANTE: toObject(Rutina::class.java) aquí solo mapeará los campos
            // que existen en el documento principal de Firestore. NO CARGA la subcolección de ejercicios.
            val rutinas = result.documents.mapNotNull { document ->
                try {
                    // Intenta mapear el documento principal a tu data class Rutina
                    // Los campos como nivelRecomendado, objetivos, lugarEntrenamiento (si están como List<String> en Firestore)
                    // y nombre, descripcion, id se mapearán automáticamente.
                    // La lista 'ejercicios' en el objeto Rutina estará vacía porque no está en el documento principal.
                    document.toObject(Rutina::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    // Manejar errores de mapeo si es necesario
                    Log.e(TAG, "Error al mapear documento de rutina (en obtenerRutinas): ${e.message}")
                    null
                }
            }

            // Si se filtró solo por un objetivo, hacer el filtrado adicional en cliente
            val rutinasFiltradas = if (objetivos != null && objetivos.size > 1) {
                rutinas.filter { rutina ->
                    // Este filtro en cliente asume que 'objetivos' en tu objeto Rutina
                    // ya contiene los datos correctos cargados (lo cual no sucede
                    // si solo usas toObject en el documento principal).
                    // Si quieres que este filtro funcione correctamente, deberías
                    // haber cargado las rutinas de forma diferente, o filtrar
                    // completamente en cliente después de obtener todos los documentos.
                    objetivos.all { objetivo -> rutina.objetivos.contains(objetivo) }
                }
            } else {
                rutinas
            }

            // NOTA: Las rutinas devueltas aquí NO tendrán la lista de ejercicios cargada.
            // Si necesitas los ejercicios, debes obtener cada rutina individualmente
            // con getRutinaByIdFromFirestore después de obtener esta lista, o reestructurar
            // esta función para cargar los ejercicios.
            onResult(rutinasFiltradas)
        }
        .addOnFailureListener {
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

// Función para obtener una rutina específica por su ID desde Firestore
suspend fun addRutinaToFirestore(rutina: Rutina): Result<Unit> {
    val db = FirebaseFirestore.getInstance()
    val rutinaRef = db.collection("rutinas").document(rutina.id) // Usar el ID de la rutina como ID del documento

    return try {
        // 1. Crear el objeto RutinaFirestore para guardar en el documento principal
        // Mapeamos los campos relevantes de tu Rutina de modelo a RutinaFirestore
        val rutinaFirestoreData = RutinaFirestore(
            id = rutina.id,
            nombre = rutina.nombre,
            descripcion = rutina.descripcion,
            nivelRecomendado = rutina.nivelRecomendado,
            objetivos = rutina.objetivos,
            // ¡CAMBIO AQUI!: Asigna List<String> (Modelo Rutina) a List<String> (RutinaFirestore)
            lugarEntrenamiento = rutina.lugarEntrenamiento
        )

        // Guardar el documento principal de la rutina. SetOptions.merge() es útil si
        // el documento ya existe y solo quieres añadir o actualizar campos.
        rutinaRef.set(rutinaFirestoreData, SetOptions.merge()).await()

        // 2. Guardar cada ejercicio en la subcolección "ejercicios"
        val ejerciciosSubcollectionRef = rutinaRef.collection("ejercicios")

        // Usamos un batch para escribir todos los ejercicios de forma atómica (si es posible)
        // o simplemente añadimos/establecemos cada uno. Usar SetOptions.merge() permite
        // añadir ejercicios o actualizar existentes sin borrar la subcolección.
        val batch = db.batch()

        rutina.ejercicios.forEach { ejercicio ->
            // Mapear el Ejercicio del modelo a EjercicioFirestore
            val ejercicioFirestoreData = EjercicioFirestore(
                id = ejercicio.id.ifEmpty { ejerciciosSubcollectionRef.document().id }, // Genera un nuevo ID si está vacío
                nombre = ejercicio.nombre,
                descripcion = ejercicio.descripcion,
                imagenUrl = ejercicio.imagenUrl,
                videoUrl = ejercicio.videoUrl,
                duracionSegundos = ejercicio.duracionSegundos,
                repeticiones = ejercicio.repeticiones,
                series = ejercicio.series,
                // Mapea List<Enum> (Modelo Ejercicio) a List<String> (EjercicioFirestore). Esto ya lo tenías y está bien.
                grupoMuscular = ejercicio.grupoMuscular.map { it.name },
                equipamientoNecesario = ejercicio.equipamientoNecesario, // List<String> a List<String> (OK)
                // ¡CAMBIO AQUI!: Mapea List<String> (Modelo Ejercicio) a List<String> (EjercicioFirestore)
                lugarEntrenamiento = ejercicio.lugarEntrenamiento, // Asigna directamente la lista de Strings
                orden = ejercicio.orden
            )
            // Usa set() con merge para añadir o actualizar el documento del ejercicio
            batch.set(ejerciciosSubcollectionRef.document(ejercicioFirestoreData.id), ejercicioFirestoreData, SetOptions.merge())
        }

        // Ejecutar el batch de escrituras para los ejercicios
        batch.commit().await()

        Log.d(TAG, "Rutina y ejercicios guardados exitosamente en Firestore: ${rutina.id}")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Error al guardar rutina y ejercicios en Firestore: ${e.message}", e)
        Result.failure(e)
    }
}

suspend fun getRutinaById(rutinaId: String): Rutina? {
    val db = FirebaseFirestore.getInstance()
    return try {
        val documentSnapshot = db.collection("rutinas") // <-- Asegúrate de que tu colección se llama "rutinas"
            .document(rutinaId)
            .get()
            .await()

        if (documentSnapshot.exists()) {
            // Intenta mapear el documento a tu data class Rutina
            val rutina = documentSnapshot.toObject(Rutina::class.java)
            // Firestore no asigna el ID del documento automáticamente al objeto,
            // así que lo asignamos manualmente después de la conversión.
            rutina?.copy(id = documentSnapshot.id)
        } else {
            Log.w(TAG, "Rutina con ID $rutinaId no encontrada en Firestore.")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener rutina con ID $rutinaId desde Firestore.", e)
        throw e // Propaga la excepción para que el ViewModel pueda manejarla
    }
}
data class ResumenSemanal(
    val rutinas: Int,
    val tiempoTotal: Int,
    val objetivosRecurrentes: List<String>
)




