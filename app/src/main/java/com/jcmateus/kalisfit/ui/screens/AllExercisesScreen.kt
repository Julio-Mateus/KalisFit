package com.jcmateus.kalisfit.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Equipamiento
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.viewmodel.AllExercisesUiState
import com.jcmateus.kalisfit.viewmodel.AllExercisesViewModel

object NavigationKeys {
    const val SELECTED_EXERCISE_ID_KEY = "selectedExerciseIdResult"
}

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllExercisesScreen(
    navController: NavHostController,
    viewModel: AllExercisesViewModel = viewModel(),
    isSelectingForRoutine: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMuscleGroups by rememberSaveable { mutableStateOf<Set<GrupoMuscular>>(emptySet()) }
    var selectedEquipments by rememberSaveable { mutableStateOf<Set<Equipamiento>>(emptySet()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isSelectingForRoutine) "Seleccionar" else "Biblioteca", fontWeight = FontWeight.Black)
                        Text("${uiState.exercises.size} ejercicios disponibles", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = uiState.searchTerm,
                onValueChange = { viewModel.updateSearchTerm(it) },
                placeholder = { Text("¿Qué músculo entrenamos hoy?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchTerm.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchTerm("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            AnimatedVisibility(visible = showFilters) {
                FilterSection(
                    allMuscleGroups = GrupoMuscular.values().toList(),
                    selectedMuscleGroups = selectedMuscleGroups,
                    onMuscleGroupSelected = { group, isSelected ->
                        selectedMuscleGroups = if (isSelected) selectedMuscleGroups + group else selectedMuscleGroups - group
                    },
                    allEquipments = Equipamiento.values().toList(),
                    selectedEquipments = selectedEquipments,
                    onEquipmentSelected = { equipment, isSelected ->
                        selectedEquipments = if (isSelected) selectedEquipments + equipment else selectedEquipments - equipment
                    },
                    onClearAllFilters = {
                        selectedMuscleGroups = emptySet()
                        selectedEquipments = emptySet()
                    }
                )
            }

            RenderExercisesContent(
                uiState = uiState,
                isSelectingForRoutine = isSelectingForRoutine,
                selectedMuscleGroups = selectedMuscleGroups,
                selectedEquipments = selectedEquipments,
                imageLoader = imageLoader,
                onExerciseClick = { exerciseId ->
                    if (isSelectingForRoutine) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(NavigationKeys.SELECTED_EXERCISE_ID_KEY, exerciseId)
                        navController.popBackStack()
                    }
                },
                onAddExerciseToRoutine = { exerciseId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(NavigationKeys.SELECTED_EXERCISE_ID_KEY, exerciseId)
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    allMuscleGroups: List<GrupoMuscular>,
    selectedMuscleGroups: Set<GrupoMuscular>,
    onMuscleGroupSelected: (GrupoMuscular, Boolean) -> Unit,
    allEquipments: List<Equipamiento>,
    selectedEquipments: Set<Equipamiento>,
    onEquipmentSelected: (Equipamiento, Boolean) -> Unit,
    onClearAllFilters: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Filtrar por", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClearAllFilters) { Text("Limpiar") }
        }

        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            allMuscleGroups.forEach { group ->
                val isSelected = selectedMuscleGroups.contains(group)
                FilterChip(
                    selected = isSelected,
                    onClick = { onMuscleGroupSelected(group, !isSelected) },
                    label = { Text(group.displayName) },
                    leadingIcon = if (isSelected) { { Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) } } else null
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RenderExercisesContent(
    uiState: AllExercisesUiState,
    isSelectingForRoutine: Boolean,
    selectedMuscleGroups: Set<GrupoMuscular>,
    selectedEquipments: Set<Equipamiento>,
    imageLoader: ImageLoader,
    onExerciseClick: (String) -> Unit,
    onAddExerciseToRoutine: (String) -> Unit
) {
    val filteredExercises = uiState.exercises.filter { ej ->
        val searchMatch = uiState.searchTerm.isBlank() || ej.nombre.contains(uiState.searchTerm, ignoreCase = true)
        val muscleMatch = selectedMuscleGroups.isEmpty() || selectedMuscleGroups.any { ej.grupoMuscular.contains(it) }
        val equipmentMatch = selectedEquipments.isEmpty() || selectedEquipments.any { eq -> ej.equipamientoNecesario.any { it == eq.name } }
        searchMatch && muscleMatch && equipmentMatch
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredExercises, key = { it.id }) { ejercicio ->
                ExerciseGridItem(
                    ejercicio = ejercicio,
                    isSelecting = isSelectingForRoutine,
                    imageLoader = imageLoader,
                    onClick = { onExerciseClick(ejercicio.id) },
                    onAdd = { onAddExerciseToRoutine(ejercicio.id) }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseGridItem(
    ejercicio: Ejercicio,
    isSelecting: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = ejercicio.imagenUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                    imageLoader = imageLoader
                )
                
                if (isSelecting) {
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape).size(32.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    ejercicio.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    ejercicio.grupoMuscular.firstOrNull()?.displayName ?: "Cuerpo completo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
