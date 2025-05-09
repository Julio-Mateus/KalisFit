package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.ui.di.Ejercicio
import kotlinx.coroutines.delay

@Composable
fun RoutineScreen(
    navController: NavController,
    onRoutineComplete: () -> Unit
) {
    val rutina = listOf(
        Ejercicio("Flexiones", "Trabaja pecho y tríceps", R.drawable.flexiones, 30),
        Ejercicio("Sentadillas", "Fuerza en piernas y glúteos", R.drawable.sentadillas, 40),
        Ejercicio("Planchas", "Activa core y postura", R.drawable.planchas, 45),
        Ejercicio("Fondos", "Enfocados en tríceps", R.drawable.fondos, 30)
    )

    var index by remember { mutableStateOf(0) }
    var segundosRestantes by remember { mutableStateOf(rutina[index].duracionSegundos) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(index) {
        segundosRestantes = rutina[index].duracionSegundos
        while (segundosRestantes > 0) {
            delay(1000L)
            segundosRestantes--
        }
    }

    val ejercicioActual = rutina[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ejercicio ${index + 1} de ${rutina.size}", style = MaterialTheme.typography.titleMedium)

        LinearProgressIndicator(
            progress = (segundosRestantes.toFloat() / ejercicioActual.duracionSegundos),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        Image(
            painter = painterResource(id = ejercicioActual.imagenRes),
            contentDescription = ejercicioActual.nombre,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Text(ejercicioActual.nombre, style = MaterialTheme.typography.displayLarge)
        Text(ejercicioActual.descripcion, style = MaterialTheme.typography.bodyLarge)

        Text(
            text = "⏱️ $segundosRestantes segundos restantes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            if (index > 0) {
                OutlinedButton(onClick = { index-- }) {
                    Text("Anterior")
                }
            }
            if (index < rutina.size - 1 && segundosRestantes == 0) {
                Button(onClick = { index++ }) {
                    Text("Siguiente")
                }
            } else if (index == rutina.size - 1 && segundosRestantes == 0) {
                Button(onClick = onRoutineComplete) {
                    Text("Finalizar rutina")
                }
            }
        }
    }
}
