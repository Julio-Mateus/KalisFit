package com.jcmateus.kalisfit.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    navController: NavHostController,
    viewModel: EditRoutineViewModel = viewModel() // Obtén el ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                        Text("Guardar")
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()) {
                if (uiState.isLoading && uiState.routineToEdit == null) { // Loading inicial
                    CircularProgressIndicator(modifier = Modifier.align(
                        Alignment.Center
                    ))
                } else if (uiState.routineToEdit == null) {
                    Text(
                        text = uiState.errorMessages.joinToString("\n").ifEmpty { "No se pudo cargar la rutina para editar." },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // El contenido de la edición va aquí
                    EditRoutineContent(
                        routine = uiState.routineToEdit!!, // Sabemos que no es nulo aquí
                        isLoadingDuringSave = uiState.isLoading, // Para deshabilitar campos mientras guarda

                        // Callbacks para la Rutina
                        onNameChange = { viewModel.onRoutineNameChanged(it) },
                        onDescriptionChange = { viewModel.onDescriptionChanged(it) },
                        onRoundsChange = { viewModel.onRoundsChanged(it) },
                        onRestBetweenRoundsChange = { viewModel.onRestBetweenRoundsChanged(it) },

                        // Callbacks para Ejercicios
                        onExerciseSeriesChange = { index, series -> viewModel.onExerciseSeriesChanged(index, series) },
                        onExerciseRepsChange = { index, reps -> viewModel.onExerciseSimpleRepsChanged(index, reps) }, // Asume que onExerciseSimpleRepsChanged es el correcto para reps generales
                        onExerciseDurationChange = { index, duration -> viewModel.onExerciseSimpleDurationChanged(index, duration) }, // Asume similar para duration
                        onExerciseRestChange = { index, rest -> viewModel.onExerciseRestBetweenSeriesChanged(index, rest) },
                        onExerciseTempoChange = { index, tempo -> viewModel.onExerciseTempoChanged(index, tempo) },
                        onExerciseIsUnilateralChange = { index, isUnilateral -> viewModel.onExerciseIsUnilateralChanged(index, isUnilateral) },
                        onRemoveExercise = { index -> viewModel.onRemoveExercise(index) },
                        onMoveExerciseUp = { index -> viewModel.onMoveExerciseUp(index) },
                        onMoveExerciseDown = { index -> viewModel.onMoveExerciseDown(index) },
                        onDuplicateExercise = { index -> viewModel.onDuplicateExercise(index) },

                        // Callbacks para Componentes de Ejercicio
                        onExerciseComponentRepsChange = { exerciseIndex, componentIndex, newReps ->
                            viewModel.onExerciseComponentRepsChanged(exerciseIndex, componentIndex, newReps)
                        },
                        onExerciseComponentDurationChange = { exerciseIndex, componentIndex, newDuration ->
                            viewModel.onExerciseComponentDurationChanged(exerciseIndex, componentIndex, newDuration)
                        },
                        onExerciseComponentNameChange = { exerciseIndex, componentIndex, newName ->
                            viewModel.onExerciseComponentNameChanged(exerciseIndex, componentIndex, newName)
                        },
                        onAddComponentToExercise = { exerciseIndex ->
                            viewModel.onAddComponentToExercise(exerciseIndex)
                        },
                        onRemoveComponentFromExercise = { exerciseIndex, componentIndex ->
                            viewModel.onRemoveComponentFromExercise(exerciseIndex, componentIndex)
                        },

                        // Callback para añadir nuevo ejercicio
                        onAddNewBlankExercise = { viewModel.onAddNewBlankExercise() }
                    )
                }
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class) // Para algunos componentes de Material 3
@Composable
fun EditRoutineContent(
    routine: UserCustomRoutine,
    isLoadingDuringSave: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRoundsChange: (String) -> Unit,
    onRestBetweenRoundsChange: (String) -> Unit,

    onExerciseSeriesChange: (exerciseIndex: Int, newSeries: String) -> Unit,
    onExerciseRepsChange: (exerciseIndex: Int, newReps: String) -> Unit,
    onExerciseDurationChange: (exerciseIndex: Int, newDuration: String) -> Unit,
    onExerciseRestChange: (exerciseIndex: Int, newRest: String) -> Unit,
    onExerciseTempoChange: (exerciseIndex: Int, newTempo: String) -> Unit,
    onExerciseIsUnilateralChange: (exerciseIndex: Int, isUnilateral: Boolean) -> Unit,
    onRemoveExercise: (exerciseIndex: Int) -> Unit,
    onMoveExerciseUp: (exerciseIndex: Int) -> Unit,
    onMoveExerciseDown: (exerciseIndex: Int) -> Unit,
    onDuplicateExercise: (exerciseIndex: Int) -> Unit,

    onExerciseComponentRepsChange: (exerciseIndex: Int, componentIndex: Int, newReps: String) -> Unit,
    onExerciseComponentDurationChange: (exerciseIndex: Int, componentIndex: Int, newDuration: String) -> Unit,
    onExerciseComponentNameChange: (exerciseIndex: Int, componentIndex: Int, newName: String) -> Unit,
    onAddComponentToExercise: (exerciseIndex: Int) -> Unit,
    onRemoveComponentFromExercise: (exerciseIndex: Int, componentIndex: Int) -> Unit,

    onAddNewBlankExercise: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ejercicios", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onAddNewBlankExercise, enabled = !isLoadingDuringSave) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir Ejercicio")
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir")
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
            items(routine.ejercicios.size, key = { index -> routine.ejercicios[index].id }) { exerciseIndex ->
                val ejercicio = routine.ejercicios[exerciseIndex]
                EditableExerciseItem(
                    exercise = ejercicio,
                    exerciseIndex = exerciseIndex,
                    isLoadingDuringSave = isLoadingDuringSave,
                    onSeriesChange = { onExerciseSeriesChange(exerciseIndex, it) },
                    onRepsChange = { onExerciseRepsChange(exerciseIndex, it) },
                    onDurationChange = { onExerciseDurationChange(exerciseIndex, it) },
                    onRestChange = { onExerciseRestChange(exerciseIndex, it) },
                    onTempoChange = { onExerciseTempoChange(exerciseIndex, it) },
                    onIsUnilateralChange = { onExerciseIsUnilateralChange(exerciseIndex, it) },
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
                    onRemoveComponent = { compIdx -> onRemoveComponentFromExercise(exerciseIndex, compIdx) }
                )
                if (exerciseIndex < routine.ejercicios.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun EditableExerciseItem(
    exercise: Ejercicio,
    exerciseIndex: Int,
    isLoadingDuringSave: Boolean,
    onSeriesChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onRestChange: (String) -> Unit,
    onTempoChange: (String) -> Unit,
    onIsUnilateralChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    isFirstExercise: Boolean,
    isLastExercise: Boolean,

    // Componentes
    onComponentNameChange: (componentIndex: Int, newName: String) -> Unit,
    onComponentRepsChange: (componentIndex: Int, newReps: String) -> Unit,
    onComponentDurationChange: (componentIndex: Int, newDuration: String) -> Unit,
    onAddComponent: () -> Unit,
    onRemoveComponent: (componentIndex: Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(exercise.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)) // El nombre del ejercicio base no se edita aquí, se edita si es "Nuevo Ejercicio"
                Row {
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
                        Icon(Icons.Filled.Delete, "Eliminar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Si es un ejercicio "Nuevo Ejercicio", permitir editar el nombre
            if (exercise.nombre == "Nuevo Ejercicio") {
                OutlinedTextField(
                    value = exercise.nombre, // Deberíamos tener un callback onExerciseNameChange si queremos que esto se refleje
                    onValueChange = { /* TODO: viewModel.onExerciseNameChanged(exerciseIndex, it) */ },
                    label = { Text("Nombre del Ejercicio")},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingDuringSave,
                    singleLine = true
                )
            }


            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = exercise.numeroDeSeries.toString(),
                    onValueChange = onSeriesChange,
                    label = { Text("Series") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = exercise.descansoEntreSeriesSegundos.toString(),
                    onValueChange = onRestChange,
                    label = { Text("Descanso (s)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar campos según el TipoDeEjercicio
            when (exercise.tipoEjercicio) {
                TipoDeEjercicio.SIMPLE -> {
                    if (exercise.duracionSegundosOriginal > 0) { // Priorizar duración si existe
                        OutlinedTextField(
                            value = exercise.duracionSegundosOriginal.toString(),
                            onValueChange = onDurationChange,
                            label = { Text("Duración (s)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingDuringSave,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else {
                        OutlinedTextField(
                            value = exercise.repeticionesOriginal,
                            onValueChange = onRepsChange,
                            label = { Text("Repeticiones") }, // Ej: "10", "AMRAP", "Al fallo"
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingDuringSave
                        )
                    }
                }
                TipoDeEjercicio.CON_TEMPO -> {
                    OutlinedTextField(
                        value = exercise.repeticionesOriginal,
                        onValueChange = onRepsChange,
                        label = { Text("Repeticiones") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadingDuringSave
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exercise.notaTempo ?: "",
                        onValueChange = onTempoChange,
                        label = { Text("Tempo (ej: 3-1-2)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadingDuringSave
                    )
                }
                TipoDeEjercicio.POR_LADO_ALTERNADO -> {
                    OutlinedTextField(
                        value = exercise.repeticionesOriginal, // Ej: "10 por lado"
                        onValueChange = onRepsChange,
                        label = { Text("Repeticiones (ej: 10 por lado)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadingDuringSave
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exercise.esUnilateral, // Ya debería estar en true, pero es para consistencia
                            onCheckedChange = onIsUnilateralChange,
                            enabled = !isLoadingDuringSave
                        )
                        Text("Unilateral", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                TipoDeEjercicio.SUPERSET_SEQUENCIAL,
                TipoDeEjercicio.COMBINADO_TEMPORIZADO,
                TipoDeEjercicio.CIRCUITO_TEMPORIZADO -> {
                    Text("Componentes:", style = MaterialTheme.typography.titleSmall)
                    exercise.componentes.forEachIndexed { compIndex, componente ->
                        EditableComponentItem(
                            component = componente,
                            componentIndex = compIndex,
                            exerciseIndex = exerciseIndex, // Necesario para los callbacks del ViewModel
                            isLoadingDuringSave = isLoadingDuringSave,
                            onNameChange = { onComponentNameChange(compIndex, it) },
                            onRepsChange = { onComponentRepsChange(compIndex, it) },
                            onDurationChange = { onComponentDurationChange(compIndex, it) },
                            onRemove = { onRemoveComponent(compIndex) }
                        )
                    }
                    Button(onClick = onAddComponent, enabled = !isLoadingDuringSave) {
                        Text("Añadir Componente")
                    }
                }
            }

            // Checkbox para esUnilateral (puede ser aplicable a varios tipos)
            // Si el tipo es POR_LADO_ALTERNADO, ya se maneja arriba.
            // Aquí es para casos donde el tipo es SIMPLE pero podría ser unilateral.
            if (exercise.tipoEjercicio != TipoDeEjercicio.POR_LADO_ALTERNADO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Checkbox(
                        checked = exercise.esUnilateral,
                        onCheckedChange = onIsUnilateralChange,
                        enabled = !isLoadingDuringSave
                    )
                    Text("Marcar como unilateral (ej: se realiza por cada lado)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun EditableComponentItem(
    component: ComponenteEjercicio,
    componentIndex: Int,
    exerciseIndex: Int, // Para referencia si es necesario en callbacks futuros más complejos
    isLoadingDuringSave: Boolean,
    onNameChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Componente ${componentIndex + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove, enabled = !isLoadingDuringSave) {
                    Icon(Icons.Filled.Delete, "Eliminar Componente")
                }
            }
            OutlinedTextField(
                value = component.nombreEspecifico ?: "",
                onValueChange = onNameChange,
                label = { Text("Nombre del Componente") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingDuringSave,
                singleLine = true
            )
            Spacer(Modifier.height(4.dp))
            Row {
                OutlinedTextField(
                    value = component.repeticiones ?: "",
                    onValueChange = onRepsChange,
                    label = { Text("Reps") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = component.duracionSegundos?.toString() ?: "",
                    onValueChange = onDurationChange,
                    label = { Text("Duración (s)") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingDuringSave,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
    }
}

