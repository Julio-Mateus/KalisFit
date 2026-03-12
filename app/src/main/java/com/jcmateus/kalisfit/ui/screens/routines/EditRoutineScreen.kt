package com.jcmateus.kalisfit.ui.screens.routines

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.model.esTipoComplejo
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.ui.screens.NavigationKeys
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModel
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditRoutineScreen(navController: NavHostController) {
    val owner = LocalSavedStateRegistryOwner.current
    val viewModel: EditRoutineViewModel = viewModel(factory = EditRoutineViewModelFactory(owner))
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, navController) {
        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            savedStateHandle.getLiveData<String>(NavigationKeys.SELECTED_EXERCISE_ID_KEY)
                .observe(lifecycleOwner) { exerciseId ->
                    if (exerciseId != null) {
                        viewModel.addOrReplaceExerciseFromSelection(exerciseId)
                        savedStateHandle.remove<String>(NavigationKeys.SELECTED_EXERCISE_ID_KEY)
                    }
                }
        }
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DISEÑAR ENTRENAMIENTO", fontWeight = FontWeight.Black, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Button(onClick = { viewModel.saveRoutine() }, shape = RoundedCornerShape(12.dp)) {
                        Text("GUARDAR")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            uiState.routineToEdit?.let { routine ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { RoutineHeaderSection(routine, viewModel, uiState) }
                    
                    item { 
                        Text("EJERCICIOS", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }

                    itemsIndexed(routine.ejercicios, key = { _, ej -> ej.id }) { index, exercise ->
                        DetailedExerciseCard(exercise, index, viewModel, imageLoader)
                    }

                    item {
                        OutlinedButton(
                            onClick = { 
                                viewModel.prepareForExerciseSelection(null)
                                navController.navigate(Routes.allExercises(true)) 
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("AÑADIR DESDE BIBLIOTECA")
                        }
                    }
                }
            }
            if (uiState.isLoading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.onSaveHandled()
            navController.popBackStack()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineHeaderSection(routine: UserCustomRoutine, viewModel: EditRoutineViewModel, uiState: com.jcmateus.kalisfit.viewmodel.EditRoutineUiState) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = routine.nombrePersonalizado,
                onValueChange = { viewModel.onRoutineNameChanged(it) },
                label = { Text("Nombre de la Rutina") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = routine.descripcion,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Descripción / Nota") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = routine.numeroDeRondas.toString(),
                    onValueChange = { viewModel.onRoundsChanged(it) },
                    label = { Text("Rondas") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = routine.descansoEntreRondasSegundos.toString(),
                    onValueChange = { viewModel.onRestBetweenRoundsChanged(it) },
                    label = { Text("Descanso Rondas (s)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Text("Configuración:", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Principiante", "Intermedio", "Avanzado").forEach { nivel ->
                    FilterChip(
                        selected = uiState.editableNivelRutina.contains(nivel),
                        onClick = { viewModel.onNivelRutinaChanged(nivel, !uiState.editableNivelRutina.contains(nivel)) },
                        label = { Text(nivel) }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedExerciseCard(exercise: Ejercicio, index: Int, viewModel: EditRoutineViewModel, imageLoader: ImageLoader) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = exercise.imagenUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    imageLoader = imageLoader
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(exercise.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val infoText = if (exercise.duracionSegundosOriginal > 0) "${exercise.numeroDeSeries} Ser x ${exercise.duracionSegundosOriginal}s"
                                  else "${exercise.numeroDeSeries} Ser x ${exercise.repeticionesOriginal} Rep"
                    Text(infoText, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                IconButton(onClick = { viewModel.onRemoveExercise(index) }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exercise.numeroDeSeries.toString(),
                            onValueChange = { viewModel.onExerciseSeriesChanged(index, it) },
                            label = { Text("Series") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = exercise.repeticionesOriginal,
                            onValueChange = { viewModel.onExerciseSimpleRepsChanged(index, it) },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // CAMPO DE TIEMPO RESTAURADO
                        OutlinedTextField(
                            value = exercise.duracionSegundosOriginal.toString(),
                            onValueChange = { viewModel.onExerciseSimpleDurationChanged(index, it) },
                            label = { Text("Tiempo (s)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = exercise.descansoEntreSeriesSegundos.toString(),
                            onValueChange = { viewModel.onExerciseRestBetweenSeriesChanged(index, it) },
                            label = { Text("Desc. Serie (s)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exercise.descansoDespuesEjercicioSegundos.toString(),
                            onValueChange = { viewModel.onExercisePostRestChanged(index, it) },
                            label = { Text("Desc. Final (s)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = exercise.notaTempo ?: "",
                            onValueChange = { viewModel.onExerciseTempoChanged(index, it) },
                            label = { Text("Tempo") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("4010") }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exercise.esUnilateral, onCheckedChange = { viewModel.onExerciseIsUnilateralChanged(index, it) })
                        Text("Ejercicio Unilateral", style = MaterialTheme.typography.bodySmall)
                    }

                    if (exercise.esTipoComplejo()) {
                        Text("COMPONENTES DEL CIRCUITO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        exercise.componentes.forEach { comp ->
                            ComponentReadOnlyItem(comp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentReadOnlyItem(comp: ComponenteEjercicio) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(comp.nombreEspecifico ?: "Ejercicio", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(if (!comp.repeticiones.isNullOrBlank()) "${comp.repeticiones} r" else "${comp.duracionSegundos} s", style = MaterialTheme.typography.labelSmall)
        }
    }
}
