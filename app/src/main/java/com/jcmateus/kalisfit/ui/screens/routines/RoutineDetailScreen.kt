package com.jcmateus.kalisfit.ui.screens.routines

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.esTipoComplejo
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors( containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = "Iniciar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("INICIAR RUTINA", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
@Composable
fun ChipComponent(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun EjercicioInDetailCard(ejercicio: Ejercicio) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Elevación sutil para la card principal
    ) {
        Column { // Usar Column para apilar la info del ejercicio y luego los componentes
            // --- Sección de detalles del Ejercicio Padre ---
            Row(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = if (ejercicio.componentes.isEmpty()) 16.dp else 8.dp
                    ) // Menos padding bottom si hay componentes
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Columna para los detalles del texto del EJERCICIO PADRE
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ejercicio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (ejercicio.esTipoComplejo()) {
                            Spacer(Modifier.width(8.dp))
                            ChipComponent(text = ejercicio.tipoEjercicio.displayName)
                        }
                    }

                    Text("Series: ${ejercicio.numeroDeSeries}", style = MaterialTheme.typography.bodyMedium)

                    val esSimpleOTempo = ejercicio.tipoEjercicio == TipoDeEjercicio.SIMPLE ||
                            ejercicio.tipoEjercicio == TipoDeEjercicio.CON_TEMPO

                    if (esSimpleOTempo || (ejercicio.componentes.isEmpty() && (ejercicio.repeticionesOriginal.isNotBlank() || ejercicio.duracionSegundosOriginal > 0))) {
                        if (ejercicio.repeticionesOriginal.isNotBlank() && ejercicio.repeticionesOriginal != "0") {
                            Text(
                                "Repeticiones: ${ejercicio.repeticionesOriginal}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (ejercicio.duracionSegundosOriginal > 0) {
                            Text(
                                "Duración: ${ejercicio.duracionSegundosOriginal}s",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else if (ejercicio.duracionSegundosOriginal > 0 && ejercicio.componentes.isNotEmpty()) {
                        if (ejercicio.tipoEjercicio == TipoDeEjercicio.CIRCUITO_TEMPORIZADO) {
                            Text(
                                "Duración total: ${ejercicio.duracionSegundosOriginal}s",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (ejercicio.descansoEntreSeriesSegundos > 0) {
                        Text(
                            "Descanso entre series: ${ejercicio.descansoEntreSeriesSegundos}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } // Fin de la Columna de detalles del ejercicio padre

                // --- IMAGEN PRINCIPAL DEL EJERCICIO (SOLO SI NO ES COMPLEJO y tiene URL) ---
                if (!ejercicio.esTipoComplejo()) { // Solo para ejercicios simples o sin componentes visibles con imagen propia
                    Spacer(modifier = Modifier.width(16.dp)) // Espacio a la izquierda de la imagen
                    if (!ejercicio.imagenUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(ejercicio.imagenUrl)
                                .decoderFactory(ImageDecoderDecoder.Factory())
                                .crossfade(true)
                                .placeholder(R.drawable.ic_default_placeholder) // Reemplaza con tu placeholder
                                .error(R.drawable.ic_error_placeholder)         // Reemplaza con tu error placeholder
                                .build(),
                            contentDescription = "Imagen de ${ejercicio.nombre}",
                            modifier = Modifier
                                .size(120.dp) // Tamaño para la imagen principal
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder si el ejercicio simple no tiene imagen
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = "No hay imagen disponible",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } // Fin de la Row para info del ejercicio padre

            // --- SECCIÓN DE COMPONENTES ---
            val componentesOrdenados = ejercicio.componentes.sortedBy { it.orden }
            if (componentesOrdenados.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp))
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Text(
                        "Componentes:",
                        style = MaterialTheme.typography.titleSmall, // Un poco más pequeño que el título del ejercicio
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    componentesOrdenados.forEachIndexed { index, comp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (index < componentesOrdenados.size - 1) 10.dp else 0.dp), // Espacio entre cards de componentes
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Menor elevación para componentes
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top // Alinea al top para que el texto y la imagen empiecen igual
                            ) {
                                // Columna para detalles del componente
                                Column(
                                    modifier = Modifier.weight(1f), // Toma el espacio disponible
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${index + 1}. ",
                                            style = MaterialTheme.typography.labelLarge, // Etiqueta para la numeración
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            comp.nombreEspecifico ?: "Componente sin nombre",
                                            style = MaterialTheme.typography.bodyLarge, // Texto principal del componente
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    val detallesComponente = mutableListOf<String>()
                                    comp.repeticiones?.takeIf { it.isNotBlank() && it != "0" }?.let { detallesComponente.add("Reps: $it") }
                                    comp.duracionSegundos?.takeIf { it > 0 }?.let { detallesComponente.add("Tiempo: ${it}s") }

                                    if (detallesComponente.isNotEmpty()) {
                                        Text(
                                            detallesComponente.joinToString(" / "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            // modifier = Modifier.padding(start = 20.dp) // Opcional: indentar detalles
                                        )
                                    }
                                }

                                // Espacio entre texto e imagen del componente
                                Spacer(modifier = Modifier.width(12.dp))

                                // --- IMAGEN DEL COMPONENTE ---
                                if (!comp.imagenUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(comp.imagenUrl)
                                            .decoderFactory(ImageDecoderDecoder.Factory())
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_default_placeholder) // Reemplaza
                                            .error(R.drawable.ic_error_placeholder)         // Reemplaza
                                            .build(),
                                        contentDescription = "Imagen de ${comp.nombreEspecifico}",
                                        modifier = Modifier
                                            .size(90.dp) // Tamaño para la imagen del componente, un poco más pequeño
                                            .clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop // Crop suele verse mejor para items de lista
                                    )
                                } else {
                                    // Placeholder si el COMPONENTE no tiene imagen
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.5f
                                                ), MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ImageNotSupported,
                                            contentDescription = "No hay imagen para este componente",
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // Fin de la Column principal de EjercicioInDetailCard
    }
}


