package com.jcmateus.kalisfit.viewmodel

import android.net.Uri
import android.util.Log
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

    init { loadInitialData() }

    private fun loadInitialData() {
        if (userId.isNullOrBlank()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                when {
                    !customId.isNullOrBlank() -> {
                        val routine = getUserCustomRoutineById(userId, customId)
                        routine?.let { r ->
                            _uiState.update { it.copy(
                                routineToEdit = r, isNewRoutine = false,
                                currentCoverImageUrl = r.imagenUrl,
                                editableNivelRutina = r.nivelRecomendado,
                                editableObjetivosRutina = r.objetivos,
                                editableLugarEntrenamientoRutina = r.lugarEntrenamiento.map { it.name },
                                isLoading = false
                            ) }
                        }
                    }
                    !templateId.isNullOrBlank() -> {
                        val template = getRutinaByIdFromFirestore(templateId)
                        template?.let { t ->
                            _uiState.update { it.copy(
                                routineToEdit = createCustomClone(t), isNewRoutine = true,
                                currentCoverImageUrl = t.imagenUrl,
                                editableNivelRutina = t.nivelRecomendado,
                                editableObjetivosRutina = t.objetivos,
                                editableLugarEntrenamientoRutina = t.lugarEntrenamiento.map { it.name },
                                isLoading = false
                            ) }
                        }
                    }
                    else -> {
                        val newR = UserCustomRoutine(id = UUID.randomUUID().toString(), userId = userId, nombrePersonalizado = "Mi Rutina")
                        _uiState.update { it.copy(routineToEdit = newR, isNewRoutine = true, isLoading = false) }
                    }
                }
            } catch (e: Exception) { _uiState.update { it.copy(isLoading = false) } }
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

    fun onExerciseSeriesChanged(idx: Int, s: String) = updateEx(idx) { it.copy(numeroDeSeries = s.toIntOrNull() ?: 1) }
    fun onExerciseSimpleRepsChanged(idx: Int, r: String) = updateEx(idx) { it.copy(repeticionesOriginal = r) }
    fun onExerciseSimpleDurationChanged(idx: Int, d: String) = updateEx(idx) { it.copy(duracionSegundosOriginal = d.toIntOrNull() ?: 0) }
    fun onExerciseRestBetweenSeriesChanged(idx: Int, r: String) = updateEx(idx) { it.copy(descansoEntreSeriesSegundos = r.toIntOrNull() ?: 0) }
    fun onExercisePostRestChanged(idx: Int, r: String) = updateEx(idx) { it.copy(descansoDespuesEjercicioSegundos = r.toIntOrNull() ?: 0) }
    fun onExerciseTempoChanged(idx: Int, t: String) = updateEx(idx) { it.copy(notaTempo = t) }
    fun onExerciseIsUnilateralChanged(idx: Int, u: Boolean) = updateEx(idx) { it.copy(esUnilateral = u) }

    private fun updateEx(idx: Int, block: (Ejercicio) -> Ejercicio) {
        _uiState.update { s ->
            val list = s.routineToEdit?.ejercicios?.toMutableList() ?: return@update s
            if (idx in list.indices) list[idx] = block(list[idx])
            s.copy(routineToEdit = s.routineToEdit?.copy(ejercicios = list))
        }
    }

    fun addOrReplaceExerciseFromSelection(exerciseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val doc = firestore.collection("ejercicios_todos").document(exerciseId).get().await()
                val ef = doc.toObject(EjercicioFirestore::class.java)?.apply { id = doc.id }
                ef?.let {
                    val grupos = it.grupoMuscular.mapNotNull { g -> try { GrupoMuscular.valueOf(g.uppercase()) } catch(e:Exception) { null } }
                    val lugares = it.lugarEntrenamiento.mapNotNull { l -> try { LugarEntrenamiento.valueOf(l.uppercase()) } catch(e:Exception) { null } }
                    val base = parsearEjercicioFirestore(it, grupos, lugares)
                    
                    _uiState.update { state ->
                        val list = state.routineToEdit?.ejercicios?.toMutableList() ?: mutableListOf()
                        val newEx = base.copy(id = UUID.randomUUID().toString())
                        
                        targetExerciseIndexForSelection?.let { idx ->
                            if (idx in list.indices) list[idx] = newEx
                            else list.add(newEx)
                        } ?: list.add(newEx)
                        
                        state.copy(routineToEdit = state.routineToEdit?.copy(ejercicios = list), isLoading = false)
                    }
                }
            } catch (e: Exception) { setError("Error al añadir") }
            targetExerciseIndexForSelection = null
        }
    }

    fun saveRoutine() {
        val routine = _uiState.value.routineToEdit ?: return
        val uid = userId ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val toSave = routine.copy(
                    nivelRecomendado = _uiState.value.editableNivelRutina,
                    objetivos = _uiState.value.editableObjetivosRutina,
                    lugarEntrenamiento = _uiState.value.editableLugarEntrenamientoRutina.map { LugarEntrenamiento.valueOf(it) },
                    fechaUltimaModificacion = Timestamp.now()
                )
                firestore.collection("users").document(uid).collection("customRoutines").document(toSave.id).set(toSave).await()
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            } catch (e: Exception) { setError("Error al guardar: ${e.message}") }
        }
    }

    fun onRoutineNameChanged(n: String) = _uiState.update { it.copy(routineToEdit = it.routineToEdit?.copy(nombrePersonalizado = n)) }
    fun onDescriptionChanged(d: String) = _uiState.update { it.copy(routineToEdit = it.routineToEdit?.copy(descripcion = d)) }
    fun onRoundsChanged(r: String) = _uiState.update { it.copy(routineToEdit = it.routineToEdit?.copy(numeroDeRondas = r.toIntOrNull() ?: 1)) }
    fun onRestBetweenRoundsChanged(r: String) = _uiState.update { it.copy(routineToEdit = it.routineToEdit?.copy(descansoEntreRondasSegundos = r.toIntOrNull() ?: 0)) }
    fun onNivelRutinaChanged(n: String, s: Boolean) = _uiState.update { it.copy(editableNivelRutina = if(s) (it.editableNivelRutina + n).distinct() else it.editableNivelRutina - n) }
    fun onObjetivoRutinaChanged(o: String, s: Boolean) = _uiState.update { it.copy(editableObjetivosRutina = if(s) (it.editableObjetivosRutina + o).distinct() else it.editableObjetivosRutina - o) }
    fun onLugarEntrenamientoRutinaChanged(l: String, s: Boolean) = _uiState.update { it.copy(editableLugarEntrenamientoRutina = if(s) (it.editableLugarEntrenamientoRutina + l).distinct() else it.editableLugarEntrenamientoRutina - l) }
    
    fun onRemoveExercise(i: Int) = updateExList { it.removeAt(i) }
    fun onMoveExerciseUp(i: Int) = moveEx(i, i - 1)
    fun onMoveExerciseDown(i: Int) = moveEx(i, i + 1)

    private fun moveEx(from: Int, to: Int) = updateExList { 
        if (from in it.indices && to in it.indices) { val item = it.removeAt(from); it.add(to, item) }
    }

    private fun updateExList(block: (MutableList<Ejercicio>) -> Unit) {
        _uiState.update { s ->
            val list = s.routineToEdit?.ejercicios?.toMutableList() ?: return@update s
            block(list)
            s.copy(routineToEdit = s.routineToEdit?.copy(ejercicios = list))
        }
    }

    fun prepareForExerciseSelection(idx: Int?) { targetExerciseIndexForSelection = idx }
    fun onSaveHandled() { _uiState.update { it.copy(saveSuccess = false) } }
    private fun setError(m: String) = _uiState.update { it.copy(isLoading = false, errorMessages = listOf(m)) }
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
