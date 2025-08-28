package com.jcmateus.kalisfit.viewmodel


import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.getUserCustomRoutineById
import com.jcmateus.kalisfit.data.parsearEjercicioFirestore
import com.jcmateus.kalisfit.data.saveOrUpdateUserCustomRoutine
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.io.path.exists

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
    val isUploadingCoverImage: Boolean = false,
    val editableNivelRutina: List<String> = emptyList(),
    val editableObjetivosRutina: List<String> = emptyList(),
    val editableLugarEntrenamientoRutina: List<String> = emptyList()
    // No hay picker global de ejercicios en esta fase
)
class EditRoutineViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG = "EditRoutineViewModel_Phase1"
    private val _uiState = MutableStateFlow(EditRoutineUiState())
    val uiState: StateFlow<EditRoutineUiState> = _uiState.asStateFlow()
    private var targetExerciseIndexForSelection: Int? = null
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
    fun prepareForExerciseSelection(index: Int?) {
        targetExerciseIndexForSelection = index
        Log.d(TAG, "Preparado para seleccionar ejercicio. Índice a reemplazar: $index")
    }
    private fun loadInitialRoutine() {
        // Resetear los estados relacionados con la imagen al iniciar la carga
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            selectedCoverImageUri = null, // Limpiar cualquier URI seleccionado previamente
            currentCoverImageUrl = null,   // Limpiar la URL actual antes de cargar la nueva
            editableNivelRutina = emptyList(), // Resetear
            editableObjetivosRutina = emptyList(), // Resetear
            editableLugarEntrenamientoRutina = emptyList()
        )
        viewModelScope.launch {
            try {
                when {
                    // CASO 1: Editando una rutina personalizada existente directamente
                    customRoutineIdArg != null && !currentUserIdArg.isNullOrBlank() -> {
                        Log.d(TAG, "Cargando rutina personalizada EXISTENTE para edición. customId: $customRoutineIdArg, userId: $currentUserIdArg")
                        val existingCustomRoutine: UserCustomRoutine? =
                            getUserCustomRoutineById(currentUserIdArg, customRoutineIdArg) // Asumo que esta función existe y es suspend
                        if (existingCustomRoutine != null) {
                            _uiState.update {
                                it.copy(
                                    routineToEdit = existingCustomRoutine,
                                    isNewRoutine = false,
                                    originalTemplateId = existingCustomRoutine.originalTemplateId,
                                    currentCoverImageUrl = existingCustomRoutine.imagenUrl,
                                    editableNivelRutina = existingCustomRoutine.nivelRecomendado,
                                    editableObjetivosRutina = existingCustomRoutine.objetivos,
                                    editableLugarEntrenamientoRutina = existingCustomRoutine.lugarEntrenamiento.map { lugarEnum -> lugarEnum.name },
                                    isLoading = false,
                                    errorMessages = emptyList()
                                )
                            }
                        } else {
                            Log.w(TAG, "Rutina personalizada (customId: $customRoutineIdArg) no encontrada para el usuario $currentUserIdArg.")
                            _uiState.update { it.copy(errorMessages = listOf("Rutina personalizada no encontrada."), isLoading = false) }
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
                            imagenUrl = null, // Sin imagen de portada por defecto
                            ejercicios = emptyList(),
                            numeroDeRondas = 3,
                            descansoEntreRondasSegundos = 60,
                            // Los campos de lista se inicializan vacíos por defecto en UserCustomRoutine
                            // Así que nivelRecomendado, objetivos, lugarEntrenamiento en blankRoutine serán emptyList()
                            fechaCreacion = Timestamp.now(),
                            fechaUltimaModificacion = Timestamp.now()
                        )
                        _uiState.update {
                            it.copy(
                                routineToEdit = blankRoutine,
                                isNewRoutine = true,
                                currentCoverImageUrl = null, // Sin imagen de portada inicial
                                // --- MODIFICACIÓN: Poblar con los valores (vacíos) de la rutina en blanco ---
                                editableNivelRutina = blankRoutine.nivelRecomendado,      // Será emptyList()
                                editableObjetivosRutina = blankRoutine.objetivos,        // Será emptyList()
                                editableLugarEntrenamientoRutina = blankRoutine.lugarEntrenamiento.map { lugarEnum -> lugarEnum.name }, // Será emptyList()
                                isLoading = false,
                                errorMessages = emptyList()
                            )
                        }
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
    fun onNivelRutinaChanged(nivel: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val currentNiveles = currentState.editableNivelRutina.toMutableList()
            if (isSelected) {
                if (!currentNiveles.contains(nivel)) currentNiveles.add(nivel)
            } else {
                currentNiveles.remove(nivel)
            }
            currentState.copy(editableNivelRutina = currentNiveles.toList())
        }
    }

    fun onObjetivoRutinaChanged(objetivo: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val currentObjetivos = currentState.editableObjetivosRutina.toMutableList()
            if (isSelected) {
                if (!currentObjetivos.contains(objetivo)) currentObjetivos.add(objetivo)
            } else {
                currentObjetivos.remove(objetivo)
            }
            currentState.copy(editableObjetivosRutina = currentObjetivos.toList())
        }
    }

    // Asumiendo que guardas los NOMBRES de los enums de LugarEntrenamiento como Strings
    fun onLugarEntrenamientoRutinaChanged(lugarNombre: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val currentLugares = currentState.editableLugarEntrenamientoRutina.toMutableList()
            if (isSelected) {
                if (!currentLugares.contains(lugarNombre)) currentLugares.add(lugarNombre)
            } else {
                currentLugares.remove(lugarNombre)
            }
            currentState.copy(editableLugarEntrenamientoRutina = currentLugares.toList())
        }
    }
    // --- Funciones para gestionar la lista de ejercicios en la rutina ---
    fun onRemoveExercise(exerciseIndex: Int) {
        _uiState.value.routineToEdit?.let { currentRoutine -> // Obtiene la rutina actual del estado
            if (exerciseIndex >= 0 && exerciseIndex < currentRoutine.ejercicios.size) { // Comprueba límites
                val updatedExercises = currentRoutine.ejercicios.toMutableList() // 1. Crea una COPIA MUTABLE de la lista de ejercicios
                    .apply { removeAt(exerciseIndex) } // 2. Elimina el ejercicio de ESTA COPIA
                    .mapIndexed { index, ej -> ej.copy(orden = index.toDouble()) } // 3. Reasigna 'orden' en una NUEVA LISTA (inmutable por defecto de .mapIndexed)

                // 4. Actualiza el estado:
                //    - Se crea una NUEVA instancia de 'routineToEdit' usando .copy()
                //    - A esta nueva instancia se le asigna la 'updatedExercises' (que es una nueva lista)
                _uiState.value = _uiState.value.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises))
                Log.d(TAG, "Ejercicio en índice $exerciseIndex eliminado. Nueva lista de ejercicios: ${updatedExercises.joinToString { it.nombre }}") // Añadido Log
            } else {
                Log.w(TAG, "Índice de ejercicio inválido para eliminar: $exerciseIndex. Tamaño de la lista: ${currentRoutine.ejercicios.size}") // Añadido Log de advertencia
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
        val uiStateValue = _uiState.value // Capturar el valor actual para consistencia en esta función
        val routineToProcess = uiStateValue.routineToEdit ?: run {
            _uiState.update { it.copy(errorMessages = listOf("No hay rutina para guardar.")) }
            return
        }
        val userId = currentUserIdArg
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessages = listOf("Error: Usuario no identificado para guardar.")) }
            return
        }
        if (routineToProcess.nombrePersonalizado.isBlank()) {
            _uiState.update { it.copy(errorMessages = listOf("El nombre de la rutina no puede estar vacío.")) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessages = emptyList(), saveSuccess = false) }
        viewModelScope.launch {
            try {
                var finalImageUrl = uiStateValue.currentCoverImageUrl // URL de imagen existente o heredada
                val newImageUri = uiStateValue.selectedCoverImageUri
                // 1. Subir nueva imagen de portada si se seleccionó una
                if (newImageUri != null) {
                    _uiState.update { it.copy(isUploadingCoverImage = true) }
                    Log.d(TAG, "Subiendo nueva imagen de portada desde URI: $newImageUri")
                    val imageFileName = "rutina_portada_${UUID.randomUUID()}.jpg"
                    val storageRefPath = "rutinas_portadas/$userId/$imageFileName" // Usar userId verificado
                    val storageRef = firebaseStorage.reference.child(storageRefPath)

                    storageRef.putFile(newImageUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                    Log.d(TAG, "Imagen subida exitosamente. URL: $finalImageUrl")
                    // Actualizar el estado de la UI con la nueva URL y limpiar la URI seleccionada
                    _uiState.update { it.copy(isUploadingCoverImage = false, currentCoverImageUrl = finalImageUrl, selectedCoverImageUri = null) }
                }
                // 2. Construir el objeto UserCustomRoutine actualizado
                // Los campos como nombre, descripción, ejercicios, etc., ya deberían estar actualizados en
                // uiStateValue.routineToEdit a través de sus respectivos callbacks.
                val finalRoutineToSave = routineToProcess.copy(
                    imagenUrl = finalImageUrl,
                    // --- MODIFICACIÓN: Usar los valores editables del UiState ---
                    nivelRecomendado = uiStateValue.editableNivelRutina,
                    objetivos = uiStateValue.editableObjetivosRutina,
                    lugarEntrenamiento = uiStateValue.editableLugarEntrenamientoRutina.mapNotNull { lugarNombre ->
                        try {
                            LugarEntrenamiento.valueOf(lugarNombre.uppercase()) // Convierte el String (nombre del enum) al objeto Enum
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Lugar de entrenamiento inválido '$lugarNombre' encontrado durante el guardado. Será omitido.")
                            null // Omite los valores que no se puedan convertir
                        }
                    },
                    fechaUltimaModificacion = Timestamp.now(),
                    userId = userId // Asegurar que el userId esté
                )
                Log.d(TAG, "Guardando rutina: ID=${finalRoutineToSave.id}, Nombre=${finalRoutineToSave.nombrePersonalizado}, ImagenURL=${finalRoutineToSave.imagenUrl}")
                Log.d(TAG, "Niveles: ${finalRoutineToSave.nivelRecomendado}, Objetivos: ${finalRoutineToSave.objetivos}, Lugares: ${finalRoutineToSave.lugarEntrenamiento}")

                // 3. Guardar en Firestore
                // Asumo que tienes una función como la que usabas antes: saveOrUpdateUserCustomRoutine(userId, finalRoutineToSave)
                // O directamente:
                val routineDocRef = firestore.collection("users").document(userId)
                    .collection("customRoutines").document(finalRoutineToSave.id) // Asegúrate que el ID sea el correcto
                routineDocRef.set(finalRoutineToSave).await()
                _uiState.update { it.copy(isLoading = false, saveSuccess = true, routineToEdit = finalRoutineToSave /* Actualizar con la rutina guardada */) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar rutina", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUploadingCoverImage = false,
                        errorMessages = listOf("Error al guardar la rutina: ${e.message ?: "Error desconocido"}"),
                        saveSuccess = false
                    )
                }
            }
        }
    }
    fun clearErrorMessages() {
        _uiState.value = _uiState.value.copy(errorMessages = emptyList())
    }
    fun onSaveHandled() {
        _uiState.value = _uiState.value.copy(saveSuccess = false) // Resetea el flag de éxito
    }
    fun addOrReplaceExerciseFromSelection(selectedExerciseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Opcional: indicar carga
            Log.d(TAG, "Intentando añadir/reemplazar desde selección. ID: $selectedExerciseId, Índice objetivo: $targetExerciseIndexForSelection")

            // 1. Obtener el Ejercicio base completo (NECESITAS IMPLEMENTAR ESTO CORRECTAMENTE)
            // Esto es un placeholder, debes tener una fuente real para tus ejercicios base.
            val baseExerciseDetails: Ejercicio? = fetchBaseExerciseDetails(selectedExerciseId)

            if (baseExerciseDetails == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessages = it.errorMessages + "Ejercicio seleccionado no encontrado (ID: $selectedExerciseId)."
                    )
                }
                targetExerciseIndexForSelection = null // Resetear
                return@launch
            }

            _uiState.update { currentState ->
                val currentRoutine = currentState.routineToEdit ?: return@update currentState.copy(isLoading = false, errorMessages = currentState.errorMessages + "Error: Rutina no disponible para editar.")

                // 2. Crear una nueva instancia de Ejercicio para esta rutina
                val newExerciseInstanceForRoutine = baseExerciseDetails.copy(
                    id = UUID.randomUUID().toString(), // NUEVO ID para la instancia en la rutina
                    nombre = baseExerciseDetails.nombre, // Mantener el nombre original del ejercicio base
                    // Aquí decides qué campos deben ser personalizables por rutina y cuáles vienen del base.
                    // Por ejemplo, las series y reps podrían ser por defecto del base, pero luego editables.
                    numeroDeSeries = baseExerciseDetails.numeroDeSeries.takeIf { it > 0 } ?: 1, // Ej: default a 1 si el base no tiene
                    repeticionesOriginal = baseExerciseDetails.repeticionesOriginal.ifBlank { "10" }, // Ej: default a "10"
                    duracionSegundosOriginal = baseExerciseDetails.duracionSegundosOriginal,
                    descansoEntreSeriesSegundos = baseExerciseDetails.descansoEntreSeriesSegundos.takeIf { it > 0 } ?: 30,
                    notaTempo = baseExerciseDetails.notaTempo,
                    esUnilateral = baseExerciseDetails.esUnilateral,
                    tipoEjercicio = baseExerciseDetails.tipoEjercicio, // Heredar tipo
                    componentes = baseExerciseDetails.componentes.map { it.copy(id = UUID.randomUUID().toString()) }, // Copia profunda de componentes con nuevos IDs si aplica
                    orden = 0.0 // Se reasignará después
                    // imagenUrl y videoUrl se heredan del baseExerciseDetails
                )

                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                val targetIndex = targetExerciseIndexForSelection

                if (targetIndex != null && targetIndex >= 0 && targetIndex < updatedExercises.size) {
                    // REEMPLAZAR
                    Log.d(TAG, "Reemplazando ejercicio en índice: $targetIndex")
                    updatedExercises[targetIndex] = newExerciseInstanceForRoutine
                } else {
                    // AÑADIR NUEVO
                    Log.d(TAG, "Añadiendo nuevo ejercicio a la rutina")
                    updatedExercises.add(newExerciseInstanceForRoutine)
                }

                // 3. Reasignar el campo 'orden' para asegurar consistencia
                val reorderedExercises = updatedExercises.mapIndexed { index, ej -> ej.copy(orden = index.toDouble()) }

                currentState.copy(
                    routineToEdit = currentRoutine.copy(ejercicios = reorderedExercises),
                    isLoading = false,
                    errorMessages = currentState.errorMessages.filterNot { it.startsWith("Ejercicio seleccionado no encontrado") }
                )
            }
            targetExerciseIndexForSelection = null // Resetear después de usarlo
        }
    }
    private suspend fun fetchBaseExerciseDetails(exerciseId: String): Ejercicio? {
        val db = FirebaseFirestore.getInstance() // O accede a través de tu capa de datos
        Log.d(TAG, "Fetching base exercise details for ID: $exerciseId from Firestore 'ejercicios_todos'")
        try {
            val exerciseDocumentSnapshot = db.collection("ejercicios_todos") // <--- USA TU COLECCIÓN
                .document(exerciseId)
                .get()
                .await()

            if (exerciseDocumentSnapshot.exists()) {
                // Mapear el documento de Firestore a tu data class EjercicioFirestore
                val ejercicioFirestore = exerciseDocumentSnapshot.toObject(com.jcmateus.kalisfit.data.EjercicioFirestore::class.java)

                if (ejercicioFirestore != null) {
                    // Es buena práctica asegurarse de que el ID del documento se asigne al campo 'id'
                    // de tu objeto, especialmente si 'id' en EjercicioFirestore no es var o si
                    // tu documento tiene un campo 'id' separado (lo cual no parece ser tu caso aquí,
                    // ya que el ID es el del documento).
                    // Si 'id' en EjercicioFirestore es 'var', puedes hacer:
                    // ejercicioFirestore.id = exerciseDocumentSnapshot.id
                    // Si no es var pero el campo 'id' se lee directamente del documento, está bien.
                    // Tu EjercicioFirestore tiene 'var id: String = ""', así que es bueno asignarlo.
                    ejercicioFirestore.id = exerciseDocumentSnapshot.id


                    // Mapeo de List<String> desde Firestore a List<Enum> para tu modelo de app
                    val gruposMuscularesEnum = ejercicioFirestore.grupoMuscular.mapNotNull { str ->
                        try {
                            GrupoMuscular.valueOf(str.trim().uppercase().replace(" ", "_"))
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Grupo muscular desconocido ('$str') en Firestore para ejercicio base ${ejercicioFirestore.id}")
                            null
                        }
                    }

                    val lugaresEntrenamientoEnum = ejercicioFirestore.lugarEntrenamiento.mapNotNull { str: String -> // Especificar tipo para str
                        try {
                            LugarEntrenamiento.valueOf(str.trim().uppercase())
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Lugar de entrenamiento desconocido ('$str') en Firestore para ejercicio base ${ejercicioFirestore.id}")
                            null
                        }
                    }

                    // LLAMADA A TU FUNCIÓN DE PARSEO DE FirestoreUtils.kt
                    // Asegúrate de que esta función es accesible (public y con los imports correctos)
                    return parsearEjercicioFirestore(
                        ef = ejercicioFirestore,
                        gruposMuscularesEnum = gruposMuscularesEnum,
                        lugaresEntrenamientoEnum = lugaresEntrenamientoEnum
                    )

                } else {
                    Log.e(TAG, "Error al mapear documento de 'ejercicios_todos' (ID: $exerciseId) a EjercicioFirestore.")
                    return null
                }
            } else {
                Log.w(TAG, "Ejercicio base con ID '$exerciseId' no encontrado en 'ejercicios_todos'.")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ejercicio base (ID: $exerciseId) desde 'ejercicios_todos'.", e)
            return null
        }
    }
    fun onExerciseNameChanged(exerciseIndex: Int, newName: String) {
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                val oldExercise = updatedExercises[exerciseIndex]
                // Solo actualiza el nombre si sigue siendo un "ejercicio en blanco"
                // o si decides permitir cambiar el nombre de cualquier ejercicio aquí.
                // Si el nombre solo viene del ejercicio base, este campo es solo visual
                // hasta que se elija uno de la biblioteca.
                if (oldExercise.id.isBlank() || oldExercise.nombre == "Nuevo Ejercicio") { // O una lógica más robusta para identificar ejercicios en blanco
                    updatedExercises[exerciseIndex] = oldExercise.copy(nombre = newName)
                    _uiState.update { it.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises)) }
                }
            }
        }
    }
    fun onExercisePostRestChanged(exerciseIndex: Int, newRestString: String) {
        val newRest = newRestString.toIntOrNull()
        _uiState.value.routineToEdit?.let { currentRoutine ->
            if (exerciseIndex in currentRoutine.ejercicios.indices && newRest != null && newRest >= 0) {
                val updatedExercises = currentRoutine.ejercicios.toMutableList()
                updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(descansoDespuesEjercicioSegundos = newRest)
                _uiState.update { it.copy(routineToEdit = currentRoutine.copy(ejercicios = updatedExercises)) }
            }
        }
    }
}