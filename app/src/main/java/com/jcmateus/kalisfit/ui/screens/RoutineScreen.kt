package com.jcmateus.kalisfit.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutineScreen(
    navController: NavController,
    rutinaId: String?,
    routineViewModel: RoutineViewModel = viewModel(),
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    val rutinaState = routineViewModel.rutina.collectAsState()
    val isLoading by routineViewModel.isLoading.collectAsState()
    val errorMessage by routineViewModel.errorMessage.collectAsState() // Error de carga de rutina
    val userProfile by userProfileViewModel.user.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados para el progreso de la rutina
    var rutinaEmpezada by remember { mutableStateOf(false) }
    var countdownInicial by remember { mutableStateOf(3) } // Countdown antes de empezar

    var rondaActual by remember { mutableStateOf(1) }
    var descansoEntreRondasActivo by remember { mutableStateOf(false) }
    var segundosDescansoRondaRestantes by remember { mutableStateOf(0) }

    var indiceEjercicioActual by remember { mutableStateOf(0) }
    var serieActualEjercicio by remember { mutableStateOf(1) }
    var descansoEntreSeriesActivo by remember { mutableStateOf(false) }
    var segundosDescansoSerieRestantes by remember { mutableStateOf(0) }

    var segundosEjercicioRestantes by remember { mutableStateOf(0) }

    // NUEVO: Estado para el tiempo total de la sesión
    var tiempoTotalSesionSegundos by remember { mutableStateOf(0) }
    // NUEVO: Estado para controlar si el temporizador de sesión está activo
    var temporizadorSesionActivo by remember { mutableStateOf(false) }


    val rutinaCargada = rutinaState.value

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            // .components { // Descomenta y configura si necesitas decoders especiales como GIF
            //     if (Build.VERSION.SDK_INT >= 28) {
            //         add(ImageDecoderDecoder.Factory())
            //     } else {
            //         add(GifDecoder.Factory())
            //     }
            // }
            .build()
    }

    // --- MANEJO DEL BOTÓN DE RETROCESO ---
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = rutinaEmpezada || descansoEntreRondasActivo || descansoEntreSeriesActivo) {
        showExitConfirmationDialog = true
    }

    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text(stringResource(R.string.dialog_exit_routine_title)) },
            text = { Text(stringResource(R.string.dialog_exit_routine_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmationDialog = false
                        navController.popBackStack() // O navega a una pantalla específica
                    }
                ) { Text(stringResource(R.string.dialog_exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmationDialog = false }) {
                    Text(stringResource(R.string.dialog_exit_cancel))
                }
            }
        )
    }


    // --- EFECTOS DE CARGA Y PREPARACIÓN ---
    LaunchedEffect(rutinaId) {
        if (rutinaId != null) {
            routineViewModel.loadRutina(rutinaId)
        } else {
            Toast.makeText(context, context.getString(R.string.error_no_routine_id), Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        userProfileViewModel.loadUserProfile()
    }

    fun prepararEjercicioActual() {
        if (rutinaCargada == null || rutinaCargada.ejercicios.isEmpty()) return
        val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
        ejercicio?.let {
            segundosEjercicioRestantes = it.duracionSegundos
        }
    }

    // --- TEMPORIZADORES Y LÓGICA DE PROGRESIÓN ---

    // NUEVO: Temporizador para el tiempo total de la sesión
    LaunchedEffect(temporizadorSesionActivo) {
        if (temporizadorSesionActivo) {
            while (temporizadorSesionActivo) { // Continuará mientras esté activo
                delay(1000)
                if (temporizadorSesionActivo) { // Doble chequeo por si se detuvo durante el delay
                    tiempoTotalSesionSegundos++
                }
            }
        }
    }


    // 1. Countdown Inicial
    LaunchedEffect(rutinaCargada, rutinaEmpezada, descansoEntreRondasActivo, descansoEntreSeriesActivo) {
        if (rutinaCargada != null && !rutinaEmpezada &&
            !descansoEntreRondasActivo && !descansoEntreSeriesActivo
        ) {
            countdownInicial = 3
            while (countdownInicial > 0) {
                // context.playBeepSound(R.raw.beep_countdown)
                delay(1000)
                countdownInicial--
            }
            if (countdownInicial == 0) {
                rutinaEmpezada = true
                temporizadorSesionActivo = true // INICIA el temporizador de sesión
                prepararEjercicioActual()
            }
        }
    }

    // 2. Temporizador del Ejercicio (si es por tiempo)
    LaunchedEffect(
        segundosEjercicioRestantes,
        rutinaEmpezada,
        descansoEntreRondasActivo,
        descansoEntreSeriesActivo,
        // Añadir keys relevantes que puedan cambiar el comportamiento del temporizador
        indiceEjercicioActual,
        serieActualEjercicio,
        rondaActual
    ) {
        if (rutinaEmpezada && !descansoEntreRondasActivo && !descansoEntreSeriesActivo && segundosEjercicioRestantes > 0) {
            val currentEjercicio = rutinaCargada?.ejercicios?.getOrNull(indiceEjercicioActual)
            if (currentEjercicio != null && currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0) {
                delay(1000)
                // Doble chequeo por si el estado cambió mientras el delay estaba activo
                if (rutinaEmpezada && !descansoEntreRondasActivo && !descansoEntreSeriesActivo && segundosEjercicioRestantes > 0) {
                    segundosEjercicioRestantes--
                    // if (segundosEjercicioRestantes in 1..3) {
                    //     context.playBeepSound(R.raw.beep_final_seconds)
                    // }
                }
            }
        }
    }

    // 3. Temporizador de Descanso entre Series
    LaunchedEffect(descansoEntreSeriesActivo, rutinaCargada, indiceEjercicioActual) {
        if (descansoEntreSeriesActivo && rutinaCargada != null) {
            val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
            if (ejercicio != null && ejercicio.descansoEntreSeriesSegundos > 0) {
                segundosDescansoSerieRestantes = ejercicio.descansoEntreSeriesSegundos
                while (segundosDescansoSerieRestantes > 0 && descansoEntreSeriesActivo) {
                    delay(1000)
                    if (descansoEntreSeriesActivo) {
                        segundosDescansoSerieRestantes--
                    }
                }
                if (descansoEntreSeriesActivo) { // Solo si no se interrumpió/skipeó
                    descansoEntreSeriesActivo = false
                    serieActualEjercicio++
                    prepararEjercicioActual()
                }
            } else {
                descansoEntreSeriesActivo = false
                // No se avanza automáticamente la serie aquí si no hay descanso,
                // la función avanzarRutina o el onSkip lo manejarán.
            }
        }
    }

    // 4. Temporizador de Descanso entre Rondas
    LaunchedEffect(descansoEntreRondasActivo, rutinaCargada) {
        if (descansoEntreRondasActivo && rutinaCargada != null) {
            if (rutinaCargada.descansoEntreRondasSegundos > 0) {
                segundosDescansoRondaRestantes = rutinaCargada.descansoEntreRondasSegundos
                while (segundosDescansoRondaRestantes > 0 && descansoEntreRondasActivo) {
                    delay(1000)
                    if (descansoEntreRondasActivo) {
                        segundosDescansoRondaRestantes--
                    }
                }
                if (descansoEntreRondasActivo) {
                    descansoEntreRondasActivo = false
                    rondaActual++
                    indiceEjercicioActual = 0
                    serieActualEjercicio = 1
                    prepararEjercicioActual()
                }
            } else { // Sin descanso entre rondas, transicionar inmediatamente
                descansoEntreRondasActivo = false
                rondaActual++
                indiceEjercicioActual = 0
                serieActualEjercicio = 1
                prepararEjercicioActual()
            }
        }
    }

    // --- LÓGICA DE BOTONES PARA AVANZAR ---
    fun avanzarRutina() {
        if (rutinaCargada == null) return
        val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual) ?: return

        // Caso 1: Aún quedan series en el ejercicio actual
        if (serieActualEjercicio < ejercicio.numeroDeSeries) {
            if (ejercicio.descansoEntreSeriesSegundos > 0) {
                descansoEntreSeriesActivo = true
                // El LaunchedEffect del descanso se encarga del resto
            } else { // No hay descanso entre series
                serieActualEjercicio++
                prepararEjercicioActual()
            }
        }
        // Caso 2: Última serie del ejercicio actual, pero quedan más ejercicios en la ronda
        else if (indiceEjercicioActual < rutinaCargada.ejercicios.size - 1) {
            serieActualEjercicio = 1
            indiceEjercicioActual++
            prepararEjercicioActual()
        }
        // Caso 3: Última serie del último ejercicio de la ronda, pero quedan más rondas
        else if (rondaActual < rutinaCargada.numeroDeRondas) {
            if (rutinaCargada.descansoEntreRondasSegundos > 0) {
                descansoEntreRondasActivo = true
                // El LaunchedEffect del descanso se encarga del resto
            } else { // No hay descanso entre rondas
                rondaActual++
                indiceEjercicioActual = 0
                serieActualEjercicio = 1
                prepararEjercicioActual()
            }
        }
        // Caso 4: Última serie, último ejercicio, última ronda ¡Rutina completada!
        else {
            temporizadorSesionActivo = false // DETIENE el temporizador de sesión
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val userProfileVal = userProfile
            val rutinaCargadaVal = rutinaCargada

            if (currentUserId != null && userProfileVal != null && rutinaCargadaVal != null && rutinaId != null) {
                Log.d("RoutineScreen", "Rutina completada. Guardando progreso... Rondas: $rondaActual, Tiempo: $tiempoTotalSesionSegundos")
                // Asegúrate que la función en el ViewModel espera Build.VERSION_CODES.O si es necesario
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    routineViewModel.saveRoutineProgress(
                        userId = currentUserId,
                        userProfile = userProfileVal,
                        completedRoutine = rutinaCargadaVal,
                        rondasCompletadas = rondaActual, // Pasar rondas completadas
                        tiempoTotalSegundos = tiempoTotalSesionSegundos, // Pasar tiempo total
                        onSuccess = {
                            Log.d("RoutineScreen", "Progreso guardado con éxito. Navegando a success screen.")
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.toast_routine_completed_saving_success))
                            }
                            val currentDetailRoute = Routes.routineDetail(rutinaId)
                            navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                popUpTo(currentDetailRoute) { inclusive = true }
                            }
                        },
                        onError = { errorMsg ->
                            Log.e("RoutineScreen", "Error al guardar progreso: $errorMsg")
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.error_saving_progress, errorMsg),
                                    duration = SnackbarDuration.Long
                                )
                            }
                            val currentDetailRoute = Routes.routineDetail(rutinaId)
                            navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                popUpTo(currentDetailRoute) { inclusive = true }
                            }
                        }
                    )
                } else {
                    // Manejar el caso donde la API es menor a O si saveRoutineProgress lo requiere
                    // Podrías mostrar un mensaje o no guardar.
                    Log.w("RoutineScreen", "No se puede guardar el progreso, API level demasiado bajo.")
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.error_api_level_too_low_for_saving), duration = SnackbarDuration.Long)
                    }
                    // Igualmente, navega a la pantalla de éxito para sacar al usuario
                    val currentDetailRoute = Routes.routineDetail(rutinaId)
                    navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                        popUpTo(currentDetailRoute) { inclusive = true }
                    }
                }
            } else {
                Log.w("RoutineScreen", "No se puede guardar el progreso: Faltan datos. UID: $currentUserId, Profile: $userProfileVal, Rutina: $rutinaCargadaVal, RutinaID: $rutinaId")
                temporizadorSesionActivo = false // Asegúrate que esté detenido
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_cannot_save_progress_user_data), duration = SnackbarDuration.Long)
                }
                if (rutinaId != null) {
                    val currentDetailRoute = Routes.routineDetail(rutinaId)
                    navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) { // Navega a éxito de todas formas
                        popUpTo(currentDetailRoute) { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            }
        }
    }

    // --- UI ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- ESTADOS DE CARGA, ERROR, COUNTDOWN INICIAL ---
            if (isLoading && rutinaCargada == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Scaffold
            }

            if (errorMessage != null && rutinaCargada == null) {
                Text(
                    stringResource(R.string.error_loading_routine_param, errorMessage ?: "Unknown error"),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
                return@Scaffold
            }

            if (rutinaCargada != null && !rutinaEmpezada && !descansoEntreRondasActivo && !descansoEntreSeriesActivo) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (countdownInicial > 0) "$countdownInicial" else stringResource(R.string.routine_lets_go),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                return@Scaffold
            }

            // --- PANTALLA DE DESCANSO ENTRE SERIES ---
            if (rutinaCargada != null && descansoEntreSeriesActivo) {
                val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
                val proxSerie = serieActualEjercicio + 1 // Lo que sería la siguiente serie
                val totalSeries = ejercicio?.numeroDeSeries ?: serieActualEjercicio

                DescansoScreen(
                    titulo = stringResource(R.string.rest_between_sets_title),
                    segundosRestantes = segundosDescansoSerieRestantes,
                    mensajeSiguiente = ejercicio?.let {
                        stringResource(R.string.rest_next_set_info, proxSerie, totalSeries, it.nombre)
                    } ?: stringResource(R.string.getting_ready_for_next_set),
                    onSkip = {
                        descansoEntreSeriesActivo = false
                        // Avanzar a la siguiente serie y preparar
                        if (ejercicio != null) {
                            if (serieActualEjercicio < ejercicio.numeroDeSeries) {
                                serieActualEjercicio++ // Avanza la serie aquí
                                prepararEjercicioActual()
                            } else { // Ya era la última serie, skipear descanso implica pasar al siguiente ejercicio/ronda
                                avanzarRutina()
                            }
                        }
                    }
                )
                return@Scaffold
            }

            // --- PANTALLA DE DESCANSO ENTRE RONDAS ---
            if (rutinaCargada != null && descansoEntreRondasActivo) {
                val proxRonda = rondaActual + 1
                val totalRondas = rutinaCargada.numeroDeRondas

                DescansoScreen(
                    titulo = stringResource(R.string.rest_between_rounds_title),
                    segundosRestantes = segundosDescansoRondaRestantes,
                    mensajeSiguiente = stringResource(R.string.rest_next_round_info, proxRonda, totalRondas),
                    onSkip = {
                        descansoEntreRondasActivo = false
                        // Avanzar a la siguiente ronda y preparar
                        if (rondaActual < rutinaCargada.numeroDeRondas) {
                            rondaActual++ // Avanza la ronda aquí
                            indiceEjercicioActual = 0
                            serieActualEjercicio = 1
                            prepararEjercicioActual()
                        } else { // Ya era la última ronda (improbable skipear aquí si ya terminó)
                            avanzarRutina() // Para la lógica de finalización
                        }
                    }
                )
                return@Scaffold
            }


            // --- UI PRINCIPAL DEL EJERCICIO ---
            if (rutinaCargada != null && rutinaEmpezada && !descansoEntreRondasActivo && !descansoEntreSeriesActivo) {
                val ejercicioActual = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)

                ejercicioActual?.let { currentEjercicio ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .systemBarsPadding(), // Para evitar solapamiento con barras de sistema
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(
                                R.string.progress_indicator_round_exercise_series,
                                rondaActual, rutinaCargada.numeroDeRondas,
                                indiceEjercicioActual + 1, rutinaCargada.ejercicios.size,
                                serieActualEjercicio, currentEjercicio.numeroDeSeries
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        // Barra de progreso del tiempo del ejercicio o de las series
                        if (currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0) {
                            LinearProgressIndicator(
                                progress = { // Cambio aquí para que sea un lambda
                                    if (currentEjercicio.duracionSegundos > 0) {
                                        (segundosEjercicioRestantes.toFloat() / currentEjercicio.duracionSegundos.toFloat()).coerceIn(0f, 1f)
                                    } else { 0f }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        } else { // Ejercicio por repeticiones
                            if (currentEjercicio.numeroDeSeries > 1) { // Mostrar progreso de series si hay más de una
                                LinearProgressIndicator(
                                    progress = { // Cambio aquí para que sea un lambda
                                        (serieActualEjercicio.toFloat() / currentEjercicio.numeroDeSeries.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            } else { // Si solo es una serie (o no es por tiempo), un simple divisor
                                Divider(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                            }
                        }


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 250.dp) // Ajustar altura
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.medium
                                )
                                .clip(MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentEjercicio.imagenUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentEjercicio.imagenUrl)
                                        .crossfade(true)
                                        .placeholder(R.drawable.ic_default_placeholder) // Asegúrate que estos drawables existen
                                        .error(R.drawable.ic_error_placeholder)         // Asegúrate que estos drawables existen
                                        .build(),
                                    imageLoader = imageLoader, // Usar el imageLoader definido
                                    contentDescription = currentEjercicio.nombre,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!currentEjercicio.videoUrl.isNullOrBlank()) {
                                // Placeholder si solo hay video y no imagen
                                Icon(Icons.Filled.PlayCircleOutline, stringResource(R.string.desc_exercise_video_placeholder), modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Filled.ImageNotSupported, stringResource(R.string.no_media_available), modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            currentEjercicio.videoUrl?.takeIf { it.isNotBlank() }?.let { videoUriString ->
                                FilledTonalButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUriString))
                                            ContextCompat.startActivity(context, intent, null)
                                        } catch (e: Exception) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.error_opening_video_link) + ": ${e.localizedMessage}",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Filled.PlayCircleFilled, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.button_watch_video))
                                }
                            }
                        }

                        Text(currentEjercicio.nombre, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Text(currentEjercicio.descripcion.ifBlank { stringResource(R.string.no_description_available) }, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))

                        if (currentEjercicio.repeticiones > 0) {
                            Text(
                                text = stringResource(R.string.routine_repetitions_target, currentEjercicio.repeticiones),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (currentEjercicio.duracionSegundos > 0) {
                            Text(
                                text = stringResource(R.string.routine_seconds_remaining, segundosEjercicioRestantes.coerceAtLeast(0)),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f)) // Empuja el botón hacia abajo

                        val esEjercicioPorTiempo = currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0
                        val isButtonEnabled = if (esEjercicioPorTiempo) segundosEjercicioRestantes == 0 else true

                        val esUltimaSerieDelEjercicio = serieActualEjercicio == currentEjercicio.numeroDeSeries
                        val esUltimoEjercicioDeLaRonda = indiceEjercicioActual == rutinaCargada.ejercicios.size - 1
                        val esUltimaRondaDeLaRutina = rondaActual == rutinaCargada.numeroDeRondas

                        val buttonText = when {
                            !esUltimaSerieDelEjercicio -> stringResource(R.string.button_next_set)
                            !esUltimoEjercicioDeLaRonda -> stringResource(R.string.button_next_exercise_action)
                            !esUltimaRondaDeLaRutina -> stringResource(R.string.button_next_round_action)
                            else -> stringResource(R.string.button_finish_routine)
                        }

                        Button(
                            onClick = { avanzarRutina() },
                            enabled = isButtonEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min) // Usa IntrinsicSize.Min para altura adaptativa
                        ) {
                            AnimatedContent(
                                targetState = buttonText,
                                transitionSpec = {
                                    slideInHorizontally { width -> width } togetherWith
                                            slideOutHorizontally { width -> -width }
                                },
                                label = "buttonTextAnimation"
                            ) { text ->
                                Text(text, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp)) // Espacio al final
                    }
                } ?: run {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (rutinaCargada?.ejercicios?.isEmpty() == true) {
                            Text(stringResource(R.string.routine_no_exercises))
                        } else {
                            Text(stringResource(R.string.routine_loading_exercise))
                        }
                    }
                }
            } else if (!isLoading && rutinaCargada == null && errorMessage == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.routine_failed_to_load))
                }
            }
        } // Fin del Box principal
    } // Fin del Scaffold
}


