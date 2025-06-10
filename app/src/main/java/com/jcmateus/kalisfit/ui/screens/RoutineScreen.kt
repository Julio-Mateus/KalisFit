package com.jcmateus.kalisfit.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineExecutionState
import com.jcmateus.kalisfit.viewmodel.RoutineUiState
import com.jcmateus.kalisfit.viewmodel.RoutineViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

import kotlinx.coroutines.launch

/*
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
    val errorMessage by routineViewModel.errorMessage.collectAsState()
    val userProfile by userProfileViewModel.user.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var rutinaEmpezada by remember { mutableStateOf(false) }
    var countdownInicial by remember { mutableStateOf(3) }
    var rondaActual by remember { mutableStateOf(1) }
    var descansoEntreRondasActivo by remember { mutableStateOf(false) }
    var segundosDescansoRondaRestantes by remember { mutableStateOf(0) }
    var indiceEjercicioActual by remember { mutableStateOf(0) }
    var serieActualEjercicio by remember { mutableStateOf(1) }
    var descansoEntreSeriesActivo by remember { mutableStateOf(false) }
    var segundosDescansoSerieRestantes by remember { mutableStateOf(0) }
    // Nuevos estados para el descanso entre ejercicios
    var descansoEntreEjerciciosActivo by remember { mutableStateOf(false) }
    var segundosDescansoEjercicioRestantes by remember { mutableStateOf(0) }
    var segundosEjercicioRestantes by remember { mutableStateOf(0) }
    var tiempoTotalSesionSegundos by remember { mutableStateOf(0) }
    var temporizadorSesionActivo by remember { mutableStateOf(false) }
    val rutinaCargada = rutinaState.value
    // Función para encapsular la lógica de finalización y guardado
    fun finalizarRutinaYGuardarProgreso() {
        temporizadorSesionActivo = false
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val userProfileVal = userProfile
        val rutinaCargadaVal = rutinaCargada

        if (currentUserId != null && userProfileVal != null && rutinaCargadaVal != null && rutinaId != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                routineViewModel.saveRoutineProgress(
                    userId = currentUserId,
                    userProfile = userProfileVal,
                    completedRoutine = rutinaCargadaVal,
                    rondasCompletadas = rondaActual, // Asegúrate que sea el número correcto
                    tiempoTotalSegundos = tiempoTotalSesionSegundos,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_routine_completed_saving_success))
                        }
                        val currentDetailRoute = Routes.routineDetail(rutinaId)
                        navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                            popUpTo(currentDetailRoute) { inclusive = true }
                        }
                    },
                    onError = { errorMsg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.error_saving_progress, errorMsg),
                                duration = SnackbarDuration.Long
                            )
                        }
                        val currentDetailRoute = Routes.routineDetail(rutinaId)
                        navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) { // Considerar una pantalla de error/reintento
                            popUpTo(currentDetailRoute) { inclusive = true }
                        }
                    }
                )
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_api_level_too_low_for_saving), duration = SnackbarDuration.Long)
                }
                val currentDetailRoute = Routes.routineDetail(rutinaId)
                navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                    popUpTo(currentDetailRoute) { inclusive = true }
                }
            }
        } else {
            temporizadorSesionActivo = false
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_cannot_save_progress_user_data), duration = SnackbarDuration.Long)
            }
            if (rutinaId != null) {
                val currentDetailRoute = Routes.routineDetail(rutinaId)
                navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                    popUpTo(currentDetailRoute) { inclusive = true }
                }
            } else {
                navController.popBackStack()
            }
        }
    }
    fun prepararEjercicioActual() {
        if (rutinaCargada == null || rutinaCargada.ejercicios.isEmpty()) return
        val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
        ejercicio?.let {
            // Solo establecer segundosEjercicioRestantes si es un ejercicio por tiempo
            if (it.duracionSegundos > 0 && it.repeticiones <= 0) {
                segundosEjercicioRestantes = it.duracionSegundos
            } else {
                segundosEjercicioRestantes = 0 // Para ejercicios por repeticiones, el tiempo no es el contador principal
            }
        }
    }
    // MODIFICADO: avanzarRutina ahora incluye la lógica para descansoEntreEjercicios
    fun avanzarRutina() {
        if (rutinaCargada == null) return
        val ejercicioActual = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual) ?: return

        // CASO 1: Aún quedan series en el ejercicio actual
        if (serieActualEjercicio < ejercicioActual.numeroDeSeries) {
            if (ejercicioActual.descansoEntreSeriesSegundos > 0) {
                segundosDescansoSerieRestantes = ejercicioActual.descansoEntreSeriesSegundos
                descansoEntreSeriesActivo = true
            } else {
                serieActualEjercicio++
                prepararEjercicioActual()
            }
        }
        // CASO 2: Series completadas para el ejercicio actual. Ver qué sigue.
        else {
            val descansoPostEjercicio = ejercicioActual.descansoDespuesEjercicioSegundos // Asume que este campo existe

            // 2a: ¿Hay más ejercicios en esta ronda?
            if (indiceEjercicioActual < rutinaCargada.ejercicios.size - 1) {
                if (descansoPostEjercicio > 0) {
                    segundosDescansoEjercicioRestantes = descansoPostEjercicio
                    descansoEntreEjerciciosActivo = true
                } else {
                    serieActualEjercicio = 1
                    indiceEjercicioActual++
                    prepararEjercicioActual()
                }
            }
            // 2b: Último ejercicio de la ronda. ¿Hay más rondas?
            else if (rondaActual < rutinaCargada.numeroDeRondas) {
                if (descansoPostEjercicio > 0) {
                    // Descanso post-ejercicio, luego el LaunchedEffect de este descanso
                    // activará el descanso entre rondas si es necesario.
                    segundosDescansoEjercicioRestantes = descansoPostEjercicio
                    descansoEntreEjerciciosActivo = true
                } else if (rutinaCargada.descansoEntreRondasSegundos > 0) {
                    segundosDescansoRondaRestantes = rutinaCargada.descansoEntreRondasSegundos
                    descansoEntreRondasActivo = true
                } else { // Sin descanso post-ejercicio ni entre rondas, pasar a la siguiente ronda
                    rondaActual++
                    indiceEjercicioActual = 0
                    serieActualEjercicio = 1
                    prepararEjercicioActual()
                }
            }
            // 2c: Último ejercicio, última serie, última ronda -> Finalizar.
            else {
                if (descansoPostEjercicio > 0) {
                    // Descanso post-ejercicio final, luego el LaunchedEffect llamará a finalizarRutina
                    segundosDescansoEjercicioRestantes = descansoPostEjercicio
                    descansoEntreEjerciciosActivo = true
                } else {
                    finalizarRutinaYGuardarProgreso()
                }
            }
        }
    }
    val imageLoader = remember(context) {
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
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    // Actualizado para incluir todos los estados de actividad/descanso
    BackHandler(enabled = rutinaEmpezada || descansoEntreRondasActivo || descansoEntreSeriesActivo || descansoEntreEjerciciosActivo) {
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
                        navController.popBackStack()
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
    LaunchedEffect(temporizadorSesionActivo) {
        if (temporizadorSesionActivo) {
            while (isActive && temporizadorSesionActivo) { // Usar isActive para coroutines
                delay(1000)
                if (temporizadorSesionActivo) { // Doble check por si se canceló
                    tiempoTotalSesionSegundos++
                }
            }
        }
    }
    // Actualizado para incluir todos los estados de descanso en la condición del countdown
    LaunchedEffect(rutinaCargada, rutinaEmpezada, descansoEntreRondasActivo, descansoEntreSeriesActivo, descansoEntreEjerciciosActivo) {
        if (rutinaCargada != null && !rutinaEmpezada &&
            !descansoEntreRondasActivo && !descansoEntreSeriesActivo && !descansoEntreEjerciciosActivo
        ) {
            countdownInicial = 3
            while (isActive && countdownInicial > 0) {
                delay(1000)
                countdownInicial--
            }
            if (isActive && countdownInicial == 0) {
                rutinaEmpezada = true
                temporizadorSesionActivo = true
                prepararEjercicioActual()
            }
        }
    }
    // Actualizado para incluir todos los estados de descanso en la condición del temporizador de ejercicio
    LaunchedEffect(
        segundosEjercicioRestantes,
        rutinaEmpezada,
        descansoEntreRondasActivo,
        descansoEntreSeriesActivo,
        descansoEntreEjerciciosActivo,
        indiceEjercicioActual,
        serieActualEjercicio, // Importante para re-evaluar si el ejercicio es por tiempo
        rondaActual,
        rutinaCargada // rutinaCargada también como key
    ) {
        val rutina = rutinaCargada ?: return@LaunchedEffect
        val currentEjercicio = rutina.ejercicios.getOrNull(indiceEjercicioActual) ?: return@LaunchedEffect

        // Este temporizador SOLO debe correr si:
        // 1. La rutina ha empezado.
        // 2. NO hay NINGÚN descanso activo.
        // 3. El ejercicio actual ES POR TIEMPO.
        // 4. Quedan segundos para ese ejercicio.
        if (rutinaEmpezada &&
            !descansoEntreRondasActivo && !descansoEntreSeriesActivo && !descansoEntreEjerciciosActivo &&
            currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0 && // Es por tiempo
            segundosEjercicioRestantes > 0
        ) {
            delay(1000) // Esperar un segundo
            if (isActive && // Corutina sigue activa
                rutinaEmpezada && // La rutina sigue empezada
                !descansoEntreRondasActivo && !descansoEntreSeriesActivo && !descansoEntreEjerciciosActivo && // Ningún descanso se activó mientras esperábamos
                segundosEjercicioRestantes > 0 // Aún quedan segundos (por si acaso cambió)
            ) {
                // Verificar de nuevo por si el estado cambió mientras el delay estaba activo
                val ejercicioAunActual = rutinaCargada?.ejercicios?.getOrNull(indiceEjercicioActual)
                if (ejercicioAunActual != null && ejercicioAunActual.id == currentEjercicio.id && // Asegurarse que el ejercicio no cambió
                    ejercicioAunActual.duracionSegundos > 0 && ejercicioAunActual.repeticiones <= 0 && // Sigue siendo por tiempo
                    segundosEjercicioRestantes > 0) { // Y aún quedan segundos

                    segundosEjercicioRestantes--

                    if (segundosEjercicioRestantes == 0) {
                        // El tiempo del ejercicio (o de la serie por tiempo) ha terminado.
                        // avanzarRutina decidirá si hay descanso entre series, o si se completó el ejercicio.
                        avanzarRutina() // <--- ¡¡AÑADIR ESTO!!
                    }
                }
            }
        }
    }
    LaunchedEffect(descansoEntreSeriesActivo, rutinaCargada, indiceEjercicioActual) {
        if (descansoEntreSeriesActivo && rutinaCargada != null) {
            val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
            if (ejercicio != null && ejercicio.descansoEntreSeriesSegundos > 0) {
                // El valor inicial de segundosDescansoSerieRestantes se setea antes de activar el descanso
                // o aquí si es necesario asegurar que siempre se reinicie
                if (segundosDescansoSerieRestantes <= 0) segundosDescansoSerieRestantes = ejercicio.descansoEntreSeriesSegundos

                while (isActive && segundosDescansoSerieRestantes > 0 && descansoEntreSeriesActivo) {
                    delay(1000)
                    if (descansoEntreSeriesActivo) {
                        segundosDescansoSerieRestantes--
                    }
                }
                if (isActive && descansoEntreSeriesActivo) { // Si el descanso completó su curso
                    descansoEntreSeriesActivo = false
                    // Avanzar la serie aquí, ya que el descanso era ANTES de la siguiente serie
                    if (serieActualEjercicio < ejercicio.numeroDeSeries) {
                        serieActualEjercicio++
                        prepararEjercicioActual()
                    } else { // Si era la última serie
                        avanzarRutina() // Esto debería manejar la transición al siguiente ejercicio o ronda
                    }
                }
            } else { // No hay descanso definido o ejercicio nulo
                descansoEntreSeriesActivo = false
                // Si no hay descanso, pero se activó, avanzar directamente
                if (ejercicio != null && serieActualEjercicio < ejercicio.numeroDeSeries) {
                    serieActualEjercicio++
                    prepararEjercicioActual()
                } else {
                    avanzarRutina()
                }
            }
        }
    }
    // NUEVO: LaunchedEffect para el descanso entre ejercicios
    LaunchedEffect(descansoEntreEjerciciosActivo, rutinaCargada) {
        if (descansoEntreEjerciciosActivo && rutinaCargada != null) {
            if (descansoEntreSeriesActivo || descansoEntreRondasActivo) {
                descansoEntreEjerciciosActivo = false // Evitar solapamientos
                return@LaunchedEffect
            }
            // Asumimos que segundosDescansoEjercicioRestantes se seteó antes de activar el flag
            while (isActive && segundosDescansoEjercicioRestantes > 0 && descansoEntreEjerciciosActivo) {
                delay(1000)
                if (descansoEntreEjerciciosActivo) {
                    segundosDescansoEjercicioRestantes--
                }
            }
            if (isActive && descansoEntreEjerciciosActivo) { // Si el descanso completó su curso
                descansoEntreEjerciciosActivo = false
                // Lógica de transición después del descanso entre ejercicios:
                if (indiceEjercicioActual < rutinaCargada.ejercicios.size - 1) {
                    serieActualEjercicio = 1
                    indiceEjercicioActual++
                    prepararEjercicioActual()
                } else if (rondaActual < rutinaCargada.numeroDeRondas) {
                    if (rutinaCargada.descansoEntreRondasSegundos > 0) {
                        segundosDescansoRondaRestantes = rutinaCargada.descansoEntreRondasSegundos // Prepara para el siguiente descanso
                        descansoEntreRondasActivo = true
                    } else {
                        rondaActual++
                        indiceEjercicioActual = 0
                        serieActualEjercicio = 1
                        prepararEjercicioActual()
                    }
                } else {
                    finalizarRutinaYGuardarProgreso()
                }
            }
        }
    }
    LaunchedEffect(descansoEntreRondasActivo, rutinaCargada) {
        if (descansoEntreRondasActivo && rutinaCargada != null) {
            if (rutinaCargada.descansoEntreRondasSegundos > 0) {
                // El valor inicial de segundosDescansoRondaRestantes se setea antes de activar el descanso
                if(segundosDescansoRondaRestantes <= 0) segundosDescansoRondaRestantes = rutinaCargada.descansoEntreRondasSegundos

                while (isActive && segundosDescansoRondaRestantes > 0 && descansoEntreRondasActivo) {
                    delay(1000)
                    if (descansoEntreRondasActivo) {
                        segundosDescansoRondaRestantes--
                    }
                }
                if (isActive && descansoEntreRondasActivo) { // Si el descanso completó su curso
                    descansoEntreRondasActivo = false
                    if (rondaActual < rutinaCargada.numeroDeRondas) {
                        rondaActual++
                        indiceEjercicioActual = 0
                        serieActualEjercicio = 1
                        prepararEjercicioActual()
                    } else { // Si era la última ronda, aunque no debería llegar aquí si avanzarRutina funciona bien
                        finalizarRutinaYGuardarProgreso()
                    }
                }
            } else { // No hay descanso entre rondas definido
                descansoEntreRondasActivo = false
                // Avanzar directamente si se activó por error
                if (rondaActual < rutinaCargada.numeroDeRondas) {
                    rondaActual++
                    indiceEjercicioActual = 0
                    serieActualEjercicio = 1
                    prepararEjercicioActual()
                } else {
                    finalizarRutinaYGuardarProgreso()
                }
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            // Manejo de estados de carga y error (sin cambios)
            AnimatedVisibility(visible = isLoading && rutinaCargada == null, enter = fadeIn(), exit = fadeOut()) { /* ... */ }
            AnimatedVisibility(visible = errorMessage != null && rutinaCargada == null, enter = fadeIn(), exit = fadeOut()) { /* ... */ }

            // Countdown inicial (sin cambios en su llamada, pero su LaunchedEffect fue actualizado)
            AnimatedVisibility(
                visible = rutinaCargada != null && !rutinaEmpezada && !descansoEntreRondasActivo && !descansoEntreSeriesActivo && !descansoEntreEjerciciosActivo,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InitialCountdown(countdownInicial = countdownInicial)
            }

            // --- DIÁLOGO DE DESCANSO UNIFICADO ---
            val isAnyRestActive = descansoEntreSeriesActivo || descansoEntreEjerciciosActivo || descansoEntreRondasActivo
            var currentRestTitle by remember { mutableStateOf("") }
            var currentRestSecondsRemaining by remember { mutableStateOf(0) }
            var currentTotalRestSeconds by remember { mutableStateOf(0) }
            var currentRestNextUpMessage by remember { mutableStateOf("") }
            var currentRestOnSkip: (() -> Unit)? by remember { mutableStateOf(null) }

            LaunchedEffect(
                isAnyRestActive, // Re-evaluar cuando cualquier descanso se active/desactive
                // Dependencias para actualizar los mensajes y tiempos del diálogo
                segundosDescansoSerieRestantes, segundosDescansoEjercicioRestantes, segundosDescansoRondaRestantes,
                serieActualEjercicio, indiceEjercicioActual, rondaActual, rutinaCargada
            ) {
                if (!isAnyRestActive || rutinaCargada == null) {
                    // Si no hay ningún descanso activo, o la rutina no está cargada, no hacemos nada.
                    // Esto también ayuda a limpiar los valores cuando se cierra el diálogo.
                    currentRestTitle = ""
                    currentRestSecondsRemaining = 0
                    currentTotalRestSeconds = 0
                    currentRestNextUpMessage = ""
                    currentRestOnSkip = null
                    return@LaunchedEffect
                }

                when {
                    descansoEntreSeriesActivo -> {
                        val ejercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
                        currentRestTitle = context.getString(R.string.rest_between_sets_title)
                        currentRestSecondsRemaining = segundosDescansoSerieRestantes
                        currentTotalRestSeconds = ejercicio?.descansoEntreSeriesSegundos ?: 0
                        // Para el mensaje, consideramos la serie que VIENE después del descanso
                        val proxSerieMostrada = if (ejercicio != null && serieActualEjercicio < ejercicio.numeroDeSeries) serieActualEjercicio + 1 else serieActualEjercicio
                        val totalSeries = ejercicio?.numeroDeSeries ?: serieActualEjercicio
                        currentRestNextUpMessage = ejercicio?.let {
                            context.getString(R.string.rest_next_set_info, proxSerieMostrada, totalSeries, it.nombre)
                        } ?: context.getString(R.string.getting_ready_for_next_set)

                        currentRestOnSkip = {
                            descansoEntreSeriesActivo = false // Desactiva este descanso
                            // Lógica de skip para descanso entre series
                            if (ejercicio != null) {
                                if (serieActualEjercicio < ejercicio.numeroDeSeries) {
                                    serieActualEjercicio++ // Avanza la serie manualmente
                                    prepararEjercicioActual()
                                } else {
                                    avanzarRutina() // Ya era la última serie, avanzar rutina
                                }
                            } else { avanzarRutina() } // Fallback
                        }
                    }
                    descansoEntreEjerciciosActivo -> {
                        val ejercicioActual = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual)
                        val proximoEjercicio = rutinaCargada.ejercicios.getOrNull(indiceEjercicioActual + 1)
                        currentRestTitle = context.getString(R.string.rest_between_exercises_title)
                        currentRestSecondsRemaining = segundosDescansoEjercicioRestantes
                        currentTotalRestSeconds = ejercicioActual?.descansoDespuesEjercicioSegundos ?: 0 // Asume campo existe
                        currentRestNextUpMessage = proximoEjercicio?.nombre?.let {
                            context.getString(R.string.rest_next_exercise_is, it)
                        } ?: run {
                            if (rondaActual < rutinaCargada.numeroDeRondas) {
                                context.getString(R.string.rest_next_is_round_break_or_next_round)
                            } else {
                                context.getString(R.string.rest_well_done_routine_ending)
                            }
                        }
                        currentRestOnSkip = {
                            descansoEntreEjerciciosActivo = false
                            // Lógica de skip para descanso entre ejercicios
                            if (indiceEjercicioActual < rutinaCargada.ejercicios.size - 1) {
                                serieActualEjercicio = 1
                                indiceEjercicioActual++
                                prepararEjercicioActual()
                            } else if (rondaActual < rutinaCargada.numeroDeRondas) {
                                if (rutinaCargada.descansoEntreRondasSegundos > 0) {
                                    segundosDescansoRondaRestantes = rutinaCargada.descansoEntreRondasSegundos
                                    descansoEntreRondasActivo = true
                                } else {
                                    rondaActual++; indiceEjercicioActual = 0; serieActualEjercicio = 1; prepararEjercicioActual()
                                }
                            } else {
                                finalizarRutinaYGuardarProgreso()
                            }
                        }
                    }
                    descansoEntreRondasActivo -> {
                        currentRestTitle = context.getString(R.string.rest_between_rounds_title)
                        currentRestSecondsRemaining = segundosDescansoRondaRestantes
                        currentTotalRestSeconds = rutinaCargada.descansoEntreRondasSegundos
                        val proxRondaMostrada = if (rondaActual < rutinaCargada.numeroDeRondas) rondaActual + 1 else rondaActual
                        val totalRondas = rutinaCargada.numeroDeRondas
                        currentRestNextUpMessage = context.getString(R.string.rest_next_round_info, proxRondaMostrada, totalRondas)
                        currentRestOnSkip = {
                            descansoEntreRondasActivo = false
                            // Lógica de skip para descanso entre rondas
                            if (rondaActual < rutinaCargada.numeroDeRondas) {
                                rondaActual++
                                indiceEjercicioActual = 0
                                serieActualEjercicio = 1
                                prepararEjercicioActual()
                            } else {
                                finalizarRutinaYGuardarProgreso()
                            }
                        }
                    }
                }
            }
            IntegratedRestDialog(
                visible = isAnyRestActive,
                onDismissRequest = {
                    // Al tocar fuera, simplemente cerramos el diálogo sin skipear.
                    // Los LaunchedEffects de los descansos seguirán su curso.
                    // Si se quiere que skipee: currentRestOnSkip?.invoke()
                    // Pero eso requiere que la lógica de skip esté muy bien afinada.
                    // Por ahora, solo cerramos.
                    if(descansoEntreSeriesActivo) descansoEntreSeriesActivo = false
                    if(descansoEntreEjerciciosActivo) descansoEntreEjerciciosActivo = false
                    if(descansoEntreRondasActivo) descansoEntreRondasActivo = false
                },
                title = currentRestTitle,
                secondsRemaining = currentRestSecondsRemaining,
                totalRestSeconds = currentTotalRestSeconds,
                nextUpMessage = currentRestNextUpMessage,
                onSkip = currentRestOnSkip
            )
            // UI Principal del Ejercicio (actualizado para usar !isAnyRestActive)
            AnimatedVisibility(
                visible = rutinaCargada != null && rutinaEmpezada && !isAnyRestActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                rutinaCargada?.let { rutina ->
                    val ejercicioActualData = rutina.ejercicios.getOrNull(indiceEjercicioActual)
                    ejercicioActualData?.let { currentEjercicio ->
                        ExerciseContent(
                            currentEjercicio = currentEjercicio,
                            rondaActual = rondaActual,
                            totalRondas = rutina.numeroDeRondas,
                            ejercicioActualNum = indiceEjercicioActual + 1,
                            totalEjercicios = rutina.ejercicios.size,
                            serieActual = serieActualEjercicio,
                            totalSeries = currentEjercicio.numeroDeSeries,
                            segundosRestantes = segundosEjercicioRestantes,
                            imageLoader = imageLoader,
                            onWatchVideoClick = { videoUrl ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
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
                            onNextClick = { avanzarRutina() },
                            isButtonEnabled = if (currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0) segundosEjercicioRestantes == 0 else true,
                            buttonText = when {
                                serieActualEjercicio < currentEjercicio.numeroDeSeries -> stringResource(R.string.button_next_set)
                                indiceEjercicioActual < rutina.ejercicios.size - 1 -> stringResource(R.string.button_next_exercise_action)
                                rondaActual < rutina.numeroDeRondas -> stringResource(R.string.button_next_round_action)
                                else -> stringResource(R.string.button_finish_routine)
                            }
                        )
                    } ?: run {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.routine_no_exercises))
                        }
                    }
                }
            }
            // Fallback (sin cambios)
            AnimatedVisibility(visible = !isLoading && rutinaCargada == null && errorMessage == null, enter = fadeIn(), exit = fadeOut()) { /* ... */ }
        }
    }
}
 */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    navController: NavController,
    rutinaId: String?,
    viewModel: RoutineViewModel = viewModel(),
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ImageLoader configurado para GIFs y versiones de Android
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

    LaunchedEffect(rutinaId) {
        if (rutinaId != null && uiState.rutina == null && uiState.estado == RoutineExecutionState.IDLE) {
            viewModel.startRoutine(rutinaId)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.rutina?.nombre ?: stringResource(R.string.routine_loading)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.estado == RoutineExecutionState.IDLE || uiState.estado == RoutineExecutionState.LOADING || uiState.estado == RoutineExecutionState.FINISHED) {
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
                        enabled = uiState.estado != RoutineExecutionState.IDLE &&
                                uiState.estado != RoutineExecutionState.LOADING &&
                                uiState.estado != RoutineExecutionState.FINISHED &&
                                uiState.estado != RoutineExecutionState.ERROR
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
                        enabled = uiState.estado != RoutineExecutionState.IDLE &&
                                uiState.estado != RoutineExecutionState.LOADING &&
                                uiState.estado != RoutineExecutionState.FINISHED &&
                                uiState.estado != RoutineExecutionState.ERROR
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.skip_step))
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (uiState.estado) {
                RoutineExecutionState.IDLE, RoutineExecutionState.LOADING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading_routine), modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 80.dp))
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
                            textAlign = TextAlign.Center
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
                    Log.d("RoutineScreen", "EXERCISE_ACTIVE: ${uiState.ejercicioActual?.nombre}, tiempoRestante UI: ${uiState.tiempoRestante}, duracionSegundos Modelo: ${uiState.ejercicioActual?.duracionSegundos}")
                    uiState.rutina?.let { rutina ->
                        uiState.ejercicioActual?.let { currentEjercicio ->
                            ExerciseContent(
                                currentEjercicio = currentEjercicio,
                                rondaActual = uiState.rondaActual,
                                totalRondas = rutina.numeroDeRondas,
                                ejercicioActualNum = uiState.indiceEjercicioActual + 1,
                                totalEjercicios = rutina.ejercicios.size,
                                serieActual = uiState.serieActualEjercicio,
                                totalSeries = currentEjercicio.numeroDeSeries,
                                segundosRestantes = uiState.tiempoRestante,
                                imageLoader = imageLoader, // <--- PASAR EL IMAGE LOADER
                                onWatchVideoClick = { videoUrl ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.no_app_for_video), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onNextClick = {
                                    scope.launch {
                                        viewModel.saltarSiguientePaso()
                                    }
                                },
                                isButtonEnabled = if (currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0) uiState.tiempoRestante <= 0 else true, // <= 0 para habilitar cuando llega a cero
                                buttonText = getNextButtonText(uiState, context)
                            )
                        } ?: Text(stringResource(R.string.error_exercise_not_found))
                    } ?: Text(stringResource(R.string.error_routine_not_found))
                }
                RoutineExecutionState.REST_BETWEEN_SETS,
                RoutineExecutionState.REST_BETWEEN_EXERCISES,
                RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                    val title = when (uiState.estado) {
                        RoutineExecutionState.REST_BETWEEN_SETS -> stringResource(R.string.rest_between_sets_title)
                        RoutineExecutionState.REST_BETWEEN_EXERCISES -> stringResource(R.string.rest_between_exercises_title)
                        RoutineExecutionState.REST_BETWEEN_ROUNDS -> stringResource(R.string.rest_between_rounds_title)
                        else -> ""
                    }
                    val totalRestSeconds = when (uiState.estado) {
                        RoutineExecutionState.REST_BETWEEN_SETS -> uiState.rutina?.ejercicios?.getOrNull(uiState.indiceEjercicioActual)?.descansoEntreSeriesSegundos ?: 0
                        RoutineExecutionState.REST_BETWEEN_EXERCISES -> uiState.rutina?.ejercicios?.getOrNull(uiState.indiceEjercicioActual)?.descansoDespuesEjercicioSegundos ?: 0
                        RoutineExecutionState.REST_BETWEEN_ROUNDS -> uiState.rutina?.descansoEntreRondasSegundos ?: 0
                        else -> 0
                    }
                    val nextUpMessage = getNextUpMessage(uiState, context)
                    IntegratedRestDialog(
                        visible = true,
                        onDismissRequest = { viewModel.setShowExitConfirmation(true) },
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
                    uiState.rutina?.let { rutina ->
                        uiState.ejercicioActual?.let { currentEjercicio ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (uiState.previousState) {
                                    RoutineExecutionState.EXERCISE_ACTIVE -> {
                                        ExerciseContent(
                                            currentEjercicio = currentEjercicio,
                                            rondaActual = uiState.rondaActual,
                                            totalRondas = rutina.numeroDeRondas,
                                            ejercicioActualNum = uiState.indiceEjercicioActual + 1,
                                            totalEjercicios = rutina.ejercicios.size,
                                            serieActual = uiState.serieActualEjercicio,
                                            totalSeries = currentEjercicio.numeroDeSeries,
                                            segundosRestantes = uiState.tiempoRestante,
                                            imageLoader = imageLoader, // <--- PASAR EL IMAGE LOADER
                                            onWatchVideoClick = { /* No-op o mostrar mensaje */ },
                                            onNextClick = { /* No se puede saltar en pausa */ },
                                            isButtonEnabled = false,
                                            buttonText = getNextButtonText(uiState, context)
                                        )
                                    }
                                    RoutineExecutionState.REST_BETWEEN_SETS,
                                    RoutineExecutionState.REST_BETWEEN_EXERCISES,
                                    RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                                        val title = when (uiState.previousState) {
                                            RoutineExecutionState.REST_BETWEEN_SETS -> stringResource(R.string.rest_between_sets_title)
                                            RoutineExecutionState.REST_BETWEEN_EXERCISES -> stringResource(R.string.rest_between_exercises_title)
                                            RoutineExecutionState.REST_BETWEEN_ROUNDS -> stringResource(R.string.rest_between_rounds_title)
                                            else -> ""
                                        }
                                        val totalRestSeconds = when (uiState.previousState) {
                                            RoutineExecutionState.REST_BETWEEN_SETS -> uiState.rutina?.ejercicios?.getOrNull(uiState.indiceEjercicioActual)?.descansoEntreSeriesSegundos ?: 0
                                            RoutineExecutionState.REST_BETWEEN_EXERCISES -> uiState.rutina?.ejercicios?.getOrNull(uiState.indiceEjercicioActual)?.descansoDespuesEjercicioSegundos ?: 0
                                            RoutineExecutionState.REST_BETWEEN_ROUNDS -> uiState.rutina?.descansoEntreRondasSegundos ?: 0
                                            else -> 0
                                        }
                                        val nextUpMessage = getNextUpMessage(uiState, context)
                                        IntegratedRestDialog(
                                            visible = true,
                                            onDismissRequest = { viewModel.setShowExitConfirmation(true) },
                                            title = title,
                                            secondsRemaining = uiState.tiempoRestante,
                                            totalRestSeconds = totalRestSeconds,
                                            nextUpMessage = nextUpMessage,
                                            onSkip = { /* No se puede saltar en pausa */ }
                                        )
                                    }
                                    else -> {}
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.routine_paused),
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { viewModel.togglePausa() }) {
                                            Text(stringResource(R.string.resume_routine))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {
                                            viewModel.setShowExitConfirmation(false) // Cerrar diálogo de confirmación si estaba abierto
                                            viewModel.reiniciarRutina()
                                            navController.popBackStack()
                                        }) {
                                            Text(stringResource(R.string.exit_routine))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                RoutineExecutionState.FINISHED -> {
                    LaunchedEffect(Unit) {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        val userProfileVal = userProfileViewModel.user.value
                        if (currentUserId != null && userProfileVal != null && uiState.rutina != null) {
                            // No es necesario verificar Build.VERSION.SDK_INT >= Build.VERSION_CODES.O para esta lógica
                            viewModel.saveRoutineProgress(
                                userId = currentUserId,
                                userProfile = userProfileVal,
                                rutinaId = uiState.rutina!!.id,
                                onSuccess = {
                                    scope.launch {
                                        navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                            popUpTo(Routes.routineDetail(rutinaId ?: "")) { inclusive = true }
                                            popUpTo(navController.graph.startDestinationId) { inclusive = false } // Opcional: limpiar más stack
                                        }
                                    }
                                },
                                onError = { errorMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(errorMsg, duration = SnackbarDuration.Long)
                                        // Aún navegar a la pantalla de éxito o a una de error específica
                                        navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                            popUpTo(Routes.routineDetail(rutinaId ?: "")) { inclusive = true }
                                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                        }
                                    }
                                }
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.error_cannot_save_progress_user_data), duration = SnackbarDuration.Long)
                                navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) {
                                    popUpTo(Routes.routineDetail(rutinaId ?: "")) { inclusive = true }
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.routine_finishing_and_saving), // Mensaje más descriptivo
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
                        viewModel.reiniciarRutina()
                        navController.popBackStack()
                    }
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

