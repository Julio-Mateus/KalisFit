package com.jcmateus.kalisfit.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.model.*
import com.jcmateus.kalisfit.viewmodel.UserProfile
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit

// =====================================================
// 1. MODELOS PARA FIRESTORE (DTOs) - RESTAURADOS COMPLETAMENTE
// =====================================================

data class ComponenteEjercicioFirestore(
    val nombreEspecifico: String? = null,
    val repeticiones: String? = null,
    val duracionSegundos: Long? = null,
    val orden: Long = 0,
    val imagenUrl: String? = null
)

data class EjercicioFirestore(
    var id: String = "",
    val nombre: String = "",
    val tipoEjercicio: String? = null,
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val imagenUrl1: String? = null,
    val imagenUrl2: String? = null,
    val videoUrl: String? = null,
    val duracionSegundos: Long = 0,
    val repeticiones: String = "0",
    val numeroDeSeries: Long = 0,
    val descansoEntreSeriesSegundos: Long = 0,
    val descansoDespuesEjercicioSegundos: Long = 0,
    val grupoMuscular: List<String> = emptyList(),
    val equipamientoNecesario: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(),
    var esUnilateral: Boolean = false,
    val orden: Double = 0.0,
    val componentes: List<ComponenteEjercicioFirestore> = emptyList(),
    val notaTempo: String? = null
)

data class EjercicioProgresoFirestore(
    val ejercicioIdOriginal: String = "",
    val nombre: String = "",
    val duracionPorSerieSegundos: Int = 0,
    val repeticionesPorSerie: Int = 0,
    val seriesRealizadas: Int = 0,
    val orden: Int = 0
)

data class ProgresoRutinaFirestore(
    val userId: String = "",
    val rutinaIdOriginal: String = "",
    val nombreRutina: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val nivelUsuarioAlCompletar: String = "",
    val objetivosUsuarioAlCompletar: List<String> = emptyList(),
    val ejerciciosCompletados: List<EjercicioProgresoFirestore> = emptyList(),
    val rondasRealizadas: Int = 0,
    val tiempoTotalSesionSegundos: Int = 0
)

data class RutinaFirestore(
    var id: String = "",
    val slug: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val nivelRecomendado: List<String> = emptyList(),
    val objetivos: List<String> = emptyList(),
    val lugarEntrenamiento: List<String> = emptyList(),
    val numeroDeRondas: Int = 1,
    val descansoEntreRondasSegundos: Int = 0
)

data class ResumenSemanal(
    val rutinas: Int = 0,
    val tiempoTotal: Int = 0,
    val objetivosRecurrentes: List<String> = emptyList(),
    val totalEjercicios: Int = 0,
    val ejerciciosPorTiempo: Int = 0,
    val ejerciciosPorRepeticiones: Int = 0
)

// Modelos Calistenia (DTOs)
data class CalisthenicsLevelFirestore(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val targetReps: String? = null,
    val targetSets: String? = null,
    val targetHoldTime: String? = null,
    val videoUrl: String? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    val formCues: List<String> = emptyList(),
    val commonMistakes: List<String> = emptyList(),
    val orden: Int = 0
)

data class CalisthenicsProgressionFirestore(
    val id: String = "",
    val name: String = "",
    val iconUrl: String? = null,
    val description: String? = null,
    val category: String = "General",
    val difficulty: String = "Principiante",
    val prerequisites: String? = null,
    val levels: List<CalisthenicsLevelFirestore> = emptyList()
)

private const val TAG = "FirestoreUtils"

// =====================================================
// 2. FUNCIONES DE RUTINAS E HISTORIAL - RESTAURADAS
// =====================================================

@RequiresApi(Build.VERSION_CODES.O)
fun guardarProgresoRutina(userIdAuth: String, rutinaRealizada: Rutina, perfilUsuarioActual: UserProfile, rondasCompletadasEnSesion: Int, tiempoTotalDeLaSesionSegundos: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val ejerciciosParaProgresoFirestore = rutinaRealizada.ejercicios.mapIndexed { index, ejercicioApp ->
        val repeticionesNumericas = ejercicioApp.repeticionesOriginal.substringBefore(" ").filter { it.isDigit() }.toIntOrNull() ?: 0
        EjercicioProgresoFirestore(ejercicioIdOriginal = ejercicioApp.id, nombre = ejercicioApp.nombre, duracionPorSerieSegundos = ejercicioApp.duracionSegundosOriginal, repeticionesPorSerie = repeticionesNumericas, seriesRealizadas = ejercicioApp.numeroDeSeries, orden = index)
    }
    val progresoFirestore = ProgresoRutinaFirestore(userId = userIdAuth, rutinaIdOriginal = rutinaRealizada.id, nombreRutina = rutinaRealizada.nombre, fecha = Timestamp.now(), nivelUsuarioAlCompletar = perfilUsuarioActual.nivel, objetivosUsuarioAlCompletar = perfilUsuarioActual.objetivos, ejerciciosCompletados = ejerciciosParaProgresoFirestore, rondasRealizadas = rondasCompletadasEnSesion, tiempoTotalSesionSegundos = tiempoTotalDeLaSesionSegundos)
    val batch = db.batch()
    val progresoRef = db.collection("users").document(userIdAuth).collection("progresoRutinas").document()
    val userRef = db.collection("users").document(userIdAuth)
    batch.set(progresoRef, progresoFirestore)
    batch.update(userRef, "rutinasCompletadas", FieldValue.increment(1))
    batch.commit().addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onError(e.message ?: "Error al guardar") }
}

