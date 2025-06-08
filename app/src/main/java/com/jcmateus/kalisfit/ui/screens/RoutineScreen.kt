package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
// --- COMPOSABLE PARA EL DIÁLOGO DE DESCANSO INTEGRADO ---
@OptIn(ExperimentalAnimationApi::class) // Necesario para AnimatedContent
@Composable
fun IntegratedRestDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit, // Se llama cuando el usuario intenta cerrar el diálogo (ej. tocando fuera)
    title: String,
    secondsRemaining: Int,
    totalRestSeconds: Int, // Usado para el CircularProgressIndicator
    nextUpMessage: String,
    onSkip: (() -> Unit)? // Lambda para la acción del botón "Saltar"
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true) // Permite cerrar
        ) {
            Card(
                shape = MaterialTheme.shapes.large, // Bordes más redondeados
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)) // Fondo con elevación
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp) // Buen padding interno
                        .widthIn(min = 280.dp, max = 340.dp), // Controla el ancho del diálogo para que no sea demasiado ancho ni estrecho
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically) // Espaciado entre elementos
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) { // Tamaño del indicador de progreso
                        CircularProgressIndicator(
                            progress = { // Expresión lambda para el progreso
                                if (totalRestSeconds > 0) {
                                    // Calcula el progreso, asegurándose de que esté entre 0 y 1
                                    (secondsRemaining.toFloat() / totalRestSeconds.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    1f // Progreso completo si no hay segundos de descanso (evita división por cero)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp, // Grosor del trazo del indicador
                            color = MaterialTheme.colorScheme.primary, // Color del progreso
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), // Color de fondo del trazo
                            strokeCap = StrokeCap.Round // Bordes redondeados para el trazo
                        )
                        AnimatedContent( // Anima el cambio del texto de los segundos restantes
                            targetState = secondsRemaining.coerceAtLeast(0).toString(), // Asegura que no sea negativo
                            transitionSpec = {
                                // Define la animación para el cambio de texto (entrada y salida)
                                (slideInVertically { height -> height / 2 } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height / 2 } + fadeOut())
                                    .using(SizeTransform(clip = false)) // Permite que el tamaño cambie durante la animación
                            },
                            label = "restDialogTimerText" // Etiqueta para herramientas de testing/inspección
                        ) { targetCount ->
                            Text(
                                text = targetCount,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold), // Estilo grande y en negrita
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = nextUpMessage,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // Color que contrasta bien con `surfaceVariant`
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    // Muestra el botón de "Saltar" solo si la lambda onSkip no es nula
                    onSkip?.let { skipAction ->
                        Spacer(Modifier.height(8.dp)) // Espacio adicional antes del botón
                        FilledTonalButton(
                            onClick = skipAction, // Acción a ejecutar al hacer clic
                            modifier = Modifier.fillMaxWidth(0.8f), // Que ocupe el 80% del ancho disponible en la columna
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.button_skip_rest_description), // Descripción para accesibilidad
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing)) // Espacio estándar entre icono y texto
                            Text(stringResource(R.string.button_skip_rest))
                        }
                    }
                }
            }
        }
    }
}
// --- COMPOSABLES REUTILIZABLES PARA UNA UI MÁS LIMPIA ---
@Composable
fun InitialCountdown(countdownInicial: Int) {
    val currentCountdown by rememberUpdatedState(countdownInicial)
    val transition = updateTransition(targetState = currentCountdown, label = "countdownTransition")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState == 0) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            else tween(durationMillis = 300)
        }, label = "countdownScale"
    ) { count ->
        if (count == currentCountdown && count != 0) 1.2f else 1f
    }
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) }, label = "countdownAlpha"
    ) { count ->
        if (count == currentCountdown || count == 0) 1f else 0.7f
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (currentCountdown > 0) "$currentCountdown" else stringResource(R.string.routine_lets_go),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontSize = 100.sp), // Tamaño grande
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        )
    }
}

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
    imageLoader: ImageLoader,
    onWatchVideoClick: (String) -> Unit,
    onNextClick: () -> Unit,
    isButtonEnabled: Boolean,
    buttonText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Aumento del espaciado
    ) {
        // Indicador de Progreso General (Rondas, Ejercicios, Series)
        Text(
            stringResource(
                R.string.progress_indicator_round_exercise_series,
                rondaActual, totalRondas,
                ejercicioActualNum, totalEjercicios,
                serieActual, totalSeries
            ),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Barra de progreso del tiempo del ejercicio o de las series
        val isTimedExercise = currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0
        LinearProgressIndicator(
            progress = {
                if (isTimedExercise) {
                    if (currentEjercicio.duracionSegundos > 0) {
                        (segundosRestantes.toFloat() / currentEjercicio.duracionSegundos.toFloat()).coerceIn(0f, 1f)
                    } else { 0f }
                } else { // Ejercicio por repeticiones o series
                    if (totalSeries > 0) {
                        (serieActual.toFloat() / totalSeries.toFloat()).coerceIn(0f, 1f)
                    } else { 0f }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)), // Barras de progreso redondeadas
            color = if (isTimedExercise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f), // Un ratio más común para contenido visual
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // Mayor elevación
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                if (!currentEjercicio.imagenUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentEjercicio.imagenUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_default_placeholder)
                            .error(R.drawable.ic_error_placeholder)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = currentEjercicio.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder mejorado si solo hay video o no hay medios
                    Icon(
                        Icons.Filled.Info, // O un icono más específico para "no media"
                        contentDescription = stringResource(R.string.no_media_available),
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                currentEjercicio.videoUrl?.takeIf { it.isNotBlank() }?.let { videoUriString ->
                    FilledTonalButton(
                        onClick = { onWatchVideoClick(videoUriString) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .height(IntrinsicSize.Min), // Ajuste de altura
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.PlayCircleFilled, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.button_watch_video), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Información del Ejercicio
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp), // Espaciado menor para texto
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(currentEjercicio.nombre, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Text(
                currentEjercicio.descripcion.ifBlank { stringResource(R.string.no_description_available) },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        // Indicador de Repeticiones/Tiempo Restante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentEjercicio.repeticiones > 0) {
                Text(
                    text = stringResource(R.string.routine_repetitions_target, currentEjercicio.repeticiones),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (currentEjercicio.duracionSegundos > 0) {
                Text(
                    text = stringResource(R.string.routine_seconds_remaining, segundosRestantes.coerceAtLeast(0)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }


        Spacer(modifier = Modifier.weight(1f)) // Empuja el botón hacia abajo

        // Botón de Siguiente/Finalizar
        Button(
            onClick = onNextClick,
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Altura fija para el botón
            shape = MaterialTheme.shapes.extraLarge, // Botón más redondeado
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            AnimatedContent(
                targetState = buttonText,
                transitionSpec = {
                    slideInHorizontally { width -> width } togetherWith
                            slideOutHorizontally { width -> -width }
                },
                label = "buttonTextAnimation"
            ) { text ->
                Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp)) // Espacio al final
    }
}