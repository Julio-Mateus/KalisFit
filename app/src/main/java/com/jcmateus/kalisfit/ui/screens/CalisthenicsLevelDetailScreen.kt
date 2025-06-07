package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.isLevelCompleted
import com.jcmateus.kalisfit.model.isLevelNextOrPast
import com.jcmateus.kalisfit.viewmodel.CalisthenicsViewModel
import kotlinx.coroutines.launch


@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalisthenicsLevelDetailScreen(
    navController: NavHostController,
    progressionId: String,
    levelId: String,
    viewModel: CalisthenicsViewModel = viewModel()
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components { add(GifDecoder.Factory()) }
        .build()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(progressionId, levelId) {
        viewModel.loadExerciseLevelDetails(progressionId, levelId)
        // Opcional: Asegurarse que los estados de progresión del usuario están cargados
        // viewModel.loadCurrentUserProgressionStates() // Si no se carga globalmente
    }

    val exerciseDetails: ExerciseLevel? by viewModel.exerciseLevelDetails.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val error: String? by viewModel.error.collectAsState()
    // Ya no necesitamos userProgressionStates aquí directamente si el ViewModel provee las funciones isLevelCompleted/isLevelUnlocked

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            viewModel.clearExerciseLevelDetails()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            // Mostrar Snackbar de error solo si no estamos en el estado de error de carga inicial
            if (exerciseDetails != null || (isLoading && exerciseDetails == null && error != null /* Error durante la carga inicial pero se muestra snackbar*/)) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = it,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearError()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        exerciseDetails?.name?.takeIf { it.isNotBlank() && it != "Ejercicio no encontrado" }
                            ?: stringResource(R.string.title_calisthenics_level_detail_loading)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && exerciseDetails == null -> { // Cargando detalles iniciales
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null && exerciseDetails == null -> { // Error al cargar detalles iniciales
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.error_loading_details_title), // Título más específico
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error ?: stringResource(R.string.unknown_error_occurred),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.clearError()
                                viewModel.loadExerciseLevelDetails(progressionId, levelId)
                            }
                        ) {
                            Text(stringResource(R.string.button_retry))
                        }
                    }
                }

                exerciseDetails != null -> {
                    val currentDetails = exerciseDetails!! // Seguro por la condición

                    // Obtener el estado del nivel desde el ViewModel
                    val isCompleted = viewModel.isLevelCompleted(progressionId, levelId)
                    val isUnlocked = viewModel.isLevelUnlocked(progressionId, levelId) // Necesario para la lógica del botón

                    if (currentDetails.name == "Ejercicio no encontrado") { // Caso especial de "no encontrado"
                        Text(
                            currentDetails.description, // Asumiendo que la descripción contiene el mensaje
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp) // Espacio para el botón de completar al final
                        ) {
                            // Sección de Imagen/Video (sin cambios)
                            if (currentDetails.imageUrl != null || currentDetails.videoUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.4f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentDetails.imageUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(currentDetails.imageUrl)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_default_placeholder)
                                                .error(R.drawable.ic_error_placeholder)
                                                .build(),
                                            imageLoader = imageLoader,
                                            contentDescription = stringResource(R.string.desc_exercise_image, currentDetails.name),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (currentDetails.videoUrl != null) { // Placeholder si solo hay video
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircleOutline, // Outline para diferenciar de botón
                                            contentDescription = stringResource(R.string.desc_exercise_video_placeholder),
                                            modifier = Modifier.size(120.dp), // Más grande
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    currentDetails.videoUrl?.let { videoUriString ->
                                        FilledTonalButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUriString))
                                                    ContextCompat.startActivity(context, intent, null)
                                                } catch (e: Exception) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(R.string.error_opening_video_link),
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayCircleFilled,
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.button_watch_video))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Sección de Nombre y Descripción
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = currentDetails.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Usar el Composable DetailItem si está definido y es apropiado,
                                // o simplemente Text para la descripción principal.
                                Text(
                                    text = currentDetails.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Sección de Metas (Sets, Reps, Hold Time)
                            if (currentDetails.targetSets != null || currentDetails.targetReps != null || currentDetails.targetHoldTime != null) {
                                Spacer(modifier = Modifier.height(24.dp)) // Más espacio antes de esta sección
                                Text(
                                    text = stringResource(R.string.header_targets).uppercase(), // Título en mayúsculas
                                    style = MaterialTheme.typography.titleSmall, // titleSmall o labelLarge
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.primary // Color primario para el encabezado
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    currentDetails.targetSets?.let { DetailItem(label = stringResource(R.string.detail_label_sets), value = it) }
                                    currentDetails.targetReps?.let { DetailItem(label = stringResource(R.string.detail_label_reps_duration), value = it) } // Asumo que repeticiones y duración usan la misma etiqueta aquí
                                    currentDetails.targetHoldTime?.let { DetailItem(label = stringResource(R.string.detail_label_hold_time), value = it) }
                                }
                            }

                            // Sección de Notas/Consejos
                            currentDetails.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    stringResource(R.string.detail_label_notes_tips).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp), // Alineado con el título
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f)) // Empuja el botón hacia abajo si el contenido es corto

                            // --- Lógica del Botón "Marcar como Completado" ---
                            // El botón solo debe aparecer si el nivel está DESBLOQUEADO y AÚN NO ESTÁ COMPLETADO.
                            if (isUnlocked && !isCompleted) {
                                Button(
                                    onClick = {
                                        viewModel.markLevelAsCompleted(progressionId, levelId)
                                        // El Snackbar se mostrará cuando el estado 'isCompleted' cambie
                                        // o puedes mostrar uno inmediatamente.
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.level_marked_completed_feedback, currentDetails.name),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp), // Añadir padding vertical
                                    enabled = !isLoading // Deshabilitar si el ViewModel está ocupado (opcional)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircleOutline, // Outline para diferenciar de estado completado
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.button_mark_as_completed))
                                }
                            } else if (isCompleted) { // Si ya está completado
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), // Color diferente para completado
                                            MaterialTheme.shapes.medium
                                        )
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer, // Color que contraste
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(
                                        stringResource(R.string.level_already_completed),
                                        style = MaterialTheme.typography.labelLarge, // labelLarge o bodyMedium
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            // No se muestra nada si está bloqueado y no completado
                            // (el usuario no debería poder llegar a esta pantalla si está bloqueado,
                            // pero por si acaso, no se muestra botón de acción).

                            Spacer(modifier = Modifier.height(16.dp)) // Espacio final
                        }
                    }
                }
                else -> { // Fallback si exerciseDetails es null y no hay error/loading
                    Text(
                        stringResource(R.string.info_no_data_available),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}


// El Composable DetailItem que usas:
@Composable
fun DetailItem(label: String, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let { nonEmptyValue ->
        Row( // Usar Row para tener etiqueta y valor en la misma línea si caben o diseño preferido
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp), // Ajustar padding
            verticalAlignment = Alignment.Top // Alinear al inicio si el valor es multilínea
        ) {
            Text(
                text = "$label:", // Añadir dos puntos para claridad
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Color primario para la etiqueta
                modifier = Modifier.weight(0.4f) // Darle un peso a la etiqueta
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = nonEmptyValue,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.6f) // Darle un peso al valor
            )
        }
    }
}