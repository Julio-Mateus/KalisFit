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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.guardarProgresoRutina
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.EjercicioSimple
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutineScreen(
    navController: NavController,
    // Recibe el ID de la rutina desde los argumentos de navegación
    rutinaId: String?,
    // ViewModel para obtener la rutina específica
    routineViewModel: RoutineViewModel = viewModel(),
    // Necesitas el UserProfileViewModel para guardar el progreso (nivel y objetivos)
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    // Estado para la rutina cargada desde el RoutineViewModel
    val rutinaState = routineViewModel.rutina.collectAsState()
    val isLoading by routineViewModel.isLoading.collectAsState()
    val errorMessage by routineViewModel.errorMessage.collectAsState()

    // Estado del perfil del usuario desde el UserProfileViewModel (para guardar progreso)
    val user by userProfileViewModel.user.collectAsState()

    // Cargar la rutina cuando el ID esté disponible y la pantalla sea visible
    LaunchedEffect(rutinaId) {
        if (rutinaId != null) {
            routineViewModel.loadRutina(rutinaId)
        } else {
            // Si no hay ID de rutina, quizás mostrar un error o navegar de vuelta
            Toast.makeText(
                navController.context,
                "ID de rutina no proporcionado",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack() // Ejemplo: navegar de vuelta
        }
    }

    // Cargar el perfil del usuario para guardar el progreso
    LaunchedEffect(Unit) {
        userProfileViewModel.loadUserProfile()
    }


    // Estados locales para el progreso de la rutina (índice, temporizador, etc.)
    var index by remember { mutableStateOf(0) }
    var segundosRestantes by remember { mutableStateOf(0) } // Inicializar en 0, se actualiza al cargar la rutina
    var countdownStart by remember { mutableStateOf(3) }
    var rutinaEmpezada by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ⏱ Cuenta atrás inicial (solo si la rutina se ha cargado y no ha empezado)
    // Depende de rutinaState.value para asegurarse de que la rutina está cargada antes del countdown
    LaunchedEffect(rutinaState.value) {
        if (rutinaState.value != null && !rutinaEmpezada) {
            while (countdownStart > 0) {
                context.playBeepSound()
                delay(1000)
                countdownStart--
            }
            rutinaEmpezada = true
            // Iniciar el temporizador del primer ejercicio una vez que el countdown termina
            rutinaState.value?.ejercicios?.firstOrNull()?.let {
                segundosRestantes = it.duracionSegundos
            }
        }
    }

    // 🔁 Temporizador del ejercicio actual
    // Depende del índice, de rutinaEmpezada y de rutinaState.value
    LaunchedEffect(index, rutinaEmpezada, rutinaState.value) {
        // Solo iniciar el temporizador si la rutina ha empezado y la rutina está cargada
        if (rutinaEmpezada && rutinaState.value != null) {
            val currentEjercicio = rutinaState.value?.ejercicios?.getOrNull(index)
            if (currentEjercicio != null) {
                // Solo iniciar temporizador si el ejercicio es basado en tiempo (duracionSegundos > 0)
                if (currentEjercicio.duracionSegundos > 0) {
                    segundosRestantes = currentEjercicio.duracionSegundos
                    while (segundosRestantes > 0) {
                        delay(1000)
                        segundosRestantes--
                        if (segundosRestantes in 1..5) {
                            context.playBeepSound()
                        }
                    }
                } else {
                    // Si el ejercicio es basado en repeticiones, no hay temporizador de segundos restantes
                    segundosRestantes = 0
                }
            }
        }
    }

    // Mostrar estado de carga o error
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val rutinaCargada = rutinaState.value

    // Mostrar cuenta atrás inicial si la rutina está cargada pero no ha empezado
    if (rutinaCargada != null && !rutinaEmpezada) {
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

    // Si la rutina se ha cargado, ha empezado, y no hay errores, mostrar la UI de la rutina
    if (rutinaCargada != null) {
        val ejercicioActual = rutinaCargada.ejercicios.getOrNull(index)

        ejercicioActual?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Ejercicio ${index + 1} de ${rutinaCargada.ejercicios.size}",
                    style = MaterialTheme.typography.titleMedium
                )

                // Asegúrate de que la división por cero no ocurra si la duración es 0
                if (it.duracionSegundos > 0) {
                    LinearProgressIndicator(
                        progress = segundosRestantes.coerceAtLeast(0)
                            .toFloat() / it.duracionSegundos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                } else {
                    // Si es un ejercicio basado en repeticiones, quizás mostrar una barra de progreso "llena"
                    // o simplemente omitir el LinearProgressIndicator. Depende del diseño.
                    // Aquí lo omitimos para ejercicios sin duración.
                }


                // Usar Coil para cargar la imagen desde la URL
                val painter = rememberAsyncImagePainter(model = it.imagenUrl)
                Image(
                    painter = painter,
                    contentDescription = it.nombre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Text(it.nombre, style = MaterialTheme.typography.displayLarge)
                Text(it.descripcion, style = MaterialTheme.typography.bodyLarge)
                // Mostrar repeticiones si es un ejercicio basado en repeticiones
                if (it.repeticiones > 0) {
                    Text(
                        text = "Repeticiones: ${it.repeticiones}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Mostrar segundos restantes si es un ejercicio basado en tiempo
                    Text(
                        text = "⏱️ $segundosRestantes segundos restantes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }


                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Botón Anterior
                    if (index > 0) {
                        OutlinedButton(onClick = { index-- }) {
                            Text("Anterior")
                        }
                    } else {
                        // Espacio para alinear el botón "Siguiente" o "Finalizar" a la derecha
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Botón Siguiente o Finalizar
                    // Habilitado solo cuando el temporizador llega a 0 O si el ejercicio es basado en repeticiones
                    val isButtonEnabled = segundosRestantes == 0 || it.repeticiones > 0

                    if (index < rutinaCargada.ejercicios.size - 1) {
                        Button(
                            onClick = { index++ },
                            enabled = isButtonEnabled // Habilitar solo si el temporizador ha terminado
                        ) {
                            Text("Siguiente")
                        }
                    } else {
                        // Último ejercicio, botón para finalizar la rutina
                        Button(
                            onClick = {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                // Asegúrate de que el usuario y su perfil estén cargados antes de guardar
                                if (userId != null && user != null) {
                                    // Necesitas transformar la lista de Ejercicio a EjercicioSimple
                                    // si guardarProgresoRutina espera EjercicioSimple
                                    val ejerciciosSimple =
                                        rutinaCargada.ejercicios.map { ejercicio ->
                                            EjercicioSimple(
                                                id = ejercicio.id, // Asegúrate de que EjercicioSimple tiene ID
                                                nombre = ejercicio.nombre,
                                                duracionSegundos = ejercicio.duracionSegundos,
                                                repeticiones = ejercicio.repeticiones
                                                // Añade otros campos si EjercicioSimple los necesita
                                            )
                                        }

                                    guardarProgresoRutina(
                                        userId = userId,
                                        nivel = user!!.nivel, // Usar nivel del perfil cargado
                                        objetivos = user!!.objetivos, // Usar objetivos del perfil cargado
                                        rutina = rutinaCargada.ejercicios, // Pasar la Rutina completa si guardarProgresoRutina lo acepta
                                        // O pasar la lista de EjercicioSimple si la función espera eso
                                        // ejerciciosCompletados = ejerciciosSimple, // <-- Usa esta línea si la función lo espera
                                        onSuccess = {
                                            // Navegar a la pantalla de éxito después de guardar
                                            val currentDetailRoute = rutinaId?.let { Routes.routineDetail(it) } // Usa la función helper
                                            navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) { // <--- CORREGIDO
                                                // Opcional: Eliminar la pantalla actual de la pila para no poder volver con Back
                                                if (currentDetailRoute != null) {
                                                    popUpTo(currentDetailRoute) { inclusive = true }
                                                }
                                                // Considera limpiar más el stack si es necesario,
                                                // por ejemplo, si desde success se va a un tab principal.
                                            }
                                        },
                                        onError = { msg ->
                                            Toast.makeText(
                                                context,
                                                "Error al guardar progreso: $msg",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            // A pesar del error al guardar, quizás aún quieres navegar a la pantalla de éxito
                                            val currentDetailRoute = rutinaId?.let { Routes.routineDetail(it) }
                                            navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) { // <--- CORREGIDO
                                                if (currentDetailRoute != null) {
                                                    popUpTo(currentDetailRoute) { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "No se pudo guardar el progreso: Usuario o perfil no disponibles",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Navegar a la pantalla de éxito incluso si no se pudo guardar
                                    val currentDetailRoute = rutinaId?.let { Routes.routineDetail(it) }
                                    navController.navigate(Routes.ROUTINE_SUCCESS_SCREEN) { // <--- CORREGIDO
                                        if (currentDetailRoute != null) {
                                            popUpTo(currentDetailRoute) { inclusive = true }
                                        }
                                    }
                                }
                            },
                            enabled = isButtonEnabled // Habilitar solo si el temporizador ha terminado o es de repeticiones
                        ) {
                            Text("Finalizar rutina")
                        }
                    }
                }
            }
        } ?: run {
            // Esto manejaría el caso muy improbable de que ejercicioActual sea nulo aquí,
            // aunque la lógica anterior debería prevenirlo si rutinaCargada no es nulo.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Ejercicio no encontrado")
            }
        }
    } else {
        // Esto manejaría el caso de que rutinaCargada sea nulo después de haber pasado
        // las comprobaciones iniciales (loading, error, not started countdown).
        // En teoría, no debería ocurrir con la lógica actual.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Esperando rutina...")
        }
    }
}

// Función de extensión para reproducir el sonido (mantenida de tu código original)
fun Context.playBeepSound() {
    // Considera usar SoundPool para sonidos cortos y de baja latencia
    // MediaPlayer es más adecuado para archivos de audio más largos.
    val mediaPlayer = MediaPlayer.create(this, R.raw.beep)
    mediaPlayer?.setOnCompletionListener { it.release() }
    mediaPlayer?.start()
    // Manejo básico de posible fallo si MediaPlayer.create devuelve null
}