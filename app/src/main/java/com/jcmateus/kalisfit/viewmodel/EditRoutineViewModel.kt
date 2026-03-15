package com.jcmateus.kalisfit.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.view.indices
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.data.*
import com.jcmateus.kalisfit.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object EditRoutineArgs {
    const val TEMPLATE_ID_ARG = "templateId"
    const val CUSTOM_ROUTINE_ID_ARG = "customRoutineId"
    const val USER_ID_ARG = "userId"
}

class EditRoutineViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val TAG = "EditRoutineViewModel"

    private val _uiState = MutableStateFlow(EditRoutineUiState())
    val uiState: StateFlow<EditRoutineUiState> = _uiState.asStateFlow()

    private val userId: String? = savedStateHandle[EditRoutineArgs.USER_ID_ARG]
    private val templateId: String? = savedStateHandle[EditRoutineArgs.TEMPLATE_ID_ARG]
    private val customId: String? = savedStateHandle[EditRoutineArgs.CUSTOM_ROUTINE_ID_ARG]

    private var targetExerciseIndexForSelection: Int? = null

    init {
        loadInitialData()
    }

    // Dentro de EditRoutineViewModel.kt

    private fun loadInitialData() {
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessages = listOf("Error: Usuario no identificado")) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                when {
                    !customId.isNullOrBlank() -> {
                        val routine = getUserCustomRoutineById(userId, customId)
                        if (routine != null) {
                            _uiState.update {
                                it.copy(
                                    routineToEdit = routine,
                                    isNewRoutine = false,
                                    currentCoverImageUrl = routine.imagenUrl,
                                    editableNivelRutina = routine.nivelRecomendado,
                                    editableObjetivosRutina = routine.objetivos,
                                    editableLugarEntrenamientoRutina = routine.lugarEntrenamiento.map { it.name },
                                    isLoading = false // <--- IMPORTANTE
                                )
                            }
                        } else {
                            // Si la rutina es null, detenemos el cargando y avisamos
                            setError("No se pudo encontrar la rutina personalizada")
                        }
                    }

                    !templateId.isNullOrBlank() -> {
                        val template = getRutinaByIdFromFirestore(templateId)
                        if (template != null) {
                            _uiState.update {
                                it.copy(
                                    routineToEdit = createCustomClone(template),
                                    isNewRoutine = true,
                                    currentCoverImageUrl = template.imagenUrl,
                                    editableNivelRutina = template.nivelRecomendado,
                                    editableObjetivosRutina = template.objetivos,
                                    editableLugarEntrenamientoRutina = template.lugarEntrenamiento.map { it.name },
                                    isLoading = false
                                )
                            }
                        } else {
                            setError("No se pudo cargar la plantilla de la rutina")
                        }
                    }

                    else -> {
                        // Caso para crear una rutina desde cero
                        val newR = UserCustomRoutine(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            nombrePersonalizado = "Mi Rutina"
                        )
                        _uiState.update { it.copy(routineToEdit = newR, isNewRoutine = true, isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos iniciales: ${e.message}")
                setError("Error al cargar la rutina: ${e.message}")
            }
        }
    }

    private fun createCustomClone(t: Rutina) = UserCustomRoutine(
        id = UUID.randomUUID().toString(),
        userId = userId ?: "",
        nombrePersonalizado = t.nombre,
        descripcion = t.descripcion,
        imagenUrl = t.imagenUrl,
        ejercicios = t.ejercicios.map { it.copy(id = UUID.randomUUID().toString()) },
        numeroDeRondas = t.numeroDeRondas,
        descansoEntreRondasSegundos = t.descansoEntreRondasSegundos,
        nivelRecomendado = t.nivelRecomendado,
        objetivos = t.objetivos,
        lugarEntrenamiento = t.lugarEntrenamiento,
        fechaCreacion = Timestamp.now(),
        fechaUltimaModificacion = Timestamp.now(),
        originalTemplateId = t.id
    )

    fun onExerciseSeriesChanged(idx: Int, s: String) =
        updateEx(idx) { it.copy(numeroDeSeries = s.toIntOrNull() ?: 1) }

    fun onExerciseSimpleRepsChanged(idx: Int, r: String) =
        updateEx(idx) { it.copy(repeticionesOriginal = r) }

    fun onExerciseSimpleDurationChanged(idx: Int, d: String) =
        updateEx(idx) { it.copy(duracionSegundosOriginal = d.toIntOrNull() ?: 0) }

    fun onExerciseRestBetweenSeriesChanged(idx: Int, r: String) =
        updateEx(idx) { it.copy(descansoEntreSeriesSegundos = r.toIntOrNull() ?: 0) }

    fun onExercisePostRestChanged(idx: Int, r: String) =
        updateEx(idx) { it.copy(descansoDespuesEjercicioSegundos = r.toIntOrNull() ?: 0) }

    fun onExerciseTempoChanged(idx: Int, t: String) = updateEx(idx) { it.copy(notaTempo = t) }
    fun onExerciseIsUnilateralChanged(idx: Int, u: Boolean) =
        updateEx(idx) { it.copy(esUnilateral = u) }

    private fun updateEx(idx: Int, block: (Ejercicio) -> Ejercicio) {
        _uiState.update { s ->
            val list = s.routineToEdit?.ejercicios?.toMutableList() ?: return@update s
            if (idx in list.indices) list[idx] = block(list[idx])
            s.copy(routineToEdit = s.routineToEdit?.copy(ejercicios = list))
        }
    }

    private fun updateExerciseItem(idx: Int, block: (Ejercicio) -> Ejercicio) {
        _uiState.update { s ->
            val list = s.routineToEdit?.ejercicios?.toMutableList() ?: return@update s
            if (idx in list.indices) {
                list[idx] = block(list[idx])
            }
            s.copy(routineToEdit = s.routineToEdit?.copy(ejercicios = list))
        }
    }

    fun addOrReplaceExerciseFromSelection(exerciseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val doc =
                    firestore.collection("ejercicios_todos").document(exerciseId).get().await()
                val ef = doc.toObject(EjercicioFirestore::class.java)?.apply { id = doc.id }
                ef?.let {
                    val grupos = it.grupoMuscular.mapNotNull { g ->
                        try {
                            GrupoMuscular.valueOf(g.uppercase())
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val lugares = it.lugarEntrenamiento.mapNotNull { l ->
                        try {
                            LugarEntrenamiento.valueOf(l.uppercase())
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val base = parsearEjercicioFirestore(it, grupos, lugares)

                    _uiState.update { state ->
                        val list =
                            state.routineToEdit?.ejercicios?.toMutableList() ?: mutableListOf()
                        val newEx = base.copy(id = UUID.randomUUID().toString())

                        targetExerciseIndexForSelection?.let { idx ->
                            if (idx in list.indices) list[idx] = newEx
                            else list.add(newEx)
                        } ?: list.add(newEx)

                        state.copy(
                            routineToEdit = state.routineToEdit?.copy(ejercicios = list),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                setError("Error al añadir")
            }
            targetExerciseIndexForSelection = null
        }
    }

    fun saveRoutine() {
        val routine = _uiState.value.routineToEdit ?: return
        val uid = userId ?: return
        val selectedUri = _uiState.value.selectedCoverImageUri
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                var finalImageUrl = routine.imagenUrl

                //1. Si el usuario seleccionó una imagen nueva, subirla a Firebase Storage
                if (selectedUri != null) {
                    val storageRef =
                        storage.reference.child("routine_covers/${uid}/${UUID.randomUUID()}.jpg")
                    val uploadTask = storageRef.putFile(selectedUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }
                //2. Preparar el objeto para guardar con la nueva URL (si aplica)
                val toSave = routine.copy(
                    imagenUrl = finalImageUrl,
                    nivelRecomendado = _uiState.value.editableNivelRutina,
                    objetivos = _uiState.value.editableObjetivosRutina,
                    lugarEntrenamiento = _uiState.value.editableLugarEntrenamientoRutina.map {
                        try {
                            LugarEntrenamiento.valueOf(it)
                        } catch (e: Exception) {
                            LugarEntrenamiento.CASA
                        }
                    },
                    fechaUltimaModificacion = Timestamp.now()
                )
                //3. Guardar en Firestore
                firestore.collection("users").document(uid)
                    .collection("customRoutines")
                    .document(toSave.id)
                    .set(toSave)
                    .await()
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar rutina con imagen ${e.message}")
                setError("Error al guardar: ${e.message}")
            }
        }
    }

    fun onExerciseComponentRepsChanged(exerciseIndex: Int, componentIndex: Int, newReps: String) {
        updateExerciseItem(exerciseIndex) { exercise ->
            val updatedComponents = exercise.componentes.toMutableList()
            if (componentIndex in updatedComponents.indices) {
                updatedComponents[componentIndex] =
                    updatedComponents[componentIndex].copy(repeticiones = newReps)
            }
            exercise.copy(componentes = updatedComponents)
        }
    }

    fun onCoverImageSelected(uri: Uri?) {
        _uiState.update { currentState ->
            currentState.copy(selectedCoverImageUri = uri)
        }
    }

    fun onExerciseComponentDurationChanged(
        exerciseIndex: Int,
        componentIndex: Int,
        newDuration: String
    ) {
        val duration = newDuration.toIntOrNull() ?: 0
        updateExerciseItem(exerciseIndex) { exercise ->
            val updatedComponents = exercise.componentes.toMutableList()
            if (componentIndex in updatedComponents.indices) {
                updatedComponents[componentIndex] =
                    updatedComponents[componentIndex].copy(duracionSegundos = duration)
            }
            exercise.copy(componentes = updatedComponents)
        }
    }

    fun onRoutineNameChanged(n: String) =
        _uiState.update { it.copy(routineToEdit = it.routineToEdit?.copy(nombrePersonalizado = n)) }

    fun onDescriptionChanged(d: String) =
        _uiState.update { it.copy(routineToEdit = it.routineToEdit?.copy(descripcion = d)) }

    fun onRoundsChanged(r: String) = _uiState.update {
        it.copy(
            routineToEdit = it.routineToEdit?.copy(
                numeroDeRondas = r.toIntOrNull() ?: 1
            )
        )
    }

    fun onRestBetweenRoundsChanged(r: String) = _uiState.update {
        it.copy(
            routineToEdit = it.routineToEdit?.copy(
                descansoEntreRondasSegundos = r.toIntOrNull() ?: 0
            )
        )
    }

    fun onNivelRutinaChanged(n: String, s: Boolean) =
        _uiState.update { it.copy(editableNivelRutina = if (s) (it.editableNivelRutina + n).distinct() else it.editableNivelRutina - n) }

    fun onObjetivoRutinaChanged(o: String, s: Boolean) =
        _uiState.update { it.copy(editableObjetivosRutina = if (s) (it.editableObjetivosRutina + o).distinct() else it.editableObjetivosRutina - o) }

    fun onLugarEntrenamientoRutinaChanged(l: String, s: Boolean) =
        _uiState.update { it.copy(editableLugarEntrenamientoRutina = if (s) (it.editableLugarEntrenamientoRutina + l).distinct() else it.editableLugarEntrenamientoRutina - l) }

    fun onRemoveExercise(i: Int) = updateExList { it.removeAt(i) }
    fun onMoveExerciseUp(i: Int) = moveEx(i, i - 1)
    fun onMoveExerciseDown(i: Int) = moveEx(i, i + 1)

    private fun moveEx(from: Int, to: Int) = updateExList {
        if (from in it.indices && to in it.indices) {
            val item = it.removeAt(from); it.add(to, item)
        }
    }

    private fun updateExList(block: (MutableList<Ejercicio>) -> Unit) {
        _uiState.update { s ->
            val list = s.routineToEdit?.ejercicios?.toMutableList() ?: return@update s
            block(list)
            s.copy(routineToEdit = s.routineToEdit?.copy(ejercicios = list))
        }
    }

    fun prepareForExerciseSelection(idx: Int?) {
        targetExerciseIndexForSelection = idx
    }

    fun onSaveHandled() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
    fun clearError(){
        _uiState.update { it.copy(errorMessages = emptyList())}
    }
    private fun setError(m: String) =
        _uiState.update { it.copy(isLoading = false, errorMessages = listOf(m)) }
}

data class EditRoutineUiState(
    val isLoading: Boolean = false,
    val routineToEdit: UserCustomRoutine? = null,
    val isNewRoutine: Boolean = true,
    val saveSuccess: Boolean = false,
    val errorMessages: List<String> = emptyList(),
    val isUploadingCoverImage: Boolean = false,
    val selectedCoverImageUri: Uri? = null,
    val currentCoverImageUrl: String? = null,
    val editableNivelRutina: List<String> = emptyList(),
    val editableObjetivosRutina: List<String> = emptyList(),
    val editableLugarEntrenamientoRutina: List<String> = emptyList()
)
