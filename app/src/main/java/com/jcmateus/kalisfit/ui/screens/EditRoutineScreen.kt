package com.jcmateus.kalisfit.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    navController: NavHostController,
    viewModel: EditRoutineViewModel = viewModel() // Obtén el ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Rutina guardada", Toast.LENGTH_SHORT).show()
            viewModel.onSaveHandled() // Resetea el flag
            navController.popBackStack() // Vuelve a la pantalla anterior
        }
    }

    LaunchedEffect(uiState.errorMessages) {
        uiState.errorMessages.firstOrNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // Aquí podrías tener una lógica para limpiar los mensajes de error del uiState
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewRoutine && uiState.originalTemplateId != null) "Personalizar Rutina" else if (uiState.isNewRoutine) "Crear Rutina" else "Editar Rutina") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveRoutine() },
                        enabled = !uiState.isLoading && uiState.routineToEdit != null
                    ) {
                        Text("Guardar")
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()) {
                if (uiState.isLoading && uiState.routineToEdit == null) { // Loading inicial
                    CircularProgressIndicator(modifier = Modifier.align(
                        Alignment.Center
                    ))
                } else if (uiState.routineToEdit == null) {
                    Text(
                        text = uiState.errorMessages.joinToString("\n").ifEmpty { "No se pudo cargar la rutina para editar." },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // El contenido de la edición va aquí
                    EditRoutineContent(
                        routine = uiState.routineToEdit!!, // Sabemos que no es nulo aquí
                        isLoadingDuringSave = uiState.isLoading, // Para deshabilitar campos mientras guarda
                        onNameChange = { viewModel.onRoutineNameChanged(it) }
                        // ... más callbacks para otros campos ...
                    )
                }
            }
        }
    )
}

@Composable
fun EditRoutineContent(
    routine: UserCustomRoutine,
    isLoadingDuringSave: Boolean,
    onNameChange: (String) -> Unit
    // ...
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Editando Rutina", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = routine.nombrePersonalizado,
                onValueChange = onNameChange,
                label = { Text("Nombre de la Rutina") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingDuringSave
            )
        }

        item {
            Text("Descripción (Próximamente)", style = MaterialTheme.typography.titleMedium)
            // TextField para descripción
        }

        item {
            Text("Rondas: ${routine.numeroDeRondas} (Próximamente editable)", style = MaterialTheme.typography.titleMedium)
            // Slider o TextField para rondas
        }

        item {
            Text("Ejercicios (Próximamente)", style = MaterialTheme.typography.titleLarge)
            if (routine.ejercicios.isEmpty()) {
                Text("No hay ejercicios en esta rutina.")
            } else {
                routine.ejercicios.forEachIndexed { index, ejercicio ->
                    Text("(${index + 1}) ${ejercicio.nombre} - Series: ${ejercicio.numeroDeSeries}, Reps: ${ejercicio.repeticionesOriginal}")
                    // Aquí luego irán los controles para editar/eliminar/reordenar
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