fun obtenerHistorialProgreso(userId: String, onResult: (List<ProgresoRutina>) -> Unit, onError: (String) -> Unit) {
    FirebaseFirestore.getInstance().collection("users").document(userId).collection("progresoRutinas").orderBy("fecha", Query.Direction.DESCENDING).get()
        .addOnSuccessListener { result -> onResult(result.documents.mapNotNull { it.toObject(ProgresoRutina::class.java) }) }
        .addOnFailureListener { onError(it.message ?: "Error historial") }
}

fun calcularResumenParaSemanaEspecifica(progresosDeLaSemana: List<ProgresoRutinaFirestore>): ResumenSemanal {
    if (progresosDeLaSemana.isEmpty()) return ResumenSemanal()
    val totalRutinas = progresosDeLaSemana.size
    val tiempoTotalSegundos = progresosDeLaSemana.sumOf { it.tiempoTotalSesionSegundos }
    val objetivos = progresosDeLaSemana.flatMap { it.objetivosUsuarioAlCompletar }.filter { it.isNotBlank() }.groupingBy { it.trim().lowercase() }.eachCount().entries.sortedByDescending { it.value }.map { it.key.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString() } }.take(3)
    var totalEjerciciosCompletados = progresosDeLaSemana.sumOf { it.ejerciciosCompletados.size }
    var ejerciciosContadosPorTiempo = 0
    var ejerciciosContadosPorRepeticiones = 0
    progresosDeLaSemana.forEach { rp -> rp.ejerciciosCompletados.forEach { if (it.repeticionesPorSerie > 0) ejerciciosContadosPorRepeticiones += it.seriesRealizadas else if (it.duracionPorSerieSegundos > 0) ejerciciosContadosPorTiempo += it.seriesRealizadas } }
    return ResumenSemanal(totalRutinas, tiempoTotalSegundos, objetivos, totalEjerciciosCompletados, ejerciciosContadosPorTiempo, ejerciciosContadosPorRepeticiones)
}

@RequiresApi(Build.VERSION_CODES.O)
fun calcularResumenSemanal(historialProgreso: List<ProgresoRutinaFirestore>): ResumenSemanal {
    val ahora = Instant.now()
    val hace7Dias = ahora.minus(7, ChronoUnit.DAYS)
    val recientes = historialProgreso.filter { it.fecha.toDate().toInstant().isAfter(hace7Dias) }
    if (recientes.isEmpty()) return ResumenSemanal()
    return calcularResumenParaSemanaEspecifica(recientes)
}

fun obtenerRutinas(nivel: String? = null, objetivos: List<String>? = null, lugaresEntrenamiento: List<LugarEntrenamiento>? = null, onResult: (List<Rutina>) -> Unit, onError: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var query: Query = db.collection("rutinas")
    if (nivel != null) query = query.whereArrayContains("nivelRecomendado", nivel)

    query.get().addOnSuccessListener { result ->
        val rutinas = result.documents.mapNotNull { document ->
            val rf = document.toObject(RutinaFirestore::class.java)?.apply { id = document.id }
            rf?.let {
                val lugaresEnum = it.lugarEntrenamiento.mapNotNull { s -> try { LugarEntrenamiento.valueOf(s.uppercase()) } catch (e: Exception) { null } }
                Rutina(
                    id = it.id, slug = it.slug, nombre = it.nombre, descripcion = it.descripcion, imagenUrl = it.imagenUrl,
                    numeroDeRondas = it.numeroDeRondas, descansoEntreRondasSegundos = it.descansoEntreRondasSegundos,
                    nivelRecomendado = it.nivelRecomendado, objetivos = it.objetivos, lugarEntrenamiento = lugaresEnum, ejercicios = emptyList()
                )
            }
        }
        onResult(rutinas)
    }.addOnFailureListener { onError(it.message ?: "Error al obtener rutinas") }
}

