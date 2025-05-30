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
import androidx.compose.material.icons.filled.PlayCircleFilled
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
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
    viewModel: CalisthenicsViewModel = viewModel() // Asegúrate que CalisthenicsViewModel está importado
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(progressionId, levelId) {
        viewModel.loadExerciseLevelDetails(progressionId, levelId)
    }

    val exerciseDetails: ExerciseLevel? by viewModel.exerciseLevelDetails.collectAsState() // Asegúrate que ExerciseLevel está importado
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val error: String? by viewModel.error.collectAsState()
    val userProgressionStates by viewModel.userProgressionStates.collectAsState() // Asegúrate que la clave (progressionId) y el valor (UserProgression) son correctos


    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            viewModel.clearExerciseLevelDetails()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            if (exerciseDetails != null) {
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
                isLoading && exerciseDetails == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null && exerciseDetails == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.error_loading_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error ?: stringResource(R.string.unknown_error_occurred), // CORRECCIÓN: Usar valor o default
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.clearError() // Asegúrate que no es suspend
                                viewModel.loadExerciseLevelDetails(progressionId, levelId) // Asegúrate que no es suspend
                            }
                        ) {
                            Text(stringResource(R.string.button_retry))
                        }
                    }
                }

                exerciseDetails != null -> {
                    val currentDetails = exerciseDetails!! // Seguro por la condición
                    if (currentDetails.name == "Ejercicio no encontrado") {
                        Text(
                            currentDetails.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        val progressionState = userProgressionStates[progressionId] // Puede ser UserProgression?
                        val isLevelCompleted = progressionState?.isLevelCompleted(levelId) ?: false // CORRECCIÓN

                        val currentProgression = viewModel.progressions.value.firstOrNull { it.id == progressionId }
                        val isNextLevelOrPast = progressionState?.isLevelNextOrPast(
                            levelId,
                            currentDetails,
                            currentProgression?.levels ?: emptyList()
                        ) ?: false // CORRECCIÓN


                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp)
                        ) {
                            // Sección de Imagen/Video
                            if (currentDetails.imageUrl != null || currentDetails.videoUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentDetails.imageUrl != null) {
                                        AsyncImage( // CORRECCIÓN: Usando Coil correctamente
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(currentDetails.imageUrl)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_default_placeholder)
                                                .error(R.drawable.ic_error_placeholder)
                                                .build(),
                                            contentDescription = stringResource(R.string.desc_exercise_image, currentDetails.name),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Alternativa más simple:
                                        // AsyncImage(
                                        // model = currentDetails.imageUrl,
                                        // placeholder = painterResource(id = R.drawable.ic_default_placeholder),
                                        // error = painterResource(id = R.drawable.ic_error_placeholder),
                                        // contentDescription = stringResource(R.string.desc_exercise_image, currentDetails.name),
                                        // modifier = Modifier.fillMaxSize(),
                                        // contentScale = ContentScale.Crop
                                        // )
                                    } else if (currentDetails.videoUrl != null) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircleFilled,
                                            contentDescription = stringResource(R.string.desc_exercise_video_placeholder),
                                            modifier = Modifier.size(100.dp),
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

                            // Sección de Nombre y Descripción (con padding horizontal)
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = currentDetails.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // DetailItem(label = stringResource(R.string.detail_label_description), value = currentDetails.description) // Asegúrate que DetailItem está definido
                            }


                            // Sección de Metas (Sets, Reps, Hold Time)
                            if (currentDetails.targetSets != null || currentDetails.targetReps != null || currentDetails.targetHoldTime != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.header_targets),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                     currentDetails.targetSets?.let { DetailItem(label = stringResource(R.string.detail_label_sets), value = it.toString()) } // Asegúrate que DetailItem está definido y `it` se pasa como String
                                     currentDetails.targetReps?.let { DetailItem(label = stringResource(R.string.detail_label_reps_duration), value = it.toString()) }
                                     currentDetails.targetHoldTime?.let { DetailItem(label = stringResource(R.string.detail_label_hold_time), value = it.toString()) }
                                }
                            }

                            // Sección de Notas/Consejos
                            currentDetails.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.detail_label_notes_tips),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isNextLevelOrPast && !isLevelCompleted) {
                                Button(
                                    onClick = {
                                        viewModel.markLevelAsCompleted(progressionId, levelId) // Asegúrate que no es suspend
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.level_marked_completed, currentDetails.name),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    enabled = !isLoading
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.button_mark_as_completed))
                                }
                            } else if (isLevelCompleted) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.3f
                                            ), MaterialTheme.shapes.medium
                                        )
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(
                                        stringResource(R.string.level_already_completed),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                else -> {
                    Text(
                        stringResource(R.string.info_no_data_available),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}


// El Composable DetailItem sigue igual, es genérico.
@Composable
fun DetailItem(label: String, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let { nonEmptyValue ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp) // Reducido para más densidad si es necesario
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge, // Un poco más pequeño que titleSmall
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = nonEmptyValue,
                style = MaterialTheme.typography.bodyLarge, // Mantenemos el tamaño para la legibilidad del valor
                color = MaterialTheme.colorScheme.onSurface
            )
            // Quité el Spacer aquí para más densidad, el padding vertical en Column lo maneja
        }
    }
}