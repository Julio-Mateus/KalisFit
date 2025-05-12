package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.viewmodel.RoutineExplorerViewModel
import com.jcmateus.kalisfit.viewmodel.RoutineExplorerViewModelFactory
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineExplorerScreen(
    navController: NavController,
    // Obtienes el UserProfileViewModel. Podría venir de un nivel superior
    // en tu navegación si ya lo tienes disponible, o crearlo aquí.
    userProfileViewModel: UserProfileViewModel = viewModel(),
    // Pasas el UserProfileViewModel a la fábrica para crear RoutineExplorerViewModel
    routineExplorerViewModel: RoutineExplorerViewModel = viewModel(factory = RoutineExplorerViewModelFactory(userProfileViewModel))
) {
    // Observamos los estados del RoutineExplorerViewModel
    val rutinasState = routineExplorerViewModel.rutinas.collectAsState()
    val isLoading by routineExplorerViewModel.isLoading.collectAsState()
    val errorMessage by routineExplorerViewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Explorar Rutinas") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                if (rutinasState.value.isEmpty()) {
                    Text(
                        text = "No se encontraron rutinas para tu perfil.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(rutinasState.value) { rutina ->
                            RutinaCard(rutina = rutina, onClick = {
                                // Navegar a la RoutineScreen pasando el ID de la rutina seleccionada
                                navController.navigate("routine/${rutina.id}")
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RutinaCard(rutina: Rutina, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(rutina.nombre, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(rutina.descripcion, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Nivel: ${rutina.nivelRecomendado.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Text("Objetivos: ${rutina.objetivos.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            // Modifica esta línea:
            Text("Lugares: ${rutina.lugarEntrenamiento.joinToString(", ")}", style = MaterialTheme.typography.bodySmall) // <--- Elimina .name
        }
    }
}