package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.*
import com.jcmateus.kalisfit.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

enum class RoutineExecutionState {
    IDLE, LOADING, INITIAL_COUNTDOWN, EXERCISE_ACTIVE,
    REST_BETWEEN_SETS, REST_BETWEEN_EXERCISES, REST_BETWEEN_ROUNDS,
    PAUSED, FINISHED, ERROR
}

data class RoutineUiState(
    val rutina: Rutina? = null,
    val ejercicioActual: Ejercicio? = null,
    val componenteEjercicioActual: ComponenteEjercicio? = null,
    val indiceComponenteActual: Int = -1,
    val estado: RoutineExecutionState = RoutineExecutionState.IDLE,
    val tiempoRestante: Int = 0,
    val tiempoTotalSesionSegundos: Int = 0,
    val rondaActual: Int = 1,
    val indiceEjercicioActual: Int = 0,
    val serieActualEjercicio: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSavingProgress: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val previousState: RoutineExecutionState = RoutineExecutionState.IDLE,
    val userProfile: UserProfile? = null,
    val canResume: Boolean = false
)

class RoutineViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val repository = RoutineRepository(application)
    private val settingsDataStore = application.settingsDataStore
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var tts: TextToSpeech? = TextToSpeech(application, this)
    private var isTtsReady = false
    private var voiceCoachEnabled = true
    private var vibrationEnabled = true

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private val _soundEvents = MutableSharedFlow<String>()
    val soundEvents: SharedFlow<String> = _soundEvents.asSharedFlow()

    private var currentCountdownJob: Job? = null
    private val TAG = "RoutineViewModel"

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.data.collect { prefs ->
                voiceCoachEnabled = prefs[SettingsKeys.VOICE_COACH_ENABLED] ?: true
                vibrationEnabled = prefs[SettingsKeys.VIBRATION_ENABLED] ?: true
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                it.setLanguage(Locale("es", "ES"))
                isTtsReady = true
            }
        }
    }

    private fun speak(text: String) {
        if (isTtsReady && voiceCoachEnabled) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .build()
                audioManager.requestAudioFocus(focusRequest)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WorkoutTTS")
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    fun startRoutine(rutinaId: String, userProfile: UserProfile?) {
        _uiState.update { it.copy(isLoading = true, userProfile = userProfile, estado = RoutineExecutionState.LOADING) }
        viewModelScope.launch {
            try {
                val customRoutine = getUserCustomRoutineById(userProfile?.uid ?: "", rutinaId)
                val finalRutina = if (customRoutine != null) {
                    Rutina(
                        id = customRoutine.id,
                        nombre = customRoutine.nombrePersonalizado,
                        descripcion = customRoutine.descripcion,
                        imagenUrl = customRoutine.imagenUrl ?: "",
                        ejercicios = customRoutine.ejercicios,
                        numeroDeRondas = customRoutine.numeroDeRondas,
                        descansoEntreRondasSegundos = customRoutine.descansoEntreRondasSegundos,
                        nivelRecomendado = customRoutine.nivelRecomendado,
                        objetivos = customRoutine.objetivos,
                        lugarEntrenamiento = customRoutine.lugarEntrenamiento,
                        slug = customRoutine.id
                    )
                } else {
                    getRutinaByIdFromFirestore(rutinaId)
                }
                
                if (finalRutina != null) {
                    _uiState.update { it.copy(rutina = finalRutina, isLoading = false, estado = RoutineExecutionState.IDLE) }
                    startInitialCountdown()
                } else {
                    setError("Rutina no encontrada")
                }
            } catch (e: Exception) { setError(e.localizedMessage ?: "Error") }
        }
    }

    private fun startInitialCountdown() {
        _uiState.update { it.copy(estado = RoutineExecutionState.INITIAL_COUNTDOWN, tiempoRestante = 5) }
        speak("Prepárate para comenzar")
        startActiveTimer()
    }

    private fun startActiveTimer() {
        currentCountdownJob?.cancel()
        currentCountdownJob = viewModelScope.launch {
            try {
                while (isActive && _uiState.value.tiempoRestante > 0) {
                    val rem = _uiState.value.tiempoRestante
                    if (rem == 5 && _uiState.value.estado.name.contains("REST")) {
                        speak("Prepárate en cinco segundos")
                        vibrate(longArrayOf(0, 200))
                    } else if (rem <= 3) {
                        _soundEvents.emit("beep")
                        vibrate(longArrayOf(0, 100))
                    }
                    delay(1000)
                    if (_uiState.value.estado != RoutineExecutionState.PAUSED) {
                        _uiState.update { it.copy(tiempoRestante = it.tiempoRestante - 1) }
                    }
                }
                if (isActive && _uiState.value.estado != RoutineExecutionState.PAUSED && _uiState.value.tiempoRestante == 0) {
                    moveToNextStep()
                }
            } catch (e: CancellationException) { /* Ignored */ }
        }
    }

    suspend fun moveToNextStep() {
        val st = _uiState.value
        val rutina = st.rutina ?: return
        val ex = st.ejercicioActual ?: rutina.ejercicios.firstOrNull() ?: return
        val compIdx = st.indiceComponenteActual

        when (st.estado) {
            RoutineExecutionState.INITIAL_COUNTDOWN -> prepareExercise(0, 1, -1)
            RoutineExecutionState.EXERCISE_ACTIVE -> {
                // LÓGICA DE SUPERSETS / COMPONENTES RESTAURADA
                if (ex.esTipoComplejo() && compIdx < ex.componentes.size - 1) {
                    prepareExercise(st.indiceEjercicioActual, st.serieActualEjercicio, compIdx + 1)
                } else {
                    if (st.serieActualEjercicio < ex.numeroDeSeries) {
                        val rest = ex.descansoEntreSeriesSegundos
                        if (rest > 0) {
                            _uiState.update { it.copy(estado = RoutineExecutionState.REST_BETWEEN_SETS, tiempoRestante = rest) }
                            speak("Descanso")
                            vibrate(longArrayOf(0, 600, 200, 600))
                            startActiveTimer()
                        } else prepareExercise(st.indiceEjercicioActual, st.serieActualEjercicio + 1, -1)
                    } else if (st.indiceEjercicioActual < rutina.ejercicios.size - 1) {
                        val rest = ex.descansoDespuesEjercicioSegundos
                        if (rest > 0) {
                            _uiState.update { it.copy(estado = RoutineExecutionState.REST_BETWEEN_EXERCISES, tiempoRestante = rest) }
                            speak("Descanso de ejercicio")
                            vibrate(longArrayOf(0, 600, 200, 600))
                            startActiveTimer()
                        } else prepareExercise(st.indiceEjercicioActual + 1, 1, -1)
                    } else if (st.rondaActual < rutina.numeroDeRondas) {
                        val rest = rutina.descansoEntreRondasSegundos
                        _uiState.update { it.copy(estado = RoutineExecutionState.REST_BETWEEN_ROUNDS, tiempoRestante = rest, rondaActual = it.rondaActual + 1) }
                        speak("Ronda completada")
                        vibrate(longArrayOf(0, 800, 200, 800))
                        startActiveTimer()
                    } else finishRoutine()
                }
            }
            RoutineExecutionState.REST_BETWEEN_SETS -> prepareExercise(st.indiceEjercicioActual, st.serieActualEjercicio + 1, -1)
            RoutineExecutionState.REST_BETWEEN_EXERCISES -> prepareExercise(st.indiceEjercicioActual + 1, 1, -1)
            RoutineExecutionState.REST_BETWEEN_ROUNDS -> prepareExercise(0, 1, -1)
            else -> {}
        }
    }

    private fun prepareExercise(idx: Int, set: Int, compIdx: Int) {
        val exercise = _uiState.value.rutina?.ejercicios?.getOrNull(idx) ?: return
        val component = if (compIdx >= 0) exercise.componentes.getOrNull(compIdx) else null
        
        val time = component?.duracionSegundos ?: exercise.duracionSegundosOriginal
        val name = component?.nombreEspecifico ?: exercise.nombre

        _uiState.update { it.copy(
            estado = RoutineExecutionState.EXERCISE_ACTIVE, 
            ejercicioActual = exercise, 
            componenteEjercicioActual = component,
            indiceEjercicioActual = idx, 
            serieActualEjercicio = set, 
            indiceComponenteActual = compIdx,
            tiempoRestante = time
        ) }
        
        speak("Comienza $name")
        vibrate(longArrayOf(0, 500))
        if (time > 0) startActiveTimer()
    }

    private fun finishRoutine() {
        _uiState.update { it.copy(estado = RoutineExecutionState.FINISHED) }
        speak("Entrenamiento completado")
        vibrate(longArrayOf(0, 500, 200, 500, 200, 1000))
        saveProgress()
    }

    private fun saveProgress() {
        val st = _uiState.value
        val user = st.userProfile ?: return
        val routine = st.rutina ?: return
        viewModelScope.launch {
            guardarProgresoRutina(user.uid, routine, user, st.rondaActual, st.tiempoTotalSesionSegundos, {}, {})
        }
    }

    fun togglePausa() {
        if (_uiState.value.estado == RoutineExecutionState.PAUSED) {
            _uiState.update { it.copy(estado = it.previousState) }
            if (_uiState.value.tiempoRestante > 0) startActiveTimer()
        } else {
            _uiState.update { it.copy(previousState = it.estado, estado = RoutineExecutionState.PAUSED) }
            currentCountdownJob?.cancel()
        }
    }

    fun saltarSiguientePaso() { viewModelScope.launch { moveToNextStep() } }
    fun exitAndCleanUpRoutine() { _uiState.update { RoutineUiState() } }
    private fun setError(msg: String) { _uiState.update { it.copy(errorMessage = msg, estado = RoutineExecutionState.ERROR, isLoading = false) } }
    fun setShowExitConfirmation(show: Boolean) = _uiState.update { it.copy(showExitConfirmation = show) }
    
    private fun mapToRutina(c: UserCustomRoutine) = Rutina(
        id = c.id,
        slug = c.id,
        nombre = c.nombrePersonalizado,
        descripcion = c.descripcion,
        imagenUrl = c.imagenUrl,
        numeroDeRondas = c.numeroDeRondas,
        descansoEntreRondasSegundos = c.descansoEntreRondasSegundos,
        nivelRecomendado = c.nivelRecomendado,
        objetivos = c.objetivos,
        lugarEntrenamiento = c.lugarEntrenamiento,
        ejercicios = c.ejercicios
    )

    override fun onCleared() { super.onCleared(); tts?.shutdown() }
}
