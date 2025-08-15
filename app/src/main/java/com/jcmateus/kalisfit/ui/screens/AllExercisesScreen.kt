package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.copy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.values
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Equipamiento
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.viewmodel.AllExercisesUiState
import com.jcmateus.kalisfit.viewmodel.AllExercisesViewModel
import kotlin.text.contains
import kotlin.text.lowercase

// Clave para el resultado del ejercicio seleccionado (defínela en un lugar accesible, ej. un object)
object NavigationKeys {
    const val SELECTED_EXERCISE_ID_KEY = "selectedExerciseIdResult"
}

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Añadir ExperimentalLayoutApi para FlowRow
@Composable
fun AllExercisesScreen(
    navController: NavHostController,
    viewModel: AllExercisesViewModel = viewModel(),
    isSelectingForRoutine: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMuscleGroups by rememberSaveable { mutableStateOf<Set<GrupoMuscular>>(emptySet()) }
    var selectedEquipments by rememberSaveable { mutableStateOf<Set<Equipamiento>>(emptySet()) }
    // Podrías añadir más estados para otros filtros (lugar, etc.)

    var showFilters by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectingForRoutine) "Seleccionar Ejercicio" else "Biblioteca de Ejercicios") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                },
                actions = {
                    if (!isSelectingForRoutine) { // Mostrar botón de filtro solo si no estamos seleccionando
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                imageVector = if (showFilters) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                                contentDescription = if (showFilters) "Ocultar filtros" else "Mostrar filtros"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                //.background(MaterialTheme.colorScheme.surface)
        ) {
            // --- CAMPO DE BÚSQUEDA ---
            OutlinedTextField(
                value = uiState.searchTerm,
                onValueChange = { viewModel.updateSearchTerm(it) },
                label = { Text("Buscar ejercicio...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (uiState.searchTerm.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchTerm("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = MaterialTheme.shapes.extraLarge
            )
            // --- SECCIÓN DE FILTROS ANIMADA ---
            AnimatedVisibility(visible = showFilters && !isSelectingForRoutine) {
                FilterSection(
                    allMuscleGroups = GrupoMuscular.values().toList(),
                    selectedMuscleGroups = selectedMuscleGroups,
                    onMuscleGroupSelected = { group, isSelected ->
                        selectedMuscleGroups = if (isSelected) {
                            selectedMuscleGroups + group
                        } else {
                            selectedMuscleGroups - group
                        }
                    },
                    allEquipments = Equipamiento.values().toList(), // Asumiendo que tienes Equipamiento.values()
                    selectedEquipments = selectedEquipments,
                    onEquipmentSelected = { equipment, isSelected ->
                        selectedEquipments = if (isSelected) {
                            selectedEquipments + equipment
                        } else {
                            selectedEquipments - equipment
                        }
                    },
                    onClearAllFilters = {
                        selectedMuscleGroups = emptySet()
                        selectedEquipments = emptySet()
                        // Limpia otros filtros aquí si los añades
                    }
                )
            }
            RenderExercisesContent(
                uiState = uiState,
                isSelectingForRoutine = isSelectingForRoutine,
                selectedMuscleGroups = selectedMuscleGroups,
                selectedEquipments = selectedEquipments,
                onExerciseClick = { exerciseId, isExpanded ->
                    if (isSelectingForRoutine) {
                        Log.d("AllExercisesScreen", "Ejercicio SELECCIONADO: $exerciseId")
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(NavigationKeys.SELECTED_EXERCISE_ID_KEY, exerciseId)
                        navController.popBackStack()
                    } else {
                        Log.d("AllExercisesScreen", "Clic en tarjeta: $exerciseId, Expandido: $isExpanded")
                        // La expansión se maneja en la card
                    }
                },
                onAddExerciseToRoutine = { exerciseId ->
                    Log.d("AllExercisesScreen", "AÑADIR ejercicio a rutina: $exerciseId")
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavigationKeys.SELECTED_EXERCISE_ID_KEY, exerciseId)
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class) // Para FlowRow y FilterChip
@Composable
fun FilterSection(
    allMuscleGroups: List<GrupoMuscular>,
    selectedMuscleGroups: Set<GrupoMuscular>,
    onMuscleGroupSelected: (GrupoMuscular, Boolean) -> Unit,
    allEquipments: List<Equipamiento>, // Necesitas pasar la lista de todos los equipamientos
    selectedEquipments: Set<Equipamiento>,
    onEquipmentSelected: (Equipamiento, Boolean) -> Unit,
    onClearAllFilters: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filtros", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClearAllFilters) {
                Text("Limpiar Todos")
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        FilterGroup(title = "Grupo Muscular") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio vertical entre filas de chips
            ) {
                allMuscleGroups.forEach { group ->
                    val isSelected = selectedMuscleGroups.contains(group)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMuscleGroupSelected(group, !isSelected) },
                        label = { Text(group.displayName) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Done, contentDescription = "Seleccionado", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        FilterGroup(title = "Equipamiento") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Filtra NINGUNO si no quieres que sea una opción seleccionable
                allEquipments.filter { it != Equipamiento.NINGUNO }.forEach { equipment ->
                    val isSelected = selectedEquipments.contains(equipment)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEquipmentSelected(equipment, !isSelected) },
                        label = { Text(equipment.displayName) }, // Asume que Equipamiento tiene displayName
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Done, contentDescription = "Seleccionado", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
        }
        // Puedes añadir más FilterGroup para LugarEntrenamiento, etc.
        Spacer(Modifier.height(8.dp)) // Espacio al final de la sección de filtros
    }
}
@Composable
fun FilterGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RenderExercisesContent(
    uiState: AllExercisesUiState,
    isSelectingForRoutine: Boolean,
    selectedMuscleGroups: Set<GrupoMuscular>,
    selectedEquipments: Set<Equipamiento>,
    onExerciseClick: (exerciseId: String, isExpanded: Boolean) -> Unit,
    onAddExerciseToRoutine: (String) -> Unit
) {
    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(Modifier.size(48.dp)) }
        uiState.errorMessage != null -> Box(Modifier
            .fillMaxSize()
            .padding(16.dp), Alignment.Center) {
            Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        else -> {
            val exercisesAfterTextSearch = if (uiState.searchTerm.isBlank()) {
                uiState.exercises
            } else {
                uiState.exercises.filter { ejercicio ->
                    val searchTermLower = uiState.searchTerm.lowercase()
                    ejercicio.nombre.lowercase().contains(searchTermLower) ||
                            ejercicio.descripcion.lowercase().contains(searchTermLower)
                }
            }
            val filteredExercises = exercisesAfterTextSearch.filter { ejercicio ->
                val muscleMatch = selectedMuscleGroups.isEmpty() || selectedMuscleGroups.any { ejercicio.grupoMuscular.contains(it) }
                val equipmentMatch = selectedEquipments.isEmpty() || selectedEquipments.any { selEquip ->
                    // Asumimos que ejercicio.equipamientoNecesario es List<String>
                    // y Equipamiento tiene un método fromString o comparamos por displayName/name
                    ejercicio.equipamientoNecesario.mapNotNull { Equipamiento.fromString(it) }.any { it == selEquip }
                }
                // Añade más condiciones de filtro aquí si es necesario (ej. lugar)
                // val placeMatch = selectedPlaces.isEmpty() || selectedPlaces.any { ejercicio.lugarEntrenamiento.contains(it) }
                muscleMatch && equipmentMatch // && placeMatch
            }
            if (filteredExercises.isEmpty() && (uiState.searchTerm.isNotBlank() || selectedMuscleGroups.isNotEmpty() || selectedEquipments.isNotEmpty())) {
                Box(Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp), Alignment.Center) {
                    Text(
                        "No se encontraron ejercicios que coincidan con tu búsqueda y filtros.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (uiState.exercises.isEmpty() && uiState.searchTerm.isBlank() && selectedMuscleGroups.isEmpty() && selectedEquipments.isEmpty()){
                Box(Modifier
                    .fillMaxSize()
                    .padding(16.dp), Alignment.Center) {
                    Text("No hay ejercicios disponibles en la biblioteca.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp), // Ajusta según el diseño de tu card
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredExercises, key = { it.id }) { ejercicio ->
                        ExerciseGridItemCard(
                            ejercicio = ejercicio,
                            isSelectingForRoutine = isSelectingForRoutine,
                            onCardClick = { isExpanded ->
                                onExerciseClick(ejercicio.id, isExpanded)
                            },
                            onAddToRoutineClick = { onAddExerciseToRoutine(ejercicio.id) }
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
fun ExerciseGridItemCard(
    ejercicio: Ejercicio,
    isSelectingForRoutine: Boolean,
    onCardClick: (isExpanded: Boolean) -> Unit,
    onAddToRoutineClick: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }
    val elevation = animateDpAsState(targetValue = if (isExpanded && !isSelectingForRoutine) 7.dp else 2.dp, label = "card_elevation")
    Card(
        onClick = {
            if (isSelectingForRoutine) {
                onCardClick(false)
            } else {
                isExpanded = !isExpanded
                onCardClick(isExpanded)
            }
        },
        modifier = Modifier
            .fillMaxWidth() // La grid se encarga del tamaño real
            .animateContentSize(),
        shape = MaterialTheme.shapes.small, // Bordes un poco menos redondeados para un look más "grid"
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) // Un poco más sutil
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.value)
    ) {
        Column {
            // --- IMAGEN ---
            Box( // Contenedor para la imagen con altura fija o proporción definida
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp) // Altura fija para la imagen, ayuda a uniformar cards
                    // O usa .aspectRatio(4f / 3f) si prefieres proporción
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) // Fondo por si la imagen no carga
            ) {
                if (!ejercicio.imagenUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(data = ejercicio.imagenUrl)
                                .crossfade(true)
                                .placeholder(R.drawable.ic_default_placeholder) // Asegúrate que exista
                                .error(R.drawable.ic_error_placeholder)         // Asegúrate que exista
                                .build(),
                            imageLoader = imageLoader
                        ),
                        contentDescription = "Imagen de ${ejercicio.nombre}",
                        modifier = Modifier
                            .fillMaxSize() // La imagen llena el Box
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)), // Redondeo solo arriba
                        contentScale = ContentScale.Crop // CAMBIO: Para que llene el espacio, recortando si es necesario
                    )
                } else {
                    // Placeholder visual si no hay imagen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_default_placeholder), // Usa tu placeholder
                            contentDescription = "Sin imagen",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            // --- CONTENIDO ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 10.dp,
                        bottom = if (isSelectingForRoutine || (isExpanded && !isSelectingForRoutine)) 6.dp else 10.dp
                    )
            ) {
                Text(
                    text = ejercicio.nombre,
                    style = MaterialTheme.typography.titleSmall, // Un poco más pequeño para grid
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (isExpanded && !isSelectingForRoutine) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Mostrar brevemente el grupo muscular principal si no está expandido y no seleccionando
                if (!isExpanded && !isSelectingForRoutine && ejercicio.grupoMuscular.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = ejercicio.grupoMuscular.first().displayName, // Solo el primer grupo para brevedad
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                // --- CONTENIDO EXPANDIDO (Visible solo si isExpanded y no isSelectingForRoutine) ---
                if (isExpanded && !isSelectingForRoutine) {
                    Spacer(Modifier.height(6.dp))
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        "Descripción:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary // Destacar un poco el título
                    )
                    Text(
                        ejercicio.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4, // Limitar para no hacer la card demasiado grande
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (ejercicio.grupoMuscular.isNotEmpty()) {
                        Text(
                            "Músculos: ${ejercicio.grupoMuscular.joinToString { it.displayName }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp, // Un poco más pequeño
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    // Puedes añadir más detalles como equipamiento, etc. si el espacio lo permite
                    // o un botón "Ver más" que navegue a una pantalla de detalle completa.
                }
            }
            // --- ACCIONES EN LA CARD ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (isExpanded && !isSelectingForRoutine) 4.dp else 0.dp) // Menos padding vertical si está expandida
                    .heightIn(min = 36.dp), // Altura mínima para asegurar espacio para botones
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!ejercicio.videoUrl.isNullOrBlank() && !isSelectingForRoutine) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ejercicio.videoUrl))
                            try { context.startActivity(intent) } catch (e: Exception) { Log.e("ExerciseGridItemCard", "Error al abrir video", e) }
                        },
                        modifier = Modifier.size(32.dp) // Más pequeño
                    ) {
                        Icon(
                            Icons.Filled.PlayCircleOutline,
                            contentDescription = "Ver video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp) // Icono más pequeño
                        )
                    }
                }
                if (isSelectingForRoutine) {
                    Spacer(Modifier.weight(1f)) // Empuja el botón al final
                    FilledTonalButton(
                        onClick = onAddToRoutineClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Añadir",
                            modifier = Modifier.size(16.dp),

                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir", style = MaterialTheme.typography.labelSmall)
                    }
                } else if (isExpanded) {
                    // Botón para ir a detalles completos si está expandida
                    // Spacer(Modifier.weight(1f))
                    // TextButton(onClick = { /* TODO: Navegar a pantalla de detalle del ejercicio ejercicio.id */ }) {
                    //     Text("Ver Más", style = MaterialTheme.typography.labelSmall)
                    // }
                }
            }
            // Espacio inferior si no hay botones visibles en modo no selección y no expandido
            if (!isSelectingForRoutine && !isExpanded) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}