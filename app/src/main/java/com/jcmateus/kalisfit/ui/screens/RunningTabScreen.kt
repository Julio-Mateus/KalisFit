package com.jcmateus.kalisfit.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.ActivityState
import com.jcmateus.kalisfit.viewmodel.RunningViewModel
import com.jcmateus.kalisfit.viewmodel.Split


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningTabScreen(
    navController: NavHostController, // No se usa directamente aquí, pero útil para navegación futura
    runningViewModel: RunningViewModel = viewModel()
) {
    val context = LocalContext.current

    // Obtener estados y datos del ViewModel
    val activityState by runningViewModel.activityState.collectAsState()
    val elapsedTimeSeconds by runningViewModel.elapsedTimeSeconds.collectAsState()
    val distanceKm by runningViewModel.distanceKm.collectAsState()
    val currentPace by runningViewModel.currentPace.collectAsState()
    val avgPace by runningViewModel.avgPace.collectAsState() // Para el resumen
    val caloriesBurned by runningViewModel.caloriesBurned.collectAsState()
    val routePoints by runningViewModel.routePoints.collectAsState()
    val currentLocation by runningViewModel.currentLocation.collectAsState()
    val hasLocationPermission by runningViewModel.hasLocationPermission.collectAsState()
    val splits by runningViewModel.splits.collectAsState()
    // val heartRate by runningViewModel.heartRate.collectAsState() // TODO: Descomentar para FC

    // --- Gestión de pantalla encendida ---
    val currentView = LocalView.current
    DisposableEffect(activityState) {
        if (activityState == ActivityState.RUNNING) {
            currentView.keepScreenOn = true
        }
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // --- Lógica de Permisos de Ubicación ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            runningViewModel.updateLocationPermission(isGranted)
            // El ViewModel manejará las consecuencias
        }
    )

    // --- Lógica de Permisos de Notificación (para Foreground Service en Android 13+) ---
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // No se necesita para versiones anteriores
            }
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                // Informar al usuario que las notificaciones son importantes para el servicio en primer plano
                // Podrías mostrar un Snackbar
                Log.w("RunningTabScreen", "Permiso de notificación denegado.")
            }
        }
    )

    // Solicitar Permiso de ubicación al inicio si no se tiene y estamos IDLE
    LaunchedEffect(hasLocationPermission, activityState) {
        if (!hasLocationPermission && activityState == ActivityState.IDLE) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- (Opcional) Inicialización de TextToSpeech ---
    // val tts = remember { TextToSpeech(...) }
    // LaunchedEffect(Unit) { /* ... configurar tts ... */ }
    // DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    // El ViewModel podría tener un Flow para emitir mensajes a vocalizar


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding() // Padding del Scaffold
            .padding(16.dp), // Padding general adicional
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Razón del Permiso de Ubicación (si es necesario y estamos IDLE)
        if (!hasLocationPermission && activityState == ActivityState.IDLE) {
            PermissionRationale(
                icon = Icons.Filled.LocationOn,
                title = stringResource(R.string.running_location_permission_rationale_title),
                message = stringResource(R.string.running_location_permission_rationale_message),
                onRequestPermission = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )
        }
        // 2. Razón del Permiso de Notificación (si es necesario, estamos IDLE y no lo tenemos)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            (activityState == ActivityState.IDLE || activityState == ActivityState.PAUSED) && // Mostrar si está idle o quiere reanudar
            hasLocationPermission // Solo mostrar si ya tenemos permiso de ubicación
        ) {
            PermissionRationale(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.running_notification_permission_rationale_title),
                message = stringResource(R.string.running_notification_permission_rationale_message),
                onRequestPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }
        // 3. Resumen de la Actividad (si está FINALIZADA)
        else if (activityState == ActivityState.FINISHED) {
            ActivitySummary(
                distanceKm = distanceKm,
                elapsedTimeSeconds = elapsedTimeSeconds,
                avgPace = avgPace, // Usar el avgPace del ViewModel
                caloriesBurned = caloriesBurned,
                routePoints = routePoints,
                splits = splits, // Pasar los splits
                onDone = { runningViewModel.onSummaryDone() }
            )
        }
        // 4. Vista Principal de Actividad (Mapa, Métricas, Controles)
        else {
            // Mapa y Métricas
            if (hasLocationPermission) {
                MapAndMetricsSection(
                    modifier = Modifier.weight(1f), // Que ocupe el espacio disponible
                    routePoints = routePoints,
                    currentLocation = currentLocation,
                    activityState = activityState,
                    hasLocationPermission = hasLocationPermission
                )
            } else {
                // Mensaje si no hay permiso pero la actividad no está IDLE (ej. se revocó el permiso)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.running_location_needed_for_map))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LiveMetrics(
                elapsedTimeSeconds = elapsedTimeSeconds,
                distanceKm = distanceKm,
                currentPace = currentPace,
                caloriesBurned = caloriesBurned,
                // heartRate = heartRate // TODO: Descomentar para FC
            )

            Spacer(modifier = Modifier.height(24.dp))

            ActivityControls(
                activityState = activityState,
                onStart = {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        runningViewModel.onStartClicked()
                    }
                },
                onPause = { runningViewModel.onPauseClicked() },
                onResume = {
                    // Al reanudar, si se revocó el permiso de notificación, pedirlo de nuevo
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!hasLocationPermission) { // También chequear ubicación por si se revocó
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        runningViewModel.onResumeClicked()
                    }
                },
                onStop = { showConfirmDialog -> /* Ahora se maneja con diálogo */ }, // Modificado para diálogo
                runningViewModel = runningViewModel // Pasar el ViewModel para el diálogo de confirmación
            )
        }
    }
}


