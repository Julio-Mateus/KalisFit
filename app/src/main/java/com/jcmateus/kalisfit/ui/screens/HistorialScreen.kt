package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jcmateus.kalisfit.data.ResumenSemanal
import com.jcmateus.kalisfit.model.ProgresoRutina
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import com.jcmateus.kalisfit.data.captureComposableAsImage
import com.jcmateus.kalisfit.model.UserActivity
import com.jcmateus.kalisfit.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import kotlin.text.format
import kotlin.time.Duration.Companion.seconds
import java.util.Locale


// Función de formato de tiempo (ya la tenías)
fun formatSecondsToMinutesSeconds(totalSeconds: Int): String {
    if (totalSeconds < 0) return "00:00"
    val duration = totalSeconds.seconds
    return duration.toComponents { minutes, seconds, _ ->
        String.format("%02d:%02d", minutes, seconds)
    }
}
fun formatSecondsToHMS(totalSeconds: Long): String {
    if (totalSeconds < 0) return "00:00:00"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

// --- Funciones para compartir Actividad Libre ---

fun buildActivityShareText(activity: UserActivity): String {
    val dateString = activity.timestamp?.let { activityDateFormatter.format(it) } ?: "Fecha desconocida"
    val activityType = if (activity.distanceKm > 2) "Carrera" else "Caminata"
    val iconActivity = if (activity.distanceKm > 2) "🏃‍♀️" else "🚶‍♂️" // O iconos más específicos

    return """
        ¡${iconActivity} Nueva actividad registrada con KalisFit! ${iconActivity}
        -----------------------------------
        📅 Fecha: $dateString
        ⏱️ Duración: ${formatSecondsToHMS(activity.elapsedTimeSeconds)}
        📏 Distancia: ${String.format(Locale.US, "%.2f km", activity.distanceKm)}
        👟 Ritmo Promedio: ${activity.avgPace}
        🔥 Calorías: ${activity.caloriesBurned} kcal
        -----------------------------------
        #KalisFit #ActividadFisica #$activityType #Fitness
    """.trimIndent()
}

@Composable
fun UserActivityVisualCard(
    activity: UserActivity,
    routePointsToDraw: List<LatLng> = emptyList() // Asumimos que LatLng aquí ES com.google.android.gms.maps.model.LatLng
) {
    val context = LocalContext.current
    val esTemaOscuro = isSystemInDarkTheme()
    val mapStyleOptions = remember(context, esTemaOscuro) {
        val resourceId = if (esTemaOscuro) R.raw.map_style_dark else R.raw.map_style // Asegúrate que estos R.raw existen
        try {
            MapStyleOptions.loadRawResourceStyle(context, resourceId)
        } catch (e: Exception) {
            Log.e("UserActivityVisualCard", "Error loading map style: ${e.message}")
            null
        }
    }

    // --- ¡¡¡PRUEBA DE COLORES ABSOLUTOS!!! ---
    val EXPECTED_CARD_BACKGROUND = Color(0xFFFFF0C9) // MustardPale
    val EXPECTED_ON_CARD_TEXT_COLOR = Color(0xFF1C1C1E) // TextPrimaryLight (casi negro)
    val EXPECTED_PRIMARY_COLOR_TITLE = Color(0xFFC2850B) // MustardDark (tu mostaza oscuro)
    val EXPECTED_SECONDARY_COLOR_ICON = Color(0xFF1976D2) // AccentBlue
    val EXPECTED_TERTIARY_COLOR_HASHTAG = Color(0xFF388E3C) // AccentGreen
    // --- FIN DE PRUEBA DE COLORES ABSOLUTOS ---

    // Usaremos estos colores forzados en lugar de los del tema para esta prueba
    val cardBackgroundColor = EXPECTED_CARD_BACKGROUND
    val onCardColor = EXPECTED_ON_CARD_TEXT_COLOR
    val primaryColor = EXPECTED_PRIMARY_COLOR_TITLE
    val secondaryColor = EXPECTED_SECONDARY_COLOR_ICON
    val tertiaryColor = EXPECTED_TERTIARY_COLOR_HASHTAG

    Log.d("VisualCardColors", "FORZADO - Fondo Tarjeta: $cardBackgroundColor")
    Log.d("VisualCardColors", "FORZADO - Texto Detalles: $onCardColor")
    Log.d("VisualCardColors", "FORZADO - Título/Polilínea: $primaryColor")

    Card(
        modifier = Modifier
            .width(380.dp) // Un poco más ancha para el logo y mejor espaciado
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Sin elevación para captura de imagen
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Encabezado con Logo ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                // horizontalArrangement = Arrangement.SpaceBetween // Lo ajustamos para que el título pueda crecer
            ) {
                // Reemplaza `R.drawable.ic_kalisfit_logo_small` con tu recurso de logo real
                // Si no tienes uno, puedes comentarlo temporalmente o usar un Icon de Material.
                Image(
                    painter = painterResource(id = R.drawable.ic_logo2), // ¡¡¡CAMBIA ESTO A TU LOGO!!!
                    contentDescription = "Logo KalisFit",
                    modifier = Modifier
                        .size(48.dp) // Ajusta el tamaño según tu logo
                        .padding(end = 12.dp) // Más espacio entre logo y texto
                )
                Column(modifier = Modifier.weight(1f)) { // Columna para título y fecha
                    Text(
                        "¡Mi Actividad!",
                        style = MaterialTheme.typography.titleLarge.copy(color = primaryColor),
                        textAlign = TextAlign.Start, // El título puede empezar desde la izquierda
                    )
                    Text(
                        text = activity.timestamp?.let { activityDateFormatter.format(it) } ?: "Fecha desconocida",
                        style = MaterialTheme.typography.bodyMedium.copy(color = onCardColor.copy(alpha = 0.8f)),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Más espacio después del encabezado

            // --- Mapa (si hay puntos) ---
            if (routePointsToDraw.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val cameraPositionState = rememberCameraPositionState()

                    LaunchedEffect(routePointsToDraw) {
                        if (routePointsToDraw.isNotEmpty()) { // Ya verificado afuera, pero bueno para la lógica interna
                            if (routePointsToDraw.size >= 2) {
                                val builder = LatLngBounds.builder()
                                routePointsToDraw.forEach { latLng ->
                                    builder.include(latLng)
                                }
                                try {
                                    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 50))
                                } catch (e: IllegalStateException) {
                                    Log.e("UserActivityVisualCard", "Error setting map bounds: ${e.message}. Falling back.")
                                    routePointsToDraw.firstOrNull()?.let {
                                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
                                    }
                                }
                            } else { // Solo un punto
                                routePointsToDraw.firstOrNull()?.let {
                                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
                                }
                            }
                        }
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
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
                        if (routePointsToDraw.size >= 2) {
                            Polyline(
                                points = routePointsToDraw,
                                color = primaryColor, // Usar color primario del tema
                                width = 10f
                            )
                        }
                        // Marcador de inicio (verde)
                        routePointsToDraw.firstOrNull()?.let { position ->
                            Marker(
                                state = rememberMarkerState(position = position),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                alpha = 0.9f
                            )
                        }
                        // Marcador de fin (rojo) solo si hay más de un punto y es diferente al primero
                        if (routePointsToDraw.size > 1 && routePointsToDraw.last() != routePointsToDraw.first()) {
                            routePointsToDraw.lastOrNull()?.let { position ->
                                Marker(
                                    state = rememberMarkerState(position = position),
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                                    alpha = 0.9f
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- Detalles de la Actividad ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsRun,
                    contentDescription = "Actividad",
                    tint = secondaryColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    InfoRowVisual("Duración:", formatSecondsToHMS(activity.elapsedTimeSeconds), onCardColor)
                    InfoRowVisual("Distancia:", String.format(Locale.US, "%.2f km", activity.distanceKm), onCardColor)
                    InfoRowVisual("Ritmo:", activity.avgPace, onCardColor)
                    InfoRowVisual("Calorías:", "${activity.caloriesBurned} kcal", onCardColor)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Hashtags y Nombre de App al Pie ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compartido desde KalisFit",
                    style = MaterialTheme.typography.labelSmall.copy(color = onCardColor.copy(alpha = 0.7f)),
                )
                Text(
                    "#KalisFit #${if (activity.distanceKm > 2) "Running" else "Walking"}", // Ajusta la lógica del hashtag si es necesario
                    style = MaterialTheme.typography.bodySmall.copy(color = tertiaryColor),
                )
            }
        }
    }
}

// Actualiza InfoRowVisual para aceptar un color y así asegurar la consistencia del tema
@Composable
fun InfoRowVisual(label: String, value: String, textColor: Color) { // Añadido textColor
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.45f), // Ligeramente ajustado el peso
            color = textColor.copy(alpha = 0.9f) // Usar el color pasado
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, // Valor un poco más destacado
            modifier = Modifier.weight(0.55f),
            color = textColor // Usar el color pasado
        )
    }
    Spacer(modifier = Modifier.height(6.dp)) // Un poco más de espacio
}

