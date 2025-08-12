package com.jcmateus.kalisfit.viewmodel


import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.getUserCustomRoutineById
import com.jcmateus.kalisfit.data.saveOrUpdateUserCustomRoutine
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

object EditRoutineArgs {
    const val TEMPLATE_ID_ARG = "templateId"
    const val CUSTOM_ROUTINE_ID_ARG = "customRoutineId"
    const val USER_ID_ARG = "userId"
}

data class EditRoutineUiState(
    val isLoading: Boolean = false,
    val routineToEdit: UserCustomRoutine? = null,
    val originalTemplateId: String? = null,
    val isNewRoutine: Boolean = true,
    val errorMessages: List<String> = emptyList(),
    val saveSuccess: Boolean = false,
    val selectedCoverImageUri: Uri? = null, // URI de la imagen seleccionada localmente
    val currentCoverImageUrl: String? = null, // URL de la imagen de portada actual (si existe)
    val isUploadingCoverImage: Boolean = false
    // No hay picker global de ejercicios en esta fase
)

class EditRoutineViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val TAG = "EditRoutineViewModel_Phase1"

    private val _uiState = MutableStateFlow(EditRoutineUiState())
    val uiState: StateFlow<EditRoutineUiState> = _uiState.asStateFlow()

    private val templateRoutineIdArg: String? = savedStateHandle[EditRoutineArgs.TEMPLATE_ID_ARG]
    private val customRoutineIdArg: String? = savedStateHandle[EditRoutineArgs.CUSTOM_ROUTINE_ID_ARG]
    private val currentUserIdArg: String? = savedStateHandle[EditRoutineArgs.USER_ID_ARG]

    init {
        if (currentUserIdArg.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessages = listOf("Error: Usuario no identificado."),
                isLoading = false
            )
        } else {
            // En esta Fase 1, no cargamos una lista global de ejercicios base.
            loadInitialRoutine()
        }
    }
    private fun loadInitialRoutine() {
        // Resetear los estados relacionados con la imagen al iniciar la carga
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            selectedCoverImageUri = null, // Limpiar cualquier URI seleccionado previamente
            currentCoverImageUrl = null   // Limpiar la URL actual antes de cargar la nueva
        )
        viewModelScope.launch {
            try {
                when {
                    // CASO 1: Editando una rutina personalizada existente directamente
                    customRoutineIdArg != null && !currentUserIdArg.isNullOrBlank() -> {
                        Log.d(TAG, "Cargando rutina personalizada EXISTENTE para edición. customId: $customRoutineIdArg, userId: $currentUserIdArg")
                        val existingCustomRoutine: UserCustomRoutine? =
                            getUserCustomRoutineById(currentUserIdArg, customRoutineIdArg)
                        if (existingCustomRoutine != null) {
                            _uiState.value = _uiState.value.copy(
                                routineToEdit = existingCustomRoutine,
                                isNewRoutine = false,
                                originalTemplateId = existingCustomRoutine.originalTemplateId,
                                currentCoverImageUrl = existingCustomRoutine.imagenUrl, // Correcto: cargar la imagen existente
                                isLoading = false
                            )
                        } else {
                            Log.w(TAG, "Rutina personalizada (customId: $customRoutineIdArg) no encontrada para el usuario $currentUserIdArg.")
                            _uiState.value = _uiState.value.copy(errorMessages = listOf("Rutina personalizada no encontrada."), isLoading = false)
                        }
                    }
                    // CASO 2: Creando una NUEVA rutina, posiblemente basada en una plantilla o en una copia de otra UserCustomRoutine
                    templateRoutineIdArg != null && !currentUserIdArg.isNullOrBlank() -> {
                        Log.d(TAG, "Creando NUEVA rutina basada en templateId: $templateRoutineIdArg, para userId: $currentUserIdArg")
                        var baseForNewRoutine: Rutina? = null
                        var sourceOriginalTemplateId: String? = templateRoutineIdArg

                        val globalTemplate: Rutina? = getRutinaByIdFromFirestore(templateRoutineIdArg)

                        if (globalTemplate != null) {
                            Log.d(TAG, "templateId '$templateRoutineIdArg' encontrado como PLANTILLA GLOBAL: ${globalTemplate.nombre}")
                            baseForNewRoutine = globalTemplate
                        } else {
                            val baseUserCustomRoutine: UserCustomRoutine? = getUserCustomRoutineById(currentUserIdArg, templateRoutineIdArg)
                            if (baseUserCustomRoutine != null) {
                                Log.d(TAG, "templateId '$templateRoutineIdArg' encontrado como USER CUSTOM ROUTINE: ${baseUserCustomRoutine.nombrePersonalizado}. Se usará para COPIA.")
                                baseForNewRoutine = Rutina(
                                    id = baseUserCustomRoutine.id,
                                    nombre = baseUserCustomRoutine.nombrePersonalizado,
                                    descripcion = baseUserCustomRoutine.descripcion,
                                    imagenUrl = baseUserCustomRoutine.imagenUrl, // Heredar imagen de la rutina base
                                    ejercicios = baseUserCustomRoutine.ejercicios,
                                    numeroDeRondas = baseUserCustomRoutine.numeroDeRondas,
                                    descansoEntreRondasSegundos = baseUserCustomRoutine.descansoEntreRondasSegundos,
                                    nivelRecomendado = baseUserCustomRoutine.nivelRecomendado.toList(),
                                    objetivos = baseUserCustomRoutine.objetivos.toList(),
                                    lugarEntrenamiento = baseUserCustomRoutine.lugarEntrenamiento.map { it }
                                )
                                sourceOriginalTemplateId = baseUserCustomRoutine.originalTemplateId ?: baseUserCustomRoutine.id
                            } else {
                                Log.w(TAG, "templateId '$templateRoutineIdArg' NO encontrado ni como plantilla global ni como UserCustomRoutine del usuario '$currentUserIdArg'.")
                            }
                        }
                        if (baseForNewRoutine != null) {
                            val newCustomRoutine = UserCustomRoutine(
                                id = UUID.randomUUID().toString(),
                                userId = currentUserIdArg,
                                originalTemplateId = sourceOriginalTemplateId,
                                nombrePersonalizado = baseForNewRoutine.nombre,
                                descripcion = baseForNewRoutine.descripcion,
                                imagenUrl = baseForNewRoutine.imagenUrl, // Asignar la imagen heredada
                                ejercicios = baseForNewRoutine.ejercicios.map { ejercicioPlantilla ->
                                    ejercicioPlantilla.copy(
                                        id = UUID.randomUUID().toString(),
                                        componentes = ejercicioPlantilla.componentes.map { it.copy() }
                                    )
                                },
                                numeroDeRondas = baseForNewRoutine.numeroDeRondas,
                                descansoEntreRondasSegundos = baseForNewRoutine.descansoEntreRondasSegundos,
                                nivelRecomendado = baseForNewRoutine.nivelRecomendado.toList(),
                                objetivos = baseForNewRoutine.objetivos.toList(),
                                lugarEntrenamiento = baseForNewRoutine.lugarEntrenamiento.toList(),
                                fechaCreacion = Timestamp.now(),
                                fechaUltimaModificacion = Timestamp.now()
                            )
                            _uiState.value = _uiState.value.copy(
                                routineToEdit = newCustomRoutine,
                                isNewRoutine = true,
                                originalTemplateId = newCustomRoutine.originalTemplateId,
                                currentCoverImageUrl = newCustomRoutine.imagenUrl, // Establecer la imagen heredada
                                isLoading = false
                            )
                        } else {
                            Log.e(TAG, "No se pudo encontrar una base (plantilla o rutina custom) para templateId: $templateRoutineIdArg.")
                            _uiState.value = _uiState.value.copy(errorMessages = listOf("Plantilla o rutina base no encontrada: $templateRoutineIdArg."), isLoading = false)
                        }
                    }
                    // CASO 3: Creando una rutina personalizada totalmente nueva (desde cero)
                    !currentUserIdArg.isNullOrBlank() -> {
                        Log.d(TAG, "Creando rutina personalizada totalmente NUEVA (desde cero) para userId: $currentUserIdArg")
                        val blankRoutine = UserCustomRoutine(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserIdArg,
                            nombrePersonalizado = "Nueva Rutina",
                            descripcion = "",
                            // imagenUrl se deja null por defecto para una rutina en blanco
                            ejercicios = emptyList(),
                            numeroDeRondas = 3,
                            descansoEntreRondasSegundos = 60,
                            fechaCreacion = Timestamp.now(),
                            fechaUltimaModificacion = Timestamp.now()
                        )
                        _uiState.value = _uiState.value.copy(
                            routineToEdit = blankRoutine,
                            isNewRoutine = true,
                            currentCoverImageUrl = null, // Una rutina nueva desde cero no tiene imagen de portada inicial
                            isLoading = false
                        )
                    }
                    else -> {
                        Log.e(TAG, "Error crítico: ID de usuario no disponible o estado de argumentos inesperado.")
                        _uiState.value = _uiState.value.copy(errorMessages = listOf("Error crítico: ID de usuario no disponible."), isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en loadInitialRoutine", e)
                _uiState.value = _uiState.value.copy(errorMessages = listOf("Error al cargar la rutina: ${e.message}"), isLoading = false)
            }
        }
    }
    // --- Funciones de modificación de la Rutina ---
    fun onRoutineNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(
            routineToEdit = _uiState.value.routineToEdit?.copy(nombrePersonalizado = newName)
        )
    }
    fun onDescriptionChanged(newDescription: String) {
        _uiState.value = _uiState.value.copy(
            routineToEdit = _uiState.value.routineToEdit?.copy(descripcion = newDescription)
        )
    }
    fun onRoundsChanged(newRoundsString: String) {
        val newRounds = newRoundsString.toIntOrNull() ?: _uiState.value.routineToEdit?.numeroDeRondas ?: 1
        if (newRounds > 0) {
            _uiState.value = _uiState.value.copy(
                routineToEdit = _uiState.value.routineToEdit?.copy(numeroDeRondas = newRounds)
            )
        }
    }
    fun onRestBetweenRoundsChanged(newRestString: String) {
        val newRest = newRestString.toIntOrNull() ?: _uiState.value.routineToEdit?.descansoEntreRondasSegundos ?: 0
        if (newRest >= 0) {
            _uiState.value = _uiState.value.copy(
                routineToEdit = _uiState.value.routineToEdit?.copy(descansoEntreRondasSegundos = newRest)
            )
        }
    }
    // --- Funciones de modificación de Ejercicio (existente en la rutina) ---
    fun onExerciseSeriesChanged(exerciseIndex: Int, newSeriesString: String) {
        val newSeries = newSeriesString.toIntOrNull()
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices && newSeries != null && newSeries > 0) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(numeroDeSeries = newSeries)
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onExerciseSimpleRepsChanged(exerciseIndex: Int, newReps: String) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(repeticionesOriginal = newReps)
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onExerciseSimpleDurationChanged(exerciseIndex: Int, newDurationString: String) {
        val newDuration = newDurationString.toIntOrNull()
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices && newDuration != null && newDuration >= 0) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(duracionSegundosOriginal = newDuration)
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onExerciseRestBetweenSeriesChanged(exerciseIndex: Int, newRestString: String) {
        val newRest = newRestString.toIntOrNull()
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices && newRest != null && newRest >= 0) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(descansoEntreSeriesSegundos = newRest)
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onExerciseTempoChanged(exerciseIndex: Int, newTempo: String) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(notaTempo = newTempo.ifBlank { null })
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onExerciseIsUnilateralChanged(exerciseIndex: Int, isUnilateral: Boolean) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(esUnilateral = isUnilateral)
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    // --- Funciones de modificación de ComponenteEjercicio ---
    fun onExerciseComponentRepsChanged(exerciseIndex: Int, componentIndex: Int, newReps: String) {
        updateExerciseComponent(exerciseIndex, componentIndex) { it.copy(repeticiones = newReps.ifBlank { null }) }
    }
    fun onExerciseComponentDurationChanged(exerciseIndex: Int, componentIndex: Int, newDurationString: String) {
        val newDuration = newDurationString.toIntOrNull()
        if (newDuration != null && newDuration >= 0) {
            updateExerciseComponent(exerciseIndex, componentIndex) { it.copy(duracionSegundos = newDuration) }
        } else if (newDurationString.isBlank()) {
            updateExerciseComponent(exerciseIndex, componentIndex) { it.copy(duracionSegundos = null) }
        }
    }
    fun onExerciseComponentNameChanged(exerciseIndex: Int, componentIndex: Int, newName: String) {
        updateExerciseComponent(exerciseIndex, componentIndex) { it.copy(nombreEspecifico = newName) }
    }
    fun onAddComponentToExercise(exerciseIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val exerciseToUpdate = currentRoutine.ejercicios[exerciseIndex]
                val newComponent = ComponenteEjercicio(
                    nombreEspecifico = "Nuevo Componente", // Nombre por defecto
                    orden = exerciseToUpdate.componentes.size // Siguiente orden
                )
                val updatedComponents = exerciseToUpdate.componentes.toMutableList().apply { add(newComponent) }
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                // Si añadir un componente cambia el tipo de ejercicio, se debería actualizar aquí.
                // Por ahora, el tipo de ejercicio se mantiene o se infiere en la UI/modelo.
                updatedExercises[exerciseIndex] = exerciseToUpdate.copy(
                    componentes = updatedComponents
                )
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onRemoveComponentFromExercise(exerciseIndex: Int, componentIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val exerciseToUpdate = currentRoutine.ejercicios[exerciseIndex]
                if (componentIndex in exerciseToUpdate.componentes.indices) {
                    val updatedComponents = exerciseToUpdate.componentes.toMutableList()
                        .apply { removeAt(componentIndex) }
                        .mapIndexed { index, comp -> comp.copy(orden = index) } // Reordenar
                    val updatedExercises = currentRoutine.ejercicios.toMutableList()
                    updatedExercises[exerciseIndex] = exerciseToUpdate.copy(
                        componentes = updatedComponents
                    )
                    _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
                }
            }
        }
    }

    fun onCoverImageSelected(uri: Uri?) {
        _uiState.update { currentState ->
            currentState.copy(selectedCoverImageUri = uri)
        }
        // Opcionalmente, si seleccionas null, podrías querer resetear algo más
        // o iniciar la subida si el URI no es nulo y tienes auto-subida.
    }
    fun clearSelectedCoverImage() {
        _uiState.update { currentState ->
            currentState.copy(selectedCoverImageUri = null)
        }
    }
    private fun updateExerciseComponent(
        exerciseIndex: Int,
        componentIndex: Int,
        updateAction: (ComponenteEjercicio) -> ComponenteEjercicio
    ) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val exerciseToUpdate = currentRoutine.ejercicios[exerciseIndex]
                if (componentIndex in exerciseToUpdate.componentes.indices) {
                    val updatedComponents = exerciseToUpdate.componentes.toMutableList()
                    updatedComponents[componentIndex] = updateAction(updatedComponents[componentIndex])

                    val updatedExercises = currentRoutine.ejercicios.toMutableList()
                    updatedExercises[exerciseIndex] = exerciseToUpdate.copy(componentes = updatedComponents)
                    _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
                }
            }
        }
    }
    // --- Funciones para gestionar la lista de ejercicios en la rutina ---
    fun onRemoveExercise(exerciseIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                    .apply { removeAt(exerciseIndex) }
                    .mapIndexed { index, ej -> ej.copy(orden = index.toDouble()) } // Reasignar orden
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    /**
     * Añade un ejercicio "en blanco" a la rutina actual.
     * El usuario deberá configurar este ejercicio manualmente.
     * Útil para rutinas creadas desde cero o si el usuario quiere añadir algo no previsto.
     */
    fun onAddNewBlankExercise() {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            val newBlankExercise = Ejercicio(
                id = UUID.randomUUID().toString(),
                nombre = "Nuevo Ejercicio", // El usuario deberá cambiar esto
                descripcion = "", // Por defecto vacío, el usuario puede añadirla
                imagenUrl = null,
                imagenUrl1 = null,
                imagenUrl2 = null,
                videoUrl = null,

                duracionSegundosOriginal = 0, // Por defecto 0, para ejercicios basados en repeticiones
                repeticionesOriginal = "10",  // Por defecto "10" repeticiones

                numeroDeSeries = 1,           // Por defecto 1 serie
                descansoEntreSeriesSegundos = 60, // Por defecto 60 segundos
                descansoDespuesEjercicioSegundos = 0, // Por defecto 0

                grupoMuscular = emptyList(),      // Sin grupo muscular asignado por defecto
                equipamientoNecesario = emptyList(), // Sin equipamiento por defecto
                lugarEntrenamiento = emptyList(),  // Sin lugar de entrenamiento por defecto

                orden = currentRoutine.ejercicios.size.toDouble(), // Se añade al final

                // NUEVOS CAMPOS PROCESADOS/DERIVADOS
                tipoEjercicio = TipoDeEjercicio.SIMPLE, // Por defecto un ejercicio simple
                componentes = emptyList(), // Un ejercicio simple no tiene componentes por defecto
                notaTempo = null,          // Sin nota de tempo por defecto
                esUnilateral = false       // No es unilateral por defecto
            )
            val updatedExercises = currentRoutine.ejercicios.toMutableList().apply { add(newBlankExercise) }
            _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
        }
    }
    /**
     * Duplica un ejercicio existente dentro de la rutina actual.
     * @param exerciseIndex El índice del ejercicio a duplicar.
     */
    fun onDuplicateExercise(exerciseIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val exerciseToDuplicate = currentRoutine.ejercicios[exerciseIndex]
                val duplicatedExercise = exerciseToDuplicate.copy(
                    id = UUID.randomUUID().toString(), // Nuevo ID único para la copia
                    orden = currentRoutine.ejercicios.size.toDouble(), // Se añade al final por defecto
                    componentes = exerciseToDuplicate.componentes.map { it.copy() } // Copia profunda de componentes
                )
                val updatedExercises = currentRoutine.ejercicios.toMutableList().apply { add(duplicatedExercise) }
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
            }
        }
    }
    fun onMoveExerciseUp(exerciseIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex > 0 && exerciseIndex in currentRoutine.ejercicios.indices) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                val exerciseToMove = updatedExercises.removeAt(exerciseIndex)
                updatedExercises.add(exerciseIndex - 1, exerciseToMove)
                // Reasignar el campo 'orden' para todos los ejercicios
                val reorderedExercises = updatedExercises.mapIndexed { index, ej -> ej.copy(orden = index.toDouble()) }
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = reorderedExercises))
            }
        }
    }
    fun onMoveExerciseDown(exerciseIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex >= 0 && exerciseIndex < currentRoutine.ejercicios.size - 1) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                val exerciseToMove = updatedExercises.removeAt(exerciseIndex)
                updatedExercises.add(exerciseIndex + 1, exerciseToMove)
                // Reasignar el campo 'orden' para todos los ejercicios
                val reorderedExercises = updatedExercises.mapIndexed { index, ej -> ej.copy(orden = index.toDouble()) }
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = reorderedExercises))
            }
        }
    }
    // --- Guardado de la Rutina ---
    fun saveRoutine() {
        val routineToSave = _uiState.value.routineToEdit ?: return
        val userId = currentUserIdArg
        if (userId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessages = listOf("Error: Usuario no identificado para guardar."))
            return
        }

        if (routineToSave.nombrePersonalizado.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessages = listOf("El nombre de la rutina no puede estar vacío."))
            return
        }
        // Considera si una rutina vacía es válida o si debe tener al menos un ejercicio
        // if (routineToSave.ejercicios.isEmpty() && !routineToSave.isNewRoutine) { // Ejemplo de validación
        //    _uiState.value = _uiState.value.copy(errorMessages = listOf("La rutina debe tener al menos un ejercicio."))
        //    return
        // }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessages = emptyList())
        viewModelScope.launch {
            try {
                // Asegurar que la fecha de modificación se actualiza y el userId está presente
                val finalRoutineToSave = routineToSave.copy(
                    fechaUltimaModificacion = Timestamp.now(),
                    userId = userId // Ya debería estar, pero se reasegura
                )
                // Esta función debe existir en tu FirestoreUtils.kt
                // y ser capaz de guardar/actualizar una UserCustomRoutine.
                saveOrUpdateUserCustomRoutine(userId, finalRoutineToSave)

                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)

            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar rutina", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessages = listOf("Error al guardar la rutina: ${e.message}"),
                    saveSuccess = false
                )
            }
        }
    }
    fun clearErrorMessages() {
        _uiState.value = _uiState.value.copy(errorMessages = emptyList())
    }
    fun onSaveHandled() {
        _uiState.value = _uiState.value.copy(saveSuccess = false) // Resetea el flag de éxito
    }
}