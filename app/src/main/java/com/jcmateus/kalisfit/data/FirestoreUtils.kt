package com.jcmateus.kalisfit.data


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.EjercicioSimple
import com.jcmateus.kalisfit.model.ProgresoRutina
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
fun guardarProgresoRutina(
    userId: String,
    nivel: String,
    objetivos: List<String>,
    rutina: List<Ejercicio>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val progreso = ProgresoRutina(
        fecha = Timestamp.now().toDate().toInstant().toString(),
        nivel = nivel,
        objetivos = objetivos,
        ejercicios = rutina.map { EjercicioSimple(it.nombre, it.duracionSegundos) },
        tiempoTotal = rutina.sumOf { it.duracionSegundos }
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

data class ResumenSemanal(
    val rutinas: Int,
    val tiempoTotal: Int,
    val objetivosRecurrentes: List<String>
)