@Composable
fun DescansoScreen(
    titulo: String,
    segundosRestantes: Int,
    mensajeSiguiente: String,
    onSkip: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding(), // Para evitar solapamiento con barras de sistema
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(
            //stringResource(R.string.rest_time_seconds_format, segundosRestantes.coerceAtLeast(0)), // Asegura que no sea negativo
            text = segundosRestantes.coerceAtLeast(0).toString(), // Más simple para el número grande
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(stringResource(R.string.seconds_short), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) // "seg."
        Spacer(Modifier.height(16.dp))
        Text(mensajeSiguiente, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        onSkip?.let {
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = it) {
                Text(stringResource(R.string.button_skip_rest))
            }
        }
    }
}

// Función de extensión para reproducir el sonido
fun Context.playBeepSound(soundResId: Int) {
    try {
        MediaPlayer.create(this, soundResId)?.apply {
            setOnCompletionListener { mp ->
                mp.reset() // Usa reset() antes de release() para asegurar que el MediaPlayer pueda ser reutilizado si es necesario (aunque aquí se crea uno nuevo cada vez)
                mp.release()
            }
            setOnErrorListener { _, _, _ ->
                Log.e("playBeepSound", "Error en MediaPlayer durante la reproducción del sonido: $soundResId")
                true /* Error manejado (previene el crash y la llamada a onCompletion) */
            }
            start()
        }
    } catch (e: Exception) { // Captura excepciones más generales durante la creación o inicio
        Log.e("playBeepSound", "Excepción al intentar reproducir sonido $soundResId", e)
    }
}