// Formateador de fecha para UserActivity
@SuppressLint("SimpleDateFormat")
private val activityDateFormatter = SimpleDateFormat("EEE, d MMM yyyy HH:mm",
    Locale.getDefault())


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistorialScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel()
    val historyState by viewModel.historyState.collectAsState()

    val historialRutinas = historyState.historialRutinas
    val resumenRutinas = historyState.resumenRutinas
    val isLoadingRutinas = historyState.isLoadingRutinas

    val historialActividadesLibres = historyState.historialActividadesLibres
    val isLoadingActividadesLibres = historyState.isLoadingActividadesLibres

    val errorMessage = historyState.errorMessage

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage() // Buena práctica para no mostrarlo repetidamente
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rutinas", "Carreras/Caminatas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Actividad") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Filled.FitnessCenter, contentDescription = "Historial de Rutinas")
                                1 -> Icon(Icons.Filled.DirectionsRun, contentDescription = "Historial de Carreras/Caminatas")
                            }
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> HistorialRutinasContent(
                    historialRutinas = historialRutinas,
                    resumenRutinas = resumenRutinas,
                    isLoading = isLoadingRutinas,
                    errorMessage = if (isLoadingRutinas || historialRutinas.isNotEmpty()) null else errorMessage,
                    navController = navController,
                    context = context,
                    onRetryLoadRutinas = { viewModel.loadRoutineHistory() } // Añadido callback de reintento
                )
                1 -> HistorialActividadesLibresContent(
                    historialActividades = historialActividadesLibres,
                    isLoading = isLoadingActividadesLibres,
                    errorMessage = if (isLoadingActividadesLibres || historialActividadesLibres.isNotEmpty()) null else errorMessage,
                    onDeleteActivity = { activityId ->
                        viewModel.deleteFreeActivity(activityId)
                    },
                    onRetryLoadActividades = { viewModel.loadFreeActivityHistory() }, // Añadido callback
                    context = context
                )
            }
        }
    }
}

