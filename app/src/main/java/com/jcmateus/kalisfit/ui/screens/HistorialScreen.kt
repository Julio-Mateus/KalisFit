package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
//import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.HistoryViewModel
import com.jcmateus.kalisfit.viewmodel.ResumenSemanalConFechas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
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
    val dateString =
        activity.timestamp?.let { activityDateFormatter.format(it) } ?: "Fecha desconocida"
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
        val resourceId =
            if (esTemaOscuro) R.raw.map_style_dark else R.raw.map_style // Asegúrate que estos R.raw existen
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
                        text = activity.timestamp?.let { activityDateFormatter.format(it) }
                            ?: "Fecha desconocida",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = onCardColor.copy(
                                alpha = 0.8f
                            )
                        ),
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
                        Log.d(
                            "UserActivityVisualCard",
                            "LaunchedEffect para mover cámara. Puntos: ${routePointsToDraw.size}"
                        )
                        if (routePointsToDraw.size >= 2) {
                            val builder = LatLngBounds.builder()
                            routePointsToDraw.forEach { latLng ->
                                builder.include(latLng)
                            }
                            try {
                                // Mover SIN animación para que sea más rápido para la captura
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngBounds(
                                        builder.build(),
                                        50
                                    )
                                ) // 50px padding
                                Log.d("UserActivityVisualCard", "Cámara movida a bounds.")
                            } catch (e: IllegalStateException) {
                                Log.e(
                                    "UserActivityVisualCard",
                                    "Error setting map bounds: ${e.message}. Fallback."
                                )
                                routePointsToDraw.firstOrNull()?.let {
                                    cameraPositionState.move(
                                        CameraUpdateFactory.newLatLngZoom(
                                            it,
                                            13f
                                        )
                                    )
                                }
                            }
                        } else if (routePointsToDraw.isNotEmpty()) { // Solo un punto
                            routePointsToDraw.firstOrNull()?.let {
                                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 13f))
                                Log.d("UserActivityVisualCard", "Cámara movida a un solo punto.")
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
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED
                                    ),
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
                    InfoRowVisual(
                        "Duración:",
                        formatSecondsToHMS(activity.elapsedTimeSeconds),
                        onCardColor
                    )
                    InfoRowVisual(
                        "Distancia:",
                        String.format(Locale.US, "%.2f km", activity.distanceKm),
                        onCardColor
                    )
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
private val activityDateFormatter = SimpleDateFormat(
    "EEE, d MMM yyyy HH:mm",
    Locale.getDefault()
)
private val resumenSemanalFechaFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistorialScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel()
    val historyState by viewModel.historyState.collectAsState()

    // Extraer los nuevos datos del estado
    val historialRutinas = historyState.historialRutinas
    val listaResumenesSemanales = historyState.listaResumenesSemanales // <- NUEVO
    val isLoadingRutinas = historyState.isLoadingRutinas
    val isLoadingResumenesSemanales = historyState.isLoadingResumenesSemanales // <- NUEVO

    val historialActividadesLibres = historyState.historialActividadesLibres
    val isLoadingActividadesLibres = historyState.isLoadingActividadesLibres

    val errorMessage = historyState.errorMessage

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = "Historial de Rutinas"
                                )

                                1 -> Icon(
                                    Icons.Filled.DirectionsRun,
                                    contentDescription = "Historial de Carreras/Caminatas"
                                )
                            }
                        }
                    )
                }
            }

            // Determinar el mensaje de error específico para la pestaña de Rutinas
            // Basado en si las rutinas o los resúmenes están cargando, o si hay un error y ambos están vacíos.
            val errorRutinasTab = when {
                (isLoadingRutinas || isLoadingResumenesSemanales) -> null // Si algo está cargando, no mostrar error aún
                errorMessage != null && historialRutinas.isEmpty() && listaResumenesSemanales.isEmpty() -> errorMessage
                else -> null
            }


            when (selectedTabIndex) {
                0 -> HistorialRutinasContent(
                    listaResumenesSemanales = listaResumenesSemanales, // <- CAMBIADO
                    historialRutinasCompleto = historialRutinas,      // <- Puedes pasar el historial completo si aún lo necesitas por separado, o eliminarlo si ResumenSemanalCard ya no lo usa directamente.
                    isLoading = isLoadingRutinas || isLoadingResumenesSemanales, // <- CAMBIADO
                    errorMessage = errorRutinasTab, // <- CAMBIADO
                    navController = navController,
                    context = context,
                    onRetryLoadRutinas = { viewModel.loadRoutineHistory() }
                )

                1 -> HistorialActividadesLibresContent(
                    historialActividades = historialActividadesLibres,
                    isLoading = isLoadingActividadesLibres,
                    errorMessage = if (isLoadingActividadesLibres || historialActividadesLibres.isNotEmpty()) null else errorMessage,
                    onDeleteActivity = { activityId ->
                        viewModel.deleteFreeActivity(activityId)
                    },
                    onRetryLoadActividades = { viewModel.loadFreeActivityHistory() },
                    context = context,
                    navController = navController
                )
            }
        }
    }
}

