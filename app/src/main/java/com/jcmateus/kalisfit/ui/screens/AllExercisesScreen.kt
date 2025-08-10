package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.copy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Equipamiento
import com.jcmateus.kalisfit.viewmodel.AllExercisesUiState
import com.jcmateus.kalisfit.viewmodel.AllExercisesViewModel
import kotlin.text.contains
import kotlin.text.lowercase


@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExercisesScreen(
    navController: NavHostController,
    viewModel: AllExercisesViewModel = viewModel(),
    isSelectingForRoutine: Boolean = false,
    onExerciseSelected: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectingForRoutine) "Seleccionar Ejercicio" else "Todos los Ejercicios") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                }
                // No necesitamos un icono de filtro si tenemos la barra de búsqueda directamente
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // Importante aplicar el padding del Scaffold
                .fillMaxSize()
        ) {
            // --- CAMPO DE BÚSQUEDA ---
            OutlinedTextField( // O TextField normal si prefieres ese estilo
                value = uiState.searchTerm,
                onValueChange = { viewModel.updateSearchTerm(it) }, // Llama al ViewModel para actualizar el término
                label = { Text("Buscar ejercicio...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { // Icono de lupa
                    Icon(Icons.Filled.Search, contentDescription = "Buscar")
                },
                trailingIcon = { // Icono para limpiar la búsqueda si hay texto
                    if (uiState.searchTerm.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchTerm("") }) { // Limpia el término
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search // Cambia el botón de "Enter" a "Buscar"
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        // Opcional: Podrías forzar el cierre del teclado aquí si lo deseas
                        // LocalSoftwareKeyboardController.current?.hide()
                    }
                )
            )
            // --- FIN CAMPO DE BÚSQUEDA ---

            RenderExercisesContent( // Pasa el uiState completo, que contiene el searchTerm
                uiState = uiState,
                isSelectingForRoutine = isSelectingForRoutine,
                onExerciseClick = { exerciseId ->
                    if (isSelectingForRoutine) {
                        Log.d("AllExercisesScreen", "Ejercicio SELECCIONADO: $exerciseId")
                        onExerciseSelected(exerciseId)
                    } else {
                        Log.d("AllExercisesScreen", "Ejercicio clickeado (para detalle): $exerciseId")
                        // La expansión es manejada dentro de ExerciseItemCard
                    }
                },
                onAddExerciseToRoutine = { exerciseId ->
                    Log.d("AllExercisesScreen", "Solicitud para AÑADIR ejercicio a rutina: $exerciseId")
                    onExerciseSelected(exerciseId) // Reutilizamos para cerrar/confirmar selección
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RenderExercisesContent(
    uiState: AllExercisesUiState,
    isSelectingForRoutine: Boolean, // Para pasar al ExerciseItemCard
    onExerciseClick: (String) -> Unit, // Acción principal al hacer clic en la tarjeta
    onAddExerciseToRoutine: (String) -> Unit // Acción para el botón de añadir
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.errorMessage != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        uiState.exercises.isEmpty() && uiState.searchTerm.isBlank() -> { // Modificado para que no aparezca si hay término de búsqueda
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay ejercicios disponibles.", modifier = Modifier.padding(16.dp))
            }
        }
        else -> {
            val filteredExercises = if (uiState.searchTerm.isBlank()) {
                uiState.exercises
            } else {
                uiState.exercises.filter { ejercicio ->
                    val searchTermLower = uiState.searchTerm.lowercase()
                    ejercicio.nombre.lowercase().contains(searchTermLower) ||
                            ejercicio.descripcion.lowercase().contains(searchTermLower) ||
                            ejercicio.grupoMuscular.any { grupoMuscularEnum ->
                                grupoMuscularEnum.displayName.lowercase().contains(searchTermLower) ||
                                        grupoMuscularEnum.name.lowercase().contains(searchTermLower)
                            }
                }
            }

            if (filteredExercises.isEmpty() && uiState.searchTerm.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), contentAlignment = Alignment.Center
                ) {
                    Text("No hay ejercicios que coincidan con '${uiState.searchTerm}'.")
                }
            } else if (filteredExercises.isEmpty() && uiState.exercises.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay ejercicios disponibles para mostrar.", modifier = Modifier.padding(16.dp))
                }
            }
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Ajuste de padding
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Más espacio entre cards
                ) {
                    items(filteredExercises, key = { it.id }) { ejercicio ->
                        ExerciseItemCard(
                            ejercicio = ejercicio,
                            isSelectingForRoutine = isSelectingForRoutine,
                            onCardClick = {
                                // La expansión ahora se maneja dentro de ExerciseItemCard.
                                // Si no estamos seleccionando para rutina, el onExerciseClick
                                // podría ser usado para navegar a una pantalla de detalle completa.
                                // Si estamos seleccionando, el onExerciseClick ya fue definido
                                // para seleccionar y potencialmente cerrar.
                                if (!isSelectingForRoutine) {
                                    Log.d("RenderContent", "Card click (no seleccionando): ${ejercicio.id}. Expansión manejada internamente.")
                                    // Aquí podrías llamar a onExerciseClick si quieres una acción adicional
                                    // a nivel de pantalla, además de la expansión.
                                    // onExerciseClick(ejercicio.id) // Descomenta si necesitas esta llamada también
                                } else {
                                    // Si estamos en modo selección, el clic en cualquier parte de la tarjeta la selecciona
                                    onExerciseClick(ejercicio.id)
                                }
                            },
                            onAddToRoutineClick = {
                                // Esta es la acción específica del botón "+"
                                onAddExerciseToRoutine(ejercicio.id)
                            }
                        )
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseItemCard(
    ejercicio: Ejercicio,
    isSelectingForRoutine: Boolean, // Nuevo: para saber si mostrar el botón "+"
    onCardClick: () -> Unit,        // Nuevo: para manejar el clic en la tarjeta (expansión/navegación)
    onAddToRoutineClick: () -> Unit // Nuevo: para el botón de añadir
) {
    val context = LocalContext.current
    // Estado para controlar la expansión de la tarjeta
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    // ImageLoader para GIFs (asegúrate de tener la dependencia coil-gif)
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ImageDecoderDecoder.Factory()) // Necesario para GIFs en API 28+
        }
        .build()

    Card(
        // El onClick de la Card ahora cambia el estado de expansión
        // o ejecuta la acción principal si estamos seleccionando
        onClick = {
            if (isSelectingForRoutine) {
                onCardClick() // Ejecuta la acción de selección
            } else {
                isExpanded = !isExpanded // Cambia el estado de expansión
                onCardClick() // También llama al onCardClick general (podría ser para logs o futuras acciones)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // Para animar el cambio de tamaño
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Un poco más de elevación
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Padding general para la columna interna
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top // Alinea al top para que el botón de añadir no descuadre
            ) {
                // Columna para Imagen y Nombre (ocupa la mayor parte del espacio)
                Column(modifier = Modifier.weight(1f)) {
                    if (!ejercicio.imagenUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(data = ejercicio.imagenUrl)
                                    .apply {
                                        crossfade(true)
                                        placeholder(R.drawable.ic_default_placeholder)
                                        error(R.drawable.ic_error_placeholder)
                                    }.build(),
                                imageLoader = imageLoader // Usa el imageLoader con soporte para GIF
                            ),
                            contentDescription = "Imagen de ${ejercicio.nombre}",
                            modifier = Modifier
                                .fillMaxWidth() // La imagen ocupa todo el ancho disponible en esta columna
                                .aspectRatio(1f) // Altura aumentada para la imagen
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop // Crop puede ser mejor para GIFs grandes
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Text(
                        ejercicio.nombre,
                        style = MaterialTheme.typography.titleLarge, // Estilo más grande para el nombre
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Columna para los botones de acción (Video y Añadir)
                Column(horizontalAlignment = Alignment.End) {
                    if (isSelectingForRoutine) {
                        IconButton(onClick = {
                            isExpanded = false // Colapsa si estaba expandido al añadir
                            onAddToRoutineClick()
                        }) {
                            Icon(
                                Icons.Filled.AddCircleOutline,
                                contentDescription = "Añadir ejercicio a rutina",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp) // Icono un poco más grande
                            )
                        }
                    }
                    if (!ejercicio.videoUrl.isNullOrBlank() && !isSelectingForRoutine) { // No mostrar vídeo si estamos seleccionando
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ejercicio.videoUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("ExerciseItemCard", "No se pudo abrir el video: ${ejercicio.videoUrl}", e)
                            }
                        }) {
                            Icon(
                                Icons.Filled.PlayCircleOutline,
                                contentDescription = "Ver video del ejercicio",
                                tint = MaterialTheme.colorScheme.secondary, // Color diferente para distinguir
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }


            // Contenido que se muestra solo cuando la tarjeta está expandida
            // y no estamos en modo de selección (para no saturar la vista de selección)
            if (isExpanded && !isSelectingForRoutine) {
                Spacer(Modifier.height(12.dp))
                Text(
                    ejercicio.descripcion,
                    style = MaterialTheme.typography.bodyMedium, // Un poco más grande para la descripción
                )
                Spacer(Modifier.height(10.dp))

                if (ejercicio.grupoMuscular.isNotEmpty()) {
                    Text(
                        "Músculos: ${ejercicio.grupoMuscular.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(6.dp))
                }

                if (ejercicio.equipamientoNecesario.isNotEmpty()) {
                    val equipamientoEnums = ejercicio.equipamientoNecesario
                        .mapNotNull { Equipamiento.fromString(it) }
                    if (equipamientoEnums.isNotEmpty()) {
                        val equipamientoDisplay = equipamientoEnums
                            .filter { it != Equipamiento.NINGUNO }
                            .joinToString { it.displayName }
                        if (equipamientoDisplay.isNotBlank()) {
                            Text(
                                "Equipo: $equipamientoDisplay",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                if (ejercicio.lugarEntrenamiento.isNotEmpty()) {
                    Text(
                        "Lugar: ${ejercicio.lugarEntrenamiento.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Aquí podrías añadir más detalles si los tienes:
                // Por ejemplo: Duración, Repeticiones, Tipo de ejercicio, Componentes (si es superset), etc.
                // Text("Duración Original: ${ejercicio.duracionSegundosOriginal}s", style = MaterialTheme.typography.bodySmall)
                // Text("Repeticiones Original: ${ejercicio.repeticionesOriginal}", style = MaterialTheme.typography.bodySmall)
                // Text("Series: ${ejercicio.numeroDeSeries}", style = MaterialTheme.typography.bodySmall)

            } else if (!isExpanded && !isSelectingForRoutine) {
                // Mostrar una breve descripción o pista si no está expandido y no estamos seleccionando
                Spacer(Modifier.height(8.dp))
                Text(
                    ejercicio.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, // Solo 2 líneas cuando está colapsado
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}