// Composable para el contenido de la pestaña "Rutinas"
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistorialRutinasContent(
    historialRutinas: List<ProgresoRutina>,
    resumenRutinas: ResumenSemanal?,
    isLoading: Boolean,
    errorMessage: String?,
    navController: NavHostController,
    context: Context,
    onRetryLoadRutinas: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null && historialRutinas.isEmpty() && resumenRutinas == null) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error al cargar historial de rutinas: $errorMessage", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetryLoadRutinas) {
                    Text("Reintentar")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        resumenRutinas?.let { resumen ->
            ResumenSemanalCard(resumen = resumen, historialRutinas = historialRutinas, context = context)
        }

        if (historialRutinas.isEmpty() && resumenRutinas == null && !isLoading && errorMessage == null) {
            EmptyStateHistorial(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.historial_rutinas_vacio_titulo),
                subtitle = stringResource(R.string.historial_rutinas_vacio_subtitulo),
                buttonText = stringResource(R.string.comenzar_rutina),
                onButtonClick = {
                    // navController.navigate(Routes.RoutineExplorerScreen)
                }
            )
        } else if (historialRutinas.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = if (resumenRutinas == null) 16.dp else 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historialRutinas, key = { it.fecha + (it.nombreRutina ?: "") }) { progreso ->
                    ProgresoRutinaCard(progreso = progreso)
                }
            }
        }
    }
}