fun parsearEjercicioFirestore(ef: EjercicioFirestore, gruposMuscularesEnum: List<GrupoMuscular>, lugaresEntrenamientoEnum: List<LugarEntrenamiento>): Ejercicio {
    var tipoFinal = TipoDeEjercicio.SIMPLE
    val componentesFinales = mutableListOf<ComponenteEjercicio>()
    var notaTempoDetectada: String? = null
    var esEjercicioUnilateralDetectado = ef.esUnilateral

    val nombreLimpio = ef.nombre
    val repeticionesOriginalString = ef.repeticiones.trim()

    val tempoRegex = """\(Tempo\s*([\d-]+)\)""".toRegex(RegexOption.IGNORE_CASE)
    tempoRegex.find(nombreLimpio)?.let {
        notaTempoDetectada = it.groupValues[1]
        tipoFinal = TipoDeEjercicio.CON_TEMPO
    }

    if (ef.tipoEjercicio == "SUPERSET_SECUENCIAL" && ef.componentes.isNotEmpty()) {
        tipoFinal = TipoDeEjercicio.SUPERSET_SEQUENCIAL
        ef.componentes.forEach { c -> componentesFinales.add(ComponenteEjercicio(nombreEspecifico = c.nombreEspecifico, repeticiones = c.repeticiones, duracionSegundos = c.duracionSegundos?.toInt(), orden = c.orden.toInt(), imagenUrl = c.imagenUrl)) }
    } else if (ef.tipoEjercicio == "CIRCUITO_TEMPORIZADO" && ef.componentes.isNotEmpty()) {
        tipoFinal = TipoDeEjercicio.CIRCUITO_TEMPORIZADO
        ef.componentes.forEach { c -> componentesFinales.add(ComponenteEjercicio(nombreEspecifico = c.nombreEspecifico, repeticiones = c.repeticiones, duracionSegundos = c.duracionSegundos?.toInt(), orden = c.orden.toInt(), imagenUrl = c.imagenUrl)) }
    } else if (componentesFinales.isEmpty()) {
        if (repeticionesOriginalString.contains(" + ")) {
            tipoFinal = TipoDeEjercicio.SUPERSET_SEQUENCIAL
            val partesRepeticiones = repeticionesOriginalString.split("+").map { it.trim() }
            partesRepeticiones.forEachIndexed { index, parteRep ->
                var duracionComp: Int? = null
                var repComp: String? = parteRep
                if (parteRep.endsWith("s", ignoreCase = true)) { duracionComp = parteRep.dropLast(1).toIntOrNull(); repComp = null }
                componentesFinales.add(ComponenteEjercicio(nombreEspecifico = "Parte ${index + 1}", repeticiones = repComp, duracionSegundos = duracionComp, orden = index))
            }
        } else if (repeticionesOriginalString.contains(" por lado", ignoreCase = true)) {
            esEjercicioUnilateralDetectado = true
            tipoFinal = TipoDeEjercicio.POR_LADO_ALTERNADO
        }
    }

    return Ejercicio(
        id = ef.id, nombre = nombreLimpio.replace(tempoRegex, "").trim(), descripcion = ef.descripcion, imagenUrl = ef.imagenUrl,
        imagenUrl1 = ef.imagenUrl1, imagenUrl2 = ef.imagenUrl2, videoUrl = ef.videoUrl, duracionSegundosOriginal = ef.duracionSegundos.toInt(),
        repeticionesOriginal = repeticionesOriginalString, numeroDeSeries = ef.numeroDeSeries.toInt(), descansoEntreSeriesSegundos = ef.descansoEntreSeriesSegundos.toInt(),
        descansoDespuesEjercicioSegundos = ef.descansoDespuesEjercicioSegundos.toInt(), grupoMuscular = gruposMuscularesEnum,
        equipamientoNecesario = ef.equipamientoNecesario, lugarEntrenamiento = lugaresEntrenamientoEnum, orden = ef.orden,
        tipoEjercicio = tipoFinal, componentes = componentesFinales, notaTempo = notaTempoDetectada, esUnilateral = esEjercicioUnilateralDetectado
    )
}

suspend fun getRutinaByIdFromFirestore(rutinaId: String): Rutina? {
    val db = FirebaseFirestore.getInstance()
    return try {
        val doc = db.collection("rutinas").document(rutinaId).get().await()
        val rf = doc.toObject(RutinaFirestore::class.java)?.apply { id = doc.id } ?: return null
        val snapshot = db.collection("rutinas").document(rutinaId).collection("ejercicios").orderBy("orden").get().await()
        val ejercicios = snapshot.documents.mapNotNull { d -> d.toObject(EjercicioFirestore::class.java)?.apply { id = d.id } }.map { ef ->
            val g = ef.grupoMuscular.mapNotNull { s -> try { GrupoMuscular.valueOf(s.trim().uppercase().replace(" ", "_")) } catch (e: Exception) { null } }
            val l = ef.lugarEntrenamiento.mapNotNull { s -> try { LugarEntrenamiento.valueOf(s.trim().uppercase()) } catch (e: Exception) { null } }
            parsearEjercicioFirestore(ef, g, l)
        }
        val lr = rf.lugarEntrenamiento.mapNotNull { try { LugarEntrenamiento.valueOf(it.trim().uppercase()) } catch (e: Exception) { null } }
        Rutina(id = rf.id, slug = rf.slug, nombre = rf.nombre, descripcion = rf.descripcion, imagenUrl = rf.imagenUrl, numeroDeRondas = rf.numeroDeRondas, descansoEntreRondasSegundos = rf.descansoEntreRondasSegundos, nivelRecomendado = rf.nivelRecomendado, objetivos = rf.objetivos, lugarEntrenamiento = lr, ejercicios = ejercicios)
    } catch (e: Exception) { null }
}

