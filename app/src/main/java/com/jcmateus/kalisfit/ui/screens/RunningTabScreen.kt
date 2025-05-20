package com.jcmateus.kalisfit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.type.LatLng
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.jar.Manifest

// Estados de la actividad
enum class ActivityState {
    IDLE,       // Esperando para iniciar
    RUNNING,    // Actividad en curso
    PAUSED,     // Actividad pausada
    FINISHED    // Actividad finalizada (mostrando resumen)
}

@Composable
fun RunningTabScreen(
    // navController: NavHostController, // Para navegar si es necesario (ej. a detalles del historial)
    // runningViewModel: RunningViewModel = viewModel() // ViewModel para la lógica
) {
    val context = LocalContext.current
    var activityState by remember { mutableStateOf(ActivityState.IDLE) }

    // --- Lógica de Permisos de Ubicación ---
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (isGranted) {
                // Permiso concedido, podrías iniciar la obtención de ubicación si es necesario
            } else {
                // Permiso denegado, mostrar mensaje al usuario
            }
        }
    )

    // --- Datos de la Actividad (Placeholder - vendrán del ViewModel) ---
    var elapsedTimeSeconds by remember { mutableStateOf(0L) }
    val distanceKm by remember { mutableStateOf(0.0) } // Placeholder
    val currentPace by remember { mutableStateOf("0:00 /km") } // Placeholder
    val caloriesBurned by remember { mutableStateOf(0) } // Placeholder
    val routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) } // Puntos para el polyline

    // --- Simulación de Cronómetro (si está corriendo) ---
    LaunchedEffect(activityState) {
        if (activityState == ActivityState.RUNNING) {
            while (isActive) {
                delay(1000)
                elapsedTimeSeconds++
            }
        }
    }

    // --- Solicitar Permiso al inicio si no se tiene ---
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        // Puedes tener un TopAppBar aquí si esta pestaña necesita uno diferente al principal
        // o si el título debe ser dinámico (ej. "Corriendo...")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasLocationPermission && activityState == ActivityState.IDLE) {
                PermissionRationale(onRequestPermission = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                })
            } else if (activityState == ActivityState.FINISHED) {
                ActivitySummary(
                    distanceKm = distanceKm,
                    elapsedTimeSeconds = elapsedTimeSeconds,
                    avgPace = currentPace, // Debería ser ritmo promedio
                    caloriesBurned = caloriesBurned,
                    routePoints = routePoints,
                    onDone = { activityState = ActivityState.IDLE }
                )
            } else {
                // Vista principal: Mapa y Controles
                if (hasLocationPermission) {
                    MapAndMetricsSection(
                        modifier = Modifier.weight(1f),
                        routePoints = routePoints,
                        currentLocation = routePoints.lastOrNull() // O una ubicación más precisa del GPS
                    )
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.running_location_needed_for_map))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LiveMetrics(
                    elapsedTimeSeconds = elapsedTimeSeconds,
                    distanceKm = distanceKm,
                    currentPace = currentPace,
                    caloriesBurned = caloriesBurned
                )

                Spacer(modifier = Modifier.height(24.dp))

                ActivityControls(
                    activityState = activityState,
                    onStart = {
                        if (hasLocationPermission) {
                            activityState = ActivityState.RUNNING
                            elapsedTimeSeconds = 0 // Resetear
                            // TODO: Iniciar servicio de seguimiento de ubicación en ViewModel
                        } else {
                            // Solicitar permiso de nuevo o mostrar mensaje
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onPause = {
                        activityState = ActivityState.PAUSED
                        // TODO: Pausar servicio en ViewModel
                    },
                    onResume = {
                        activityState = ActivityState.RUNNING
                        // TODO: Reanudar servicio en ViewModel
                    },
                    onStop = {
                        activityState = ActivityState.FINISHED
                        // TODO: Detener servicio en ViewModel y guardar datos
                    }
                )
            }
        }
    }
}

// --- Componentes Auxiliares ---

@Composable
fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            stringResource(R.string.running_location_permission_rationale_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            stringResource(R.string.running_location_permission_rationale_message),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.grant_permission_button))
        }
    }
}

