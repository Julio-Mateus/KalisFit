package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.data.ResumenSemanal
import com.jcmateus.kalisfit.data.calcularResumenSemanal
import com.jcmateus.kalisfit.data.obtenerHistorialProgreso
import com.jcmateus.kalisfit.model.ProgresoRutina
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.data.captureComposableAsImage
import com.jcmateus.kalisfit.viewmodel.HistoryViewModel
import kotlin.time.Duration.Companion.seconds


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistorialScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel()
    val historyState by viewModel.historyState.collectAsState()

    val historial = historyState.historial
    val resumen = historyState.resumen // Este 'resumen' ya debería tener los nuevos campos
    val cargando = historyState.isLoading
    val errorMessage = historyState.errorMessage

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            // viewModel.clearErrorMessage() // Descomenta si implementas esto
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    if (cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null && !cargando && historial.isEmpty() && resumen == null) { // Condición más precisa
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error al cargar historial: $errorMessage")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.loadHistory() }) {
                    Text("Reintentar")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) { // Agregado fillMaxSize para scrollability de toda la columna si es necesario
        resumen?.let { resumenSemanal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .padding(top = 16.dp),
                elevation = CardDefaults.cardElevation()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Resumen semanal", style = MaterialTheme.typography.titleLarge) // Título más grande
                    Spacer(modifier = Modifier.height(12.dp)) // Un poco más de espacio

                    Text("🏋️ Rutinas completadas: ${resumenSemanal.rutinas}", style = MaterialTheme.typography.bodyLarge)

                    // Usar la función de formato y mostrar los nuevos campos
                    Text("⏱ Tiempo total entrenado: ${formatSecondsToMinutesSeconds(resumenSemanal.tiempoTotal)}", style = MaterialTheme.typography.bodyLarge)

                    Text("🤸 Total ejercicios realizados: ${resumenSemanal.totalEjercicios}", style = MaterialTheme.typography.bodyLarge)

                    if (resumenSemanal.ejerciciosPorTiempo > 0) {
                        Text("⏱️ Ejercicios por tiempo: ${resumenSemanal.ejerciciosPorTiempo}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (resumenSemanal.ejerciciosPorRepeticiones > 0) {
                        Text("🔄 Ejercicios por repeticiones: ${resumenSemanal.ejerciciosPorRepeticiones}", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (resumenSemanal.objetivosRecurrentes.isNotEmpty()) {
                        Text(
                            "🎯 Objetivos más frecuentes: ${resumenSemanal.objetivosRecurrentes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Aumentado espacio
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("Rutinas/día", modifier = Modifier.padding(vertical = 12.dp)) // Más padding vertical
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("Tiempo/día", modifier = Modifier.padding(vertical = 12.dp)) // Más padding vertical
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (historial.isNotEmpty()) {
                            when (selectedTab) {
                                0 -> RutinasBarChart(
                                    historial,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .padding(8.dp)
                                )
                                1 -> TiempoBarChart(
                                    historial,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .padding(8.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth(), contentAlignment = Alignment.Center
                            ) {
                                Text("No hay datos de historial para mostrar gráficos.")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), // Para que los botones puedan usar más espacio si es necesario
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // Centrar botones si no ocupan todo el ancho
                    ) {
                        Button(onClick = {
                            val mensaje = buildString {
                                append("💪 ¡Mi resumen semanal de entrenamiento con KalisFit! 💪\n\n")
                                append("🏋️ Rutinas completadas: ${resumenSemanal.rutinas}\n")
                                append("⏱ Tiempo total entrenado: ${formatSecondsToMinutesSeconds(resumenSemanal.tiempoTotal)}\n")
                                append("🤸 Total ejercicios: ${resumenSemanal.totalEjercicios}\n")
                                if (resumenSemanal.ejerciciosPorTiempo > 0) {
                                    append("⏱️ Ejercicios por tiempo: ${resumenSemanal.ejerciciosPorTiempo}\n")
                                }
                                if (resumenSemanal.ejerciciosPorRepeticiones > 0) {
                                    append("🔄 Ejercicios por repeticiones: ${resumenSemanal.ejerciciosPorRepeticiones}\n")
                                }
                                if (resumenSemanal.objetivosRecurrentes.isNotEmpty()) {
                                    append(
                                        "🎯 Objetivos más trabajados: ${
                                            resumenSemanal.objetivosRecurrentes.joinToString(", ")
                                        }\n"
                                    )
                                }
                                append("\n¡Descarga KalisFit y entrena conmigo! 💥🔥 #KalisFit") // Añadir un hashtag
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, mensaje)
                                type = "text/plain"
                            }
                            val shareIntent =
                                Intent.createChooser(sendIntent, "Compartir resumen semanal con...")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir Resumen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Texto") // Más corto
                        }

                        Button(onClick = {
                            // Asumiendo que ResumenVisualCard está actualizado para tomar `ResumenSemanal`
                            // y mostrar los nuevos detalles.
                            captureComposableAsImage(context, {
                                ResumenVisualCard(resumen = resumenSemanal)
                            }) { file ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider", // Asegúrate que el provider esté bien configurado en el Manifest
                                    file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Compartir imagen del resumen")
                                )
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir como Imagen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imagen") // Más corto
                        }
                    }
                }
            }
        }

        if (historial.isEmpty() && resumen == null && !cargando && errorMessage == null) {
            // Caso donde no hay historial pero tampoco error, y no está cargando
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Aún no tienes historial de progreso. ¡Completa tu primera rutina!", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (historial.isNotEmpty()){ // Solo mostrar la lista si hay historial
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Ocupa el espacio restante
                    .fillMaxWidth(),
                // contentPadding añade espacio DENTRO del área de scroll del LazyColumn
                contentPadding = PaddingValues(
                    start = 16.dp,  // Padding a la izquierda de los items
                    end = 16.dp,    // Padding a la derecha de los items
                    // Padding superior si no hay resumen, o un pequeño espacio si sí hay
                    top = if (resumen == null) 16.dp else 0.dp,
                    bottom = 16.dp  // Padding en la parte inferior de la lista (antes de la Nav Bar)
                ),
                // Espaciado vertical ENTRE los items de la LazyColumn
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){ // weight(1f) si está dentro de una Columna padre con fillMaxSize y quieres que la lista ocupe el espacio restante
                items(historial) { progreso -> // .toList() no es necesario si historial ya es una List
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "📅 ${progreso.fecha.take(10)}", // Considerar formatear la fecha de forma más amigable
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Nivel: ${progreso.nivel}", style = MaterialTheme.typography.bodyLarge)
                            if (progreso.objetivos.isNotEmpty()) {
                                Text(
                                    "Objetivos: ${progreso.objetivos.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ejercicios (${progreso.ejercicios.size}):", style = MaterialTheme.typography.labelLarge)
                            progreso.ejercicios.forEach { ejercicio ->
                                val detalleEjercicio = if (ejercicio.repeticiones > 0) {
                                    "${ejercicio.repeticiones} reps"
                                } else if (ejercicio.duracionSegundos > 0) {
                                    "${ejercicio.duracionSegundos}s" // Más corto
                                } else {
                                    "N/A"
                                }
                                Text(
                                    "• ${ejercicio.nombre}: $detalleEjercicio",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "⏱️ Tiempo total: ${formatSecondsToMinutesSeconds(progreso.tiempoTotal)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
fun formatSecondsToMinutesSeconds(totalSeconds: Int): String {
    if (totalSeconds < 0) return "00:00" // O manejar el error como prefieras
    val duration = totalSeconds.seconds
    return duration.toComponents { minutes, seconds, _ ->
        String.format("%02d:%02d", minutes, seconds)
    }.toString()
}