// --- Componentes Auxiliares Actualizados ---

@Composable
fun PermissionRationale(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
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
    currentLocation: LatLng?,
    activityState: ActivityState,
    hasLocationPermission: Boolean
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = remember(context, isDarkTheme) { // Recalcular si el contexto o el tema cambian
        val styleResId = if (isDarkTheme) {
            R.raw.map_style_dark // ASUME que tienes un archivo llamado "map_style_dark.json" en res/raw
        } else {
            R.raw.map_style // ASUME que tienes un archivo llamado "map_style_light.json" en res/raw
        }
        try {
            MapStyleOptions.loadRawResourceStyle(context, styleResId)
        } catch (e: Exception) {
            Log.e("MapAndMetricsSection", "Error loading map style for theme: ${e.message}")
            null // Fallback a estilo por defecto si hay error
        }
    }

    val defaultCameraPosition = remember { LatLng(40.7128, -74.0060) } // NY como fallback estático
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation ?: defaultCameraPosition, 15f)
    }

    // Centrar en ubicación actual cuando la actividad está RUNNING
    LaunchedEffect(currentLocation, activityState) {
        if (activityState == ActivityState.RUNNING) {
            currentLocation?.let {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(it, 17f),
                    700 // Duración de la animación un poco más rápida para seguimiento
                )
            }
        }
    }

    // Ajustar cámara para ver toda la ruta cuando la actividad NO está RUNNING y hay puntos
    LaunchedEffect(routePoints, activityState) {
        if (activityState != ActivityState.RUNNING && routePoints.size >= 2) {
            val builder = LatLngBounds.builder()
            routePoints.forEach { builder.include(it) }
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 100), // 100px padding
                    1000
                )
            } catch (e: IllegalStateException) {
                Log.e(
                    "MapAndMetricsSection",
                    "Error creating LatLngBounds for map view: ${e.message}"
                )
                routePoints.lastOrNull()?.let { lastPoint ->
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(lastPoint, 15f),
                        1000
                    )
                }
            }
        } else if (activityState != ActivityState.RUNNING && routePoints.isNotEmpty() && currentLocation != null) {
            // Si no hay ruta dibujada pero sí ubicación actual (ej. al pausar justo al inicio)
            // o si solo hay un punto en la ruta, centrar en la ubicación actual o ese punto.
            val pointToFocus = routePoints.lastOrNull() ?: currentLocation
            pointToFocus?.let {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f), 1000)
            }
        }
    }


    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        // Mostrar "Esperando ubicación" solo si no tenemos ni ruta ni ubicación actual
        if (currentLocation == null && routePoints.isEmpty() && activityState != ActivityState.IDLE) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.running_waiting_for_location))
                }
            }
        } else if (hasLocationPermission) { // Asegurarse de tener permiso para mostrar el mapa
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false, // Deshabilitar para un look más limpio, el usuario puede hacer pinch-to-zoom
                    myLocationButtonEnabled = true, // Permite al usuario recentrar
                    scrollGesturesEnabled = true, // Permitir scroll cuando no está corriendo
                    zoomGesturesEnabled = true, // Permitir zoom cuando no está corriendo
                    tiltGesturesEnabled = activityState != ActivityState.RUNNING, // Permitir tilt solo si no corre
                    scrollGesturesEnabledDuringRotateOrZoom = true
                ),
                properties = MapProperties(
                    isMyLocationEnabled = true, // Muestra el punto azul de ubicación actual
                    mapType = MapType.NORMAL,
                    mapStyleOptions = mapStyleOptions
                )
            ) {
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = MaterialTheme.colorScheme.primary,
                        width = 15f,
                        zIndex = 1f // Asegurar que la polilínea esté sobre otros elementos si es necesario
                    )
                }
                if (routePoints.isNotEmpty()) {
                    // Marcador de Inicio
                    Marker(
                        state = rememberMarkerState(position = routePoints.first()),
                        title = stringResource(R.string.start_point_marker_title),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                    // Marcador de Fin (solo si la actividad está pausada o terminada y hay más de un punto)
                    if ((activityState == ActivityState.PAUSED || activityState == ActivityState.FINISHED) && routePoints.size > 1) {
                        Marker(
                            state = rememberMarkerState(position = routePoints.last()),
                            title = stringResource(R.string.end_point_marker_title),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }
                }
            }
        } else { // Si no hay permiso y no estamos IDLE (caso raro, pero por si acaso)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.running_location_needed_for_map),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LiveMetrics(
    elapsedTimeSeconds: Long,
    distanceKm: Double,
    currentPace: String,
    caloriesBurned: Int,
    // heartRate: Int? // TODO: Descomentar para FC
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricItem(
                label = stringResource(R.string.running_metric_time),
                value = formatElapsedTime(elapsedTimeSeconds)
            )
            MetricItem(
                label = stringResource(R.string.running_metric_distance),
                value = "%.2f km".format(distanceKm)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricItem(label = stringResource(R.string.running_metric_pace), value = currentPace)
            MetricItem(
                label = stringResource(R.string.running_metric_calories),
                value = "$caloriesBurned kcal"
            )
            // TODO: Descomentar y ajustar para FC
            // MetricItem(
            //     label = stringResource(R.string.running_metric_heart_rate),
            //     value = heartRate?.toString() ?: "--" + " bpm"
            // )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium
        ) // Un poco más grande
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        ) // Un poco más pequeño que headlineSmall pero más grueso
    }
}