@Composable
fun MapAndMetricsSection(
    modifier: Modifier = Modifier,
    routePoints: List<LatLng>,
    currentLocation: LatLng?
) {
    // --- Configuración del Mapa ---
    // Necesitarás añadir la dependencia de Google Maps para Compose:
    // implementation "com.google.maps.android:maps-compose:X.Y.Z" (revisa la última versión)
    // Y configurar tu API Key de Google Maps en el AndroidManifest.xml
    val defaultCameraPosition = LatLng(currentLocation?.latitude ?: 40.7128, currentLocation?.longitude ?: -74.0060) // NY como fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 15f)
    }

    // Actualizar cámara cuando la ubicación actual cambia
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(it, 17f), // Zoom más cercano para seguimiento
                1000 // Duración de la animación en ms
            )
        }
    }


    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        if (currentLocation != null || routePoints.isNotEmpty()) {
            com.google.android.gms.maps.GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false, // Puedes habilitarlos si quieres
                    myLocationButtonEnabled = true // Permite al usuario centrar en su ubicación
                ),
                properties = MapProperties(
                    isMyLocationEnabled = true, // Muestra el punto azul de ubicación actual
                    mapType = MapType.NORMAL,
                )
            ) {
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = MaterialTheme.colorScheme.primary,
                        width = 15f
                    )
                }
                // Podrías añadir un Marker para el punto de inicio
                // routePoints.firstOrNull()?.let { Marker(state = MarkerState(position = it), title = "Inicio") }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.running_waiting_for_location))
            }
        }
    }
}

@Composable
fun LiveMetrics(
    elapsedTimeSeconds: Long,
    distanceKm: Double,
    currentPace: String,
    caloriesBurned: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        MetricItem(label = stringResource(R.string.running_metric_time), value = formatElapsedTime(elapsedTimeSeconds))
        MetricItem(label = stringResource(R.string.running_metric_distance), value = "%.2f km".format(distanceKm))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        MetricItem(label = stringResource(R.string.running_metric_pace), value = currentPace)
        MetricItem(label = stringResource(R.string.running_metric_calories), value = "$caloriesBurned kcal")
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActivityControls(
    activityState: ActivityState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (activityState) {
            ActivityState.IDLE -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(72.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.running_action_start), modifier = Modifier.size(36.dp))
                }
            }
            ActivityState.RUNNING -> {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.size(64.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.running_action_pause), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp)) // Espacio entre pausa y stop
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(72.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.running_action_stop), modifier = Modifier.size(36.dp))
                }
            }
            ActivityState.PAUSED -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.size(72.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.running_action_resume), modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(64.dp), // Un poco más pequeño para diferenciar de reanudar
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.running_action_stop), modifier = Modifier.size(32.dp))
                }
            }
            ActivityState.FINISHED -> {
                // No hay controles activos, se muestra el resumen
            }
        }
    }
}

@Composable
fun ActivitySummary(
    distanceKm: Double,
    elapsedTimeSeconds: Long,
    avgPace: String,
    caloriesBurned: Int,
    routePoints: List<LatLng>,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.running_summary_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

        // Aquí podrías mostrar un mapa estático de la ruta (más complejo, o una imagen)
        // Por ahora, solo las métricas:
        MetricItem(label = stringResource(R.string.running_metric_distance), value = "%.2f km".format(distanceKm))
        Spacer(modifier = Modifier.height(8.dp))
        MetricItem(label = stringResource(R.string.running_metric_time), value = formatElapsedTime(elapsedTimeSeconds))
        Spacer(modifier = Modifier.height(8.dp))
        MetricItem(label = stringResource(R.string.running_metric_avg_pace), value = avgPace) // Necesitarás calcular esto
        Spacer(modifier = Modifier.height(8.dp))
        MetricItem(label = stringResource(R.string.running_metric_calories), value = "$caloriesBurned kcal")

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onDone) {
            Text(stringResource(R.string.done))
        }
    }
}


// --- Funciones de Utilidad ---
fun formatElapsedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun RunningTabScreenIdlePreview() {
    KalisFitTheme {
        // Mockup para el preview, no necesitas ViewModel aquí
        val mockContext = LocalContext.current
        RunningTabScreen()
    }
}

