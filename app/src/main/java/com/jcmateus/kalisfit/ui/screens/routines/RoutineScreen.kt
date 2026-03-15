package com.jcmateus.kalisfit.ui.screens.routines

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.esTipoComplejo
import com.jcmateus.kalisfit.viewmodel.RoutineExecutionState
import com.jcmateus.kalisfit.viewmodel.RoutineViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

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
    val userProfile by userProfileViewModel.user.collectAsState()
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    LaunchedEffect(rutinaId, customRutinaId, userProfile) {
        val id = customRutinaId ?: rutinaId
        if (id != null && uiState.rutina == null && userProfile != null) {
            viewModel.startRoutine(id, userProfile)
        }
    }

    BackHandler { viewModel.setShowExitConfirmation(true) }
    KeepScreenOn()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.rutina?.nombre ?: "Entrenando", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Ronda ${uiState.rondaActual} de ${uiState.rutina?.numeroDeRondas ?: "-"}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setShowExitConfirmation(true) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    // Botón de Voz del Coach
                    IconButton(onClick = { viewModel.toggleVoiceCoach() }){
                        Icon(
                            imageVector = if (uiState.isVoiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = "Voz Coach",
                            tint = if (uiState.isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    // Botón de Vibración
                    IconButton(onClick = { viewModel.toggleVibration() }){
                        Icon(
                            imageVector = if (uiState.isVibrationEnabled) Icons.Filled.Vibration else Icons.Filled.Smartphone,
                            contentDescription = "Vibración",
                            tint = if (uiState.isVibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    // Botón de Pausa
                    IconButton(onClick = { viewModel.togglePausa() }) {
                        Icon(if (uiState.estado == RoutineExecutionState.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState.estado) {
                RoutineExecutionState.LOADING -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                RoutineExecutionState.INITIAL_COUNTDOWN -> CountdownView(uiState.tiempoRestante)
                RoutineExecutionState.EXERCISE_ACTIVE -> {
                    uiState.ejercicioActual?.let { exercise ->
                        ExerciseFocusMode(
                            exercise = exercise,
                            componenteActivo = uiState.componenteEjercicioActual, // Esto es vital para Supersets
                            set = uiState.serieActualEjercicio,
                            timeLeft = uiState.tiempoRestante,
                            onNext = { viewModel.saltarSiguientePaso() },
                            imageLoader = imageLoader
                        )
                    }
                }
                RoutineExecutionState.REST_BETWEEN_SETS, 
                RoutineExecutionState.REST_BETWEEN_EXERCISES, 
                RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                    RestView(
                        timeLeft = uiState.tiempoRestante,
                        nextExerciseName = uiState.rutina?.ejercicios?.getOrNull(uiState.indiceEjercicioActual + 1)?.nombre ?: "Siguiente",
                        onSkip = { viewModel.saltarSiguientePaso() }
                    )
                }
                RoutineExecutionState.PAUSED -> PauseOverlay(onResume = { viewModel.togglePausa() })
                RoutineExecutionState.FINISHED -> RoutineFinishedView(onDone = { navController.popBackStack() })
                else -> {}
            }
        }
    }

    if (uiState.showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = { viewModel.exitAndCleanUpRoutine(); navController.popBackStack() },
            onDismiss = { viewModel.setShowExitConfirmation(false) }
        )
    }
}

@Composable
fun ExerciseFocusMode(    exercise: Ejercicio,
                          componenteActivo: ComponenteEjercicio?,
                          set: Int,
                          timeLeft: Int,
                          onNext: () -> Unit,
                          imageLoader: ImageLoader
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Imagen Dinámica (Componente o Ejercicio Padre)
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(32.dp)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            AsyncImage(
                model = componenteActivo?.imagenUrl ?: exercise.imagenUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                imageLoader = imageLoader
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Títulos y Tipo
        Text(
            text = componenteActivo?.nombreEspecifico ?: exercise.nombre,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        if (exercise.esTipoComplejo()) {
            Text(
                text = "${exercise.tipoEjercicio.displayName} - Serie $set",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Lista de componentes si es Superset/Circuito
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(exercise.componentes) { comp ->
                    val isActive = comp.id == componenteActivo?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comp.nombreEspecifico ?: "Sub-ejercicio",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = if (comp.repeticiones != null) "${comp.repeticiones} reps" else "${comp.duracionSegundos}s",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        } else {
            Text("SERIE $set DE ${exercise.numeroDeSeries}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Temporizador o Repeticiones
        val timeToShow = componenteActivo?.duracionSegundos ?: exercise.duracionSegundosOriginal
        if (timeToShow > 0) {
            TimerDisplay(timeLeft)
        } else {
            RepsDisplay(componenteActivo?.repeticiones ?: exercise.repeticionesOriginal)
        }

        Spacer(modifier = Modifier.weight(0.2f))

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) {
            Text("SIGUIENTE", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Icon(Icons.Filled.SkipNext, null, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
@Composable
fun SimpleExerciseView(exercise: Ejercicio, set: Int, timeLeft: Int, onNext: () -> Unit, imageLoader: ImageLoader) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ExerciseImageCard(exercise.imagenUrl, imageLoader)
        Spacer(modifier = Modifier.height(24.dp))
        Text(exercise.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("SERIE $set DE ${exercise.numeroDeSeries}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (exercise.duracionSegundosOriginal > 0) TimerDisplay(timeLeft)
        else RepsDisplay(exercise.repeticionesOriginal)

        Spacer(modifier = Modifier.weight(1f))
        NextButton(onNext)
    }
}

@Composable
fun ComplexExerciseView(exercise: Ejercicio, componenteActivo: ComponenteEjercicio?, set: Int, timeLeft: Int, onNext: () -> Unit, imageLoader: ImageLoader) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(exercise.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("${exercise.tipoEjercicio.displayName} - Serie $set", style = MaterialTheme.typography.labelSmall)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Imagen del componente actual o del padre
        ExerciseImageCard(componenteActivo?.imagenUrl ?: exercise.imagenUrl, imageLoader, modifier = Modifier.height(200.dp))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Lista de componentes
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(exercise.componentes) { comp ->
                val isActive = comp.id == componenteActivo?.id
                ComponentItem(comp, isActive)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        if (timeLeft > 0) TimerDisplay(timeLeft)
        NextButton(onNext)
    }
}

@Composable
fun ComponentItem(comp: ComponenteEjercicio, isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comp.nombreEspecifico ?: "Ejercicio",
                modifier = Modifier.weight(1f),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = if (!comp.repeticiones.isNullOrBlank()) "${comp.repeticiones} reps" else "${comp.duracionSegundos}s",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun ExerciseImageCard(url: String?, imageLoader: ImageLoader, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(32.dp)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            imageLoader = imageLoader
        )
    }
}

@Composable
fun NextButton(onNext: () -> Unit) {
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) {
        Text("SIGUIENTE", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Icon(Icons.Filled.SkipNext, null, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun RestView(timeLeft: Int, nextExerciseName: String, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("DESCANSO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        TimerDisplay(timeLeft)
        Spacer(modifier = Modifier.height(24.dp))
        Text("SIGUIENTE:", style = MaterialTheme.typography.labelLarge)
        Text(nextExerciseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("SALTAR DESCANSO")
        }
    }
}

@Composable
fun TimerDisplay(time: Int) {
    Text(
        text = String.format("%02d:%02d", time / 60, time % 60),
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        color = if (time <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun RepsDisplay(reps: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = reps, style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp), fontWeight = FontWeight.Black)
        Text("REPETICIONES", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun CountdownView(time: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(time.toString(), style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSA", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            FloatingActionButton(onClick = onResume, containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun RoutineFinishedView(onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.EmojiEvents, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("¡LOGRADO!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Entrenamiento completado con éxito.", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("FINALIZAR") }
    }
}

@Composable
fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Salir?") },
        text = { Text("Si sales ahora no se guardará el progreso.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Salir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