@Composable
fun ActivityControls(
    activityState: ActivityState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: (Boolean) -> Unit, // Modificado para pasar si se guarda o no
    runningViewModel: RunningViewModel // Necesario para el diálogo de confirmación
) {
    val haptic = LocalHapticFeedback.current
    var showStopConfirmationDialog by remember { mutableStateOf(false) }

    if (showStopConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmationDialog = false },
            title = { Text(stringResource(R.string.running_confirm_stop_title)) },
            text = { Text(stringResource(R.string.running_confirm_stop_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        runningViewModel.onStopClicked(saveActivity = true) // Llamar con saveActivity = true
                        showStopConfirmationDialog = false
                    }
                ) { Text(stringResource(R.string.save_activity)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        runningViewModel.onStopClicked(saveActivity = false) // Llamar con saveActivity = false
                        showStopConfirmationDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.discard_activity),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            icon = {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Usar AnimatedVisibility para transiciones suaves entre estados de botones
        AnimatedVisibility(
            visible = activityState == ActivityState.IDLE,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStart()
                },
                modifier = Modifier.size(80.dp), // Botón de inicio más grande
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.running_action_start),
                    modifier = Modifier.size(40.dp),
                    tint = Color.Black
                )
            }
        }

        AnimatedVisibility(
            visible = activityState == ActivityState.RUNNING,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Botón de Pausa (más pequeño, Outlined)
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPause()
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                ) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = stringResource(R.string.running_action_pause),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp)) // Más espacio
                // Botón de Stop (principal cuando está corriendo)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Feedback más fuerte para parar
                        showStopConfirmationDialog = true // Mostrar diálogo
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.running_action_stop),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = activityState == ActivityState.PAUSED,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Botón de Stop (más pequeño cuando está pausado)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showStopConfirmationDialog = true // Mostrar diálogo
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.running_action_stop),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                // Botón de Reanudar (principal cuando está pausado)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onResume()
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.running_action_resume),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        // Cuando está FINISHED, no se muestran controles aquí, se muestra el ActivitySummary.
    }
}