suspend fun getUserCustomRoutineById(userId: String, customRoutineId: String): UserCustomRoutine? {
    if (userId.isBlank() || customRoutineId.isBlank()) return null
    return try {
        val doc = FirebaseFirestore.getInstance().collection("users").document(userId).collection("customRoutines").document(customRoutineId).get().await()
        val routine = doc.toObject(UserCustomRoutine::class.java)
        routine?.id = doc.id
        routine
    } catch (e: Exception) { null }
}

suspend fun getUserCustomRoutines(userId: String): List<UserCustomRoutine> {
    if (userId.isBlank()) return emptyList()
    return try {
        val snapshot = FirebaseFirestore.getInstance().collection("users").document(userId).collection("customRoutines").orderBy("fechaUltimaModificacion", Query.Direction.DESCENDING).get().await()
        snapshot.documents.mapNotNull { it.toObject(UserCustomRoutine::class.java) }
    } catch (e: Exception) { emptyList() }
}

fun obtenerRutinasFlow(): Flow<List<Rutina>> = callbackFlow {
    val db = FirebaseFirestore.getInstance()
    val listener = db.collection("rutinas").addSnapshotListener { snapshot, e ->
        if (e != null) return@addSnapshotListener
        val rutinas = snapshot?.documents?.mapNotNull { document ->
            val rf = document.toObject(RutinaFirestore::class.java)?.apply { id = document.id }
            rf?.let {
                val leg = it.lugarEntrenamiento.mapNotNull { s -> try { LugarEntrenamiento.valueOf(s.uppercase()) } catch (e: Exception) { null } }
                Rutina(it.id, it.slug, it.nombre, it.descripcion, it.imagenUrl, it.numeroDeRondas, it.descansoEntreRondasSegundos, it.nivelRecomendado, it.objetivos, leg, emptyList())
            }
        } ?: emptyList()
        trySend(rutinas)
    }
    awaitClose { listener.remove() }
}

suspend fun saveOrUpdateUserCustomRoutine(userId: String, routine: UserCustomRoutine) {
    FirebaseFirestore.getInstance().collection("users").document(userId).collection("customRoutines").document(routine.id).set(routine).await()
}

// =====================================================
// 3. FUNCIONES DE CALISTENIA - INTEGRADAS
// =====================================================

suspend fun getAllCalisthenicsProgressions(): List<Progression> {
    val db = FirebaseFirestore.getInstance()
    val list = mutableListOf<Progression>()
    try {
        val snaps = db.collection("calisthenicsProgressions").get().await()
        for (doc in snaps.documents) {
            val pf = doc.toObject(CalisthenicsProgressionFirestore::class.java)
            if (pf != null) {
                val levels = pf.levels.map { lf -> ExerciseLevel(lf.id, lf.name, lf.description, lf.targetReps, lf.targetSets, lf.targetHoldTime, lf.videoUrl, lf.notes, lf.imageUrl, lf.formCues, lf.commonMistakes, lf.orden) }.sortedBy { it.order }
                list.add(Progression(pf.id.ifEmpty { doc.id }, pf.name, pf.iconUrl, pf.description, pf.category, pf.difficulty, pf.prerequisites, levels))
            }
        }
    } catch (e: Exception) { Log.e(TAG, "Error calistenia", e) }
    return list
}

suspend fun getCalisthenicsExerciseLevel(progressionId: String, levelId: String): ExerciseLevel? {
    val db = FirebaseFirestore.getInstance()
    return try {
        val doc = db.collection("calisthenicsProgressions").document(progressionId).get().await()
        val pf = doc.toObject(CalisthenicsProgressionFirestore::class.java)
        pf?.levels?.firstOrNull { it.id == levelId }?.let { lf ->
            ExerciseLevel(lf.id, lf.name, lf.description, lf.targetReps, lf.targetSets, lf.targetHoldTime, lf.videoUrl, lf.notes, lf.imageUrl, lf.formCues, lf.commonMistakes, lf.orden)
        }
    } catch (e: Exception) { null }
}
