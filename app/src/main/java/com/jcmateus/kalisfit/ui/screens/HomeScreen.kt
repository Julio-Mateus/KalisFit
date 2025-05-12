package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.navigation.Routes

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel = remember { UserProfileViewModel() }
    val userState = viewModel.user.collectAsState()
    val user = userState.value

    val tipDelDia = remember {
        listOf(
            "La constancia es tu mejor aliada.",
            "Respira y enfoca tu energía.",
            "Tu cuerpo puede más de lo que crees.",
            "La calistenia es control y mente.",
            "Hoy también es día de entrenar 💪"
        ).random()
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        user?.let {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Hola, ${it.nombre} 👋",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("Nivel: ${it.nivel}", style = MaterialTheme.typography.titleMedium)

                    Text("Objetivos", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        it.objetivos.forEach { objetivo ->
                            AssistChip(onClick = {}, label = { Text(objetivo) })
                        }
                    }
                }

                item {
                    Divider()
                    Text("Tip del día", style = MaterialTheme.typography.titleMedium)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            tipDelDia,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                item {
                    Divider()
                    Text("📚 Tips recientes", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(3) {
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(120.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(Modifier.padding(16.dp)) {
                                    Text("Consejo #${it + 1}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                item {
                    Divider()
                    Text("🏋️‍♂️ Rutinas recomendadas", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(3) { index -> // Usamos el index para simular IDs temporales si no tienes rutinas cargadas
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    // **Modificado para navegar con ID**
                                    // Usa un ID de rutina real aquí. Esto es un ejemplo usando el índice.
                                    val rutinaId = "rutina_id_${index + 1}"
                                    navController.navigate("${Routes.ROUTINE}/$rutinaId")
                                }
                            ) {
                                Box(Modifier.padding(16.dp)) {
                                    Text("Rutina #${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            // **Modificado para navegar con ID (ejemplo)**
                            // Aquí deberías tener la lógica para seleccionar o generar un ID de rutina
                            // para la rutina personalizada. Por ahora, usamos un ID de ejemplo.
                            val rutinaPersonalizadaId = "rutina_personalizada_001"
                            navController.navigate("${Routes.ROUTINE}/$rutinaPersonalizadaId")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Empezar rutina personalizada")
                    }
                    TextButton(onClick = {
                        // Asumo que "profile" es una ruta dentro del NavHost anidado en KalisMainScreen
                        navController.navigate(Routes.PROFILE)
                    }) {
                        Text("Ver mi perfil")
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