@Composable
fun ActivitySummary(
    distanceKm: Double,
    elapsedTimeSeconds: Long,
    avgPace: String,
    caloriesBurned: Int,
    routePoints: List<LatLng>,
    splits: List<Split>, // Asegúrate que el import sea correcto
    onDone: () -> Unit
    // Los botones de Guardar/Descartar se manejan antes de llegar aquí (en el diálogo de ActivityControls)
    // onDone es para cerrar la pantalla de resumen.
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = remember(context, isDarkTheme) { // <<< MODIFICAR CLAVES
        val styleResId = if (isDarkTheme) { // <<< AÑADIR LÓGICA
            R.raw.map_style_dark // Reemplaza con tu archivo de estilo oscuro
        } else {
            R.raw.map_style // Reemplaza con tu archivo de estilo claro
        }
        try {
            MapStyleOptions.loadRawResourceStyle(context, styleResId)
        } catch (e: Exception) {
            Log.e("ActivitySummary", "Error loading map style for theme: ${e.message}")
            null
        }
    }
    val cameraPositionState = rememberCameraPositionState()

    // Ajustar cámara para ver toda la ruta una vez que los puntos estén disponibles
    LaunchedEffect(routePoints) {
        if (routePoints.size >= 2) {
            val builder = LatLngBounds.builder()
            routePoints.forEach { builder.include(it) }
            try {
                // Mover sin animación para que aparezca directamente centrado
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngBounds(
                        builder.build(),
                        120
                    )
                ) // 120px padding
            } catch (e: IllegalStateException) {
                // Log.e("ActivitySummaryMap", "Error creating LatLngBounds for summary map: ${e.message}")
                routePoints.lastOrNull()?.let { lastPoint ->
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(lastPoint, 14f))
                }
            }
        } else if (routePoints.isNotEmpty()) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 14f))
        }
    }

    LazyColumn( // Usar LazyColumn para scroll si el contenido es largo (especialmente los splits)
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ), // Reducir padding vertical para más espacio
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                stringResource(R.string.running_summary_title),
                style = MaterialTheme.typography.headlineMedium, // Ajustado a Medium como antes
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp) // Añadir padding superior
            )
        }

        // Mapa Estático de la Ruta
        if (routePoints.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f) // Ratio común para mapas
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Sutil elevación
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings( // Deshabilitar interacciones
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            mapToolbarEnabled = false
                        ),
                        properties = MapProperties(
                            isMyLocationEnabled = false,
                            mapType = MapType.NORMAL,
                            mapStyleOptions = mapStyleOptions
                        )
                    ) {
                        if (routePoints.size >= 2) {
                            Polyline(
                                points = routePoints,
                                color = MaterialTheme.colorScheme.primary,
                                width = 10f // Un poco más delgada para el resumen
                            )
                        }
                        // Marcador de Inicio
                        Marker(
                            state = rememberMarkerState(position = routePoints.first()),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )
                        // Marcador de Fin
                        if (routePoints.size > 1) {
                            Marker(
                                state = rememberMarkerState(position = routePoints.last()),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                }
            }
        }

        // Métricas Principales en dos filas
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp), // Menos padding vertical entre métricas
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = stringResource(R.string.running_metric_distance),
                    value = "%.2f km".format(distanceKm)
                )
                MetricItem(
                    label = stringResource(R.string.running_metric_time),
                    value = formatElapsedTime(elapsedTimeSeconds)
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 4.dp,
                        bottom = 12.dp
                    ), // Un poco más de padding antes de la siguiente sección
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = stringResource(R.string.running_metric_avg_pace),
                    value = avgPace
                )
                MetricItem(
                    label = stringResource(R.string.running_metric_calories),
                    value = "$caloriesBurned kcal"
                )
            }
            Divider(modifier = Modifier.padding(bottom = 12.dp))
        }


        // Lista de Splits
        if (splits.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.running_summary_splits_title), // Necesitarás este string
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            itemsIndexed(splits) { index, split ->
                SplitItemRow(split = split, isLastItem = index == splits.size - 1)
                if (index < splits.size - 1) { // No añadir Divider después del último item
                    // Divider(modifier = Modifier.padding(horizontal = 8.dp)) // Opcional: Divider entre splits
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp)) // Espacio antes del botón "Done"
            }
        } else if (routePoints.isNotEmpty()) { // Mostrar si no hay splits pero sí hubo actividad
            item {
                Text(
                    stringResource(R.string.running_summary_no_splits), // Necesitarás este string
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Botón "Done" al final
        item {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp) // Más padding para el botón final
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

@Composable
fun SplitItemRow(split: Split, isLastItem: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(
                    R.string.running_split_km_label,
                    split.km
                ), // "Km ${split.km}"
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatElapsedTime(split.timeSeconds),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = split.pace, // Asume que pace ya está formateado como "X:XX /km"
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary // Destacar el ritmo
            )
        }
        if (!isLastItem) {
            Divider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ) // Divisor más sutil
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


