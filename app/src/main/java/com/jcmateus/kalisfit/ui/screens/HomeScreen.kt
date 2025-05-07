package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jcmateus.kalisfit.ui.navigation.Routes
import com.jcmateus.kalisfit.ui.viewmodel.UserProfile
import com.jcmateus.kalisfit.ui.viewmodel.UserProfileViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val user: UserProfile? = viewModel.user.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (user != null) "Hola, ${user.nombre} 👋" else "Cargando...",
            style = MaterialTheme.typography.displayLarge
        )

        Text("Nivel: ${user?.nivel}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Objetivos: ${user?.objetivos?.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            navController.navigate(Routes.PROFILE)
        }) {
            Text("Ver mi perfil")
        }
    }
}
