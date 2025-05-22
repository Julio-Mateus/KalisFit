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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.captureComposableAsImage
import com.jcmateus.kalisfit.viewmodel.HistoryViewModel
import kotlin.time.Duration.Companion.seconds



// Asumiendo que esta es tu función de formato
fun formatSecondsToMinutesSeconds(totalSeconds: Int): String {
    if (totalSeconds < 0) return "00:00"
    val duration = totalSeconds.seconds
    return duration.toComponents { minutes, seconds, _ ->
        String.format("%02d:%02d", minutes, seconds)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar
@Composable
fun HistorialScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel()
    val historyState by viewModel.historyState.collectAsState()

    val historial = historyState.historial
    val resumen = historyState.resumen
    val cargando = historyState.isLoading
    val errorMessage = historyState.errorMessage

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            // viewModel.clearErrorMessage()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    // ------ INICIO DE LA MODIFICACIÓN ------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Actividad") }, // O usa stringResource
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) { // Acción para volver atrás
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back) // NECESITARÁS este string en strings.xml
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Opcional: personaliza colores
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding -> // El contenido de tu pantalla va aquí, usando innerPadding
        if (cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // Aplica padding
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold // Importante: salir del lambda de Scaffold
        }

        if (errorMessage != null && !cargando && historial.isEmpty() && resumen == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // Aplica padding
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error al cargar historial: $errorMessage")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadHistory() }) {
                        Text("Reintentar")
                    }
                }
            }
            return@Scaffold // Importante: salir del lambda de Scaffold
        }

        // El Column principal ahora usa el padding del Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding del Scaffold aquí
        ) {
            resumen?.let { resumenSemanal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Mantén paddings internos si los necesitas además del de Scaffold
                        .padding(bottom = 16.dp)
                        .padding(top = 16.dp), // Este top padding podría ajustarse si la TopAppBar ya da suficiente espacio
                    elevation = CardDefaults.cardElevation()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📊 Resumen semanal", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("🏋️ Rutinas completadas: ${resumenSemanal.rutinas}", style = MaterialTheme.typography.bodyLarge)
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

                        Spacer(modifier = Modifier.height(16.dp))
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                Text("Rutinas/día", modifier = Modifier.padding(vertical = 12.dp))
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("Tiempo/día", modifier = Modifier.padding(vertical = 12.dp))
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
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
                                    append("\n¡Descarga KalisFit y entrena conmigo! 💥🔥 #KalisFit")
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
                                Text("Texto")
                            }

                            Button(onClick = {
                                captureComposableAsImage(context, {
                                    ResumenVisualCard(resumen = resumenSemanal)
                                }) { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
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
                                Text("Imagen")
                            }
                        }
                    }
                }
            }

            if (historial.isEmpty() && resumen == null && !cargando && errorMessage == null) {
                Box(modifier = Modifier
                    .fillMaxSize() // Este fillMaxSize se aplicará al espacio restante después de la Card de resumen
                    .padding(16.dp), // Padding adicional si es necesario
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aún no tienes historial de progreso. ¡Completa tu primera rutina!", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (historial.isNotEmpty()){
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (resumen == null) 16.dp else 0.dp, // El padding superior podría ser 0 si la card de resumen ya da espacio
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ){
                    items(historial) { progreso ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "📅 ${progreso.fecha.take(10)}",
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
                                        "${ejercicio.duracionSegundos}s"
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
    // ------ FIN DE LA MODIFICACIÓN ------
}