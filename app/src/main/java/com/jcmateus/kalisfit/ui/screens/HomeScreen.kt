package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel = remember { UserProfileViewModel() }
    val userState = viewModel.user.collectAsState()
    val user = userState.value

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    user?.let { userData ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text("Hola, ${userData.nombre} 👋", style = MaterialTheme.typography.headlineLarge)
                Text("Nivel: ${userData.nivel}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
            }

            item {
                TipDelDiaCard()
            }

            item {
                SeccionRutinasDestacadas(
                    titulo = "Recomendadas para ti",
                    imagen = R.drawable.recomendado,
                    rutinas = obtenerRutinasFiltradas(userData),
                    onClick = { navController.navigate("routine") }
                )
            }

            item {
                SeccionRutinasDestacadas(
                    titulo = "Entrena en casa",
                    imagen = R.drawable.casa,
                    rutinas = obtenerRutinasPorLugar(userData, "Casa"),
                    onClick = { navController.navigate("routine") }
                )
            }

            item {
                SeccionRutinasDestacadas(
                    titulo = "Rutinas en el gimnasio",
                    imagen = R.drawable.gym,
                    rutinas = obtenerRutinasPorLugar(userData, "Gimnasio"),
                    onClick = { navController.navigate("routine") }
                )
            }

            item {
                SeccionRutinasDestacadas(
                    titulo = "Entrena al aire libre",
                    imagen = R.drawable.airelibre,
                    rutinas = obtenerRutinasPorLugar(userData, "Exterior"),
                    onClick = { navController.navigate("routine") }
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate("profile") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver perfil")
                }
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}




