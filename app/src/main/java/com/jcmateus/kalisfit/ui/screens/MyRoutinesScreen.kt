package com.jcmateus.kalisfit.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.MyRoutinesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoutinesScreen(
    navController: NavHostController,
    viewModel: MyRoutinesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Rutinas Personalizadas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentUserId = uiState.currentUserId
                        if (currentUserId != null && currentUserId.isNotBlank()) {
                            Log.d("MyRoutinesScreen", "Acción Crear: Navegando a edición para UserID: $currentUserId (Nueva Rutina)")
                            // Usar la función helper de Routes para crear una nueva rutina
                            navController.navigate(
                                Routes.editRoutine(userId = currentUserId) // templateId y customRoutineId serán null por defecto
                            )
                        } else {
                            Log.w("MyRoutinesScreen", "Acción Crear: currentUserId es nulo o vacío. No se puede crear rutina.")
                            // Considera mostrar un mensaje al usuario (Toast, Snackbar)
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear Rutina")
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
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Column( // Envuelve en una columna para alinear botón y texto
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Ocurrió un error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = { viewModel.refreshRoutines() }) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.routines.isEmpty() && uiState.currentUserId != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Aún no tienes rutinas personalizadas.", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val currentUserId = uiState.currentUserId!! // Ya sabemos que no es nulo aquí
                            Log.d("MyRoutinesScreen", "Botón 'Crea tu primera': Navegando a edición para UserID: $currentUserId (Nueva Rutina)")
                            navController.navigate(
                                Routes.editRoutine(userId = currentUserId)
                            )
                        }) {
                            Text("¡Crea tu primera rutina!")
                        }
                    }
                }
                uiState.currentUserId == null && !uiState.isLoading -> {
                    Text(
                        text = "Por favor, inicia sesión para ver tus rutinas.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> { // routines no está vacía y currentUserId no es nulo (implícito por la carga de rutinas)
                    MyRoutinesList(
                        routines = uiState.routines,
                        onRoutineClick = { clickedRoutineId ->
                            val currentUserId = uiState.currentUserId
                            if (currentUserId != null && currentUserId.isNotBlank()) { // Doble check aunque debería estar
                                Log.d("MyRoutinesScreen", "Navegando a detalle. Rutina ID: $clickedRoutineId, Usuario ID: $currentUserId")
                                navController.navigate(
                                    Routes.routineDetail(
                                        routineId = clickedRoutineId,
                                        userId = currentUserId
                                    )
                                )
                            } else {
                                Log.e("MyRoutinesScreen", "Error: currentUserId es nulo o vacío al intentar navegar a los detalles de la rutina personalizada.")
                            }
                        },
                        onEditRoutineClick = { routineIdToEdit ->
                            val currentUserId = uiState.currentUserId
                            if (currentUserId != null && currentUserId.isNotBlank()) { // Doble check
                                Log.d("MyRoutinesScreen", "Acción Editar: Navegando a edición para UserID: $currentUserId, CustomRoutineID: $routineIdToEdit")
                                // Usar la función helper de Routes para editar una rutina existente
                                navController.navigate(
                                    Routes.editRoutine(
                                        userId = currentUserId,
                                        customRoutineId = routineIdToEdit // templateId será null por defecto
                                    )
                                )
                            } else {
                                Log.w("MyRoutinesScreen", "Acción Editar: currentUserId es nulo o vacío. No se puede editar rutina ID: $routineIdToEdit.")
                                // Considera mostrar un mensaje al usuario
                            }
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun MyRoutinesList(
    routines: List<UserCustomRoutine>,
    onRoutineClick: (String) -> Unit,
    onEditRoutineClick: (String) -> Unit
) {
    if (routines.isEmpty()) { // Esto no debería alcanzarse si la lógica en MyRoutinesScreen es correcta
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron rutinas personalizadas.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(routines, key = { it.id }) { routine ->
            CustomRoutineItem(
                routine = routine,
                onClick = { onRoutineClick(routine.id) },
                onEditClick = { onEditRoutineClick(routine.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRoutineItem(
    routine: UserCustomRoutine,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(routine.nombrePersonalizado, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (routine.descripcion?.isNotBlank() == true) {
                    Text(
                        routine.descripcion!!,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2 // Evita que descripciones largas ocupen mucho espacio
                    )
                }
                Text(
                    "Rondas: ${routine.numeroDeRondas}, Ejercicios: ${routine.ejercicios.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                // Podrías mostrar "Última modificación: ..." si quieres
                // val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                // Text("Modificado: ${sdf.format(routine.fechaUltimaModificacion.toDate())}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar Rutina")
            }
        }
    }
}
