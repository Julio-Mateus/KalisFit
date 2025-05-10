package com.jcmateus.kalisfit.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.guardarProgresoRutina
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutineScreen(
    navController: NavController,
    onRoutineComplete: () -> Unit
) {
    val viewModel = remember { UserProfileViewModel() }
    val user by viewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    val rutina = remember(user) {
        when (user?.nivel) {
            "Principiante" -> when {
                "Fuerza" in user!!.objetivos -> listOf(
                    Ejercicio("Flexiones apoyadas", "Para iniciar fuerza en brazos", R.drawable.flexiones_apoyadas, 20),
                    Ejercicio("Sentadillas", "Base para piernas fuertes", R.drawable.sentadillas, 25)
                )
                "Resistencia" in user!!.objetivos -> listOf(
                    Ejercicio("Mountain climbers", "Cardio + abdomen", R.drawable.mountain_climbers, 30),
                    Ejercicio("Jumping jacks", "Full body", R.drawable.jumping_jacks, 30)
                )
                "Masa muscular" in user!!.objetivos -> listOf(
                    Ejercicio("Flexiones", "Aumenta masa en pectorales", R.drawable.flexiones, 30),
                    Ejercicio("Fondos", "Tríceps", R.drawable.fondos, 30)
                )
                else -> listOf(
                    Ejercicio("Planchas", "Control mental y físico", R.drawable.planchas, 30)
                )
            }

            "Intermedio" -> listOf(
                Ejercicio("Flexiones", "Mayor control y fuerza", R.drawable.flexiones, 40),
                Ejercicio("Sentadillas con salto", "Explosividad", R.drawable.sentadillas_salto, 40)
            )

            "Avanzado" -> listOf(
                Ejercicio("Pistol Squats", "Una pierna", R.drawable.pistol_squat, 45),
                Ejercicio("Fondos en barra", "Tríceps y pecho", R.drawable.fondos, 45)
            )

            else -> emptyList()
        }
    }

    // ⛔ Si aún no hay rutina o usuario, no mostrar nada
    if (rutina.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var index by remember { mutableStateOf(0) }
    var segundosRestantes by remember { mutableStateOf(rutina.first().duracionSegundos) }
    var countdownStart by remember { mutableStateOf(3) }
    var rutinaEmpezada by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ⏱ Cuenta atrás inicial
    LaunchedEffect(Unit) {
        while (countdownStart > 0) {
            context.playBeepSound()
            delay(1000)
            countdownStart--
        }
        rutinaEmpezada = true
    }

    // 🔁 Temporizador del ejercicio actual
    LaunchedEffect(index, rutinaEmpezada) {
        if (rutinaEmpezada) {
            segundosRestantes = rutina[index].duracionSegundos
            while (segundosRestantes > 0) {
                delay(1000)
                segundosRestantes--
                if (segundosRestantes in 1..5) {
                    context.playBeepSound()
                }
            }
        }
    }

    if (!rutinaEmpezada) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (countdownStart > 0) "$countdownStart" else "¡Vamos!",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    val ejercicioActual = rutina.getOrNull(index)

    ejercicioActual?.let {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ejercicio ${index + 1} de ${rutina.size}", style = MaterialTheme.typography.titleMedium)

            LinearProgressIndicator(
                progress = (segundosRestantes.coerceAtLeast(0).toFloat() / it.duracionSegundos),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Image(
                painter = painterResource(id = it.imagenRes),
                contentDescription = it.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Text(it.nombre, style = MaterialTheme.typography.displayLarge)
            Text(it.descripcion, style = MaterialTheme.typography.bodyLarge)

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

                if (segundosRestantes == 0) {
                    if (index < rutina.size - 1) {
                        Button(onClick = { index++ }) {
                            Text("Siguiente")
                        }
                    } else {
                        Button(onClick = {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            if (userId != null && user != null) {
                                guardarProgresoRutina(
                                    userId = userId,
                                    nivel = user!!.nivel,
                                    objetivos = user!!.objetivos,
                                    rutina = rutina,
                                    onSuccess = {
                                        navController.navigate("routine_success")
                                    },
                                    onError = { msg ->
                                        Toast.makeText(context, "Error al guardar progreso: $msg", Toast.LENGTH_LONG).show()
                                        navController.navigate("routine_success")
                                    }
                                )
                            } else {
                                Toast.makeText(context, "No se pudo guardar el progreso", Toast.LENGTH_LONG).show()
                                navController.navigate("routine_success")
                            }
                        }) {
                            Text("Finalizar rutina")
                        }

                    }
                }
            }
        }
    }
}
fun Context.playBeepSound() {
    val mediaPlayer = MediaPlayer.create(this, R.raw.beep)
    mediaPlayer.setOnCompletionListener { it.release() }
    mediaPlayer.start()
}
