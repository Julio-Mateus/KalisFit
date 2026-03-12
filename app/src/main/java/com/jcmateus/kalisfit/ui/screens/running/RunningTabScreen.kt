package com.jcmateus.kalisfit.ui.screens.running

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
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
    navController: NavHostController,
    runningViewModel: RunningViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val activityState by runningViewModel.activityState.collectAsState()
    val elapsedTimeSeconds by runningViewModel.elapsedTimeSeconds.collectAsState()
    val distanceKm by runningViewModel.distanceKm.collectAsState()
    val currentPace by runningViewModel.currentPace.collectAsState()
    val avgPace by runningViewModel.avgPace.collectAsState()
    val caloriesBurned by runningViewModel.caloriesBurned.collectAsState()
    val routePoints by runningViewModel.routePoints.collectAsState()
    val currentLocation by runningViewModel.currentLocation.collectAsState()
    val hasLocationPermission by runningViewModel.hasLocationPermission.collectAsState()
    val splits by runningViewModel.splits.collectAsState()

    val currentView = LocalView.current
    DisposableEffect(activityState) {
        val isRunning = activityState == ActivityState.RUNNING
        currentView.keepScreenOn = isRunning
        onDispose { currentView.keepScreenOn = false }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> runningViewModel.updateLocationPermission(isGranted) }
    )

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else { true }
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) { Log.w("RunningTabScreen", "Permiso de notificación denegado.") }
        }
    )

    LaunchedEffect(hasLocationPermission, activityState) {
        if (!hasLocationPermission && activityState == ActivityState.IDLE) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasLocationPermission && activityState == ActivityState.IDLE) {
            PermissionRationale(
                icon = Icons.Outlined.LocationOn,
                title = stringResource(R.string.running_location_permission_rationale_title),
                message = stringResource(R.string.running_location_permission_rationale_message),
                onRequestPermission = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            (activityState == ActivityState.IDLE || activityState == ActivityState.PAUSED) &&
            hasLocationPermission
        ) {
            PermissionRationale(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.running_notification_permission_rationale_title),
                message = stringResource(R.string.running_notification_permission_rationale_message),
                onRequestPermission = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
        } else if (activityState == ActivityState.FINISHED) {
            ActivitySummary(
                distanceKm = distanceKm,
                elapsedTimeSeconds = elapsedTimeSeconds,
                avgPace = avgPace,
                caloriesBurned = caloriesBurned,
                routePoints = routePoints,
                splits = splits,
                onDone = { runningViewModel.onSummaryDone() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp), // Padding inferior general para los controles
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Padding horizontal para el mapa y las métricas superpuestas
                ) {
                    MapAndMetricsSection(
                        modifier = Modifier.fillMaxSize(),
                        routePoints = routePoints,
                        currentLocation = currentLocation,
                        activityState = activityState,
                        hasLocationPermission = hasLocationPermission
                    )

                    if (hasLocationPermission) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .padding(vertical = 12.dp)
                        ) {
                            LiveMetrics(
                                elapsedTimeSeconds = elapsedTimeSeconds,
                                distanceKm = distanceKm,
                                currentPace = currentPace,
                                caloriesBurned = caloriesBurned,
                                // Comenta/descomenta esta línea para probar el override de color:
                                // textColorOverride = if (isDarkTheme) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            runningViewModel.onResumeClicked()
                        }
                    },
                    onStop = { /* La lógica de onStop ahora está dentro de ActivityControls */ },
                    runningViewModel = runningViewModel
                )
            }
        }
    }
}
// --- Componentes Auxiliares Actualizados ---
@Composable
fun PermissionRationale(
    icon: ImageVector, // Recibe el ImageVector directamente (ej. Icons.Outlined.LocationOn)
    title: String,
    message: String,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize() // Ocupa todo el espacio disponible si es el único contenido
            .background(MaterialTheme.colorScheme.surface) // Fondo por si el de la pantalla es diferente
            .padding(horizontal = 28.dp, vertical = 48.dp), // Mayor padding para centrarlo más
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // El título y mensaje ya describen el propósito
            modifier = Modifier.size(72.dp), // Icono más grande y notable
            tint = MaterialTheme.colorScheme.primary // Usar el color primario del tema
        )
        Spacer(modifier = Modifier.height(24.dp)) // Más espacio
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall, // Título un poco más prominente
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface, // Color de texto principal
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge, // Cuerpo de texto claro y legible
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Color de texto secundario para el mensaje
            modifier = Modifier.padding(bottom = 32.dp) // Más espacio antes del botón
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth(0.85f) // El botón ocupa un buen porcentaje del ancho, pero no todo
                .height(50.dp), // Altura estándar para botones
            shape = RoundedCornerShape(12.dp) // Bordes redondeados consistentes
        ) {
            Text(
                stringResource(R.string.grant_permission_button), // Asumo que tienes este string
                style = MaterialTheme.typography.labelLarge // Estilo de texto para botones
            )
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
    // Estilos de mapa (tu lógica existente es buena, asegúrate que los archivos R.raw existan)
    val mapStyleOptions = remember(context, isDarkTheme) {
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
                    CameraUpdateFactory.newLatLngZoom(it, 17f), // Zoom un poco más cercano para el seguimiento
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
                    1000 // Duración de la animación
                )
            } catch (e: IllegalStateException) {
                // Esto puede ocurrir si los puntos son idénticos o muy cercanos.
                Log.e("MapAndMetricsSection", "Error creating LatLngBounds for map view: ${e.message}")
                // Fallback: centrar en el último punto conocido o la ubicación actual.
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
    Card( // Envolver el mapa en una Card para elevación y bordes redondeados
        modifier = modifier
            .fillMaxWidth()
            // Podrías querer que esta Card no tenga padding superior si se alinea con la parte superior de la pantalla
            // y que los bordes inferiores sean redondeados si hay contenido debajo.
            // Ejemplo: .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            // O simplemente bordes redondeados en todas las esquinas:
            .clip(RoundedCornerShape(16.dp)), // Aplicar clip para que el contenido del mapa también respete los bordes
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Elevación sutil
        shape = RoundedCornerShape(16.dp) // Bordes redondeados para la Card
    ) {
        Box(modifier = Modifier.fillMaxSize()) { // Box para poder superponer el indicador de carga si es necesario
            if (hasLocationPermission) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true, // Muestra el punto azul de ubicación actual
                        mapType = MapType.NORMAL,
                        mapStyleOptions = mapStyleOptions,
                        isBuildingEnabled = false, // Considera deshabilitar edificios para un look más limpio
                        isTrafficEnabled = false // El tráfico puede saturar la vista durante una carrera
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false, // Deshabilitar para un look más limpio, el usuario puede hacer pinch-to-zoom
                        myLocationButtonEnabled = false, // Usamos un botón propio de recentrado para una UX consistente
                        mapToolbarEnabled = false, // La barra de herramientas de Google Maps no suele ser necesaria aquí
                        compassEnabled = activityState != ActivityState.RUNNING, // Mostrar brújula solo si no está corriendo y el mapa no está fijo
                        scrollGesturesEnabled = activityState != ActivityState.RUNNING, // Permitir scroll solo si no está corriendo activamente
                        zoomGesturesEnabled = activityState != ActivityState.RUNNING, // Permitir zoom solo si no está corriendo
                        tiltGesturesEnabled = false, // El tilt puede ser confuso durante una carrera y consume recursos
                        rotationGesturesEnabled = activityState != ActivityState.RUNNING // Permitir rotación solo si no está corriendo
                    )
                ) {
                    // Dibujar la polilínea de la ruta(doble capa para mejor presencia visual)
                    if (routePoints.size >= 2) {
                        // Capa base tipo "halo" para que resalte sobre cualquier estilo de mapa
                        Polyline(
                            points = routePoints,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), // Usar el color primario del tema
                            width = 24f, // Polilínea un poco más gruesa y visible
                            geodesic = true, // Líneas curvas que representan mejor las distancias en un esferoide
                            startCap = RoundCap(), // Extremos de la línea redondeados
                            endCap = RoundCap(),
                            jointType = JointType.ROUND, // Uniones entre segmentos redondeadas
                            zIndex = 0.8f // Asegurar que la polilínea esté sobre otros elementos si es necesario
                        )
                        // Capa principal de la ruta
                        Polyline(
                            points = routePoints,
                            color = MaterialTheme.colorScheme.primary,
                            width = 14f,
                            geodesic = true,
                            startCap = RoundCap(),
                            endCap = RoundCap(),
                            jointType = JointType.ROUND,
                            zIndex = 1f
                        )
                    }

                    // Marcadores de inicio y fin (tu lógica actual está bien)
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

                if (hasLocationPermission && currentLocation != null){
                    FloatingActionButton(
                        onClick = {
                            val focusPoint = routePoints.lastOrNull() ?: currentLocation
                            focusPoint?.let{
                                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 17f))
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ){
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = stringResource(R.string.running_recenter_map)
                        )
                    }
                }

                // Indicador de "Esperando ubicación" mejorado, superpuesto sobre el mapa
                // Se muestra si no hay ubicación, no hay puntos de ruta, y la actividad NO está IDLE (ej. recién iniciada)
                if (currentLocation == null && routePoints.isEmpty() && activityState != ActivityState.IDLE && activityState != ActivityState.FINISHED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)), // Fondo semitransparente para legibilidad
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.running_waiting_for_location),
                                style = MaterialTheme.typography.titleSmall, // Texto claro
                                color = MaterialTheme.colorScheme.onSurface // Color que contraste con el fondo semitransparente
                            )
                        }
                    }
                }

            } else {
                // Este Box se muestra si NO hay permiso de ubicación pero la actividad ha comenzado
                // (por ejemplo, si el permiso se revoca mientras la app está en segundo plano y luego se vuelve a abrir).
                // Es un caso menos común si la lógica de permisos al inicio y al reanudar es robusta.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // Fondo distintivo
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.running_location_needed_for_map_active), // String diferente para este caso
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    textColorOverride: Color = Color.Unspecified // Nuevo parámetro opcional
    // heartRate: Int?
) {
    Column(
        modifier = Modifier.fillMaxWidth()
        // El padding horizontal se maneja ahora en el Box contenedor en RunningTabScreen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricItem(
                label = stringResource(R.string.running_metric_time).uppercase(),
                value = formatElapsedTime(elapsedTimeSeconds),
                icon = Icons.Outlined.Timer,
                textColorOverride = textColorOverride
            )
            MetricItem(
                label = stringResource(R.string.running_metric_distance).uppercase(),
                value = "%.2f km".format(distanceKm),
                icon = Icons.Outlined.Route,
                textColorOverride = textColorOverride
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricItem(
                label = stringResource(R.string.running_metric_pace).uppercase(),
                value = currentPace,
                icon = Icons.Outlined.Speed,
                textColorOverride = textColorOverride
            )
            MetricItem(
                label = stringResource(R.string.running_metric_calories).uppercase(),
                value = "$caloriesBurned kcal",
                icon = Icons.Outlined.LocalFireDepartment,
                textColorOverride = textColorOverride
            )
            // TODO: Añadir HeartRate si es necesario, pasando textColorOverride
        }
    }
}
@Composable
fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColorOverride: Color = Color.Unspecified
) {
    val finalLabelColor = if (textColorOverride != Color.Unspecified) textColorOverride.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val finalValueColor = if (textColorOverride != Color.Unspecified) textColorOverride else MaterialTheme.colorScheme.onSurface
    val finalIconColor = if (textColorOverride != Color.Unspecified) textColorOverride.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary // Icono puede ser un poco más opaco que la etiqueta

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .width(IntrinsicSize.Max) // Ayuda a mantener un ancho consistente si es posible
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(30.dp),
                tint = finalIconColor
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = finalLabelColor,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = finalValueColor,
            maxLines = 1
        )
    }
}
@Composable
fun ActivityControls(
    activityState: ActivityState,
    onStart: () -> Unit,    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: (Boolean) -> Unit, // La lógica de si se guarda o no se maneja dentro del diálogo
    runningViewModel: RunningViewModel // Necesario para llamar a onStopClicked desde el diálogo
) {
    val haptic = LocalHapticFeedback.current
    var showStopConfirmationDialog by remember { mutableStateOf(false) }

    // Diálogo de Confirmación para Detener la Actividad
    if (showStopConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmationDialog = false },
            icon = {
                Icon(
                    Icons.Filled.WarningAmber, // Icono de advertencia estándar
                    contentDescription = null, // El título y texto ya describen la acción
                    tint = MaterialTheme.colorScheme.error // Tinte de error para el icono
                )
            },
            title = { Text(stringResource(R.string.running_confirm_stop_title)) },
            text = { Text(stringResource(R.string.running_confirm_stop_message)) },
            confirmButton = {
                Button( // Botón principal para la acción positiva (Guardar)
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        runningViewModel.onStopClicked(saveActivity = true)
                        showStopConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary // O un color que indique "guardar"
                    )
                ) { Text(stringResource(R.string.save_activity)) }
            },
            dismissButton = {
                TextButton( // TextButton para la acción de descarte, con color de error
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        runningViewModel.onStopClicked(saveActivity = false)
                        showStopConfirmationDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.discard_activity),
                        color = MaterialTheme.colorScheme.error // Color de texto de error
                    )
                }
            }
        )
    }

    // Tamaños estándar para los botones
    val mainButtonSize = 88.dp // Botón de acción principal más grande
    val secondaryButtonSize = 72.dp // Botones de acción secundaria

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp), // Mayor padding vertical alrededor de la fila de controles
        horizontalArrangement = Arrangement.SpaceEvenly, // Distribuye los botones uniformemente
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Estado: IDLE (Solo se muestra el botón de Iniciar)
        AnimatedVisibility(
            visible = activityState == ActivityState.IDLE,
            enter = fadeIn() + scaleIn(initialScale = 0.8f), // Animación de entrada
            exit = fadeOut() + scaleOut(targetScale = 0.8f)   // Animación de salida
        ) {
            Button( // Botón de inicio como acción primaria única
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStart() // Llama al lambda onStart pasado como parámetro
                },
                modifier = Modifier.size(mainButtonSize),
                shape = CircleShape, // Forma circular
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp), // Elevación para destacar
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Color primario
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.running_action_start),
                    modifier = Modifier.size(48.dp), // Icono grande y claro
                    tint = MaterialTheme.colorScheme.onPrimary // Asegurar contraste con el fondo del botón
                )
            }
        }

        // Estado: RUNNING (Se muestran Pausa y Detener)
        AnimatedVisibility(
            visible = activityState == ActivityState.RUNNING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), // Espacio entre botones
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón de Pausa (secundario, pero importante)
                OutlinedButton( // OutlinedButton para una acción secundaria clara
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPause()
                    },
                    modifier = Modifier.size(secondaryButtonSize),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary) // Borde con color primario
                ) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = stringResource(R.string.running_action_pause),
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary // Icono con color primario
                    )
                }

                // Botón de Stop (acción de finalización, con énfasis)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Feedback más fuerte
                        showStopConfirmationDialog = true // Mostrar diálogo de confirmación
                    },
                    modifier = Modifier.size(mainButtonSize), // Botón de Stop es principal aquí
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error, // Color de error para Stop
                        contentColor = MaterialTheme.colorScheme.onError // Color de contenido que contraste
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.running_action_stop),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Estado: PAUSED (Se muestran Reanudar y Detener)
        AnimatedVisibility(
            visible = activityState == ActivityState.PAUSED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón de Stop (cuando está pausado, puede ser ligeramente menos prominente que reanudar)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showStopConfirmationDialog = true
                    },
                    modifier = Modifier.size(secondaryButtonSize), // Tamaño secundario
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer, // Variante de color de error
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.running_action_stop),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Botón de Reanudar (acción primaria cuando está pausado)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onResume()
                    },
                    modifier = Modifier.size(mainButtonSize), // Tamaño principal
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow, // Mismo icono que Iniciar, representa "continuar"
                        contentDescription = stringResource(R.string.running_action_resume),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        // Cuando está FINISHED, no se muestran controles aquí; se muestra el ActivitySummary.
    }
}
@Composable
fun ActivitySummary(
    distanceKm: Double,
    elapsedTimeSeconds: Long,
    avgPace: String,
    caloriesBurned: Int,
    routePoints: List<LatLng>,
    splits: List<Split>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val mapStyleOptions = remember(context, isDarkTheme) {
        val styleResId = if (isDarkTheme) R.raw.map_style_dark else R.raw.map_style
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
                cameraPositionState.move( // Mover sin animación para que aparezca directamente centrado
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 120) // 120px padding
                )
            } catch (e: IllegalStateException) {
                routePoints.lastOrNull()?.let { lastPoint ->
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(lastPoint, 14f))
                }
            }
        } else if (routePoints.isNotEmpty()) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 14f))
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Fondo para el resumen
            .padding(horizontal = 16.dp), // Padding horizontal general
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp) // Padding en la parte superior e inferior de la lista
    ) {
        item {
            Text(
                stringResource(R.string.running_summary_title),
                style = MaterialTheme.typography.headlineMedium, // Título prominente
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp) // Mayor espaciado vertical
            )
        }
        // Mapa Estático de la Ruta
        if (routePoints.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f) // Ratio común para mapas
                        .padding(bottom = 24.dp), // Más espacio después del mapa
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), // Elevación sutil
                    shape = RoundedCornerShape(12.dp) // Bordes redondeados
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings( // Deshabilitar todas las interacciones
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            mapToolbarEnabled = false
                        ),
                        properties = MapProperties(
                            isMyLocationEnabled = false, // No mostrar el punto azul en el resumen
                            mapType = MapType.NORMAL,
                            mapStyleOptions = mapStyleOptions
                        )
                    ) {
                        if (routePoints.size >= 2) {
                            Polyline(
                                points = routePoints,
                                color = MaterialTheme.colorScheme.primary,
                                width = 12f // Un poco más delgada para el resumen
                            )
                        }
                        // Marcador de Inicio
                        routePoints.firstOrNull()?.let {
                            Marker(
                                state = rememberMarkerState(position = it),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                        // Marcador de Fin
                        if (routePoints.size > 1) {
                            routePoints.lastOrNull()?.let {
                                Marker(
                                    state = rememberMarkerState(position = it),
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                        }
                    }
                }
            }
        }
        // Métricas Principales (usando el MetricItem mejorado, pero sin iconos para un look más limpio en resumen)
        item {
            Text(
                stringResource(R.string.running_summary_overall_metrics_title).uppercase(), // "MÉTRICAS GENERALES"
                style = MaterialTheme.typography.titleSmall, // Título para la sección de métricas
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem( // Reutilizamos el MetricItem mejorado (sin pasar icono)
                    label = stringResource(R.string.running_metric_distance).uppercase(),
                    value = "%.2f km".format(distanceKm)
                )
                MetricItem(
                    label = stringResource(R.string.running_metric_time).uppercase(),
                    value = formatElapsedTime(elapsedTimeSeconds)
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = stringResource(R.string.running_metric_avg_pace).uppercase(),
                    value = avgPace
                )
                MetricItem(
                    label = stringResource(R.string.running_metric_calories).uppercase(),
                    value = "$caloriesBurned kcal"
                )
            }
            Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) // Separador
        }
        // Lista de Splits
        if (splits.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.running_summary_splits_title), // "Splits" o "Parciales"
                    style = MaterialTheme.typography.titleMedium, // Título para la sección de splits
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 8.dp), // Espaciado para el título de splits
                    textAlign = TextAlign.Start // Alinear a la izquierda
                )
            }
            itemsIndexed(splits) { index, split ->
                SplitItemRow(split = split, isLastItem = index == splits.size - 1)
                // El Divider está ahora dentro de SplitItemRow
            }
            item {
                Spacer(modifier = Modifier.height(24.dp)) // Espacio antes del botón "Done"
            }
        } else if (routePoints.isNotEmpty()) { // Mostrar si no hay splits pero sí hubo actividad
            item {
                Text(
                    stringResource(R.string.running_summary_no_splits),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Botón "Done" al final
        item {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Que no ocupe todo el ancho, pero sea generoso
                    .padding(vertical = 24.dp) // Buen padding vertical para el botón final
                    .height(52.dp), // Botón más alto y fácil de presionar
                shape = RoundedCornerShape(12.dp), // Bordes redondeados
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    stringResource(R.string.done).uppercase(),
                    style = MaterialTheme.typography.labelLarge, // Texto del botón
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
@Composable
fun SplitItemRow(split: Split, isLastItem: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) { // Padding horizontal para el contenido del split
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp), // Un poco más de padding vertical para cada split
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text( // KM
                text = stringResource(R.string.running_split_km_label, split.km), // "Km ${split.km}"
                style = MaterialTheme.typography.titleSmall, // Un poco más destacado
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.25f), // Peso para distribuir espacio
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text( // Tiempo
                text = formatElapsedTime(split.timeSeconds),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.4f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text( // Ritmo
                text = split.pace, // Asume que pace ya está formateado como "X:XX /km"
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold, // Destacar el ritmo
                color = MaterialTheme.colorScheme.primary, // Color primario para el ritmo
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.35f)
            )
        }
        if (!isLastItem) {
            Divider(
                thickness = 1.dp, // Ligeramente más grueso para una separación clara
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) // Divisor sutil
            )
        }
    }
}
// --- Funciones de Utilidad ---
// Esta función ya estaba bien, la incluyo por completitud aquí.
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


