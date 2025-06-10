package com.jcmateus.kalisfit.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.guardarProgresoRutina
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Enum para los diferentes estados de la rutina, más granular que solo "Pausado"
enum class RoutineExecutionState {
    IDLE,               // No ha empezado o está cargando
    LOADING,            // Cargando la rutina
    INITIAL_COUNTDOWN,  // Cuenta atrás antes del primer ejercicio
    EXERCISE_ACTIVE,    // Ejercicio en curso (por repeticiones o tiempo)
    REST_BETWEEN_SETS,  // Descanso entre series del mismo ejercicio
    REST_BETWEEN_EXERCISES, // Descanso después de un ejercicio, antes del siguiente
    REST_BETWEEN_ROUNDS,    // Descanso entre rondas completas
    PAUSED,             // Rutina pausada (puede ser en cualquier estado activo)
    FINISHED,           // Rutina completada
    ERROR               // Algún error que detiene la rutina
}

// Clase de estado para encapsular toda la información de la UI
data class RoutineUiState(
    val rutina: Rutina? = null,
    val ejercicioActual: Ejercicio? = null,
    val estado: RoutineExecutionState = RoutineExecutionState.IDLE,
    val tiempoRestante: Int = 0, // Segundos restantes para un descanso o ejercicio por tiempo
    val tiempoTotalSesionSegundos: Int = 0, // Tiempo acumulado de la sesión
    val rondaActual: Int = 1,
    val indiceEjercicioActual: Int = 0, // Índice en la lista de ejercicios de la rutina
    val serieActualEjercicio: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSavingProgress: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val previousState: RoutineExecutionState = RoutineExecutionState.IDLE // Para togglePausa
)

class RoutineViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    // Para notificar a la UI sobre eventos de sonido
    private val _soundEvents = MutableSharedFlow<String>()
    val soundEvents: SharedFlow<String> = _soundEvents.asSharedFlow()

    private var routineJob: Job? = null // Para controlar la corrutina principal de la rutina
    private var sessionTimerJob: Job? = null // Para el temporizador total de la sesión
    private var currentCountdownJob: Job? = null // Para el temporizador del ejercicio/descanso actual

    private val TAG = "RoutineViewModel"

    init {
        // Cargar el perfil del usuario al iniciar el ViewModel si es necesario
        // userProfileViewModel.loadUserProfile() // Esto podría ir en un ViewModel separado o aquí si es simple
    }

    fun startRoutine(rutinaId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando rutina completa con ID: $rutinaId")
                val loadedRutina = getRutinaByIdFromFirestore(rutinaId)
                if (loadedRutina == null) {
                    _uiState.update { it.copy(errorMessage = "Rutina con ID $rutinaId no encontrada.", isLoading = false, estado = RoutineExecutionState.ERROR) }
                    Log.w(TAG, "Rutina con ID $rutinaId no encontrada.")
                } else {
                    _uiState.update {
                        it.copy(
                            rutina = loadedRutina,
                            ejercicioActual = loadedRutina.ejercicios.firstOrNull(),
                            isLoading = false,
                            estado = RoutineExecutionState.INITIAL_COUNTDOWN,
                            tiempoRestante = 3, // Inicia la cuenta atrás inicial
                            rondaActual = 1,
                            indiceEjercicioActual = 0,
                            serieActualEjercicio = 1,
                            tiempoTotalSesionSegundos = 0
                        )
                    }
                    Log.d(TAG, "Rutina cargada: ${loadedRutina.nombre}, Número de ejercicios: ${loadedRutina.ejercicios.size}")
                    startInitialCountdown()
                    startSessionTimer() // Inicia el temporizador de sesión al cargar la rutina
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error desconocido al cargar la rutina.", isLoading = false, estado = RoutineExecutionState.ERROR) }
                Log.e(TAG, "Error al cargar rutina con ID $rutinaId", e)
            }
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (isActive && _uiState.value.estado != RoutineExecutionState.FINISHED) {
                delay(1000)
                if (_uiState.value.estado != RoutineExecutionState.PAUSED) {
                    _uiState.update { it.copy(tiempoTotalSesionSegundos = it.tiempoTotalSesionSegundos + 1) }
                }
            }
        }
    }

    private fun startInitialCountdown() {
        currentCountdownJob?.cancel()
        // Usa el tiempo restante del estado si es > 0 y estamos reanudando, sino el default.
        val initialCountdownTime = if (_uiState.value.previousState == RoutineExecutionState.PAUSED &&
            _uiState.value.estado == RoutineExecutionState.INITIAL_COUNTDOWN && // Confirmar que estamos reanudando la cuenta inicial
            _uiState.value.tiempoRestante > 0) {
            _uiState.value.tiempoRestante
        } else {
            3 // Default initial time
        }
        if (_uiState.value.previousState != RoutineExecutionState.PAUSED || _uiState.value.tiempoRestante <= 0) {
            _uiState.update { it.copy(tiempoRestante = initialCountdownTime, estado = RoutineExecutionState.INITIAL_COUNTDOWN) }
        } else {
            _uiState.update { it.copy(estado = RoutineExecutionState.INITIAL_COUNTDOWN) } // Tiempo ya está, solo asegurar estado
        }

        currentCountdownJob = viewModelScope.launch {
            if (_uiState.value.tiempoRestante == initialCountdownTime && initialCountdownTime == 3) { // Asumiendo 3 es el default
                _soundEvents.emit("start_sound") // Solo al inicio real
            }
            while (isActive && _uiState.value.tiempoRestante > 0 && _uiState.value.estado == RoutineExecutionState.INITIAL_COUNTDOWN) {
                delay(1000)
                if (_uiState.value.estado != RoutineExecutionState.PAUSED) { // Aunque el job se cancela, es buena práctica
                    _uiState.update { it.copy(tiempoRestante = it.tiempoRestante - 1) }
                    if (_uiState.value.tiempoRestante <= 3 && _uiState.value.tiempoRestante > 0) {
                        _soundEvents.emit("beep")
                    }
                }
            }
            if (isActive && _uiState.value.tiempoRestante == 0 && _uiState.value.estado == RoutineExecutionState.INITIAL_COUNTDOWN) {
                moveToNextRoutineStep()
            }
        }
    }

    // Lógica principal para avanzar la rutina
    // Lógica principal para avanzar la rutina
    private suspend fun moveToNextRoutineStep() {
        currentCountdownJob?.cancel() // Cancelar cualquier temporizador activo (descanso o ejercicio por tiempo)

        val currentState = _uiState.value
        val rutina = currentState.rutina ?: run {
            Log.e(TAG, "Rutina no cargada al intentar avanzar paso.")
            _uiState.update { it.copy(estado = RoutineExecutionState.ERROR, errorMessage = "Error: Rutina no cargada.") }
            return
        }

        Log.d(TAG, "moveToNextRoutineStep: Estado actual al entrar: ${currentState.estado}, Ej: ${currentState.indiceEjercicioActual}, Serie: ${currentState.serieActualEjercicio}, Ronda: ${currentState.rondaActual}")

        // CASO 1: La cuenta atrás inicial acaba de terminar.
        if (currentState.estado == RoutineExecutionState.INITIAL_COUNTDOWN) {
            Log.d(TAG, "moveToNextRoutineStep: Saliendo de INITIAL_COUNTDOWN. Iniciando primer ejercicio.")
            val primerEjercicio = rutina.ejercicios.firstOrNull()
            if (primerEjercicio == null) {
                Log.e(TAG, "La rutina no tiene ejercicios.")
                _uiState.update { it.copy(estado = RoutineExecutionState.ERROR, errorMessage = "La rutina está vacía.") }
                return
            }
            // El uiState ya debería tener indiceEjercicioActual = 0, serieActualEjercicio = 1, rondaActual = 1
            // desde startRoutine.
            // ejercicioActual también debería estar seteado al primer ejercicio.
            // Solo necesitamos iniciar el temporizador/estado del ejercicio.
            startExerciseTimer() // Esta función ahora tomará el ejercicio del uiState
            return // Salimos de moveToNextRoutineStep aquí
        }

        // CASO 2: Un ejercicio o un descanso ha terminado o ha sido saltado.
        // Ahora determinamos qué viene después.
        val currentEjercicioDef = rutina.ejercicios.getOrNull(currentState.indiceEjercicioActual) // Usar un nombre diferente

        // 2a. ¿Hay más series en el ejercicio actual?
        if (currentEjercicioDef != null && currentState.serieActualEjercicio < currentEjercicioDef.numeroDeSeries) {
            Log.d(TAG, "moveToNextRoutineStep: Avanzando a la siguiente serie del ejercicio '${currentEjercicioDef.nombre}'.")
            // Pasar a la siguiente serie del mismo ejercicio.
            if (currentEjercicioDef.descansoEntreSeriesSegundos > 0) {
                _uiState.update {
                    it.copy(
                        estado = RoutineExecutionState.REST_BETWEEN_SETS,
                        tiempoRestante = currentEjercicioDef.descansoEntreSeriesSegundos
                    )
                }
                _soundEvents.emit("rest_start")
                startCountdownTimer(currentEjercicioDef.descansoEntreSeriesSegundos) {
                    // Al finalizar el descanso entre series:
                    _soundEvents.emit("rest_end")
                    _uiState.update {
                        // El estado se actualizará al iniciar el ejercicio
                        it.copy(serieActualEjercicio = it.serieActualEjercicio + 1)
                    }
                    startExerciseTimer()
                }
            } else {
                _uiState.update { it.copy(serieActualEjercicio = it.serieActualEjercicio + 1) }
                startExerciseTimer()
            }
        }
        // 2b. ¿Hemos completado todas las series del ejercicio actual? ¿Hay más ejercicios en la ronda?
        else if (currentEjercicioDef != null && currentState.indiceEjercicioActual < rutina.ejercicios.size - 1) {
            Log.d(TAG, "moveToNextRoutineStep: Ejercicio '${currentEjercicioDef.nombre}' completado. Avanzando al siguiente ejercicio.")
            // Pasar al siguiente ejercicio.
            if (currentEjercicioDef.descansoDespuesEjercicioSegundos > 0) {
                _uiState.update {
                    it.copy(
                        estado = RoutineExecutionState.REST_BETWEEN_EXERCISES,
                        tiempoRestante = currentEjercicioDef.descansoDespuesEjercicioSegundos
                    )
                }
                _soundEvents.emit("rest_start")
                startCountdownTimer(currentEjercicioDef.descansoDespuesEjercicioSegundos) {
                    // Al finalizar el descanso entre ejercicios:
                    _soundEvents.emit("rest_end")
                    val nuevoIndice = currentState.indiceEjercicioActual + 1 // Leer de currentState
                    _uiState.update {
                        it.copy(
                            indiceEjercicioActual = nuevoIndice,
                            serieActualEjercicio = 1,
                            ejercicioActual = rutina.ejercicios.getOrNull(nuevoIndice)
                        )
                    }
                    startExerciseTimer()
                }
            } else {
                val nuevoIndice = currentState.indiceEjercicioActual + 1 // Leer de currentState
                _uiState.update {
                    it.copy(
                        indiceEjercicioActual = nuevoIndice,
                        serieActualEjercicio = 1,
                        ejercicioActual = rutina.ejercicios.getOrNull(nuevoIndice)
                    )
                }
                startExerciseTimer()
            }
        }
        // 2c. ¿Hemos completado todos los ejercicios de la ronda actual? ¿Hay más rondas?
        else if (currentState.rondaActual < rutina.numeroDeRondas) {
            Log.d(TAG, "moveToNextRoutineStep: Ronda ${currentState.rondaActual} completada. Avanzando a la siguiente ronda.")
            // Pasar a la siguiente ronda.
            if (rutina.descansoEntreRondasSegundos > 0) {
                _uiState.update {
                    it.copy(
                        estado = RoutineExecutionState.REST_BETWEEN_ROUNDS,
                        tiempoRestante = rutina.descansoEntreRondasSegundos
                    )
                }
                _soundEvents.emit("rest_start")
                startCountdownTimer(rutina.descansoEntreRondasSegundos) {
                    // Al finalizar el descanso entre rondas:
                    _soundEvents.emit("rest_end")
                    _uiState.update {
                        it.copy(
                            rondaActual = it.rondaActual + 1,
                            indiceEjercicioActual = 0,
                            serieActualEjercicio = 1,
                            ejercicioActual = rutina.ejercicios.firstOrNull()
                        )
                    }
                    startExerciseTimer()
                }
            } else {
                _uiState.update {
                    it.copy(
                        rondaActual = it.rondaActual + 1,
                        indiceEjercicioActual = 0,
                        serieActualEjercicio = 1,
                        ejercicioActual = rutina.ejercicios.firstOrNull()
                    )
                }
                startExerciseTimer()
            }
        }
        // 2d. Todas las series, ejercicios y rondas completadas.
        else {
            Log.d(TAG, "moveToNextRoutineStep: Todas las rondas y ejercicios completados. Finalizando rutina.")
            finishRoutine()
        }
    }

    // Inicia el temporizador para un ejercicio (si es por tiempo) o simplemente cambia el estado
    private suspend fun startExerciseTimer() {
        currentCountdownJob?.cancel()
        val currentState = _uiState.value // Capturar el estado actual una vez
        val currentEjercicio = currentState.ejercicioActual
        val rutina = currentState.rutina

        if (currentEjercicio == null || rutina == null) {
            _uiState.update { it.copy(estado = RoutineExecutionState.ERROR, errorMessage = "Error: Ejercicio o rutina nulos.") }
            return
        }

        // Si el estado anterior era PAUSED y estamos reanudando un ejercicio activo,
        // el tiempoRestante ya debería estar en el uiState.
        // Si no, o si es un nuevo inicio de ejercicio, usamos la duración completa.
        val tiempoParaIniciarEjercicio = if (currentState.previousState == RoutineExecutionState.PAUSED &&
            currentState.estado == RoutineExecutionState.EXERCISE_ACTIVE && // Aseguramos que estamos reanudando un ejercicio
            currentState.tiempoRestante > 0) {
            currentState.tiempoRestante
        } else {
            currentEjercicio.duracionSegundos
        }

        _uiState.update { it.copy(estado = RoutineExecutionState.EXERCISE_ACTIVE) } // Asegurar que el estado es EXERCISE_ACTIVE
        _soundEvents.emit("exercise_start")

        if (currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0) { // Ejercicio por tiempo
            // Actualizar tiempoRestante solo si es necesario (ej. inicio nuevo, o si el tiempoParaIniciar es diferente)
            // O si previousState no era PAUSED (lo que implica un inicio fresco del ejercicio)
            if (currentState.previousState != RoutineExecutionState.PAUSED || currentState.tiempoRestante <= 0) {
                _uiState.update { it.copy(tiempoRestante = currentEjercicio.duracionSegundos) }
            } else {
                // Ya estamos reanudando, el tiempoRestante en uiState es el correcto.
                // No es necesario actualizarlo aquí, pero el bucle while lo usará.
                // Podrías hacer un log para confirmar:
                Log.d(TAG, "Reanudando ejercicio por tiempo con tiempoRestante: ${currentState.tiempoRestante}")
            }


            currentCountdownJob = viewModelScope.launch {
                // El bucle while usará el _uiState.value.tiempoRestante actualizado (o el existente al reanudar)
                while (isActive && _uiState.value.tiempoRestante > 0 && _uiState.value.estado == RoutineExecutionState.EXERCISE_ACTIVE) {
                    delay(1000)
                    if (_uiState.value.estado != RoutineExecutionState.PAUSED) { // Doble check, aunque el job se cancela
                        _uiState.update { it.copy(tiempoRestante = it.tiempoRestante - 1) }
                        if (_uiState.value.tiempoRestante <= 3 && _uiState.value.tiempoRestante > 0) {
                            _soundEvents.emit("beep")
                        }
                    }
                }
                if (isActive && _uiState.value.tiempoRestante == 0 && _uiState.value.estado == RoutineExecutionState.EXERCISE_ACTIVE) {
                    moveToNextRoutineStep()
                }
            }
        } else { // Ejercicio por repeticiones
            _uiState.update { it.copy(tiempoRestante = 0) }
        }
    }

    // Inicia un temporizador de cuenta atrás genérico (usado para todos los descansos y cuenta atrás inicial)
    private fun startCountdownTimer(initialTime: Int, onFinish: suspend () -> Unit) { // Asegúrate de que es suspend
        currentCountdownJob?.cancel()
        _uiState.update { it.copy(tiempoRestante = initialTime) }
        currentCountdownJob = viewModelScope.launch { // onFinish se llamará desde este contexto de corutina
            while (isActive && _uiState.value.tiempoRestante > 0 && (_uiState.value.estado == RoutineExecutionState.INITIAL_COUNTDOWN || _uiState.value.estado.name.startsWith("REST"))) {
                delay(1000)
                if (_uiState.value.estado != RoutineExecutionState.PAUSED) {
                    _uiState.update { it.copy(tiempoRestante = it.tiempoRestante - 1) }
                    if (_uiState.value.tiempoRestante <= 3 && _uiState.value.tiempoRestante > 0) {
                        _soundEvents.emit("beep") // Esto está dentro del launch, es correcto
                    }
                }
            }
            if (isActive && _uiState.value.tiempoRestante == 0) {
                onFinish() // onFinish es suspend, y se llama desde una corutina. Esto es correcto.
            }
        }
    }

    fun togglePausa() {
        val currentStateValue = _uiState.value // Captura el valor actual
        if (currentStateValue.estado == RoutineExecutionState.PAUSED) {
            // Reanudar
            _uiState.update {
                it.copy(estado = it.previousState) // Volver al estado anterior
            }
            // Necesitamos REINICIAR el temporizador con el tiempo restante.
            when (currentStateValue.previousState) {
                RoutineExecutionState.INITIAL_COUNTDOWN -> {
                    // startInitialCountdown ya es una función normal que lanza su propia corutina
                    startInitialCountdown()
                }
                RoutineExecutionState.EXERCISE_ACTIVE -> {
                    val ejercicio = currentStateValue.ejercicioActual
                    if (ejercicio != null && ejercicio.duracionSegundos > 0 && ejercicio.repeticiones <= 0) {
                        // Si el ejercicio era por tiempo, necesitamos reiniciar su temporizador.
                        // startExerciseTimer es suspend, así que la llamamos en un launch.
                        // Esta función ya utiliza el _uiState.value.tiempoRestante si es > 0
                        // o la duración completa del ejercicio si es 0 (o no está seteado).
                        // Es importante que startExerciseTimer maneje bien la reanudación.
                        viewModelScope.launch { // <--- ENVOLVER EN launch
                            startExerciseTimer()
                        }
                    }
                    // Si es por repeticiones y estaba pausado, simplemente cambiar el estado es suficiente.
                    // No hay un temporizador que reanudar para ejercicios por repeticiones.
                }
                RoutineExecutionState.REST_BETWEEN_SETS,
                RoutineExecutionState.REST_BETWEEN_EXERCISES,
                RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                    val onFinishAction = getCurrentOnFinishActionForState(currentStateValue.previousState, currentStateValue)
                    if (onFinishAction != null) {
                        if (currentStateValue.tiempoRestante > 0) {
                            // startCountdownTimer es una función normal que lanza su propia corutina
                            startCountdownTimer(currentStateValue.tiempoRestante, onFinishAction)
                        } else {
                            // Si el tiempo ya era 0 cuando se pausó, al reanudar, ejecutar onFinish
                            viewModelScope.launch { onFinishAction() }
                        }
                    }
                }
                else -> {
                    Log.d(TAG, "togglePausa: Estado previo ${currentStateValue.previousState} no requiere reinicio de temporizador explícito al reanudar.")
                }
            }

        } else {
            // Pausar
            currentCountdownJob?.cancel() // Cancelar el job del temporizador actual
            _uiState.update {
                // Asegúrate que el previousState no sea ya PAUSED para evitar bucles
                if (it.estado != RoutineExecutionState.PAUSED) {
                    it.copy(previousState = it.estado, estado = RoutineExecutionState.PAUSED)
                } else {
                    it // No cambiar si ya está en PAUSED por alguna razón anómala
                }
            }
        }
    }

    // Función helper para togglePausa
    private fun getCurrentOnFinishActionForState(stateToResume: RoutineExecutionState, currentUiStateValue: RoutineUiState): (suspend () -> Unit)? {
        val rutina = currentUiStateValue.rutina ?: return null
        val ejercicio = currentUiStateValue.ejercicioActual ?: rutina.ejercicios.getOrNull(currentUiStateValue.indiceEjercicioActual)

        return when (stateToResume) {
            RoutineExecutionState.INITIAL_COUNTDOWN -> {{ moveToNextRoutineStep() }}
            RoutineExecutionState.EXERCISE_ACTIVE -> {{ moveToNextRoutineStep() }} // Si es por tiempo, y el tiempo restante es > 0, se reanudará. Si es 0, moveToNextStep.
            RoutineExecutionState.REST_BETWEEN_SETS -> {{
                _soundEvents.emit("rest_end")
                _uiState.update {
                    it.copy(serieActualEjercicio = it.serieActualEjercicio + 1)
                }
                startExerciseTimer()
            }}
            RoutineExecutionState.REST_BETWEEN_EXERCISES -> {{
                _soundEvents.emit("rest_end")
                val nuevoIndice = currentUiStateValue.indiceEjercicioActual + 1
                _uiState.update {
                    it.copy(
                        indiceEjercicioActual = nuevoIndice,
                        serieActualEjercicio = 1,
                        ejercicioActual = rutina.ejercicios.getOrNull(nuevoIndice)
                    )
                }
                startExerciseTimer()
            }}
            RoutineExecutionState.REST_BETWEEN_ROUNDS -> {{
                _soundEvents.emit("rest_end")
                _uiState.update {
                    it.copy(
                        rondaActual = it.rondaActual + 1,
                        indiceEjercicioActual = 0,
                        serieActualEjercicio = 1,
                        ejercicioActual = rutina.ejercicios.firstOrNull()
                    )
                }
                startExerciseTimer()
            }}
            else -> null
        }
    }

    suspend fun saltarSiguientePaso() {
        currentCountdownJob?.cancel() // Siempre cancelar el temporizador actual

        val currentState = _uiState.value
        val rutina = currentState.rutina ?: return

        Log.d(TAG, "saltarSiguientePaso: Estado actual al entrar: ${currentState.estado}, Ronda: ${currentState.rondaActual}")

        when (currentState.estado) {
            RoutineExecutionState.INITIAL_COUNTDOWN -> {
                Log.d(TAG, "Saltando INITIAL_COUNTDOWN")
                // Directamente iniciar el primer ejercicio
                // (La lógica de moveToNextRoutineStep para INITIAL_COUNTDOWN ya hace esto)
                moveToNextRoutineStep() // Esto debería funcionar si moveToNextRoutineStep tiene el caso INITIAL_COUNTDOWN
            }
            RoutineExecutionState.EXERCISE_ACTIVE -> {
                Log.d(TAG, "Saltando EXERCISE_ACTIVE (ejercicio por tiempo o repeticiones)")
                // Damos por terminado el ejercicio y avanzamos
                moveToNextRoutineStep()
            }
            RoutineExecutionState.REST_BETWEEN_SETS -> {
                Log.d(TAG, "Saltando REST_BETWEEN_SETS")
                _soundEvents.emit("rest_end") // Simular fin de descanso
                _uiState.update {
                    it.copy(serieActualEjercicio = it.serieActualEjercicio + 1)
                }
                startExerciseTimer() // Iniciar la siguiente serie
            }
            RoutineExecutionState.REST_BETWEEN_EXERCISES -> {
                Log.d(TAG, "Saltando REST_BETWEEN_EXERCISES")
                _soundEvents.emit("rest_end")
                val nuevoIndice = currentState.indiceEjercicioActual + 1
                _uiState.update {
                    it.copy(
                        indiceEjercicioActual = nuevoIndice,
                        serieActualEjercicio = 1,
                        ejercicioActual = rutina.ejercicios.getOrNull(nuevoIndice)
                    )
                }
                startExerciseTimer() // Iniciar el siguiente ejercicio
            }
            RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                Log.d(TAG, "Saltando REST_BETWEEN_ROUNDS")
                _soundEvents.emit("rest_end")
                // Avanzar a la siguiente ronda o finalizar rutina
                if (currentState.rondaActual < rutina.numeroDeRondas) {
                    _uiState.update {
                        it.copy(
                            rondaActual = it.rondaActual + 1,
                            indiceEjercicioActual = 0,
                            serieActualEjercicio = 1,
                            ejercicioActual = rutina.ejercicios.firstOrNull()
                        )
                    }
                    startExerciseTimer() // Iniciar primer ejercicio de la nueva ronda
                } else {
                    finishRoutine() // Todas las rondas completadas
                }
            }
            else -> {
                Log.d(TAG, "No se puede saltar desde el estado: ${currentState.estado}")
            }
        }
    }


    fun reiniciarRutina() {
        routineJob?.cancel()
        sessionTimerJob?.cancel()
        currentCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                estado = RoutineExecutionState.IDLE,
                tiempoRestante = 0,
                tiempoTotalSesionSegundos = 0,
                rondaActual = 1,
                indiceEjercicioActual = 0,
                serieActualEjercicio = 1,
                ejercicioActual = it.rutina?.ejercicios?.firstOrNull() // Reestablecer al primer ejercicio
            )
        }
        // Puedes llamar a startRoutine() aquí si quieres que se reinicie automáticamente
        // o dejarlo en IDLE para que el usuario la inicie de nuevo.
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveRoutineProgress(
        userId: String,
        userProfile: UserProfile,
        rutinaId: String, // Ahora pasamos el ID de la rutina, el ViewModel ya tiene el objeto Rutina
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val completedRoutine = _uiState.value.rutina
        val rondasCompletadas = _uiState.value.rondaActual // Esto debería ser el número final de rondas completadas.
        val tiempoTotalSegundos = _uiState.value.tiempoTotalSesionSegundos

        if (completedRoutine == null) {
            _uiState.update { it.copy(errorMessage = "No hay rutina para guardar.", isSavingProgress = false) }
            onError("No hay rutina para guardar.")
            return
        }
        if (userId.isBlank() || userProfile == null) {
            _uiState.update { it.copy(errorMessage = "Datos de usuario incompletos para guardar.", isSavingProgress = false) }
            onError("Datos de usuario incompletos para guardar.")
            return
        }

        _uiState.update { it.copy(isSavingProgress = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Intentando guardar progreso para userID: $userId, rutina: ${completedRoutine.nombre}")

                guardarProgresoRutina(
                    userIdAuth = userId,
                    rutinaRealizada = completedRoutine,
                    perfilUsuarioActual = userProfile,
                    rondasCompletadasEnSesion = rondasCompletadas,
                    tiempoTotalDeLaSesionSegundos = tiempoTotalSegundos,
                    onSuccess = {
                        Log.i(TAG, "Progreso de rutina guardado exitosamente para userID: $userId")
                        _uiState.update { it.copy(successMessage = "¡Progreso guardado!", isSavingProgress = false) }
                        onSuccess()
                    },
                    onError = { errorMsg ->
                        Log.e(TAG, "Error al guardar progreso de rutina para userID: $userId. Error: $errorMsg")
                        _uiState.update { it.copy(errorMessage = errorMsg, isSavingProgress = false) }
                        onError(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Error desconocido al intentar guardar el progreso."
                Log.e(TAG, "Excepción al guardar progreso de rutina para userID: $userId", e)
                _uiState.update { it.copy(errorMessage = errorMsg, isSavingProgress = false) }
                onError(errorMsg)
            }
        }
    }

    private suspend fun finishRoutine() {
        routineJob?.cancel()
        sessionTimerJob?.cancel()
        currentCountdownJob?.cancel()
        _uiState.update { it.copy(estado = RoutineExecutionState.FINISHED) }
        _soundEvents.emit("routine_finish_sound") // Sonido al finalizar la rutina
        Log.d(TAG, "Rutina finalizada.")
        // La UI debería navegar a la pantalla de éxito o mostrar un resumen
    }

    override fun onCleared() {
        super.onCleared()
        routineJob?.cancel()
        sessionTimerJob?.cancel()
        currentCountdownJob?.cancel()
        Log.d(TAG, "RoutineViewModel onCleared: Jobs cancelados.")
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setShowExitConfirmation(show: Boolean) {
        _uiState.update { it.copy(showExitConfirmation = show) }
    }
}