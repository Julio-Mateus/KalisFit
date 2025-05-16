package com.jcmateus.kalisfit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.navigation.Routes
import kotlinx.coroutines.delay
import com.jcmateus.kalisfit.R

@Composable
fun SplashScreen(
    // Ya no pasamos NavController directamente, usamos callbacks
    // navController: NavController --> ELIMINAR
    onUserLoggedIn: () -> Unit,
    onUserNotLoggedIn: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.exercise)
    )
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Retardo para mostrar el texto "Bienvenido a KalisFit"
        delay(2000) // espera antes de mostrar el texto
        showText = true

        // Espera adicional antes de verificar y navegar.
        // El delay total será 2000ms (para showText) + 2000ms (este delay) = 4000ms
        // Ajusta este segundo delay si quieres que la animación se vea más tiempo después de que aparezca el texto.
        // Si el delay de 4000 en tu código original era el *total* de la splash,
        // entonces este delay debería ser 2000 (4000 total - 2000 para showText).
        delay(2000) // Espera adicional (total 4 segundos desde el inicio de la splash)

        // Verificar estado de autenticación de Firebase
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            onUserLoggedIn()
        } else {
            onUserNotLoggedIn()
        }

        // El código de navegación original se elimina de aquí,
        // porque se manejará mediante los callbacks en KalisNavGraph
        // navController.navigate(Routes.LOGIN) {
        //    popUpTo(Routes.SPLASH) { inclusive = true }
        // }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.8f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bienvenido a",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "KalisFit",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .size(220.dp)
                    .padding(bottom = 24.dp) // Ajusta si es necesario con el texto
            )
        }
    }
}

