package com.jcmateus.kalisfit.ui.screens

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
                    // Opcional: si esta pantalla no es la raíz de una sección
                     IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                     }
                },
                actions = {
                    IconButton(onClick = {
                        // Navegar a la pantalla de creación de rutina
                        // Ejemplo: navController.navigate(Screen.EditRoutine.route + "?isNewRoutine=true")
                        // O si tienes una ruta específica para crear desde cero:
                        navController.navigate("edit_routine?isNewRoutine=true")
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear Rutina")
                    }
                }
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
                    CircularProgressIndicator(modifier = Modifier.align(
                        Alignment.Center
                    ))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "Ocurrió un error",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                    // Podrías añadir un botón de reintentar aquí
                    Button(
                        onClick = { viewModel.refreshRoutines() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text("Reintentar")
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
                            navController.navigate("edit_routine?isNewRoutine=true")
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
                else -> {
                    MyRoutinesList(
                        routines = uiState.routines,
                        onRoutineClick = { routineId ->
                            // Navegar a la pantalla de detalle/inicio de la rutina
                            // Ejemplo: navController.navigate(Screen.RoutineDetail.createRoute(routineId, true))
                            // O a la pantalla de ejecución de la rutina personalizada
                            navController.navigate("start_custom_routine_screen/$routineId") // Ajusta la ruta
                        },
                        onEditRoutineClick = { routineId ->
                            // Navegar a la pantalla de edición de rutina
                            // Ejemplo: navController.navigate(Screen.EditRoutine.createRoute(routineId))
                            navController.navigate("edit_routine/$routineId")
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
