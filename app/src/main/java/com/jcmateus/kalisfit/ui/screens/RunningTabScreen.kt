package com.jcmateus.kalisfit.ui.screens

import android.Manifest
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.ActivityState
import com.jcmateus.kalisfit.viewmodel.RunningViewModel



@Composable
fun RunningTabScreen(
    runningViewModel: RunningViewModel = viewModel() // Inyectar el ViewModel
) {
    val context = LocalContext.current // Sigue siendo útil para algunas cosas como strings

    // Obtener estados y datos del ViewModel
    val activityState by runningViewModel.activityState.collectAsState()
    val elapsedTimeSeconds by runningViewModel.elapsedTimeSeconds.collectAsState()
    val distanceKm by runningViewModel.distanceKm.collectAsState()
    val currentPace by runningViewModel.currentPace.collectAsState()
    val caloriesBurned by runningViewModel.caloriesBurned.collectAsState()
    val routePoints by runningViewModel.routePoints.collectAsState()
    val currentLocation by runningViewModel.currentLocation.collectAsState() // Para centrar el mapa
    val hasLocationPermission by runningViewModel.hasLocationPermission.collectAsState()


    // --- Lógica de Permisos de Ubicación ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            runningViewModel.updateLocationPermission(isGranted)
            if (isGranted) {
                // El ViewModel manejará el inicio de la obtención de ubicación si es necesario
            } else {
                // Permiso denegado, podrías mostrar un Snackbar o mensaje
                // Por ahora, el PermissionRationale se mostrará si no hay permiso y está IDLE
            }
        }
    )

    // --- Solicitar Permiso al inicio si no se tiene ---
    // Este LaunchedEffect podría ser redundante si el ViewModel ya chequea y la UI reacciona
    // Pero es bueno para el primer lanzamiento.
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        // ... (TopAppBar si lo necesitas)
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
                    avgPace = currentPace, // Al final, currentPace será el promedio
                    caloriesBurned = caloriesBurned,
                    routePoints = routePoints,
                    onDone = { runningViewModel.onSummaryDone() }
                )
            } else {
                // Vista principal: Mapa y Controles
                if (hasLocationPermission) {
                    MapAndMetricsSection(
                        modifier = Modifier.weight(1f),
                        routePoints = routePoints,
                        currentLocation = currentLocation // Usar la ubicación del ViewModel
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
                            runningViewModel.onStartClicked()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onPause = { runningViewModel.onPauseClicked() },
                    onResume = { runningViewModel.onResumeClicked() },
                    onStop = { runningViewModel.onStopClicked() }
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
    routePoints: List<LatLng>, // De com.google.android.gms.maps.model.LatLng
    currentLocation: LatLng?  // De com.google.android.gms.maps.model.LatLng
) {
    // --- Configuración del Mapa ---
    val defaultCameraPosition = LatLng(currentLocation?.latitude ?: 40.7128, currentLocation?.longitude ?: -74.0060) // NY como fallback
    val cameraPositionState = rememberCameraPositionState { // De com.google.maps.android.compose
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(defaultCameraPosition, 15f)
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
            // Se usa GoogleMap de com.google.maps.android.compose (gracias al import)
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings( // De com.google.maps.android.compose
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true // Permite al usuario centrar en su ubicación
                ),
                properties = MapProperties( // De com.google.maps.android.compose
                    isMyLocationEnabled = true, // Muestra el punto azul de ubicación actual
                    mapType = MapType.NORMAL,   // De com.google.maps.android.compose
                )
            ) { // Contenido @Composable del GoogleMap
                if (routePoints.size >= 2) {
                    // Se usa Polyline de com.google.maps.android.compose (gracias al import)
                    Polyline(
                        points = routePoints,
                        color = MaterialTheme.colorScheme.primary,
                        width = 15f
                    )
                }
                // Ejemplo para añadir un Marker (necesitarías importar Marker y rememberMarkerState
                // de com.google.maps.android.compose):
                // routePoints.firstOrNull()?.let {
                //     val markerState = com.google.maps.android.compose.rememberMarkerState(position = it)
                //     com.google.maps.android.compose.Marker(
                //         state = markerState,
                //         title = stringResource(R.string.start_point_marker_title) // Ejemplo de string resource
                //     )
                // }
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