// Composable para el contenido de la pestaña "Rutinas"
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistorialRutinasContent(
    listaResumenesSemanales: List<ResumenSemanalConFechas>, // <- CAMBIADO
    historialRutinasCompleto: List<ProgresoRutina>, // <- MANTENIDO (para los gráficos por ahora)
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

    if (errorMessage != null && listaResumenesSemanales.isEmpty() && historialRutinasCompleto.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Error al cargar historial de rutinas: $errorMessage",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetryLoadRutinas) {
                    Text("Reintentar")
                }
            }
        }
        return
    }

    if (listaResumenesSemanales.isEmpty() && historialRutinasCompleto.isEmpty() && !isLoading && errorMessage == null) {
        EmptyStateHistorial(
            modifier = Modifier.fillMaxSize(), // Quitado el .weight(1f) si no hay otros elementos que compitan por espacio
            title = stringResource(R.string.historial_rutinas_vacio_titulo),
            subtitle = stringResource(R.string.historial_rutinas_vacio_subtitulo),
            buttonText = stringResource(R.string.comenzar_rutina),
            onButtonClick = {
                navController.navigate(Routes.ROUTINES_EXPLORER_SCREEN)
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp), // Padding general
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre cada tarjeta de resumen
        ) {
            items(
                items = listaResumenesSemanales,
                key = { resumenConFechas -> "${resumenConFechas.anio}-${resumenConFechas.semanaDelAnio}" }
            ) { resumenConFechas ->
                ResumenSemanalCard(
                    resumenConFechas = resumenConFechas, // <- CAMBIADO
                    context = context
                    // historialRutinas (para los gráficos) ahora viene de resumenConFechas.progresosDeLaSemana
                )
            }

            // Opcional: Si aún quieres mostrar la lista de todas las rutinas individuales debajo de los resúmenes semanales
            // podrías añadir otra sección aquí, pero usualmente los resúmenes ya agrupan esta info.
            // Si decides mostrar rutinas individuales, considera si deben estar *dentro* de cada tarjeta de resumen
            // o como una lista separada más abajo.
            // Por simplicidad, y dado que `ResumenSemanalConFechas` contiene `progresosDeLaSemana`,
            // nos enfocaremos en mostrar los resúmenes y sus detalles/gráficos internos.
        }
    }
}

