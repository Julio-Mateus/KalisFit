package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistorialScreen(navController: NavHostController) {
    val context = LocalContext.current

    // Obtiene una instancia del ViewModel. Jetpack Compose gestionará su ciclo de vida.
    val viewModel: HistoryViewModel = viewModel()

    // Observar el estado de la UI expuesto por el ViewModel.
    // Cada vez que el estado del ViewModel cambie, la UI que depende de 'historyState' se recompondrá.
    val historyState by viewModel.historyState.collectAsState()

    // Extraer datos y estado directamente del estado del ViewModel
    val historial = historyState.historial
    val resumen = historyState.resumen
    val cargando = historyState.isLoading
    val errorMessage = historyState.errorMessage

    // Mostrar Toast si hay un mensaje de error del ViewModel
    // Usamos LaunchedEffect con 'errorMessage' como key para reaccionar solo cuando el mensaje cambia
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            // Si quisieras limpiar el mensaje de error después de mostrarlo,
            // deberías añadir una función `clearErrorMessage()` en tu ViewModel
            // y llamarla aquí: viewModel.clearErrorMessage()
        }
    }

    // El estado de la pestaña seleccionada sigue siendo local de la UI
    var selectedTab by remember { mutableStateOf(0) }

    // **Mostrar indicador de carga si es necesario**
    if (cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return // Es importante salir de la composición si solo queremos mostrar el indicador
    }

    // **Manejar el caso de error si no hay datos y hay un mensaje de error**
    // Si no está cargando y hay un error, puedes mostrar un mensaje en la pantalla
    if (errorMessage != null && !cargando && historial.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error al cargar historial: $errorMessage")
                Spacer(modifier = Modifier.height(8.dp))
                // Botón para reintentar cargar los datos
                Button(onClick = { viewModel.loadHistory() }) {
                    Text("Reintentar")
                }
            }
        }
        return // Salir si hay un error que impide mostrar la UI principal
    }

    // **UI Principal que muestra los datos si no está cargando ni hay error fatal**
    Column(modifier = Modifier.padding(16.dp)) {
        // Mostrar el resumen solo si existe
        resumen?.let { resumenSemanal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Resumen semanal", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🏋️ Rutinas completadas: ${resumenSemanal.rutinas}")
                    Text("⏱ Tiempo total: ${resumenSemanal.tiempoTotal} segundos")
                    if (resumenSemanal.objetivosRecurrentes.isNotEmpty()) {
                        Text(
                            "🎯 Objetivos más frecuentes: ${
                                resumenSemanal.objetivosRecurrentes.joinToString(
                                    ", "
                                )
                            }"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // TabRow para seleccionar entre Rutinas por día y Tiempo por día
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("Rutinas por día", modifier = Modifier.padding(8.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("Tiempo por día", modifier = Modifier.padding(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Superficie para mostrar los gráficos o mensajes si no hay historial
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (historial.isNotEmpty()) {
                            when (selectedTab) {
                                // Asegúrate de que RutinasBarChart y TiempoBarChart acepten List<ProgresoRutina>
                                0 -> RutinasBarChart(historial, modifier = Modifier
                                    .height(200.dp)
                                    .padding(8.dp))
                                1 -> TiempoBarChart(historial, modifier = Modifier
                                    .height(200.dp)
                                    .padding(8.dp))
                            }
                        } else {
                            // Mensaje si no hay historial para mostrar en los gráficos
                            Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No hay datos de historial para mostrar gráficos.")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones para compartir el resumen
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val mensaje = buildString {
                                append("💪 Esta semana entrené con KalisFit:\n")
                                append("🏋️ Rutinas completadas: ${resumenSemanal.rutinas}\n")
                                append("⏱ Tiempo total: ${resumenSemanal.tiempoTotal} segundos\n")
                                if (resumenSemanal.objetivosRecurrentes.isNotEmpty()) {
                                    append(
                                        "🎯 Objetivos más trabajados: ${
                                            resumenSemanal.objetivosRecurrentes.joinToString(
                                                ", "
                                            )
                                        }\n"
                                    )
                                }
                                append("\nDescarga la app y únete tú también. 💥🔥")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, mensaje)
                                type = "text/plain"
                            }

                            val shareIntent =
                                Intent.createChooser(sendIntent, "Compartir resumen con...")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resumen")
                        }

                        Button(onClick = {
                            // Lógica para compartir como imagen
                            captureComposableAsImage(context, {
                                // Este composable 'ResumenVisualCard' debe estar definido
                                // en alguna parte de tu proyecto. Asegúrate de que acepta
                                // el objeto ResumenSemanal si es necesario para mostrar el resumen visualmente.
                                // Aquí se usa 'resumenSemanal' directamente del ViewModel
                                ResumenVisualCard(resumen = resumenSemanal) // Asegúrate de que ResumenVisualCard existe
                            }) { file ->
                                // Una vez que la imagen se guarda en un archivo temporal,
                                // obtén su URI para compartirla.
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider", // Reemplaza con la autoridad de tu FileProvider
                                    file
                                )

                                // Crea un intent para compartir la imagen
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png" // Tipo MIME para una imagen PNG
                                    putExtra(Intent.EXTRA_STREAM, uri) // Adjunta la URI de la imagen
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Concede permisos de lectura a la app que reciba el intent
                                }

                                // Inicia el selector de aplicaciones para compartir
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        "Compartir imagen del resumen"
                                    )
                                )
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir imagen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Como imagen")
                        }
                    }
                }
            }
        }

        // Lista detallada del historial de rutinas
        LazyColumn {
            items(historial.toList()) { progreso ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📅 ${progreso.fecha.take(10)}", // Muestra solo la fecha (primeros 10 caracteres)
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Nivel: ${progreso.nivel}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Objetivos: ${progreso.objetivos.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ejercicios:", style = MaterialTheme.typography.labelLarge)
                        progreso.ejercicios.forEach {
                            Text(
                                "• ${it.nombre} - ${it.duracionSegundos} seg",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⏱️ Tiempo total: ${progreso.tiempoTotal} seg",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

