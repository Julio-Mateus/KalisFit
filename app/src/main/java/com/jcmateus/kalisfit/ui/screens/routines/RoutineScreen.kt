package com.jcmateus.kalisfit.ui.screens.routines

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineExecutionState
import com.jcmateus.kalisfit.viewmodel.RoutineUiState
import com.jcmateus.kalisfit.viewmodel.RoutineViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

import kotlinx.coroutines.launch
import kotlin.collections.forEach


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    navController: NavController,
    rutinaId: String?,
    customRutinaId: String?,
    viewModel: RoutineViewModel = viewModel(),
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfileState by userProfileViewModel.user.collectAsState()
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // MODIFICADO: LaunchedEffect para iniciar la rutina
    LaunchedEffect(rutinaId, customRutinaId, userProfileState, uiState.estado) {
        val currentUserProfile = userProfileState
        if (currentUserProfile == null) {
            Log.w("RoutineScreen", "UserProfile es nulo, no se puede iniciar la rutina todavía.")
            // El ViewModel debería manejar la espera o mostrar un mensaje si es necesario.
            // viewModel.setLoadingMessage("Esperando perfil de usuario...") // Si tienes esta función
            return@LaunchedEffect
        }
        val idParaIniciar = customRutinaId ?: rutinaId // Prioriza customRutinaId
        // Solo intentar iniciar si:
        // 1. Aún no hay una rutina cargada (uiState.rutina == null)
        // 2. El estado actual es IDLE (no se está cargando, ni en error, etc.)
        // 3. Tenemos un ID para iniciar (idParaIniciar != null)
        if (uiState.rutina == null && uiState.estado == RoutineExecutionState.IDLE && idParaIniciar != null) {
            Log.d("RoutineScreen", "Intentando iniciar rutina con ID: $idParaIniciar y perfil de usuario.")
            // Asumimos que startRoutine ahora puede manejar tanto IDs de plantillas como IDs de rutinas personalizadas.
            // Tu ViewModel.startRoutine debería intentar primero buscar una UserCustomRoutine con ese ID
            // y si no la encuentra, buscar una plantilla global.
            viewModel.startRoutine(idParaIniciar, currentUserProfile)

        } else if (idParaIniciar == null && uiState.estado == RoutineExecutionState.IDLE) {
            // Este caso ocurre si no se proporciona ni rutinaId ni customRutinaId desde el inicio.
            Log.w("RoutineScreen", "Ni rutinaId ni customRutinaId fueron provistos. No se puede iniciar rutina.")
            viewModel.setError(context.getString(R.string.error_no_routine_specified))

        } else if (uiState.rutina != null && uiState.estado == RoutineExecutionState.IDLE) {
            Log.d("RoutineScreen", "Rutina ya cargada (${uiState.rutina?.id}) y estado es IDLE. Podría ser un re-render, navegación post-error o rutina ya finalizada y lista para otra acción. No se reinicia automáticamente desde aquí.")
            // Considera si necesitas alguna lógica específica aquí, por ejemplo, si el usuario navega hacia atrás y luego hacia adelante a una rutina ya completada.
        } else if (uiState.estado == RoutineExecutionState.LOADING && idParaIniciar != null) {
            Log.d("RoutineScreen", "Rutina (ID: $idParaIniciar) en proceso de carga. Esperando...")
        } else {
            Log.d("RoutineScreen", "LaunchedEffect revisado. Estado actual: ${uiState.estado}, Rutina cargada: ${uiState.rutina != null}, ID para iniciar: $idParaIniciar. No se requiere acción de inicio.")
        }
    }
    LaunchedEffect(viewModel.soundEvents) {
        viewModel.soundEvents.collect { event ->
            when (event) {
                "start_sound" -> playSound(context, "start")
                "beep" -> playSound(context, "beep")
                "rest_start" -> playSound(context, "rest_start")
                "rest_end" -> playSound(context, "rest_end")
                "exercise_start" -> playSound(context, "exercise_start")
                "routine_finish_sound" -> playSound(context, "routine_finish")
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearErrorMessage()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearSuccessMessage()
        }
    }
    BackHandler(enabled = uiState.estado != RoutineExecutionState.IDLE && uiState.estado != RoutineExecutionState.FINISHED) {
        viewModel.setShowExitConfirmation(true)
    }
    if (uiState.estado !in listOf(
            RoutineExecutionState.IDLE,
            RoutineExecutionState.LOADING,
            RoutineExecutionState.FINISHED,
            RoutineExecutionState.ERROR,
            RoutineExecutionState.PAUSED // No mantener pantalla encendida si está en pausa explícita
        )) {
        KeepScreenOn() // Asumiendo que KeepScreenOn es un Composable que maneja esto
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.rutina?.nombre ?: stringResource(R.string.routine_loading)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.estado == RoutineExecutionState.IDLE ||
                            uiState.estado == RoutineExecutionState.LOADING ||
                            uiState.estado == RoutineExecutionState.FINISHED ||
                            uiState.estado == RoutineExecutionState.ERROR) { // Añadido ERROR
                            navController.popBackStack()
                        } else {
                            viewModel.setShowExitConfirmation(true)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.togglePausa() },
                        enabled = uiState.estado !in listOf(
                            RoutineExecutionState.IDLE,
                            RoutineExecutionState.LOADING,
                            RoutineExecutionState.FINISHED,
                            RoutineExecutionState.ERROR,
                            RoutineExecutionState.INITIAL_COUNTDOWN // No permitir pausar en la cuenta regresiva inicial
                        )
                    ) {
                        if (uiState.estado == RoutineExecutionState.PAUSED) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.resume_routine))
                        } else {
                            Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.pause_routine))
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.saltarSiguientePaso()
                            }
                        },
                        enabled = uiState.estado !in listOf(
                            RoutineExecutionState.IDLE,
                            RoutineExecutionState.LOADING,
                            RoutineExecutionState.FINISHED,
                            RoutineExecutionState.ERROR,
                            RoutineExecutionState.PAUSED, // No permitir saltar si está en pausa
                            RoutineExecutionState.INITIAL_COUNTDOWN // No permitir saltar en cuenta regresiva inicial
                        )
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.skip_step))
                    }
                }
            )
        },
        floatingActionButton = {
            // Solo mostrar el FAB si estamos en el estado de EJERCICIO_ACTIVO
            // y hay una rutina cargada con un ejercicio actual.
            if (uiState.estado == RoutineExecutionState.EXERCISE_ACTIVE &&
                uiState.rutina != null && uiState.ejercicioActual != null) {
                val currentEjercicio = uiState.ejercicioActual!! // Sabemos que no es nulo por la condición
                val repeticionesNumericas = currentEjercicio.repeticionesOriginal.toIntOrNull() ?: 0
                val esEjercicioSimplePrincipalmentePorTiempo =
                    currentEjercicio.tipoEjercicio == TipoDeEjercicio.SIMPLE &&
                            currentEjercicio.duracionSegundosOriginal > 0 &&
                            repeticionesNumericas <= 0

                val isButtonEnabledCondition = if (esEjercicioSimplePrincipalmentePorTiempo) {
                    uiState.tiempoRestante <= 0
                } else {
                    true
                }
                val buttonText = getNextButtonText(uiState, context)

                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewModel.saltarSiguientePaso()
                        }
                    },
                    // Habilitar/deshabilitar el FAB según la misma lógica que tu botón anterior
                    // También, asegúrate de que esté habilitado solo cuando sea relevante.
                    // enabled = isButtonEnabledCondition, // Usa tu lógica de habilitación
                    modifier = Modifier.padding(16.dp) // Añade padding si es necesario
                ) {
                    // Puedes usar un Icono o Texto, o ambos.
                    // Para un FAB, a menudo se usa un icono, pero el texto también es válido.
                    // Si el buttonText es corto, puede funcionar.
                    // Si es largo, considera un icono + texto o solo un icono con buen contentDescription.
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ejemplo con icono y texto animado:
                        AnimatedContent(
                            targetState = buttonText,
                            transitionSpec = {
                                ContentTransform(
                                    targetContentEnter = slideInHorizontally { width -> width } + fadeIn(),
                                    initialContentExit = slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            },
                            label = "fabButtonTextAnimation"
                        ) { text ->
                            Text(text, fontWeight = FontWeight.SemiBold)
                        }
                        // Opcional: añade un icono si lo deseas
                        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.skip_step))
                    }
                }
            }
        },
        //floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            //color = MaterialTheme.colorScheme.background
        ) {
            when (uiState.estado) {
                RoutineExecutionState.IDLE, RoutineExecutionState.LOADING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                stringResource(R.string.loading_routine),
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            // Mostrar un mensaje si es por UserProfile nulo
                            if (userProfileState == null && (rutinaId != null || customRutinaId != null) && uiState.rutina == null) {
                                Text(
                                    stringResource(R.string.waiting_for_user_profile),
                                    modifier = Modifier.padding(top = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                RoutineExecutionState.ERROR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: stringResource(R.string.error_loading_routine),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text(stringResource(R.string.go_back))
                        }
                    }
                }
                RoutineExecutionState.INITIAL_COUNTDOWN -> {
                    InitialCountdown(
                        countdownInicial = uiState.tiempoRestante,
                        routineName = uiState.rutina?.nombre ?: ""
                    )
                }
                RoutineExecutionState.EXERCISE_ACTIVE -> {
                    uiState.rutina?.let { rutina ->
                        uiState.ejercicioActual?.let { currentEjercicio ->
                            val repeticionesNumericas = currentEjercicio.repeticionesOriginal.toIntOrNull() ?: 0
                            val esEjercicioSimplePrincipalmentePorTiempo =
                                currentEjercicio.tipoEjercicio == TipoDeEjercicio.SIMPLE &&
                                        currentEjercicio.duracionSegundosOriginal > 0 &&
                                        repeticionesNumericas <= 0

                            val isButtonEnabledCondition = if (esEjercicioSimplePrincipalmentePorTiempo) {
                                uiState.tiempoRestante <= 0
                            } else {
                                true
                            }
                            ExerciseContent(
                                currentEjercicio = currentEjercicio,
                                componenteActivo = uiState.componenteEjercicioActual,
                                rondaActual = uiState.rondaActual,
                                totalRondas = rutina.numeroDeRondas,
                                ejercicioActualNum = uiState.indiceEjercicioActual + 1,
                                totalEjercicios = rutina.ejercicios.size,
                                serieActual = uiState.serieActualEjercicio,
                                totalSeries = currentEjercicio.numeroDeSeries,
                                segundosRestantes = uiState.tiempoRestante,
                                imageLoader = imageLoader,
                                onWatchVideoClick = { videoUrl ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.no_app_for_video), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.error_opening_video) + ": ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onNextClick = {
                                    scope.launch {
                                        viewModel.saltarSiguientePaso()
                                    }
                                },
                                isButtonEnabled = isButtonEnabledCondition,
                                buttonText = getNextButtonText(uiState, context)
                            )
                        } ?: Text(stringResource(R.string.error_exercise_not_found_active), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    } ?: Text(stringResource(R.string.error_routine_not_found_active), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
                RoutineExecutionState.REST_BETWEEN_SETS,
                RoutineExecutionState.REST_BETWEEN_EXERCISES,
                RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                    val title = when (uiState.estado) {
                        RoutineExecutionState.REST_BETWEEN_SETS -> stringResource(R.string.rest_between_sets_title)
                        RoutineExecutionState.REST_BETWEEN_EXERCISES -> stringResource(R.string.rest_between_exercises_title)
                        RoutineExecutionState.REST_BETWEEN_ROUNDS -> stringResource(R.string.rest_between_rounds_title)
                        else -> "" // No debería ocurrir
                    }
                    val currentEjercicioIndex = uiState.indiceEjercicioActual.takeIf { it >= 0 } ?: 0
                    val currentEjercicio = uiState.rutina?.ejercicios?.getOrNull(currentEjercicioIndex)

                    val totalRestSeconds = when (uiState.estado) {
                        RoutineExecutionState.REST_BETWEEN_SETS -> currentEjercicio?.descansoEntreSeriesSegundos ?: 0
                        RoutineExecutionState.REST_BETWEEN_EXERCISES -> currentEjercicio?.descansoDespuesEjercicioSegundos ?: 0
                        RoutineExecutionState.REST_BETWEEN_ROUNDS -> uiState.rutina?.descansoEntreRondasSegundos ?: 0
                        else -> 0
                    }
                    val nextUpMessage = getNextUpMessage(uiState, context)
                    IntegratedRestDialog(
                        visible = true, // Asumimos que si estamos en este estado, el diálogo es visible
                        onDismissRequest = { /* Normalmente el usuario no cierra este diálogo, espera o salta */ },
                        title = title,
                        secondsRemaining = uiState.tiempoRestante,
                        totalRestSeconds = totalRestSeconds,
                        nextUpMessage = nextUpMessage,
                        onSkip = {
                            scope.launch {
                                viewModel.saltarSiguientePaso()
                            }
                        }
                    )
                }
                RoutineExecutionState.PAUSED -> {
                    val currentEjercicio = uiState.ejercicioActual
                    val previousState = uiState.previousState // Este debe estar en tu UiState y ser actualizado por el ViewModel
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Mostrar el contenido de fondo (ejercicio o descanso) semitransparente
                        if (uiState.rutina != null) {
                            val rutinaForPause = uiState.rutina!!
                            when (previousState) {
                                RoutineExecutionState.EXERCISE_ACTIVE -> {
                                    if (currentEjercicio != null) {
                                        ExerciseContent(
                                            currentEjercicio = currentEjercicio,
                                            componenteActivo = uiState.componenteEjercicioActual,
                                            rondaActual = uiState.rondaActual,
                                            totalRondas = rutinaForPause.numeroDeRondas,
                                            ejercicioActualNum = uiState.indiceEjercicioActual + 1,
                                            totalEjercicios = rutinaForPause.ejercicios.size,
                                            serieActual = uiState.serieActualEjercicio,
                                            totalSeries = currentEjercicio.numeroDeSeries,
                                            segundosRestantes = uiState.tiempoRestante,
                                            imageLoader = imageLoader,
                                            onWatchVideoClick = { /* No-op */ },
                                            onNextClick = { /* No-op */ },
                                            isButtonEnabled = false,
                                            buttonText = getNextButtonText(uiState, context),
                                            modifier = Modifier.graphicsLayer(alpha = 0.3f) // Hacerlo semitransparente
                                        )
                                    }
                                }
                                RoutineExecutionState.REST_BETWEEN_SETS,
                                RoutineExecutionState.REST_BETWEEN_EXERCISES,
                                RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                                    val title = when (previousState) {
                                        RoutineExecutionState.REST_BETWEEN_SETS -> stringResource(R.string.rest_between_sets_title)
                                        RoutineExecutionState.REST_BETWEEN_EXERCISES -> stringResource(R.string.rest_between_exercises_title)
                                        RoutineExecutionState.REST_BETWEEN_ROUNDS -> stringResource(R.string.rest_between_rounds_title)
                                        else -> ""
                                    }
                                    val currentEjercicioIndex = uiState.indiceEjercicioActual.takeIf { it >= 0 } ?: 0
                                    val ejercicioContextual = rutinaForPause.ejercicios.getOrNull(currentEjercicioIndex)
                                    val totalRestSeconds = when (previousState) {
                                        RoutineExecutionState.REST_BETWEEN_SETS -> ejercicioContextual?.descansoEntreSeriesSegundos ?: 0
                                        RoutineExecutionState.REST_BETWEEN_EXERCISES -> ejercicioContextual?.descansoDespuesEjercicioSegundos ?: 0
                                        RoutineExecutionState.REST_BETWEEN_ROUNDS -> rutinaForPause.descansoEntreRondasSegundos
                                        else -> 0
                                    }
                                    val nextUpMessage = getNextUpMessage(uiState, context)
                                    IntegratedRestDialog(
                                        visible = true,
                                        onDismissRequest = { /* No-op */ },
                                        title = title,
                                        secondsRemaining = uiState.tiempoRestante,
                                        totalRestSeconds = totalRestSeconds,
                                        nextUpMessage = nextUpMessage,
                                        onSkip = { /* No-op */ },
                                        modifier = Modifier.graphicsLayer(alpha = 0.3f) // Hacerlo semitransparente
                                    )
                                }
                                RoutineExecutionState.INITIAL_COUNTDOWN -> {
                                    InitialCountdown(
                                        countdownInicial = uiState.tiempoRestante,
                                        routineName = uiState.rutina?.nombre ?: "",
                                        modifier = Modifier.graphicsLayer(alpha = 0.3f)
                                    )
                                }
                                else -> {
                                    Box(modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)))
                                }
                            }
                        }
                        // Overlay de Pausa
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)), // Fondo del overlay de pausa
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = stringResource(R.string.routine_paused),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Button(
                                    onClick = { viewModel.togglePausa() },
                                    modifier = Modifier.defaultMinSize(minWidth = 180.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.resume_routine))
                                }
                                OutlinedButton(
                                    onClick = { viewModel.setShowExitConfirmation(true) },
                                    modifier = Modifier.defaultMinSize(minWidth = 180.dp)
                                ) {
                                    Text(stringResource(R.string.exit_routine))
                                }
                            }
                        }
                    }
                }
                RoutineExecutionState.FINISHED -> {
                    LaunchedEffect(Unit) {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        val userProfileForSaving = uiState.userProfile
                        val currentRutina = uiState.rutina

                        if (currentUserId != null && userProfileForSaving != null && currentRutina != null) {
                            Log.d("RoutineScreen FINISHED", "Guardando progreso para rutina ID: ${currentRutina.id} , CustomNavId: $customRutinaId, BaseNavId: $rutinaId")
                            viewModel.saveRoutineProgress(
                                userId = currentUserId,
                                userProfile = userProfileForSaving,
                                rutinaId = currentRutina.id, // El ViewModel debería saber si esto es un ID de plantilla o custom
                                onSuccess = {
                                    scope.launch {
                                        // Decidir a dónde volver
                                        val popUpToRoute = if (customRutinaId != null) {
                                            // Si era una rutina personalizada, quizá volver a "Mis Rutinas" o simplemente MAIN_CONTENT
                                            Routes.MAIN_CONTENT // O una ruta específica como Routes.MY_CUSTOM_ROUTINES_SCREEN
                                        } else if (rutinaId != null) {
                                            Routes.routineDetail(rutinaId) // Volver al detalle de la rutina base
                                        } else {
                                            Routes.MAIN_CONTENT // Fallback
                                        }
                                        navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                            popUpTo(popUpToRoute) { inclusive = true }
                                            // Evitar múltiples instancias de ROUTINE_SUCCESS_SCREEN
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onError = { errorMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(errorMsg, duration = SnackbarDuration.Long)
                                        // Aún así navegar a éxito para no dejar al usuario en una pantalla de carga infinita
                                        val popUpToRoute = if (customRutinaId != null) Routes.MAIN_CONTENT else if (rutinaId != null) Routes.routineDetail(rutinaId) else Routes.MAIN_CONTENT
                                        navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                            popUpTo(popUpToRoute) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        } else {
                            scope.launch {
                                var reason = context.getString(R.string.error_cannot_save_progress_user_data)
                                if (currentUserId == null) reason += " (UID no disp.)"
                                if (userProfileForSaving == null) reason += " (Perfil no disp.)"
                                if (currentRutina == null) reason += " (Rutina no disp.)"
                                Log.e("RoutineScreen FINISHED", "No se puede guardar progreso: $reason")
                                snackbarHostState.showSnackbar(reason, duration = SnackbarDuration.Long)

                                val popUpToRoute = if (customRutinaId != null) Routes.MAIN_CONTENT else if (rutinaId != null) Routes.routineDetail(rutinaId) else Routes.MAIN_CONTENT
                                navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                    popUpTo(popUpToRoute) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.routine_finishing_and_saving),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowExitConfirmation(false) },
            title = { Text(stringResource(R.string.dialog_exit_routine_title)) },
            text = { Text(stringResource(R.string.dialog_exit_routine_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setShowExitConfirmation(false)
                        viewModel.exitAndCleanUpRoutine()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.dialog_exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowExitConfirmation(false) }) {
                    Text(stringResource(R.string.dialog_exit_cancel))
                }
            }
        )
    }
}
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseContent(
    currentEjercicio: Ejercicio,
    componenteActivo: ComponenteEjercicio?,
    rondaActual: Int,
    totalRondas: Int,
    ejercicioActualNum: Int,
    totalEjercicios: Int,
    serieActual: Int,
    totalSeries: Int,
    segundosRestantes: Int,
    imageLoader: ImageLoader,
    onWatchVideoClick: (String) -> Unit,
    onNextClick: () -> Unit,
    isButtonEnabled: Boolean,
    buttonText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Indicadores de progreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoBox(
                label = stringResource(R.string.routine_round),
                value = "$rondaActual / $totalRondas",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            InfoBox(
                label = stringResource(R.string.routine_exercise),
                value = "$ejercicioActualNum / $totalEjercicios",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            InfoBox(
                label = stringResource(R.string.routine_series),
                value = if (totalSeries > 0) "$serieActual / $totalSeries" else "-",
                modifier = Modifier.weight(1f)
            )
        }
        // Nombre del ejercicio y descripción
        Text(
            text = componenteActivo?.nombreEspecifico ?: currentEjercicio.nombre, // Muestra nombre componente si activo
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        // La descripción general del ejercicio padre puede seguir siendo útil
        if (currentEjercicio.descripcion.isNotBlank()) {
            Text(
                text = currentEjercicio.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // --- LÓGICA DE IMAGEN MODIFICADA (Opción 1) ---
        // Priorizar imagen del componente activo. Si no, usar la primera imagen del ejercicio.
        // Si ninguna está disponible, se mostrará el placeholder.
        val primaryImageUrl: String? = componenteActivo?.imagenUrl?.takeIf { it.isNotBlank() }
            ?: currentEjercicio.imagenUrl?.takeIf { it.isNotBlank() }
        // Para el carrusel, si no hay componente activo con imagen,
        // usamos las imágenes del ejercicio padre. Si hay componente activo con imagen,
        // el carrusel solo tendrá esa imagen.
        val displayImageUrls = mutableListOf<String>()
        componenteActivo?.imagenUrl?.takeIf { it.isNotBlank() }?.let {
            displayImageUrls.add(it)
        }
        if (displayImageUrls.isEmpty()) { // Si el componente activo no tiene imagen o no hay componente activo
            currentEjercicio.imagenUrl?.takeIf { it.isNotBlank() }?.let { displayImageUrls.add(it) }
            currentEjercicio.imagenUrl1?.takeIf { it.isNotBlank() }?.let { displayImageUrls.add(it) }
            currentEjercicio.imagenUrl2?.takeIf { it.isNotBlank() }?.let { displayImageUrls.add(it) }
            // Añade más campos de imagen si los tienes (ej: imagenUrl3, imagenUrl4)
            // currentEjercicio.imagenUrl3?.takeIf { it.isNotBlank() }?.let { displayImageUrls.add(it) }
        }
        if (displayImageUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { displayImageUrls.size })
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // Deshabilitar el deslizamiento si solo hay una imagen (la del componente activo)
                    userScrollEnabled = displayImageUrls.size > 1
                ) { pageIndex ->
                    AsyncImage(
                        model = displayImageUrls[pageIndex],
                        imageLoader = imageLoader,
                        contentDescription = stringResource(
                            R.string.exercise_image_content_desc,
                            componenteActivo?.nombreEspecifico ?: currentEjercicio.nombre,
                            pageIndex + 1
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // Indicadores de página (dots) para el Pager, solo si hay más de una imagen
            if (displayImageUrls.size > 1) {
                Row(
                    Modifier
                        .height(24.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        } else {
            // Placeholder si ninguna imagen está disponible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_image_available))
            }
        }
        // --- Mostrar información según TipoDeEjercicio ---
        when (currentEjercicio.tipoEjercicio) {
            TipoDeEjercicio.SIMPLE -> {
                if (currentEjercicio.duracionSegundosOriginal > 0) {
                    DisplayTime(
                        currentTime = segundosRestantes,
                        label = stringResource(R.string.routine_time)
                    )
                }
                if (currentEjercicio.repeticionesOriginal.isNotBlank() && currentEjercicio.repeticionesOriginal != "0") {
                    DisplayRepetitions(
                        repetitions = currentEjercicio.repeticionesOriginal,
                        isUnilateral = currentEjercicio.esUnilateral
                    )
                }
                currentEjercicio.notaTempo?.takeIf { it.isNotBlank() }?.let { DisplayTempo(it) }
            }
            TipoDeEjercicio.CON_TEMPO -> {
                // Similar a SIMPLE, pero siempre muestra Tempo
                if (currentEjercicio.duracionSegundosOriginal > 0) {
                    DisplayTime(
                        currentTime = segundosRestantes,
                        label = stringResource(R.string.routine_time)
                    )
                }
                if (currentEjercicio.repeticionesOriginal.isNotBlank() && currentEjercicio.repeticionesOriginal != "0") {
                    DisplayRepetitions(
                        repetitions = currentEjercicio.repeticionesOriginal,
                        isUnilateral = currentEjercicio.esUnilateral
                    )
                }
                DisplayTempo(currentEjercicio.notaTempo?.takeIf { it.isNotBlank() } ?: stringResource(R.string.tempo_not_specified))
            }
            TipoDeEjercicio.SUPERSET_SEQUENCIAL,
            TipoDeEjercicio.COMBINADO_TEMPORIZADO,
            TipoDeEjercicio.CIRCUITO_TEMPORIZADO,
            TipoDeEjercicio.POR_LADO_ALTERNADO -> {
                // Para ejercicios complejos, la información de tiempo/reps principal
                // a menudo se deriva del componente activo o de la configuración general del ejercicio.
                // Aquí, el ViewModel controla `segundosRestantes`.
                // Si el componente activo tiene su propia duración/reps, se muestra en DisplayComponent.
                // Si el ejercicio padre es por tiempo (ej. circuito temporizado), se muestra el tiempo total.
                if (componenteActivo != null) {
                    // Si hay un componente activo, mostrar sus detalles de tiempo/reps
                    // si los tiene definidos Y no es un circuito general donde el tiempo es global
                    if (componenteActivo.duracionSegundos != null && componenteActivo.duracionSegundos!! > 0 && currentEjercicio.tipoEjercicio != TipoDeEjercicio.CIRCUITO_TEMPORIZADO) {
                        DisplayTime(
                            currentTime = segundosRestantes, // Asume que ViewModel maneja el tiempo del componente
                            label = stringResource(R.string.routine_component_time) // Necesitarás este string
                        )
                    }
                    if (!componenteActivo.repeticiones.isNullOrBlank() && componenteActivo.repeticiones != "0") {
                        DisplayRepetitions(
                            repetitions = componenteActivo.repeticiones!!,
                            isUnilateral = false // Asumir que `esUnilateral` se aplica al ejercicio padre o se maneja en VM
                        )
                    }
                } else if (currentEjercicio.duracionSegundosOriginal > 0 && currentEjercicio.tipoEjercicio != TipoDeEjercicio.CIRCUITO_TEMPORIZADO) {
                    // Si no hay componente activo pero el ejercicio padre es por tiempo (y no es circuito)
                    DisplayTime(
                        currentTime = segundosRestantes,
                        label = stringResource(R.string.routine_time)
                    )
                } else if (!currentEjercicio.repeticionesOriginal.isNullOrBlank() && currentEjercicio.repeticionesOriginal != "0" && currentEjercicio.componentes.isEmpty()) {
                    // Si no hay componente activo pero el ejercicio padre es por reps y no tiene componentes
                    DisplayRepetitions(
                        repetitions = currentEjercicio.repeticionesOriginal,
                        isUnilateral = currentEjercicio.esUnilateral
                    )
                }
                if (currentEjercicio.componentes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.exercise_components_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    currentEjercicio.componentes.sortedBy { it.orden }.forEach { componenteIterado ->
                        val esComponenteActivo = componenteActivo?.let { activo ->
                            activo.orden == componenteIterado.orden && activo.nombreEspecifico == componenteIterado.nombreEspecifico
                        } ?: false
                        DisplayComponent( // DisplayComponent solo muestra texto y resalta el activo
                            componente = componenteIterado,
                            isCurrentlyActive = esComponenteActivo,
                            imageLoader = imageLoader
                        )
                    }
                    if (currentEjercicio.duracionSegundosOriginal > 0 && currentEjercicio.tipoEjercicio == TipoDeEjercicio.CIRCUITO_TEMPORIZADO) {
                        DisplayTime(
                            currentTime = segundosRestantes,
                            label = stringResource(R.string.total_circuit_time)
                        )
                    }
                } else {
                    // Si es un ejercicio complejo pero no tiene componentes listados (ej. POR_LADO_ALTERNADO sin componentes explícitos)
                    if (currentEjercicio.duracionSegundosOriginal > 0) {
                        DisplayTime(
                            currentTime = segundosRestantes,
                            label = stringResource(R.string.routine_time)
                        )
                    }
                    if (currentEjercicio.repeticionesOriginal.isNotBlank() && currentEjercicio.repeticionesOriginal != "0") {
                        DisplayRepetitions(
                            repetitions = currentEjercicio.repeticionesOriginal,
                            isUnilateral = currentEjercicio.esUnilateral
                        )
                    }
                    // Text(stringResource(R.string.exercise_detail_not_available), textAlign = TextAlign.Center)
                }

                if (currentEjercicio.esUnilateral && currentEjercicio.componentes.isEmpty()){
                    Text(stringResource(R.string.perform_for_each_side), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Botón para ver video
        // El videoUrl debe ser del ejercicio padre o del componente activo si lo tiene.
        // Asumimos que el ViewModel podría actualizar currentEjercicio.videoUrl si el componente tiene uno específico.
        // O podrías tener: val videoUrlToShow = componenteActivo?.videoUrl ?: currentEjercicio.videoUrl
        currentEjercicio.videoUrl?.let { videoUrl ->
            if (videoUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { onWatchVideoClick(videoUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.watch_exercise_video_icon_desc),
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.watch_exercise_video))
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f, fill = false)) // Empuja el botón hacia abajo
        /*
        // Botón de Siguiente/Finalizar
        Button(
            onClick = onNextClick,
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            AnimatedContent(
                targetState = buttonText,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally { width -> width } + fadeIn(),
                        initialContentExit = slideOutHorizontally { width -> -width } + fadeOut()
                    )
                },
                label = "buttonTextAnimation"
            ) { text ->
                Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
         */
    }
}
@Composable
fun InitialCountdown(countdownInicial: Int, routineName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Added padding
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.routine_starting_soon),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = routineName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp) // Adjusted padding
        )
        AnimatedContent(
            targetState = countdownInicial,
            transitionSpec = {
                slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
            }, label = "CountdownAnimation"
        ) { targetCount ->
            Text(
                text = targetCount.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
@Composable
fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(IntrinsicSize.Min), // Para que todas las cards tengan la misma altura
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp) // Redondeo para los InfoBox
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight() // Para que ocupe toda la altura disponible en la Card
                .padding(horizontal = 12.dp, vertical = 8.dp), // Ajuste de padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(), // MAYÚSCULAS para label
                style = MaterialTheme.typography.labelSmall, // labelSmall es más apropiado
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp)) // Pequeño espacio
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium, // Un poco más grande para el valor
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun IntegratedRestDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    secondsRemaining: Int,
    totalRestSeconds: Int,
    nextUpMessage: String,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall, // Estilo más prominente para el título
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado entre elementos
                ) {
                    Text(
                        text = stringResource(R.string.rest_time_left),
                        style = MaterialTheme.typography.bodyLarge, // Un poco más pequeño que el título
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedContent(
                        targetState = secondsRemaining.coerceAtLeast(0),
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn() + scaleIn(),
                                initialContentExit = fadeOut() + scaleOut()
                            )
                        }, label = "RestTimeAnimation"
                    ) { targetTime ->
                        Text(
                            // text = targetTime.toString(),
                            text = formatTime(targetTime), // Usar formato MM:SS
                            style = MaterialTheme.typography.displayMedium, // Ligeramente más pequeño que en ExerciseContent
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (totalRestSeconds > 0) { // Mostrar barra solo si hay tiempo total
                        LinearProgressIndicator(
                            progress = { 1f - (secondsRemaining.toFloat() / totalRestSeconds.toFloat()).coerceIn(0f, 1f) }, // Asegurar que el progreso esté entre 0 y 1
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // No tan ancho
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }
                    Text(
                        text = nextUpMessage,
                        style = MaterialTheme.typography.titleMedium, // Ligeramente más grande
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(R.string.skip_rest_button))
                }
            },
            shape = RoundedCornerShape(16.dp) // Bordes redondeados para el diálogo
        )
    }
}
// Función auxiliar para formatear el tiempo a MM:SS o SS
fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        String.format("%02d:%02d", minutes, seconds)
    } else {
        String.format("%02d", seconds)
    }
}
// Función auxiliar para obtener el texto del botón "Siguiente"
@Composable
fun getNextButtonText(uiState: RoutineUiState, context: Context): String {
    val rutina = uiState.rutina ?: return stringResource(R.string.button_loading)
    val currentEjercicio = uiState.ejercicioActual ?: return stringResource(R.string.button_loading) // Valor por defecto si no hay ejercicio
    if (currentEjercicio.tipoEjercicio == TipoDeEjercicio.SUPERSET_SEQUENCIAL &&
        currentEjercicio.componentes.isNotEmpty() &&
        uiState.componenteEjercicioActual != null &&
        uiState.indiceComponenteActual < currentEjercicio.componentes.size - 1) {
        return stringResource(R.string.button_next_component) // Necesitarás este string
    }
    return when {
        // Si el ejercicio actual tiene series y no hemos completado todas las series para este ejercicio
        currentEjercicio.numeroDeSeries > 1 && uiState.serieActualEjercicio < currentEjercicio.numeroDeSeries -> {
            stringResource(R.string.button_next_set)
        }
        // Si aún quedan ejercicios en la ronda actual
        uiState.indiceEjercicioActual < rutina.ejercicios.size - 1 -> {
            stringResource(R.string.button_next_exercise_action)
        }
        // Si aún quedan rondas en la rutina
        uiState.rondaActual < rutina.numeroDeRondas -> {
            stringResource(R.string.button_next_round_action)
        }
        // Si es el último ejercicio de la última ronda (o el único ejercicio si solo hay uno y una ronda)
        else -> {
            stringResource(R.string.button_finish_routine)
        }
    }
}
@Composable
fun getNextUpMessage(uiState: RoutineUiState, context: Context): String {
    val rutina = uiState.rutina ?: return ""
    return when (uiState.estado) {
        RoutineExecutionState.REST_BETWEEN_SETS -> {
            val ejercicio = rutina.ejercicios.getOrNull(uiState.indiceEjercicioActual)
            // Para "próxima serie", sumamos 1 a la serie actual solo si no es ya la última.
            // Si es la última serie, el descanso es para el siguiente ejercicio o ronda.
            val proxSerieMostrada = uiState.serieActualEjercicio + 1 // Se asume que este descanso es ANTES de la siguiente serie.
            val totalSeries = ejercicio?.numeroDeSeries ?: uiState.serieActualEjercicio
            ejercicio?.let {
                context.getString(R.string.rest_next_set_info, proxSerieMostrada, totalSeries, it.nombre)
            } ?: context.getString(R.string.getting_ready_for_next_set)
        }
        RoutineExecutionState.REST_BETWEEN_EXERCISES -> {
            val proximoEjercicio = rutina.ejercicios.getOrNull(uiState.indiceEjercicioActual + 1)
            proximoEjercicio?.nombre?.let {
                context.getString(R.string.rest_next_exercise_is, it)
            } ?: run {
                // Si no hay próximo ejercicio, podría ser el descanso antes de la siguiente ronda
                if (uiState.rondaActual < rutina.numeroDeRondas) {
                    context.getString(R.string.rest_next_is_round, uiState.rondaActual + 1)
                } else {
                    context.getString(R.string.rest_well_done_routine_ending)
                }
            }
        }
        RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
            val proxRondaMostrada = uiState.rondaActual + 1
            val totalRondas = rutina.numeroDeRondas
            if (proxRondaMostrada <= totalRondas) {
                context.getString(R.string.rest_next_round_info, proxRondaMostrada, totalRondas)
            } else {
                context.getString(R.string.rest_well_done_routine_ending) // Ya se completaron todas las rondas
            }
        }
        else -> context.getString(R.string.getting_ready) // Mensaje genérico
    }
}
fun playSound(context: Context, type: String) {
    val soundId = when (type) {
        "start" -> R.raw.start_sound
        "beep" -> R.raw.beep_sound
        "rest_start" -> R.raw.beep
        "rest_end" -> R.raw.beep
        "exercise_start" -> R.raw.exercise_start_sound1
        "routine_finish" -> R.raw.routine_finish_sound
        else -> {
            Log.w("playSound", "Unknown sound type: $type")
            return
        }
    }
    try {
        val mediaPlayer = MediaPlayer.create(context, soundId)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            Log.e("playSound", "MediaPlayer Error: what $what, extra $extra for soundId $soundId")
            mp.release()
            true // True si el error fue manejado
        }
        mediaPlayer?.start()
    } catch (e: Exception) {
        Log.e("playSound", "Error playing sound $soundId: ${e.message}")
    }
}
@Composable
private fun DisplayTime(currentTime: Int, label: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge, // Anteriormente titleMedium
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            // text = formatTime(currentTime.coerceAtLeast(0)), // Ya tienes tu propia función formatTime
            text = formatTime(currentTime.coerceAtLeast(0)),
            style = MaterialTheme.typography.displayMedium, // Más grande para el tiempo
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
@Composable
private fun DisplayRepetitions(repetitions: String, isUnilateral: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.routine_repetitions_label), // "REPETICIONES"
            style = MaterialTheme.typography.titleLarge, // Anteriormente titleMedium
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = repetitions,
            style = MaterialTheme.typography.displayMedium, // Anteriormente headlineSmall
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        if (isUnilateral) {
            Text(
                text = stringResource(R.string.routine_per_side), // "(por lado)"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text( // Podrías tener un string diferente si no es unilateral y quieres indicar algo más
                text = stringResource(R.string.routine_do_it_at_your_pace), // Ejemplo: "A tu ritmo"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun DisplayTempo(tempoNote: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.routine_tempo_label), // "TEMPO"
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = tempoNote,
            style = MaterialTheme.typography.headlineSmall, // Anteriormente titleLarge
            color = MaterialTheme.colorScheme.secondary, // Un color diferente para destacar
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
private fun DisplayComponent(
    componente: ComponenteEjercicio,
    isCurrentlyActive: Boolean,
    imageLoader: ImageLoader // <-- AÑADIR imageLoader como parámetro
) {
    val backgroundColor = if (isCurrentlyActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(if (isCurrentlyActive) 3.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isCurrentlyActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Columna para Texto del Componente ---
            Column(
                modifier = Modifier.weight(1f), // Darle peso para que la imagen no lo empuje demasiado
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = componente.nombreEspecifico ?: stringResource(R.string.unnamed_component),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentlyActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrentlyActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row { // Para Reps y Tiempo en la misma línea debajo del nombre
                    componente.repeticiones?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = "Reps: $it", // Añadir etiqueta "Reps:"
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentlyActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp) // Espacio si también hay tiempo
                        )
                    }
                    componente.duracionSegundos?.let {
                        if (it > 0) {
                            Text(
                                text = "Tiempo: ${formatTime(it)}", // Añadir etiqueta "Tiempo:"
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentlyActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- Espacio e Imagen del Componente ---
            if (!componente.imagenUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = componente.imagenUrl,
                    imageLoader = imageLoader, // Usar el imageLoader pasado
                    contentDescription = "Imagen de ${componente.nombreEspecifico}",
                    modifier = Modifier
                        .size(64.dp) // Tamaño más pequeño para la imagen del componente en la lista
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            // Podrías añadir un Box con un Icon como placeholder si la imagen del componente es nula
            // else { Box(modifier = Modifier.size(64.dp)) { /* Icon placeholder */ } }
        }
    }
}