// Composable para la Card de Resumen Semanal (Extraído para claridad)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ResumenSemanalCard(
    resumenConFechas: ResumenSemanalConFechas, // <- CAMBIADO
    context: Context
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedChartTab by remember { mutableIntStateOf(0) }
    val resumen = resumenConFechas.resumen // Acceso al ResumenSemanal interno
    val progresosDeEstaSemana = resumenConFechas.progresosDeLaSemana
    // Formatear las fechas de inicio y fin de la semana
    val inicioSemanaStr =
        resumenSemanalFechaFormatter.format(resumenConFechas.fechaInicioSemana.toDate())
    val finSemanaStr = resumenSemanalFechaFormatter.format(resumenConFechas.fechaFinSemana.toDate())
    val tituloSemana =
        "Semana ${resumenConFechas.semanaDelAnio} ($inicioSemanaStr - $finSemanaStr, ${resumenConFechas.anio})"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Quitado el vertical, ya que LazyColumn lo maneja
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) { // Para que el texto se ajuste
                    Text(
                        "📊 Resumen Semanal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        // "Semana ${resumenConFechas.semanaDelAnio}, ${resumenConFechas.anio}",
                        tituloSemana,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    icon = Icons.Filled.FitnessCenter,
                    label = "Rutinas",
                    value = resumen.rutinas.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    icon = Icons.Filled.Timer,
                    label = "Tiempo Total",
                    value = formatSecondsToHMS(resumen.tiempoTotal.toLong()),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                    animationSpec = tween(
                        300
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(
                        300
                    )
                )
            ) {
                Column {
                    Text(
                        "Detalles Adicionales:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    InfoRowResumen(
                        "Total ejercicios realizados:",
                        resumen.totalEjercicios.toString()
                    )
                    if (resumen.ejerciciosPorTiempo > 0) {
                        InfoRowResumen(
                            "Ejercicios por tiempo:",
                            resumen.ejerciciosPorTiempo.toString()
                        )
                    }
                    if (resumen.ejerciciosPorRepeticiones > 0) {
                        InfoRowResumen(
                            "Ejercicios por repeticiones:",
                            resumen.ejerciciosPorRepeticiones.toString()
                        )
                    }
                    if (resumen.objetivosRecurrentes.isNotEmpty()) {
                        InfoRowResumen(
                            "Objetivos más frecuentes:",
                            resumen.objetivosRecurrentes.joinToString(", ")
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Usar progresosDeLaSemana del ResumenSemanalConFechas para los gráficos
                    val progresosDeEstaSemana = resumenConFechas.progresosDeLaSemana
                    if (progresosDeEstaSemana.isNotEmpty()) {
                        Text(
                            "Actividad Diaria (Semana ${resumenConFechas.semanaDelAnio}):",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TabRow(selectedTabIndex = selectedChartTab) {
                            Tab(
                                selected = selectedChartTab == 0,
                                onClick = { selectedChartTab = 0 }) {
                                Text("Rutinas/día", modifier = Modifier.padding(vertical = 12.dp))
                            }
                            Tab(
                                selected = selectedChartTab == 1,
                                onClick = { selectedChartTab = 1 }) {
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
                                0 -> RutinasBarChart(
                                    progresosDeEstaSemana, modifier = Modifier // <- CAMBIADO
                                        .height(150.dp)
                                        .padding(8.dp)
                                )

                                1 -> TiempoBarChart(
                                    progresosDeEstaSemana, modifier = Modifier // <- CAMBIADO
                                        .height(150.dp)
                                        .padding(8.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay datos de rutinas esta semana para mostrar gráficos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // ----- NUEVA SECCIÓN: HISTORIAL DIARIO DE RUTINAS DE LA SEMANA -----
                    if (progresosDeEstaSemana.isNotEmpty()) {
                        Text(
                            "Rutinas de esta Semana:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        // Aquí no usamos LazyColumn porque ya estamos dentro de una Card que podría estar en una LazyColumn.
                        // Usamos un Column simple. Si esperas MUCHAS rutinas por semana, considera alternativas.
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            progresosDeEstaSemana.forEach { progreso ->
                                ProgresoRutinaCard(
                                    progreso = progreso,
                                    // El modifier es opcional aquí si ProgresoRutinaCard ya tiene fillMaxWidth
                                    // y los botones de compartir están definidos dentro de ProgresoRutinaCard.
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = {
                    val mensaje =
                        buildSemanalShareText(resumen) // buildSemanalShareText sigue usando ResumenSemanal
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
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Texto")
                }
                Button(onClick = {
                    // Pasar el ResumenSemanal interno a ResumenSemanalVisualCard
                    captureComposableAsImage(
                        context,
                        {
                            ResumenSemanalVisualCardModern(
                                resumen = resumen,
                                resumenConFechas = resumenConFechas
                            )
                        }) { file -> // <- Añadido resumenConFechas
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
                            Intent.createChooser(
                                intent,
                                "Compartir imagen del resumen"
                            )
                        )
                    }
                }) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Compartir como Imagen",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Imagen", color = Color.Black)
                }
            }
        }
    }
}

// Nuevo Composable para las métricas principales
@Composable
fun MetricItem(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp) // Un poco más grandes
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun ResumenSemanalVisualCardModern( // Nuevo nombre para diferenciar
    resumen: ResumenSemanal,
    resumenConFechas: ResumenSemanalConFechas? = null
) {
    // --- COLORES MODERNOS ---
    val backgroundColorStart = Color(0xFFFDFCFB) // Blanco casi puro
    val backgroundColorEnd = Color(0xFFF5F5F5)   // Gris muy claro
    val primaryTextColor = Color(0xFF2C2C2E)     // Texto principal oscuro
    val secondaryTextColor = Color(0xFF6B6B6B)   // Texto secundario más claro
    val accentColor = Color(0xFFC2850B)           // Tu color de acento principal (dorado/naranja)
    val iconColor = accentColor // Usar el color de acento para iconos

    val visualCardDateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Box( // Usamos un Box para poder poner un gradiente de fondo
        modifier = Modifier
            .width(400.dp) // Un poco más ancha para más espacio
            .wrapContentHeight()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(backgroundColorStart, backgroundColorEnd)
                )
            )
            .border(
                1.dp, Color.LightGray.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            ) // Borde sutil
            .padding(24.dp) // Más padding
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Alineación general a la izquierda para un look más "editorial"
        ) {
            // --- ENCABEZADO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo2), // Asegúrate que el logo tenga fondo transparente o se adapte
                    contentDescription = "Logo KalisFit",
                    modifier = Modifier
                        .size(50.dp) // Un poco más pequeño y elegante
                        .clip(CircleShape) // Si el logo se ve bien circular
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RESUMEN SEMANAL", // MAYÚSCULAS para un toque de diseño
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = secondaryTextColor,
                            letterSpacing = 0.1.em
                        ), // Tracking
                        fontWeight = FontWeight.Medium
                    )
                    resumenConFechas?.let {
                        val inicioStr = visualCardDateFormat.format(it.fechaInicioSemana.toDate())
                        val finStr = visualCardDateFormat.format(it.fechaFinSemana.toDate())
                        Text(
                            "Semana ${it.semanaDelAnio}: $inicioStr - $finStr, ${it.anio}",
                            style = MaterialTheme.typography.titleMedium.copy(color = primaryTextColor),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // --- MÉTRICAS PRINCIPALES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ModernMetricItem(
                    icon = Icons.Outlined.FitnessCenter, // Iconos Outlined para un look más ligero
                    value = resumen.rutinas.toString(),
                    label = "Rutinas Completadas",
                    iconColor = iconColor,
                    textColor = primaryTextColor,
                    accentColor = accentColor
                )
                ModernMetricItem(
                    icon = Icons.Outlined.Timer, // Iconos Outlined
                    value = formatSecondsToHMS(resumen.tiempoTotal.toLong()),
                    label = "Tiempo Total Activo",
                    iconColor = iconColor,
                    textColor = primaryTextColor,
                    accentColor = accentColor
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (resumen.totalEjercicios > 0) {
                ModernMetricItem( // Usando el mismo estilo para consistencia
                    icon = Icons.Outlined.Checklist,
                    value = resumen.totalEjercicios.toString(),
                    label = "Ejercicios Registrados",
                    iconColor = iconColor,
                    textColor = primaryTextColor,
                    accentColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Centrado si es único
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
            // --- ENFOQUE ---
            if (resumen.objetivosRecurrentes.isNotEmpty()) {
                Text(
                    "🎯 PRINCIPAL ENFOQUE:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = secondaryTextColor,
                        letterSpacing = 0.08.em
                    ),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp) // Pequeña indentación
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    resumen.objetivosRecurrentes.take(2)
                        .joinToString("  •  ") { it.uppercase() }, // Mayúsculas y más espacio
                    style = MaterialTheme.typography.titleSmall.copy(color = accentColor),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // --- MENSAJE FINAL Y PIE DE PÁGINA ---
            Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom // Alinear al fondo
            ) {
                Text(
                    "Generado por KalisFit",
                    style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor),
                )
                Text(
                    "#KalisFitProgreso", // Hashtag más corto y conciso
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    ),
                )
            }
        }
    }
}

@Composable
fun ModernMetricItem( // Composable auxiliar para las nuevas métricas
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    textColor: Color,
    accentColor: Color, // Para destacar el valor
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(36.dp) // Iconos un poco más pequeños pero claros
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(color = accentColor), // Valor destacado
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label.uppercase(), // Etiqueta en mayúsculas para un look más pro
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor.copy(alpha = 0.7f),
                letterSpacing = 0.05.em
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VisualMetricItem( // Composable auxiliar para las métricas visuales
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 8.dp) // Espacio entre items si están en una Row
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(48.dp) // Iconos grandes y claros
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(color = textColor),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = textColor.copy(alpha = 0.85f))
        )
    }
}

// Nuevo Composable para las filas de información en el resumen (similar a tu InfoRow pero con estilo de resumen)
@Composable
fun InfoRowResumen(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.5f) // Dar más espacio a la etiqueta si es necesario
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.5f)
        )
    }
}
// Composable para cada item en la lista de ProgresoRutina
@Composable
fun ProgresoRutinaCard(
    progreso: ProgresoRutina,
    modifier: Modifier = Modifier
) { // Añadido modifier
    val cardDateFormat = remember {
        SimpleDateFormat(
            "EEE, dd MMM yyyy",
            Locale.getDefault()
        )
    } // EEE para día semana

    Card(
        modifier = modifier.fillMaxWidth(), // Usar el modifier pasado
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), // Un poco más de elevación
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Color sutil
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Encabezado: Fecha y Nombre de la Rutina ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.EventNote, // Icono de calendario/nota
                    contentDescription = "Fecha",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cardDateFormat.format(progreso.fecha.toDate()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = progreso.nombreRutina,
                style = MaterialTheme.typography.titleLarge, // Más grande para el nombre de la rutina
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // --- Detalles Principales de la Sesión ---
            InfoRowProgreso(
                icon = Icons.Filled.VerifiedUser, // Icono para Nivel
                label = "Nivel:",
                value = progreso.nivelUsuarioAlCompletar
            )
            if (progreso.rondasRealizadas > 0) {
                InfoRowProgreso(
                    icon = Icons.Filled.Autorenew, // Icono para Rondas
                    label = "Rondas:",
                    value = "${progreso.rondasRealizadas} completadas"
                )
            }
            if (progreso.objetivosUsuarioAlCompletar.isNotEmpty()) {
                InfoRowProgreso(
                    icon = Icons.Filled.Flag, // Icono para Objetivos
                    label = "Objetivos:",
                    value = progreso.objetivosUsuarioAlCompletar.joinToString(", ")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // --- Lista de Ejercicios (Limitada) ---
            if (progreso.ejerciciosCompletados.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "Ejercicios",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ejercicios Realizados (${progreso.ejerciciosCompletados.size}):",
                        style = MaterialTheme.typography.titleSmall, // Un poco más pequeño que el nombre de la rutina
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.padding(start = 8.dp)) { // Indentación para los ejercicios
                    progreso.ejerciciosCompletados.take(4)
                        .forEach { ejercicio -> // Mostrar hasta 4 para dejar espacio para "ver más"
                            val detalleEjercicio = if (ejercicio.repeticionesPorSerie > 0) {
                                "${ejercicio.repeticionesPorSerie} reps"
                            } else if (ejercicio.duracionPorSerieSegundos > 0) {
                                "${ejercicio.duracionPorSerieSegundos}s"
                            } else {
                                "N/A"
                            }
                            Text(
                                " • ${ejercicio.nombre}: $detalleEjercicio (${ejercicio.seriesRealizadas} series)",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 18.sp // Mejorar legibilidad si los nombres son largos
                            )
                        }
                    if (progreso.ejerciciosCompletados.size > 4) {
                        Text(
                            "... y ${progreso.ejerciciosCompletados.size - 4} más.",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // --- Tiempo Total ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End // Alineado a la derecha
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = "Tiempo total",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatSecondsToMinutesSeconds(progreso.tiempoTotalSesionSegundos),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                val context = LocalContext.current // Necesitarás el contexto
                OutlinedButton(
                    onClick = {
                        val mensaje = buildProgresoRutinaShareText(progreso) // Crea esta función
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, mensaje)
                            type = "text/plain"
                        }
                        context.startActivity(
                            Intent.createChooser(
                                sendIntent,
                                "Compartir progreso de rutina"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir Texto")
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Texto")
                }
                Button(
                    onClick = {
                        captureComposableAsImage(
                            context,
                            { ProgresoRutinaVisualCard(progreso = progreso) }) { file ->
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
                                Intent.createChooser(
                                    intent,
                                    "Compartir imagen de rutina"
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Compartir como Imagen"
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Imagen")
                }
            }
        }
    }
}

// Composable auxiliar para las filas de información dentro de ProgresoRutinaCard
@Composable
fun InfoRowProgreso(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f) // Ajustar peso para la etiqueta
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(0.65f) // Ajustar peso para el valor
        )
    }
}

@Composable
fun ProgresoRutinaVisualCard(
    progreso: ProgresoRutina,
    // Podrías pasar el nombre del usuario si lo tienes
) {
    // --- COLORES DEFINIDOS (consistentes con las otras VisualCards) ---
    val cardBackgroundColor = Color(0xFFFFF0C9)
    val onCardColor = Color(0xFF1C1C1E)
    val primaryAppColor = Color(0xFFC2850B)
    val secondaryColorDetails = Color(0xFF1976D2) // Azul para detalles o iconos secundarios
    val tertiaryColorHashtag = Color(0xFF388E3C)

    val visualCardDateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .width(380.dp)
            .wrapContentHeight()
            .background(cardBackgroundColor),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Encabezado con Logo y Título de Rutina ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo2), // TU LOGO
                    contentDescription = "Logo KalisFit",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        progreso.nombreRutina, // Nombre de la rutina como título principal
                        style = MaterialTheme.typography.titleLarge.copy(color = primaryAppColor),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        visualCardDateFormat.format(progreso.fecha.toDate()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = onCardColor.copy(
                                alpha = 0.7f
                            )
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // --- 2. Métricas Clave de la Sesión ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                VisualMetricItem(
                    icon = Icons.Filled.Timer,
                    value = formatSecondsToMinutesSeconds(progreso.tiempoTotalSesionSegundos),
                    label = "Duración",
                    iconColor = secondaryColorDetails,
                    textColor = onCardColor
                )
                VisualMetricItem(
                    icon = Icons.Filled.FormatListNumbered, // Icono para contar ejercicios
                    value = progreso.ejerciciosCompletados.size.toString(),
                    label = "Ejercicios",
                    iconColor = secondaryColorDetails,
                    textColor = onCardColor
                )
                if (progreso.rondasRealizadas > 0) {
                    VisualMetricItem(
                        icon = Icons.Filled.Autorenew,
                        value = progreso.rondasRealizadas.toString(),
                        label = "Rondas",
                        iconColor = secondaryColorDetails,
                        textColor = onCardColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // --- 3. Foco en Ejercicios (Resumido) ---
            if (progreso.ejerciciosCompletados.isNotEmpty()) {
                Text(
                    "💪 Enfoque Principal:",
                    style = MaterialTheme.typography.titleMedium.copy(color = onCardColor),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), // Un poco de padding
                    horizontalAlignment = Alignment.CenterHorizontally // Centrar los nombres de ejercicios
                ) {
                    // Mostrar los nombres de los primeros 3-4 ejercicios
                    progreso.ejerciciosCompletados.take(3).forEach { ejercicio ->
                        Text(
                            text = "• ${ejercicio.nombre}",
                            style = MaterialTheme.typography.bodyLarge.copy(color = primaryAppColor),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (progreso.ejerciciosCompletados.size > 3) {
                        Text(
                            "...¡y más enfoque!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = onCardColor.copy(
                                    alpha = 0.8f
                                )
                            ),
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            // --- 4. Frase de Logro/Cierre ---
            Text(
                "¡Rutina Completada! 🔥",
                style = MaterialTheme.typography.titleMedium.copy(color = primaryAppColor),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Opcional: Podrías añadir algo sobre los objetivos si quieres
            if (progreso.objetivosUsuarioAlCompletar.isNotEmpty()) {
                Text(
                    "Objetivos alcanzados: ${progreso.objetivosUsuarioAlCompletar.joinToString()}",
                    style = MaterialTheme.typography.bodySmall.copy(color = onCardColor.copy(alpha = 0.7f)),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // --- 5. Pie de Página ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compartido desde KalisFit",
                    style = MaterialTheme.typography.labelMedium.copy(color = onCardColor.copy(alpha = 0.7f)),
                )
                Text(
                    "#KalisFit #${
                        progreso.nombreRutina.replace(
                            " ",
                            ""
                        )
                    } #Entrenamiento", // Hashtags
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = tertiaryColorHashtag,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.End
                )
            }
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
    context: Context,
    navController: NavController
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
            onButtonClick = { navController.navigate(Routes.RUNNING_TAB) }
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
                    UserActivityCard(
                        activity = activity,
                        context = context
                    ) // Asumiendo que UserActivityCard también usa solo M3
                }
            }
        }
    }
}

// NUEVO: Composable para mostrar cada UserActivity (carrera/caminata)
@Composable
fun UserActivityCard(activity: UserActivity, context: Context) {
    val coroutineScope = rememberCoroutineScope()

    // Formateador de fecha (si no lo tienes global, defínelo aquí o pásalo)
    // Para este ejemplo, lo defino localmente si no estuviera ya accesible.
    val localActivityDateFormatter = remember {
        SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())
    }

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
                    text = activity.timestamp?.let { localActivityDateFormatter.format(it) }
                        ?: "Fecha desconocida",
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

            InfoRow(
                "Duración:",
                formatSecondsToHMS(activity.elapsedTimeSeconds)
            ) // Asume que formatSecondsToHMS existe
            InfoRow("Distancia:", String.format(Locale.US, "%.2f km", activity.distanceKm))
            InfoRow("Ritmo Promedio:", activity.avgPace)
            InfoRow("Calorías Quemadas:", "${activity.caloriesBurned} kcal")

            Spacer(modifier = Modifier.height(16.dp)) // Espacio antes de los botones

            // --- BOTONES DE COMPARTIR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    Alignment.End
                ) // Alinea a la derecha
            ) {
                // Botón Compartir Texto
                OutlinedButton(onClick = {
                    val shareText =
                        buildActivityShareText(activity) // Asume que buildActivityShareText existe
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
                    val pointsForVisualCard: List<LatLng> =
                        activity.routePoints.map { locationPoint ->
                            LatLng(locationPoint.latitude, locationPoint.longitude)
                        }

                    Log.d(
                        "UserActivityCard",
                        "Iniciando captura de imagen. Puntos: ${pointsForVisualCard.size}"
                    )

                    coroutineScope.launch {
                        // Espera un poco para que el mapa en UserActivityVisualCard se renderice.
                        // Ajusta este valor según sea necesario. Empieza con 1.5-2.5 segundos.
                        delay(2000) // 2 segundos de retraso. AUMENTA SI EL MAPA SIGUE EN BLANCO.

                        Log.d("UserActivityCard", "Retraso completado, procediendo a capturar.")

                        captureComposableAsImage( // Asume que captureComposableAsImage existe
                            context = context,
                            composable = {
                                UserActivityVisualCard( // Asume que UserActivityVisualCard existe y está bien definido
                                    activity = activity,
                                    routePointsToDraw = pointsForVisualCard
                                )
                            }
                        ) { imageFile: File -> // Especificar el tipo del parámetro lambda ayuda
                            Log.d("UserActivityCard", "Imagen capturada: ${imageFile.absolutePath}")
                            val imageUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider", // Asegúrate que la autoridad coincide con tu Manifest
                                imageFile
                            )
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, imageUri)
                                type = "image/png"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            // Intenta ser explícito con el contexto si hay ambigüedad, aunque no debería ser necesario.
                            // val currentContext = context
                            // currentContext.startActivity(Intent.createChooser(shareIntent, "Compartir imagen de actividad con..."))
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Compartir imagen de actividad con..."
                                )
                            )
                        }
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

// Función para generar el texto del resumen semanal (necesitas crearla)
fun buildSemanalShareText(resumen: ResumenSemanal): String {
    // Similar a buildActivityShareText, pero para el resumen
    return """
        🌟 ¡Mi Resumen Semanal con KalisFit! 🌟
        -----------------------------------
        🏋️ Rutinas Completadas: ${resumen.rutinas}
        ⏱️ Tiempo Total Entrenado: ${formatSecondsToHMS(resumen.tiempoTotal.toLong())}
        🤸 Total Ejercicios: ${resumen.totalEjercicios}
        ${
        if (resumen.objetivosRecurrentes.isNotEmpty()) "🎯 Objetivos: ${
            resumen.objetivosRecurrentes.joinToString(
                ", "
            )
        }\n" else ""
    }
        -----------------------------------
        ¡Una semana más fuerte! #KalisFit #Fitness #Progreso
    """.trimIndent()
}

fun buildProgresoRutinaShareText(progreso: ProgresoRutina): String {
    // Formateador para la fecha, puedes ajustarlo a tu preferencia
    val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
    val fechaFormateada = dateFormat.format(progreso.fecha.toDate())

    // Resumen de los primeros ejercicios (por ejemplo, los 3 primeros)
    val ejerciciosDestacados = progreso.ejerciciosCompletados
        .take(3) // Tomar los primeros 3
        .joinToString(separator = "\n") { ejercicio ->
            val detalle = if (ejercicio.repeticionesPorSerie > 0) {
                "${ejercicio.repeticionesPorSerie} reps"
            } else if (ejercicio.duracionPorSerieSegundos > 0) {
                "${ejercicio.duracionPorSerieSegundos}s"
            } else {
                "N/A"
            }
            "  • ${ejercicio.nombre} ($detalle x ${ejercicio.seriesRealizadas} series)"
        }

    val masEjerciciosTexto = if (progreso.ejerciciosCompletados.size > 3) {
        "\n  ...¡y ${progreso.ejerciciosCompletados.size - 3} más!"
    } else {
        ""
    }

    // Construcción del mensaje
    // Puedes personalizar los emojis y el texto como quieras
    return """
    ¡💪 Rutina '${progreso.nombreRutina}' completada con KalisFit! 🎉
    -----------------------------------
    📅 Fecha: $fechaFormateada
    ⏱️ Duración Total: ${formatSecondsToMinutesSeconds(progreso.tiempoTotalSesionSegundos)}
    ${if (progreso.rondasRealizadas > 0) "🔄 Rondas Realizadas: ${progreso.rondasRealizadas}\n" else ""}
    🔢 Total Ejercicios: ${progreso.ejerciciosCompletados.size}
    ${
        if (progreso.objetivosUsuarioAlCompletar.isNotEmpty()) "🎯 Objetivos: ${
            progreso.objetivosUsuarioAlCompletar.joinToString(
                ", "
            )
        }\n" else ""
    }
    -----------------------------------
    🏋️‍♀️ Ejercicios Destacados:
    $ejerciciosDestacados$masEjerciciosTexto
    -----------------------------------
    ¡A seguir dándolo todo! #KalisFit #EntrenamientoCompletado #${
        progreso.nombreRutina.replace(
            " ",
            ""
        )
    } #FitnessMotivation
    """.trimIndent()
}
