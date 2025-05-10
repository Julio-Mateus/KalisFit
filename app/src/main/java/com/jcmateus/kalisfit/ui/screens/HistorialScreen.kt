package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.core.content.FileProvider
import com.jcmateus.kalisfit.data.captureComposableAsImage


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistorialScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val historial = remember { mutableStateListOf<ProgresoRutina>() }
    var resumen by remember { mutableStateOf<ResumenSemanal?>(null) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            obtenerHistorialProgreso(
                userId = userId,
                onResult = {
                    historial.clear()
                    historial.addAll(it)
                    resumen = calcularResumenSemanal(it)
                    cargando = false
                },
                onError = {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    cargando = false
                }
            )
        }
    }

    if (cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.padding(16.dp)) {
        resumen?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📅 Resumen semanal", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🏋️ Rutinas completadas: ${it.rutinas}")
                    Text("⏱ Tiempo total: ${it.tiempoTotal} segundos")
                    if (it.objetivosRecurrentes.isNotEmpty()) {
                        Text("🎯 Objetivos más frecuentes: ${it.objetivosRecurrentes.joinToString(", ")}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val mensaje = buildString {
                            append("💪 Esta semana entrené con KalisFit:\n")
                            append("🏋️ Rutinas completadas: ${it.rutinas}\n")
                            append("⏱ Tiempo total: ${it.tiempoTotal} segundos\n")
                            if (it.objetivosRecurrentes.isNotEmpty()) {
                                append("🎯 Objetivos más trabajados: ${it.objetivosRecurrentes.joinToString(", ")}\n")
                            }
                            append("\nDescarga la app y únete tú también. 💥🔥")
                        }

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, mensaje)
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, "Compartir resumen con...")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir resumen")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        resumen?.let { resumenSemanal ->
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

                                context.startActivity(Intent.createChooser(intent, "Compartir imagen del resumen"))
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir imagen")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir como imagen")
                    }
                }
            }
        }

        LazyColumn {
            items(historial.toList()) { progreso ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📅 ${progreso.fecha.take(10)}", style = MaterialTheme.typography.titleMedium)
                        Text("Nivel: ${progreso.nivel}", style = MaterialTheme.typography.bodyLarge)
                        Text("Objetivos: ${progreso.objetivos.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ejercicios:", style = MaterialTheme.typography.labelLarge)
                        progreso.ejercicios.forEach {
                            Text("• ${it.nombre} - ${it.duracion} seg", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("⏱️ Tiempo total: ${progreso.tiempoTotal} seg", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

    }
}