@Composable
fun InitialCountdown(countdownInicial: Int, routineName: String) {
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExerciseContent(
    currentEjercicio: Ejercicio,
    rondaActual: Int,
    totalRondas: Int,
    ejercicioActualNum: Int,
    totalEjercicios: Int,
    serieActual: Int,
    totalSeries: Int,
    segundosRestantes: Int,
    imageLoader: ImageLoader, // <--- CAMBIO: Recibe ImageLoader
    onWatchVideoClick: (String) -> Unit,
    onNextClick: () -> Unit,
    isButtonEnabled: Boolean,
    buttonText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically) // Ajuste para espaciado y centrado
    ) {
        // Indicadores de progreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround, // O Arrangement.spacedBy(8.dp)
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
            Log.d("ExerciseContent", "Ejercicio: ${currentEjercicio.nombre}, Serie Actual: $serieActual, Total Series (recibido): $totalSeries, Desde currentEjercicio: ${currentEjercicio.numeroDeSeries}")
            InfoBox(
                label = stringResource(R.string.routine_series),
                value = if (totalSeries > 0) "$serieActual / $totalSeries" else "-", // Manejar caso de 0 series
                modifier = Modifier.weight(1f)
            )
        }

        // Nombre del ejercicio y descripción
        Text(
            text = currentEjercicio.nombre,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = currentEjercicio.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Imagen del ejercicio
        currentEjercicio.imagenUrl?.let { url ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 3f) // Proporción para la imagen (ej. 16:10 o 16:9), ajusta según tus GIFs
                    .clip(RoundedCornerShape(12.dp)), // Bordes más redondeados
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = url, // <--- Solo la URL
                    imageLoader = imageLoader, // <--- Usar el ImageLoader pasado
                    contentDescription = currentEjercicio.nombre,
                    contentScale = ContentScale.Fit, // Para asegurar que todo el GIF es visible. Cambia a Crop si prefieres llenar.
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Repeticiones o tiempo
        if (currentEjercicio.repeticiones > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.routine_repetitions, currentEjercicio.repeticiones),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.routine_do_it_at_your_pace),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (currentEjercicio.duracionSegundos > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.routine_hold_for),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedContent(
                    targetState = segundosRestantes.coerceAtLeast(0),
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    }, label = "ExerciseTimeAnimation"
                ) { targetTime ->
                    Text(
                        // text = stringResource(R.string.routine_seconds_remaining, targetTime),
                        text = formatTime(targetTime), // Usa una función de formato si quieres MM:SS
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Botón para ver video (si hay URL)
        currentEjercicio.videoUrl?.let { videoUrl ->
            if (videoUrl.isNotBlank()) {
                OutlinedButton( // Usar OutlinedButton para diferenciarlo del botón principal
                    onClick = { onWatchVideoClick(videoUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.watch_exercise_video_icon_desc), modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.watch_exercise_video))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false)) // Empuja el botón hacia abajo pero permite que la columna se encoja si el contenido es poco

        // Botón de Siguiente/Finalizar
        Button(
            onClick = onNextClick,
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium, // Un poco menos redondeado para consistencia
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
    onSkip: () -> Unit
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
                    shape = MaterialTheme.shapes.medium
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
        "rest_start" -> R.raw.rest_start_sound
        "rest_end" -> R.raw.rest_end_sound
        "exercise_start" -> R.raw.exercise_start_sound
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