package com.jcmateus.kalisfit.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.model.esTipoComplejo
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    navController: NavHostController,
    viewModel: EditRoutineViewModel = viewModel() // Obtén el ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // --- Observer para el resultado de AllExercisesScreen ---
    LaunchedEffect(
        lifecycleOwner,
        navController
    ) { // Observar cambios en lifecycleOwner o navController
        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            savedStateHandle.getLiveData<String>(NavigationKeys.SELECTED_EXERCISE_ID_KEY)
                .observe(lifecycleOwner) { exerciseId ->
                    if (exerciseId != null) {
                        Log.d("EditRoutineScreen", "Ejercicio seleccionado recibido: $exerciseId")
                        // Aquí llamas a la función del ViewModel que debe manejar este ID
                        // Asumimos que targetExerciseIndexForSelection se maneja en el ViewModel si es necesario
                        viewModel.addOrReplaceExerciseFromSelection(exerciseId)
                        // Importante: Remover el valor para que no se procese de nuevo
                        savedStateHandle.remove<String>(NavigationKeys.SELECTED_EXERCISE_ID_KEY)
                    }
                }
        }
    }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Rutina guardada", Toast.LENGTH_SHORT).show()
            viewModel.onSaveHandled() // Resetea el flag
            navController.popBackStack() // Vuelve a la pantalla anterior
        }
    }
    LaunchedEffect(uiState.errorMessages) {
        uiState.errorMessages.firstOrNull()?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // Aquí podrías tener una lógica para limpiar los mensajes de error del uiState
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewRoutine && uiState.originalTemplateId != null) "Personalizar Rutina" else if (uiState.isNewRoutine) "Crear Rutina" else "Editar Rutina") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveRoutine() },
                        enabled = !uiState.isLoading && uiState.routineToEdit != null
                    ) {
                        Text(
                            "Guardar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                if (uiState.isLoading && uiState.routineToEdit == null) { // Loading inicial
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                } else if (uiState.routineToEdit == null) {
                    Text(
                        text = uiState.errorMessages.joinToString("\n")
                            .ifEmpty { "No se pudo cargar la rutina para editar." },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // El contenido de la edición va aquí
                    EditRoutineContent(
                        navController = navController,
                        routine = uiState.routineToEdit!!,
                        isLoadingDuringSave = uiState.isLoading || uiState.isUploadingCoverImage,
                        currentCoverImageUrl = uiState.currentCoverImageUrl,
                        selectedCoverImageUri = uiState.selectedCoverImageUri,
                        isUploadingCoverImage = uiState.isUploadingCoverImage,
                        onCoverImageSelected = viewModel::onCoverImageSelected,
                        onClearSelectedCoverImage = viewModel::clearSelectedCoverImage,
                        onNameChange = viewModel::onRoutineNameChanged,
                        onDescriptionChange = viewModel::onDescriptionChanged,
                        onRoundsChange = viewModel::onRoundsChanged,
                        onRestBetweenRoundsChange = viewModel::onRestBetweenRoundsChanged,
                        // Callbacks para Ejercicios
                        onExerciseSeriesChange = viewModel::onExerciseSeriesChanged,
                        onExerciseRepsChange = viewModel::onExerciseSimpleRepsChanged,
                        onExerciseDurationChange = viewModel::onExerciseSimpleDurationChanged,
                        onExerciseRestChange = viewModel::onExerciseRestBetweenSeriesChanged, // Descanso ENTRE series
                        onExerciseTempoChange = viewModel::onExerciseTempoChanged,
                        onExerciseIsUnilateralChange = viewModel::onExerciseIsUnilateralChanged,
                        onRemoveExercise = viewModel::onRemoveExercise,
                        onMoveExerciseUp = viewModel::onMoveExerciseUp,
                        onMoveExerciseDown = viewModel::onMoveExerciseDown,
                        onDuplicateExercise = viewModel::onDuplicateExercise,
                        // Callbacks para Componentes de Ejercicio
                        onExerciseComponentRepsChange = viewModel::onExerciseComponentRepsChanged,
                        onExerciseComponentDurationChange = viewModel::onExerciseComponentDurationChanged,
                        onExerciseComponentNameChange = viewModel::onExerciseComponentNameChanged,
                        onAddComponentToExercise = viewModel::onAddComponentToExercise,
                        onRemoveComponentFromExercise = viewModel::onRemoveComponentFromExercise,
                        onAddNewBlankExercise = viewModel::onAddNewBlankExercise,
                        onNavigateToSelectExercise = { exerciseIndexToReplace ->
                            viewModel.prepareForExerciseSelection(exerciseIndexToReplace)
                            navController.navigate(Routes.allExercises(isSelectingForRoutine = true))
                        },
                        // <<< CORRECCIÓN/VERIFICACIÓN: Asegúrate de que estos callbacks se pasen al ViewModel
                        onExerciseNameChange = { index, name -> // Este es el callback para el nombre del EJERCICIO
                            viewModel.onExerciseNameChanged(index, name)
                        },
                        onExercisePostRestChange = { index, rest -> // Callback para el descanso DESPUÉS del ejercicio
                            viewModel.onExercisePostRestChanged(index, rest)
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class) // Para algunos componentes de Material 3
@Composable
fun EditRoutineContent(
    navController: NavHostController,
    routine: UserCustomRoutine,
    isLoadingDuringSave: Boolean,
    currentCoverImageUrl: String?,
    selectedCoverImageUri: Uri?,
    isUploadingCoverImage: Boolean,
    onCoverImageSelected: (Uri?) -> Unit,
    onClearSelectedCoverImage: () -> Unit,
    onNameChange: (String) -> Unit, // Nombre de la RUTINA
    onDescriptionChange: (String) -> Unit,
    onRoundsChange: (String) -> Unit,
    onRestBetweenRoundsChange: (String) -> Unit,

    // Callbacks para PROPIEDADES del ejercicio
    onExerciseSeriesChange: (exerciseIndex: Int, newSeries: String) -> Unit,
    onExerciseRepsChange: (exerciseIndex: Int, newReps: String) -> Unit,
    onExerciseDurationChange: (exerciseIndex: Int, newDuration: String) -> Unit,
    onExerciseRestChange: (exerciseIndex: Int, newRest: String) -> Unit, // Descanso ENTRE series
    onExerciseTempoChange: (exerciseIndex: Int, newTempo: String) -> Unit,
    onExerciseIsUnilateralChange: (exerciseIndex: Int, isUnilateral: Boolean) -> Unit,

    // Callbacks para ACCIONES sobre el ejercicio en la lista
    onRemoveExercise: (exerciseIndex: Int) -> Unit,
    onMoveExerciseUp: (exerciseIndex: Int) -> Unit,
    onMoveExerciseDown: (exerciseIndex: Int) -> Unit,
    onDuplicateExercise: (exerciseIndex: Int) -> Unit,

    // Callbacks para COMPONENTES del ejercicio
    onExerciseComponentRepsChange: (exerciseIndex: Int, componentIndex: Int, newReps: String) -> Unit,
    onExerciseComponentDurationChange: (exerciseIndex: Int, componentIndex: Int, newDuration: String) -> Unit,
    onExerciseComponentNameChange: (exerciseIndex: Int, componentIndex: Int, newName: String) -> Unit,
    onAddComponentToExercise: (exerciseIndex: Int) -> Unit,
    onRemoveComponentFromExercise: (exerciseIndex: Int, componentIndex: Int) -> Unit,

    // Callback para AÑADIR/SELECCIONAR ejercicio
    onAddNewBlankExercise: () -> Unit,
    onNavigateToSelectExercise: (exerciseIndexToReplace: Int?) -> Unit,

    // <<< CORRECCIÓN/VERIFICACIÓN: Estos son los parámetros que EditRoutineContent DEBE recibir
    onExerciseNameChange: (exerciseIndex: Int, newName: String) -> Unit, // Para el nombre del EJERCICIO editable
    onExercisePostRestChange: (exerciseIndex: Int, newRest: String) -> Unit // Para el descanso DESPUÉS del ejercicio
) {
    // Launcher para el Photo Picker
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> // El URI puede ser nulo si el usuario cancela
            onCoverImageSelected(uri)
        }
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECCIÓN DE IMAGEN DE PORTADA ---
        item {
            Text("Imagen de Portada", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Proporción común para portadas
                    .clip(MaterialTheme.shapes.medium)
                    //.background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !isLoadingDuringSave && !isUploadingCoverImage) {
                        getContentLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                var imageToShow: Any? = null
                if (selectedCoverImageUri != null) {
                    imageToShow = selectedCoverImageUri
                } else if (!currentCoverImageUrl.isNullOrBlank()) {
                    imageToShow = currentCoverImageUrl
                }

                if (imageToShow != null) {
                    AsyncImage(
                        model = imageToShow,
                        contentDescription = "Imagen de portada de la rutina",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_error_placeholder) // Reemplaza con tu placeholder
                    )
                    // Botón para quitar la imagen seleccionada (si hay una nueva selección)
                    if (selectedCoverImageUri != null) {
                        IconButton(
                            onClick = onClearSelectedCoverImage,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            enabled = !isLoadingDuringSave && !isUploadingCoverImage
                        ) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Quitar imagen seleccionada",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    // Placeholder si no hay imagen
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Añadir imagen de portada",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Toca para seleccionar una imagen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isUploadingCoverImage) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    getContentLauncher.launch("image/*")
                },
                enabled = !isLoadingDuringSave && !isUploadingCoverImage,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    if (selectedCoverImageUri != null || !currentCoverImageUrl.isNullOrBlank())
                        "Cambiar Imagen" else "Seleccionar Imagen de Portada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        // --- SECCIÓN DE INFORMACIÓN DE LA RUTINA ---
        item {
            Text("Información de la Rutina", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = routine.nombrePersonalizado,
                onValueChange = onNameChange,
                label = { Text("Nombre de la Rutina") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingDuringSave,
                singleLine = true
            )
            OutlinedTextField(
                value = routine.descripcion ?: "",
                onValueChange = onDescriptionChange,
                label = { Text("Descripción (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingDuringSave,
                minLines = 3
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = routine.numeroDeRondas.toString(),
                    onValueChange = onRoundsChange,
                    label = { Text("Rondas") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = routine.descansoEntreRondasSegundos.toString(),
                    onValueChange = onRestBetweenRoundsChange,
                    label = { Text("Descanso (seg)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
        // --- SECCIÓN DE EJERCICIOS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ejercicios", style = MaterialTheme.typography.titleLarge)
                Row { // Para agrupar los botones de añadir
                    Button(
                        onClick = onAddNewBlankExercise,
                        enabled = !isLoadingDuringSave, // isLoadingDuringSave no está disponible aquí directamente, considera pasarlo o inferirlo
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Añadir Ejercicio en Blanco",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "En Blanco",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onNavigateToSelectExercise(null) }, // null porque estamos añadiendo uno nuevo, no reemplazando
                        enabled = !isLoadingDuringSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Filled.LibraryAdd,
                            contentDescription = "Añadir desde Biblioteca",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Todos Ejercicios",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        if (routine.ejercicios.isEmpty()) {
            item {
                Text(
                    "No hay ejercicios en esta rutina. ¡Añade algunos!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(
                count = routine.ejercicios.size, // Es buena práctica usar 'count'
                key = { index -> routine.ejercicios[index].id }
            ) { exerciseIndex ->
                val ejercicio = routine.ejercicios[exerciseIndex]
                EditableExerciseItem(
                    exercise = ejercicio,
                    exerciseIndex = exerciseIndex,
                    isLoadingDuringSave = isLoadingDuringSave,
                    onSeriesChange = { series -> onExerciseSeriesChange(exerciseIndex, series) },
                    onRepsChange = { reps -> onExerciseRepsChange(exerciseIndex, reps) },
                    onDurationChange = { duration -> onExerciseDurationChange(exerciseIndex, duration) },
                    onRestChange = { rest -> onExerciseRestChange(exerciseIndex, rest) }, // Descanso ENTRE series
                    onTempoChange = { tempo -> onExerciseTempoChange(exerciseIndex, tempo) },
                    onIsUnilateralChange = { isUnilateral -> onExerciseIsUnilateralChange(exerciseIndex, isUnilateral) },
                    onRemove = { onRemoveExercise(exerciseIndex) },
                    onMoveUp = { onMoveExerciseUp(exerciseIndex) },
                    onMoveDown = { onMoveExerciseDown(exerciseIndex) },
                    onDuplicate = { onDuplicateExercise(exerciseIndex) },
                    isFirstExercise = exerciseIndex == 0,
                    isLastExercise = exerciseIndex == routine.ejercicios.size - 1,
                    // Componentes
                    onComponentNameChange = { compIdx, name -> onExerciseComponentNameChange(exerciseIndex, compIdx, name) },
                    onComponentRepsChange = { compIdx, reps -> onExerciseComponentRepsChange(exerciseIndex, compIdx, reps) },
                    onComponentDurationChange = { compIdx, duration -> onExerciseComponentDurationChange(exerciseIndex, compIdx, duration) },
                    onAddComponent = { onAddComponentToExercise(exerciseIndex) },
                    onRemoveComponent = { compIdx -> onRemoveComponentFromExercise(exerciseIndex, compIdx) },
                    onReplaceExerciseClicked = { onNavigateToSelectExercise(exerciseIndex) },

                    // <<< CORRECCIÓN: Así se pasan los callbacks a EditableExerciseItem
                    onExercisePostRestChange = { newRestValue ->
                        // 'onExercisePostRestChange' aquí es el parámetro de EditRoutineContent.
                        // Lo llamamos con el 'exerciseIndex' capturado.
                        onExercisePostRestChange(exerciseIndex, newRestValue)
                    },
                    onExerciseNameChanged = if (ejercicio.nombre == "Nuevo Ejercicio" || ejercicio.id.isBlank()) {
                        // El lambda que se pasa a EditableExerciseItem solo toma 'newName'.
                        { newNameValue ->
                            // 'onExerciseNameChange' aquí es el parámetro de EditRoutineContent.
                            // Lo llamamos con el 'exerciseIndex' capturado.
                            onExerciseNameChange(exerciseIndex, newNameValue)
                        }
                    } else {
                        null // <<< CORRECCIÓN: 'else null' es necesario para la expresión if
                    }
                )
                if (exerciseIndex < routine.ejercicios.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableExerciseItem(
    exercise: Ejercicio,
    exerciseIndex: Int,
    isLoadingDuringSave: Boolean,
    onSeriesChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,         // Para reps del ejercicio SIMPLE o POR_LADO
    onDurationChange: (String) -> Unit,     // Para duración del ejercicio SIMPLE o PADRE COMPLEJO
    onRestChange: (String) -> Unit,         // Descanso ENTRE series del ejercicio principal
    onExercisePostRestChange: (String) -> Unit,
    onTempoChange: (String) -> Unit,        // Para tempo del ejercicio CON_TEMPO
    onIsUnilateralChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onReplaceExerciseClicked: () -> Unit,
    isFirstExercise: Boolean,
    isLastExercise: Boolean,
    // Componentes
    onComponentNameChange: (componentIndex: Int, newName: String) -> Unit,
    onComponentRepsChange: (componentIndex: Int, newReps: String) -> Unit,       // Reps DEL COMPONENTE
    onComponentDurationChange: (componentIndex: Int, newDuration: String) -> Unit, // Duración DEL COMPONENTE
    onAddComponent: () -> Unit,
    onRemoveComponent: (componentIndex: Int) -> Unit,
    onExerciseNameChanged: ((String) -> Unit)? = null
) {
    var showComponents by remember(exercise.id, exercise.tipoEjercicio) {
        mutableStateOf(exercise.esTipoComplejo()) // Mantenemos tu función de extensión
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Fila Superior: Nombre del Ejercicio y Acciones ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onExerciseNameChanged != null && (exercise.nombre == "Nuevo Ejercicio" || exercise.id.isBlank())) {
                    OutlinedTextField(
                        value = exercise.nombre,
                        onValueChange = { newName -> onExerciseNameChanged.invoke(newName) },
                        label = { Text("Nombre Ejercicio") },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        enabled = !isLoadingDuringSave,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        exercise.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onMoveUp, enabled = !isLoadingDuringSave && !isFirstExercise) {
                        Icon(Icons.Filled.ArrowUpward, "Mover arriba")
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLoadingDuringSave && !isLastExercise) {
                        Icon(Icons.Filled.ArrowDownward, "Mover abajo")
                    }
                    IconButton(onClick = onDuplicate, enabled = !isLoadingDuringSave) {
                        Icon(Icons.Filled.ContentCopy, "Duplicar")
                    }
                    IconButton(onClick = onRemove, enabled = !isLoadingDuringSave) {
                        Icon(Icons.Filled.Delete, "Eliminar Ejercicio")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // --- Botón para reemplazar/cambiar ejercicio ---
            if (exercise.nombre == "Nuevo Ejercicio" || exercise.id.isBlank()) {
                Button(onClick = onReplaceExerciseClicked, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Elegir Ejercicio de Biblioteca")
                }
            } else {
                Button(
                    onClick = onReplaceExerciseClicked,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text("Cambiar Ejercicio", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // --- Campos del EJERCICIO PADRE ---
            // Fila 1: Series (SIEMPRE) y Descanso Entre Series (SIEMPRE)
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = exercise.numeroDeSeries.toString(),
                    onValueChange = onSeriesChange,
                    label = { Text("Series") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = exercise.descansoEntreSeriesSegundos.toString(),
                    onValueChange = onRestChange,
                    label = { Text("Desc. Series (s)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Fila 2: Reps / Duración / Tempo - Dependiente del TipoDeEjercicio del PADRE
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val fieldModifier = Modifier.weight(1f)

                when (exercise.tipoEjercicio) {
                    TipoDeEjercicio.SIMPLE -> {
                        if (exercise.duracionSegundosOriginal > 0) {
                            OutlinedTextField( // Duración para ejercicio SIMPLE
                                value = exercise.duracionSegundosOriginal.toString(),
                                onValueChange = onDurationChange, // Usa el onDurationChange principal
                                label = { Text("Duración (s)") },
                                modifier = fieldModifier,
                                enabled = !isLoadingDuringSave,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        } else {
                            OutlinedTextField( // Reps para ejercicio SIMPLE
                                value = exercise.repeticionesOriginal,
                                onValueChange = onRepsChange, // Usa el onRepsChange principal
                                label = { Text("Repeticiones") },
                                modifier = fieldModifier,
                                enabled = !isLoadingDuringSave,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        // Para ejercicios simples, podríamos necesitar un segundo campo vacío si el otro lado de la Row lo ocupa el descanso post-ejercicio
                        // Pero como el descanso post-ejercicio ahora está separado, esto debería estar bien.
                        // Si solo hay un campo (reps o duración), y quieres que el otro lado esté vacío para alinear con otros,
                        // puedes añadir un Box(modifier = fieldModifier)
                        if (exercise.duracionSegundosOriginal <= 0) { // Si se mostraron reps, deja espacio para tempo si fuera con_tempo
                            // O si no es con_tempo, deja espacio para que la UI no se comprima si hay otro campo al lado
                            // Box(modifier = fieldModifier) // Descomentar si es necesario para el layout
                        }
                    }
                    TipoDeEjercicio.CON_TEMPO -> {
                        OutlinedTextField( // Reps para CON_TEMPO
                            value = exercise.repeticionesOriginal,
                            onValueChange = onRepsChange, // Usa el onRepsChange principal
                            label = { Text("Reps") },
                            modifier = fieldModifier,
                            enabled = !isLoadingDuringSave,
                            singleLine = true
                        )
                        OutlinedTextField( // Tempo para CON_TEMPO
                            value = exercise.notaTempo ?: "",
                            onValueChange = onTempoChange,
                            label = { Text("Tempo (ej: 4010)") },
                            modifier = fieldModifier,
                            enabled = !isLoadingDuringSave,
                            singleLine = true
                        )
                    }
                    TipoDeEjercicio.POR_LADO_ALTERNADO -> {
                        OutlinedTextField( // Reps para POR_LADO_ALTERNADO
                            value = exercise.repeticionesOriginal,
                            onValueChange = onRepsChange, // Usa el onRepsChange principal
                            label = { Text("Reps (por lado)") },
                            modifier = fieldModifier.fillMaxWidth(), // Ocupa toda la fila si es el único campo aquí
                            enabled = !isLoadingDuringSave,
                            singleLine = true
                        )
                        // No necesita un segundo campo en esta fila para este tipo.
                    }
                    // Para SUPERSET_SEQUENCIAL y otros tipos COMPLEJOS:
                    // El PADRE SÍ puede tener una duración total. NO tiene repeticiones editables (esas van en componentes).
                    else -> { // Incluye SUPERSET_SEQUENCIAL, COMBINADO_TEMPORIZADO, CIRCUITO_TEMPORIZADO
                        if (exercise.esTipoComplejo()) { // Doble check, aunque el when ya lo filtra
                            OutlinedTextField( // Duración para el PADRE COMPLEJO
                                value = exercise.duracionSegundosOriginal.toString(),
                                onValueChange = onDurationChange, // Usa el onDurationChange principal
                                label = { Text("Duración Total Superset/Circuito (s)") },
                                modifier = fieldModifier.fillMaxWidth(), // Ocupa toda la fila
                                enabled = !isLoadingDuringSave,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                placeholder = { Text("0 si es por reps de componentes") }
                            )
                            // El padre complejo NO tiene un campo de "Repeticiones" editable aquí.
                            // Las repeticiones se definen en los componentes.
                            // El campo "repeticionesOriginal" del JSON ("12+15") es un resumen.
                        } else {
                            // Caso inesperado, todos los tipos deberían estar cubiertos
                            Box(modifier = fieldModifier)
                        }
                    }
                }
            } // Fin Fila 2 (Reps/Duración/Tempo del PADRE)
            Spacer(modifier = Modifier.height(8.dp))
            // Descanso POST Ejercicio (SIEMPRE visible para el ejercicio PADRE)
            OutlinedTextField(
                value = exercise.descansoDespuesEjercicioSegundos.toString(),
                onValueChange = onExercisePostRestChange,
                label = { Text("Descanso POST-Ejercicio (s)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingDuringSave,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(if (exercise.esTipoComplejo()) 0.dp else 12.dp)) // Menos espacio si vienen componentes
            // --- SECCIÓN DE COMPONENTES (SOLO si es complejo) ---
            if (exercise.esTipoComplejo()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showComponents = !showComponents }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${exercise.tipoEjercicio.displayName} (${exercise.componentes.size} ejercicios)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (showComponents) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showComponents) "Ocultar componentes" else "Mostrar componentes"
                    )
                }
                AnimatedVisibility(visible = showComponents) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (exercise.componentes.isEmpty()) {
                            Text(
                                "Este ${exercise.tipoEjercicio.displayName} aún no tiene ejercicios componentes. ¡Añade algunos!",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
                            )
                        } else {
                            exercise.componentes.forEachIndexed { compIndex, componente ->
                                EditableComponentItem( // PASAMOS LOS CALLBACKS CORRECTOS PARA COMPONENTES
                                    component = componente,
                                    componentIndex = compIndex,
                                    totalComponents = exercise.componentes.size,
                                    isLoadingDuringSave = isLoadingDuringSave,
                                    onNameChange = { newName -> onComponentNameChange(compIndex, newName) },
                                    onRepsChange = { reps -> onComponentRepsChange(compIndex, reps) }, // Reps del COMPONENTE
                                    onDurationChange = { duration -> onComponentDurationChange(compIndex, duration) }, // Duración del COMPONENTE
                                    onRemove = { onRemoveComponent(compIndex) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onAddComponent,
                            enabled = !isLoadingDuringSave,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = "Añadir Ejercicio al ${exercise.tipoEjercicio.displayName}", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Añadir Ejercicio al ${exercise.tipoEjercicio.displayName}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(12.dp))
            }
            // --- Checkbox para "Unilateral" ---
            // (La lógica que tenías aquí para el checkbox Unilateral puede permanecer igual,
            //  asegurándote de que no se muestre innecesariamente para POR_LADO_ALTERNADO si ya es implícito)
            if (exercise.tipoEjercicio != TipoDeEjercicio.POR_LADO_ALTERNADO) { // Evita duplicar si POR_LADO ya lo implica
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                        .clickable { if (!isLoadingDuringSave) onIsUnilateralChange(!exercise.esUnilateral) }
                        .fillMaxWidth()
                ) {
                    Checkbox(
                        checked = exercise.esUnilateral,
                        onCheckedChange = { if (!isLoadingDuringSave) onIsUnilateralChange(it) },
                        enabled = !isLoadingDuringSave
                    )
                    Text(
                        "Marcar como unilateral (se realiza por cada lado)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
// EditableComponentItem permanece como lo tenías, ya que maneja reps/duración DEL COMPONENTE:
@Composable
fun EditableComponentItem(
    component: ComponenteEjercicio,
    componentIndex: Int,
    totalComponents: Int,
    isLoadingDuringSave: Boolean,
    onNameChange: (String) -> Unit,
    onRepsChange: (String) -> Unit, // Callback para las REPS del COMPONENTE
    onDurationChange: (String) -> Unit, // Callback para la DURACIÓN del COMPONENTE
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = component.nombreEspecifico.takeIf { !it.isNullOrBlank() } ?: "Ejercicio Componente ${componentIndex + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, enabled = !isLoadingDuringSave) {
                    Icon(Icons.Filled.DeleteOutline, "Eliminar Componente", tint = MaterialTheme.colorScheme.error)
                }
            }
            Divider(modifier = Modifier.padding(bottom = 4.dp))

            OutlinedTextField(
                value = component.nombreEspecifico ?: "",
                onValueChange = onNameChange,
                label = { Text("Nombre Ejercicio Componente") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingDuringSave,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField( // Reps DEL COMPONENTE
                    value = component.repeticiones ?: "",
                    onValueChange = onRepsChange, // Este es onComponentRepsChange
                    label = { Text("Repeticiones Componente") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField( // Duración DEL COMPONENTE
                    value = component.duracionSegundos?.toString() ?: "",
                    onValueChange = onDurationChange, // Este es onComponentDurationChange
                    label = { Text("Duración Componente (s)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text ("0 si es por reps")}
                )
            }
        }
    }
}

