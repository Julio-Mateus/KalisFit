package com.jcmateus.kalisfit.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.viewmodel.CalisthenicsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalisthenicsLevelDetailScreen(
    navController: NavHostController,
    progressionId: String,
    levelId: String,
    viewModel: CalisthenicsViewModel = viewModel()
) {
    LaunchedEffect(progressionId, levelId) {
        // Usar la función renombrada en el ViewModel
        viewModel.loadExerciseLevelDetails(progressionId, levelId)
    }

    // Usar el StateFlow renombrado del ViewModel
    val exerciseDetails: ExerciseLevel? by viewModel.exerciseLevelDetails.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val error: String? by viewModel.error.collectAsState()

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            // Usar la función renombrada en el ViewModel
            viewModel.clearExerciseLevelDetails()
            viewModel.clearError() // Opcional, si quieres limpiar errores generales al salir
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // Acceder a las propiedades de 'exerciseDetails'
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
                .padding(16.dp),
            contentAlignment = Alignment.TopStart // Contenido principal alineado arriba
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // Mostrar error si no hay detalles y hay un mensaje de error
                error != null && exerciseDetails == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.error_loading_title), // "Error al cargar"
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            error!!, // Muestra el mensaje de error del ViewModel
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.clearError()
                                // Reintentar la carga con la función correcta
                                viewModel.loadExerciseLevelDetails(progressionId, levelId)
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.button_retry)) // "Reintentar"
                        }
                    }
                }
                // Si tenemos detalles (exerciseDetails no es null)
                exerciseDetails != null -> {
                    val currentDetails = exerciseDetails!! // Smart cast gracias a la condición
                    // Caso específico donde el nombre indica "no encontrado" (manejado por el ViewModel o FirestoreUtils)
                    if (currentDetails.name == "Ejercicio no encontrado") {
                        Text(
                            currentDetails.description, // Muestra la descripción de "no encontrado"
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // Mostrar los detalles del ejercicio
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = currentDetails.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Puedes mostrar el nombre de la progresión si lo tienes o lo pasas
                            // Text(
                            // text = "Progresión: ${currentDetails.progressionName}", // Si tu ExerciseLevel lo tiene
                            // style = MaterialTheme.typography.labelMedium,
                            // color = MaterialTheme.colorScheme.onSurfaceVariant
                            // )
                            // Spacer(modifier = Modifier.height(16.dp))

                            // Asumiendo que ExerciseLevel tiene estas propiedades. Ajusta según tu modelo.
                            DetailItem(label = stringResource(R.string.detail_label_description), value = currentDetails.description)
                            currentDetails.targetSets?.let { DetailItem(label = stringResource(R.string.detail_label_sets), value = it) }
                            currentDetails.targetReps?.let { DetailItem(label = stringResource(R.string.detail_label_reps_duration), value = it) }
                            currentDetails.targetHoldTime?.let { DetailItem(label = stringResource(R.string.detail_label_hold_time), value = it) }
                            // Considera si 'restTime' viene de 'notes' u otro campo en ExerciseLevel
                            currentDetails.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.detail_label_notes_tips), // "Notas/Consejos:"
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                // Si 'notes' es un solo String con múltiples puntos, o si es una lista.
                                // Si es un String que puede contener saltos de línea:
                                Text(
                                    notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Si 'notes' fuera una List<String> como 'tips' antes:
                                // notes.forEach { note ->
                                // Text(
                                // "• $note",
                                // style = MaterialTheme.typography.bodyMedium,
                                // modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                                // color = MaterialTheme.colorScheme.onSurfaceVariant
                                // )
                                // }
                            }
                            // Puedes añadir más DetailItem para videoUrl, imageUrl si los quieres mostrar como texto
                            // o usar Composables específicos para imágenes/videos.

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                // Estado inicial o si no hay datos ni error después de cargar
                else -> {
                    Text(
                        stringResource(R.string.info_no_data_available), // "No hay información disponible..."
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// El Composable DetailItem sigue igual, es genérico.
@Composable
fun DetailItem(label: String, value: String?) { // value ahora es nullable
    value?.takeIf { it.isNotBlank() }?.let { nonEmptyValue ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = nonEmptyValue,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}