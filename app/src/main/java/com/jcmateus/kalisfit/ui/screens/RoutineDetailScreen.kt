package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineDetailViewModel

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    navController: NavHostController,
    currentUserId: String?,
) {
    val routineDetailViewModel: RoutineDetailViewModel = viewModel()
    val uiState by routineDetailViewModel.uiState.collectAsState()
    val rutinaParaEditar by routineDetailViewModel.navigateToEditRoutine.collectAsState()
    val routineIdToExecute by routineDetailViewModel.startRoutineExecution.collectAsState()
    val rutinaParaEjecutar by routineDetailViewModel.startRoutineExecution.collectAsState()

    // Manejar navegación para personalizar
    LaunchedEffect(rutinaParaEditar) {
        rutinaParaEditar?.let { userCustomRoutine -> // userCustomRoutine es la que el ViewModel preparó
            Log.d("RoutineDetailScreen", "Navegación solicitada a edición para: ${userCustomRoutine.nombrePersonalizado}")

            val currentId = currentUserId ?: run {
                Log.e("RoutineDetailScreen", "Error: currentUserId es nulo, no se puede navegar a editar.")
                routineDetailViewModel.onNavigationToEditRoutineDone() // Resetea el trigger
                return@LaunchedEffect // No navegar si no hay userId
            }

            // --- USA TU FUNCIÓN HELPER DE Routes.kt ---
            val routeToNavigate = Routes.editRoutine(
                userId = currentId,
                // Pasa solo el ID de la plantilla original.
                // EditRoutineViewModel usará esto para cargar la plantilla y luego creará
                // una NUEVA UserCustomRoutine en memoria con un NUEVO ID generado por él mismo.
                templateId = userCustomRoutine.originalTemplateId,
                customRoutineId = null // <--- ¡IMPORTANTE!
                // O no lo incluyas si tu función Routes.editRoutine y la ruta
                // lo manejan como opcional/nullable por defecto.
            )
            // ------------------------------------------

            Log.d("RoutineDetailScreen", "Navegando a: $routeToNavigate")
            navController.navigate(routeToNavigate)

            routineDetailViewModel.onNavigationToEditRoutineDone() // Muy importante para resetear el estado
        }
    }

    // Manejar inicio de ejecución de rutina
    LaunchedEffect(routineIdToExecute) { // Cambiado de rutinaParaEjecutar a routineIdToExecute
        routineIdToExecute?.let { id -> // 'id' ahora es el String del routineId
            Log.d("RoutineDetailScreen", "Navegación solicitada para ejecutar rutina ID: $id")

            // --- NAVEGACIÓN DIRECTA USANDO EL HELPER DE ROUTES.KT ---
            navController.navigate(Routes.startRoutineExecution(id)) // <--- LLAMADA DIRECTA A NAVIGATE
            // ---------------------------------------------------------

            routineDetailViewModel.onRutinaExecutionStarted() // Resetea el estado en el ViewModel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.rutina?.nombre ?: "Detalle de Rutina") },
                navigationIcon = {
                     IconButton(onClick = { navController.popBackStack() }) {
                         Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                     }
                },
                actions = {
                    IconButton(onClick = { routineDetailViewModel.refreshRoutineDetails() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(
                        Alignment.Center
                    ))
                } else if (uiState.errorMessage != null) {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else if (uiState.rutina == null) {
                    Text(
                        text = "Rutina no disponible.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    uiState.rutina?.let { rutina ->
                        RoutineDetailContent(
                            rutina = rutina,
                            onIniciarClicked = {
                                routineDetailViewModel.onIniciarRutinaClicked()
                            },
                            onPersonalizarClicked = {
                                if (currentUserId != null) {
                                    routineDetailViewModel.onPersonalizarRutinaClicked()
                                } else {
                                    // Mostrar mensaje para iniciar sesión
                                    Log.w("RoutineDetailScreen", "Intento de personalizar sin userId.")
                                    // Considera mostrar un Snackbar o Toast
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RoutineDetailContent(
    rutina: Rutina,
    onIniciarClicked: () -> Unit,
    onPersonalizarClicked: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(rutina.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (rutina.descripcion.isNotBlank()) {
                Text(rutina.descripcion, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Mostrar más detalles como nivel, objetivos, lugar
            Text("Nivel: ${rutina.nivelRecomendado.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            Text("Objetivos: ${rutina.objetivos.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            Text("Lugar: ${rutina.lugarEntrenamiento.joinToString { it.name }}", style = MaterialTheme.typography.bodyMedium)
            Text("Rondas: ${rutina.numeroDeRondas}", style = MaterialTheme.typography.bodyMedium)
            if (rutina.numeroDeRondas > 1 && rutina.descansoEntreRondasSegundos > 0) {
                Text("Descanso entre Rondas: ${rutina.descansoEntreRondasSegundos}s", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onIniciarClicked,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = "Iniciar")
                    Spacer(Modifier.width(8.dp))
                    Text("INICIAR RUTINA")
                }
                OutlinedButton(
                    onClick = onPersonalizarClicked,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Personalizar")
                    Spacer(Modifier.width(8.dp))
                    Text("EDITAR RUTINA")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Ejercicios:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(rutina.ejercicios, key = { it.id }) { ejercicio ->
            EjercicioInDetailCard(ejercicio)
            Divider()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun EjercicioInDetailCard(ejercicio: Ejercicio) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp) // Un poco más de padding vertical para la tarjeta
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp) // Padding general dentro de la tarjeta
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Centra verticalmente el texto y la imagen
        ) {
            // Columna para los detalles del texto (ocupará el espacio disponible)
            Column(
                modifier = Modifier.weight(1f), // Toma el espacio restante después de la imagen
                verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre los textos
            ) {
                Text(ejercicio.nombre, style = MaterialTheme.typography.titleMedium)

                Text("Series: ${ejercicio.numeroDeSeries}", style = MaterialTheme.typography.bodySmall)

                if (ejercicio.repeticionesOriginal.isNotBlank() && ejercicio.repeticionesOriginal != "0") {
                    Text("Repeticiones: ${ejercicio.repeticionesOriginal}", style = MaterialTheme.typography.bodySmall)
                }
                if (ejercicio.duracionSegundosOriginal > 0) {
                    Text("Duración: ${ejercicio.duracionSegundosOriginal}s", style = MaterialTheme.typography.bodySmall)
                }
                if (ejercicio.descansoEntreSeriesSegundos > 0) {
                    Text("Descanso entre series: ${ejercicio.descansoEntreSeriesSegundos}s", style = MaterialTheme.typography.bodySmall)
                }
                if (ejercicio.componentes.isNotEmpty()) {
                    Text(
                        "Componentes:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    ejercicio.componentes.forEach { comp ->
                        Text(
                            "- ${comp.nombreEspecifico ?: ""} ${comp.repeticiones ?: ""} ${comp.duracionSegundos?.let { "${it}s" } ?: ""}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Espacio entre el texto y la imagen
            Spacer(modifier = Modifier.width(16.dp))

            // Imagen/GIF del ejercicio (si la URL existe)
            if (!ejercicio.imagenUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ejercicio.imagenUrl)
                        .decoderFactory(ImageDecoderDecoder.Factory()) // Necesario para GIFs en API 28+
                        .crossfade(true) // Efecto de fundido suave al cargar
                        .placeholder(R.drawable.ic_default_placeholder) // Reemplaza con tu drawable
                        .error(R.drawable.ic_error_placeholder)       // Reemplaza con tu drawable
                        .build(),
                    contentDescription = "Imagen de ${ejercicio.nombre}",
                    modifier = Modifier
                        .size(100.dp) // Tamaño fijo para la imagen, ajústalo como necesites
                        .align(Alignment.CenterVertically), // Asegura que esté centrado si la altura del texto es diferente
                    contentScale = ContentScale.Fit // O ContentScale.Crop, según prefieras
                )
            } else {
                // Opcional: Mostrar un placeholder si no hay imagenUrl
                // Podrías usar un Icono o una imagen de placeholder genérica aquí
                // Image(
                //     painter = painterResource(id = R.drawable.ic_no_image_available),
                //     contentDescription = "No hay imagen disponible",
                //     modifier = Modifier.size(100.dp),
                //     contentScale = ContentScale.Fit
                // )
            }
        }
    }
}