// Composable para la Card de Resumen Semanal (Extraído para claridad)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ResumenSemanalCard(
    resumen: ResumenSemanal,
    historialRutinas: List<ProgresoRutina>, // Para los gráficos
    context: Context
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedChartTab by remember { mutableStateOf(0) } // Estado para las sub-pestañas del gráfico

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 Resumen semanal de Rutinas", style = MaterialTheme.typography.titleLarge)
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Colapsar" else "Expandir"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("🏋️ Rutinas completadas: ${resumen.rutinas}", style = MaterialTheme.typography.bodyLarge)
            Text("⏱ Tiempo total entrenado: ${formatSecondsToMinutesSeconds(resumen.tiempoTotal)}", style = MaterialTheme.typography.bodyLarge)
            // Contenido expandible
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("🤸 Total ejercicios realizados: ${resumen.totalEjercicios}", style = MaterialTheme.typography.bodyLarge)
                    if (resumen.ejerciciosPorTiempo > 0) {
                        Text("⏱️ Ejercicios por tiempo: ${resumen.ejerciciosPorTiempo}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (resumen.ejerciciosPorRepeticiones > 0) {
                        Text("🔄 Ejercicios por repeticiones: ${resumen.ejerciciosPorRepeticiones}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (resumen.objetivosRecurrentes.isNotEmpty()) {
                        Text(
                            "🎯 Objetivos más frecuentes: ${resumen.objetivosRecurrentes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            // Gráficos (si tienes historial para ellos)
            if (historialRutinas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                TabRow(selectedTabIndex = selectedChartTab) {
                    Tab(selected = selectedChartTab == 0, onClick = { selectedChartTab = 0 }) {
                        Text("Rutinas/día", modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Tab(selected = selectedChartTab == 1, onClick = { selectedChartTab = 1 }) {
                        Text("Tiempo/día", modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (selectedChartTab) {
                        0 -> RutinasBarChart(historialRutinas, modifier = Modifier
                            .height(150.dp)
                            .padding(8.dp))
                        1 -> TiempoBarChart(historialRutinas, modifier = Modifier
                            .height(150.dp)
                            .padding(8.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No hay datos de historial de rutinas para mostrar gráficos.", textAlign = TextAlign.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp))
            }


            Spacer(modifier = Modifier.height(16.dp))
            // Botones de compartir (como los tenías)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(onClick = {
                    val mensaje = buildString { /* ... tu lógica de mensaje ... */ }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Compartir resumen semanal con...")
                    context.startActivity(shareIntent)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir Resumen")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Texto")
                }

                Button(onClick = {
                    captureComposableAsImage(context, { ResumenVisualCard(resumen = resumen) }) { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir imagen del resumen"))
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

// Composable para cada item en la lista de ProgresoRutina
@Composable
fun ProgresoRutinaCard(progreso: ProgresoRutina) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📅 ${progreso.fecha.take(10)} - ${progreso.nombreRutina}", // Incluye el nombre de la rutina
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
            progreso.ejercicios.take(5).forEach { ejercicio -> // Muestra solo los primeros 5 para brevedad
                val detalleEjercicio = if (ejercicio.repeticiones > 0) {
                    "${ejercicio.repeticiones} reps"
                } else if (ejercicio.duracionSegundos > 0) {
                    "${ejercicio.duracionSegundos}s"
                } else { "N/A" }
                Text("• ${ejercicio.nombre}: $detalleEjercicio", style = MaterialTheme.typography.bodySmall)
            }
            if (progreso.ejercicios.size > 5) {
                Text("... y ${progreso.ejercicios.size - 5} más.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "⏱️ Tiempo total: ${formatSecondsToMinutesSeconds(progreso.tiempoTotal)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


// NUEVO: Composable para el contenido de la pestaña "Actividades Libres" (Carreras/Caminatas)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistorialActividadesLibresContent(
    historialActividades: List<UserActivity>,
    isLoading: Boolean,
    errorMessage: String?,
    onDeleteActivity: (String?) -> Unit,
    onRetryLoadActividades: () -> Unit,
    context: Context
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator() // De Material 3
        }
        return
    }

    if (errorMessage != null && historialActividades.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text( // De Material 3
                    "Error al cargar historial de actividades: $errorMessage",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetryLoadActividades) { // De Material 3
                    Text("Reintentar") // De Material 3
                }
            }
        }
        return
    }

    if (historialActividades.isEmpty()) {
        EmptyStateHistorial( // Asumiendo que EmptyStateHistorial también usa solo M3
            modifier = Modifier.fillMaxSize(),
            title = stringResource(R.string.historial_actividades_vacio_titulo),
            subtitle = stringResource(R.string.historial_actividades_vacio_subtitulo),
            buttonText = stringResource(R.string.comenzar_actividad_libre),
            onButtonClick = { /* Lógica de navegación */ }
        )
    } else {
        LazyColumn( // De Foundation
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items( // De Foundation
                historialActividades,
                key = { it.id ?: it.timestamp.toString() }
            ) { activity ->
                var showDialog by remember { mutableStateOf(false) }
                val currentItemActivity by rememberUpdatedState(activity)

                val dismissState = rememberSwipeToDismissBoxState( // De Material 3
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart || // De Material 3
                            dismissValue == SwipeToDismissBoxValue.StartToEnd // De Material 3
                        ) {
                            showDialog = true
                            false
                        } else {
                            false
                        }
                    }
                )

                if (showDialog) {
                    AlertDialog( // De Material 3
                        onDismissRequest = { showDialog = false },
                        title = { Text("Confirmar Eliminación") }, // De Material 3
                        text = {
                            Text( // De Material 3
                                "¿Estás seguro de que quieres eliminar esta actividad del ${
                                    currentItemActivity.timestamp?.let { ts ->
                                        activityDateFormatter.format(ts)
                                    } ?: "historial"
                                }?"
                            )
                        },
                        confirmButton = {
                            Button( // De Material 3
                                onClick = {
                                    onDeleteActivity(currentItemActivity.id)
                                    showDialog = false
                                },
                                colors = ButtonDefaults.buttonColors( // De Material 3
                                    containerColor = MaterialTheme.colorScheme.error // De Material 3
                                )
                            ) { Text("Eliminar") } // De Material 3
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDialog = false }) { // De Material 3
                                Text("Cancelar") // De Material 3
                            }
                        }
                    )
                }

                SwipeToDismissBox( // De Material 3
                    state = dismissState,
                    modifier = Modifier.animateItemPlacement(tween(durationMillis = 300)), // De Foundation
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val direction = dismissState.dismissDirection
                        val color = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart ->
                                MaterialTheme.colorScheme.errorContainer // De Material 3
                            else -> Color.Transparent
                        }
                        val alignment = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = alignment
                        ) {
                            if (direction != null && direction != SwipeToDismissBoxValue.Settled) {
                                Icon( // De Material 3
                                    imageVector = Icons.Filled.Delete, // De Material Icons
                                    contentDescription = "Eliminar Actividad",
                                    tint = MaterialTheme.colorScheme.onErrorContainer // De Material 3
                                )
                            }
                        }
                    }
                ) {
                    UserActivityCard(activity = activity, context = context) // Asumiendo que UserActivityCard también usa solo M3
                }
            }
        }
    }
}


// NUEVO: Composable para mostrar cada UserActivity (carrera/caminata)
@Composable
fun UserActivityCard(activity: UserActivity, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.timestamp?.let { activityDateFormatter.format(it) } ?: "Fecha desconocida",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.DirectionsRun, // O un icono más específico si es caminata vs carrera
                    contentDescription = "Tipo de actividad",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow("Duración:", formatSecondsToHMS(activity.elapsedTimeSeconds))
            InfoRow("Distancia:", String.format(Locale.US, "%.2f km", activity.distanceKm))
            InfoRow("Ritmo Promedio:", activity.avgPace)
            InfoRow("Calorías Quemadas:", "${activity.caloriesBurned} kcal")

            // Opcional: Mostrar miniatura del mapa si tienes la URL
            // activity.mapImageUrl?.let { url ->
            //     Spacer(modifier = Modifier.height(8.dp))
            //     AsyncImage(
            //         model = url,
            //         contentDescription = "Mapa de la ruta",
            //         modifier = Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.small),
            //         contentScale = ContentScale.Crop
            //     )
            // }

            // Opcional: Mostrar algunos puntos de la ruta o un botón para ver el detalle
            // if (activity.routePoints.isNotEmpty()) {
            //     Text("Puntos de ruta: ${activity.routePoints.size}", style = MaterialTheme.typography.bodySmall)
            // }
            Spacer(modifier = Modifier.height(16.dp)) // Espacio antes de los botones

            // --- BOTONES DE COMPARTIR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End) // Alinea a la derecha
            ) {
                // Botón Compartir Texto
                OutlinedButton(onClick = {
                    val shareText = buildActivityShareText(activity)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Compartir actividad con...")
                    context.startActivity(shareIntent)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir como Texto")
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Texto")
                }

                // Botón Compartir Imagen
                Button(onClick = {
                    // --- ESTA ES LA PARTE IMPORTANTE A MODIFICAR EN UserActivityCard ---
                    // 1. Obtén y transforma los puntos de ruta si es necesario.
                    //    (Ajusta esto según la estructura de tu 'UserActivity' y 'LocationPoint')
                    val pointsForVisualCard: List<com.google.android.gms.maps.model.LatLng> =
                        activity.routePoints.map { locationPoint -> // Asumiendo que activity.routePoints es una lista de tu tipo LocationPoint
                            com.google.android.gms.maps.model.LatLng(locationPoint.latitude, locationPoint.longitude)
                        }
                    //    Si activity.routePoints ya es List<com.google.android.gms.maps.model.LatLng>,
                    //    entonces simplemente:
                    //    val pointsForVisualCard = activity.routePoints

                    captureComposableAsImage(
                        context = context,
                        // 2. Pasa los 'pointsForVisualCard' al UserActivityVisualCard
                        composable = {
                            UserActivityVisualCard(
                                activity = activity,
                                routePointsToDraw = pointsForVisualCard // <--- Aquí pasas los puntos
                            )
                        }
                    ) { imageFile -> // El callback con el File
                        val imageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider", // Asegúrate que este authority coincide con tu Manifest
                            imageFile
                        )
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            type = "image/png"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir imagen de actividad con..."))
                    }
                }) {
                    Icon(Icons.Filled.Image, contentDescription = "Compartir como Imagen")
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Imagen")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f) // Ajusta el peso según necesites
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}


// Composable genérico para estados vacíos (como lo tenías, pero más reutilizable)
@Composable
fun EmptyStateHistorial(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter, // Podrías cambiarlo según el contexto
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onButtonClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(buttonText)
            }
        }
    }
}