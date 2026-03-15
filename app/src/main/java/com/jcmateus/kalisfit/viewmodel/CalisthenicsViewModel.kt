package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.data.getAllCalisthenicsProgressions
import com.jcmateus.kalisfit.data.getCalisthenicsExerciseLevel
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.model.UserProgressionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirestoreCollections {
    const val USER_PROGRESSION_STATES = "userProgressStates"
}

class CalisthenicsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var authListener: FirebaseAuth.AuthStateListener? = null

    private val _progressions = MutableStateFlow<List<Progression>>(emptyList())
    val progressions: StateFlow<List<Progression>> = _progressions.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<String?>(null)
    val selectedDifficulty: StateFlow<String?> = _selectedDifficulty.asStateFlow()

    val filteredProgressions = combine(
        _progressions,
        _selectedCategory,
        _selectedDifficulty
    ) { list, cat, diff ->
        list.filter {
            (cat == null || it.category == cat) &&
            (diff == null || it.difficulty == diff)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedProgressionId = MutableStateFlow<String?>(null)
    val expandedProgressionId: StateFlow<String?> = _expandedProgressionId.asStateFlow()

    private val _exerciseLevelDetails = MutableStateFlow<ExerciseLevel?>(null)
    val exerciseLevelDetails: StateFlow<ExerciseLevel?> = _exerciseLevelDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _nextLevelToNavigate = MutableStateFlow<Pair<String, String>?>(null)
    val nextLevelToNavigate: StateFlow<Pair<String, String>?> = _nextLevelToNavigate.asStateFlow()

    private val _userProgressionStates = MutableStateFlow<Map<String, UserProgressionState>>(emptyMap())
    val userProgressionStates: StateFlow<Map<String, UserProgressionState>> = _userProgressionStates.asStateFlow()

    init {
        fetchCalisthenicsProgressions()
        observeAuthChangesAndLoadProgress()
    }

    private fun observeAuthChangesAndLoadProgress() {
        authListener?.let { auth.removeAuthStateListener(it) }

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadCurrentUserProgressionStates()
            } else {
                _userProgressionStates.value = emptyMap()
                _expandedProgressionId.value = null
            }
        }
        auth.addAuthStateListener(authListener!!)

        if (auth.currentUser != null) {
            loadCurrentUserProgressionStates()
        }
    }

    fun fetchCalisthenicsProgressions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val progressionList = getAllCalisthenicsProgressions()
                _progressions.value = progressionList
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading progressions", e)
                _error.value = "Error loading progressions: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectDifficulty(difficulty: String?) {
        _selectedDifficulty.value = difficulty
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
                    _error.value = "Details not found for this level."
                }
            } catch (e: Exception) {
                _error.value = "Error loading level details: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCurrentUserProgressionStates() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection(FirestoreCollections.USER_PROGRESSION_STATES)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val statesMap = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProgressionState::class.java)
                }.associateBy { it.progressionId }

                _userProgressionStates.value = statesMap
            } catch (e: Exception) {
                _error.value = "Error loading your progress: ${e.localizedMessage}"
            }
        }
    }

    private fun getInMemoryOrDefaultUserProgressionState(progressionId: String): UserProgressionState {
        val userId = auth.currentUser?.uid
        val existingState = _userProgressionStates.value[progressionId]

        if (existingState != null) {
            if (userId != null && existingState.userId != userId && existingState.userId.isBlank()) {
                return existingState.copy(userId = userId)
            }
            return existingState
        }

        if (userId != null) {
            val targetProgression = _progressions.value.firstOrNull { it.id == progressionId }
            val firstLevelId = targetProgression?.levels?.firstOrNull()?.id

            return UserProgressionState(
                userId = userId,
                progressionId = progressionId,
                currentAttemptLevelId = firstLevelId,
                lastCompletedLevelId = null,
                completedLevelIds = emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
        }
        return UserProgressionState(progressionId = progressionId)
    }

    fun markLevelAsCompleted(progressionId: String, completedLevelId: String) {
        val userId = auth.currentUser?.uid ?: return

        val currentProgression = _progressions.value.firstOrNull { it.id == progressionId } ?: return
        val sortedLevels = currentProgression.levels

        val completedLevelIndex = sortedLevels.indexOfFirst { it.id == completedLevelId }
        if (completedLevelIndex == -1) return

        var userStateToUpdate = getInMemoryOrDefaultUserProgressionState(progressionId)
        if (userStateToUpdate.userId.isBlank()) userStateToUpdate = userStateToUpdate.copy(userId = userId)

        val updatedCompletedIds = (userStateToUpdate.completedLevelIds + completedLevelId).distinct()

        val nextLevelId: String? = if (completedLevelIndex < sortedLevels.size - 1) {
            sortedLevels[completedLevelIndex + 1].id
        } else null

        userStateToUpdate = userStateToUpdate.copy(
            lastCompletedLevelId = completedLevelId,
            currentAttemptLevelId = nextLevelId,
            completedLevelIds = updatedCompletedIds,
            lastUpdated = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val firestoreDocId = "${userId}_${progressionId}"
                db.collection(FirestoreCollections.USER_PROGRESSION_STATES)
                    .document(firestoreDocId)
                    .set(userStateToUpdate)
                    .await()

                _userProgressionStates.value = _userProgressionStates.value + (progressionId to userStateToUpdate)
            } catch (e: Exception) {
                _error.value = "Error saving progress: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markLevelAsCompletedAndPrepareNext(progressionId: String, levelId: String) {
        markLevelAsCompleted(progressionId, levelId)
        viewModelScope.launch {
            val currentProgression = _progressions.value.find { it.id == progressionId }
            currentProgression?.let { prog ->
                val completedLevelIndex = prog.levels.indexOfFirst { it.id == levelId }
                if (completedLevelIndex != -1 && completedLevelIndex < prog.levels.size - 1) {
                    val nextLevel = prog.levels[completedLevelIndex + 1]
                    if (isLevelUnlocked(progressionId, nextLevel.id)) {
                        _nextLevelToNavigate.value = Pair(progressionId, nextLevel.id)
                    }
                }
            }
        }
    }

    fun consumedNextLevelNavigation() {
        _nextLevelToNavigate.value = null
    }

    fun isLevelCompleted(progressionId: String, levelId: String): Boolean {
        return _userProgressionStates.value[progressionId]?.completedLevelIds?.contains(levelId) ?: false
    }

    fun isLevelUnlocked(progressionId: String, levelId: String): Boolean {
        val targetProgression = _progressions.value.firstOrNull { it.id == progressionId } ?: return false
        val sortedLevels = targetProgression.levels
        val levelToCheck = sortedLevels.find { it.id == levelId } ?: return false

        if (levelToCheck.id == sortedLevels.firstOrNull()?.id) return true
        if (isLevelCompleted(progressionId, levelId)) return true

        val currentLevelIndex = sortedLevels.indexOfFirst { it.id == levelId }
        if (currentLevelIndex > 0) {
            val previousLevel = sortedLevels[currentLevelIndex - 1]
            if (isLevelCompleted(progressionId, previousLevel.id)) return true
        }
        return false
    }

    fun onProgressionHeaderClick(progressionId: String) {
        _expandedProgressionId.value = if (_expandedProgressionId.value == progressionId) null else progressionId
    }

    fun clearError() {
        _error.value = null
    }

    fun clearExerciseLevelDetails() {
        _exerciseLevelDetails.value = null
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) }
    }
}