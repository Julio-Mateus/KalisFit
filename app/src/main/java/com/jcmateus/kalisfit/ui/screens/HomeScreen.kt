package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcmateus.kalisfit.ui.viewmodel.UserProfileViewModel
import com.jcmateus.kalisfit.ui.viewmodel.UserProfile
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.navigation.NavController
import com.jcmateus.kalisfit.ui.navigation.Routes

@Composable
fun HomeScreen(navController: NavController) {
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
            .padding(24.dp)
    ) {
        user?.let {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {  navController.navigate(Routes.ROUTINE)  },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Empezar rutina")
                }

                TextButton(onClick = { navController.navigate("profile") }) {
                    Text("Ver mi perfil")
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}


