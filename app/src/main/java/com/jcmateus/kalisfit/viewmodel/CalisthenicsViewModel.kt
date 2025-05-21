package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.getAllCalisthenicsProgressions
import com.jcmateus.kalisfit.data.getCalisthenicsExerciseLevel
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.Progression
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalisthenicsViewModel(application: Application) : AndroidViewModel(application) {

    private val _progressions = MutableStateFlow<List<Progression>>(emptyList())
    val progressions: StateFlow<List<Progression>> = _progressions.asStateFlow()

    private val _expandedProgressionId = MutableStateFlow<String?>(null)
    val expandedProgressionId: StateFlow<String?> = _expandedProgressionId.asStateFlow()

    // Estado para los detalles del nivel de ejercicio (usa tu clase de modelo de UI)
    private val _exerciseLevelDetails = MutableStateFlow<ExerciseLevel?>(null)
    val exerciseLevelDetails: StateFlow<ExerciseLevel?> = _exerciseLevelDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchCalisthenicsProgressions()
    }

    fun fetchCalisthenicsProgressions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _progressions.value = getAllCalisthenicsProgressions()
                if (_progressions.value.isEmpty()) {
                    Log.d("CalisthenicsVM", "No calisthenics progressions found in Firestore.")
                }
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading progressions: ${e.message}", e)
                _error.value = "Error loading progressions: ${e.localizedMessage}"
                _progressions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExerciseLevelDetails(progressionId: String, levelId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _exerciseLevelDetails.value = null
            try {
                val fetchedDetails = getCalisthenicsExerciseLevel(progressionId, levelId)
                if (fetchedDetails != null) {
                    _exerciseLevelDetails.value = fetchedDetails
                } else {
                    Log.w("CalisthenicsVM", "No details found for level $progressionId - $levelId.")
                    _error.value = "Details not found for this level."
                    // Opcional: _exerciseLevelDetails.value = ExerciseLevel(id = levelId, name = "Not Found"...)
                }
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading level details: ${e.message}", e)
                _error.value = "Error loading level details: ${e.localizedMessage}"
                _exerciseLevelDetails.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onProgressionHeaderClick(progressionId: String) {
        _expandedProgressionId.value = if (_expandedProgressionId.value == progressionId) null else progressionId
    }

    fun clearError() {
        _error.value = null
    }

    fun clearExerciseLevelDetails() {
        _exerciseLevelDetails.value = null
        // _error.value = null // Descomenta si también quieres limpiar el error aquí
    }
}