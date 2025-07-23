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
import kotlin.coroutines.cancellation.CancellationException

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
    private val tiempoCuentaAtrasInicialGlobal = 3 // Constante para la cuenta atrás inicial

    private fun startActiveTimerForCurrentStep() {
        currentCountdownJob?.cancel()
        val currentState = _uiState.value
        val currentEjercicio = currentState.ejercicioActual

        // No iniciar temporizador para estados no activos o ya finalizados
        if (currentState.estado == RoutineExecutionState.IDLE ||
            currentState.estado == RoutineExecutionState.LOADING ||
            currentState.estado == RoutineExecutionState.FINISHED ||
            currentState.estado == RoutineExecutionState.ERROR ||
            currentState.estado == RoutineExecutionState.PAUSED) {
            Log.d(TAG, "startActiveTimerForCurrentStep: No se inicia temporizador para estado ${currentState.estado}")
            return
        }

        // Caso especial: Ejercicio por REPETICIONES. No necesita cuenta atrás.
        // El avance es manual (botón en UI) o al saltar.
        if (currentState.estado == RoutineExecutionState.EXERCISE_ACTIVE &&
            currentEjercicio != null && currentEjercicio.repeticiones > 0 && currentEjercicio.duracionSegundos <= 0) {
            Log.d(TAG, "startActiveTimerForCurrentStep: Ejercicio '${currentEjercicio.nombre}' por repeticiones. No se inicia cuenta atrás automática.")
            // Aseguramos que el tiempo restante sea 0 para que la UI no muestre un contador
            if (currentState.tiempoRestante != 0) {
                _uiState.update { it.copy(tiempoRestante = 0) }
            }
            return
        }

        // Si el tiempo restante es 0 para un estado que SÍ usa temporizador,
        // debemos avanzar al siguiente paso inmediatamente.
        // Excepción: INITIAL_COUNTDOWN puede empezar con 0 para avanzar si así se configuró (aunque usualmente no).
        // Y un ejercicio por tiempo que legítimamente dure 0s.
        if (currentState.tiempoRestante <= 0) {
            if (currentState.estado == RoutineExecutionState.INITIAL_COUNTDOWN ||
                (currentState.estado == RoutineExecutionState.EXERCISE_ACTIVE && currentEjercicio != null && currentEjercicio.duracionSegundos == 0 && currentEjercicio.repeticiones <= 0) ||
                currentState.estado.name.startsWith("REST")) {
                Log.d(TAG, "startActiveTimerForCurrentStep: Tiempo restante es 0 para ${currentState.estado}, llamando a moveToNextRoutineStep.")
                viewModelScope.launch { moveToNextRoutineStep() }
                return
            } else {
                // Para otros casos (ej. un ejercicio por tiempo que no es de 0s pero su tiempoRestante es 0),
                // esto podría indicar que ya terminó y moveToNextRoutineStep ya fue llamado o será llamado.
                Log.d(TAG, "startActiveTimerForCurrentStep: Tiempo restante es 0 para ${currentState.estado}, no se inicia temporizador. El flujo normal debería avanzar.")
                return
            }
        }

        Log.d(TAG, "startActiveTimerForCurrentStep: Iniciando temporizador para ${currentState.estado} con ${currentState.tiempoRestante}s")

        currentCountdownJob = viewModelScope.launch {
            try {
                // Usamos el tiempoRestante del estado en el momento de iniciar el job,
                // pero actualizamos el estado directamente.
                var internalRemainingTime = _uiState.value.tiempoRestante
                while (isActive && internalRemainingTime > 0 && _uiState.value.estado == currentState.estado) {
                    delay(1000)
                    if (_uiState.value.estado != RoutineExecutionState.PAUSED) { // Solo decrementar si no está pausado
                        internalRemainingTime--
                        _uiState.update { it.copy(tiempoRestante = internalRemainingTime) }

                        if (internalRemainingTime in 1..3) { // Beep en los últimos 3 segundos
                            _soundEvents.emit("beep")
                        }
                    }
                }

                // Cuando el bucle termina (tiempo llega a 0, estado cambia, job cancelado o pausado)
                if (isActive && internalRemainingTime == 0 && _uiState.value.estado == currentState.estado && _uiState.value.estado != RoutineExecutionState.PAUSED) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Temporizador finalizado naturalmente para ${currentState.estado}. Llamando a moveToNextRoutineStep.")
                    moveToNextRoutineStep()
                } else if (!isActive) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Job cancelado para estado ${currentState.estado}")
                } else if (_uiState.value.estado == RoutineExecutionState.PAUSED) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Pausado durante temporizador para ${currentState.estado}. Tiempo restante ${internalRemainingTime}s guardado en uiState.")
                } else if (_uiState.value.estado != currentState.estado) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Estado cambió de ${currentState.estado} a ${_uiState.value.estado} durante el temporizador.")
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "startActiveTimerForCurrentStep: Job de temporizador explícitamente cancelado para estado ${currentState.estado}")
            }
        }
    }

    fun startRoutine(rutinaId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null, rutina = null) }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando rutina completa con ID: $rutinaId")
                val loadedRutina = getRutinaByIdFromFirestore(rutinaId) // Simulación
                if (loadedRutina == null) {
                    _uiState.update { it.copy(errorMessage = "Rutina con ID $rutinaId no encontrada.", isLoading = false, estado = RoutineExecutionState.ERROR) }
                    Log.w(TAG, "Rutina con ID $rutinaId no encontrada.")
                } else {
                    _uiState.update {
                        it.copy(
                            rutina = loadedRutina,
                            ejercicioActual = loadedRutina.ejercicios.firstOrNull(),
                            isLoading = false,
                            // El estado y tiempoRestante se establecen en startInitialCountdown
                            rondaActual = 1,
                            indiceEjercicioActual = 0,
                            serieActualEjercicio = 1,
                            tiempoTotalSesionSegundos = 0,
                            previousState = RoutineExecutionState.IDLE // Limpiar estado previo
                        )
                    }
                    Log.d(TAG, "Rutina cargada: ${loadedRutina.nombre}, Número de ejercicios: ${loadedRutina.ejercicios.size}")
                    if (loadedRutina.ejercicios.isNotEmpty()) {
                        startInitialCountdown()
                        startSessionTimer()
                    } else {
                        finishRoutineWithError("La rutina cargada no tiene ejercicios.")
                    }
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
            while (isActive && _uiState.value.estado != RoutineExecutionState.FINISHED && _uiState.value.estado != RoutineExecutionState.ERROR) {
                delay(1000)
                if (_uiState.value.estado != RoutineExecutionState.PAUSED) {
                    _uiState.update { it.copy(tiempoTotalSesionSegundos = it.tiempoTotalSesionSegundos + 1) }
                }
            }
        }
    }

    private fun startInitialCountdown() {
        val currentUiState = _uiState.value
        val tiempoDeCuentaAtras: Int

        // Si estamos reanudando desde PAUSA y el estado ANTERIOR era INITIAL_COUNTDOWN
        if (currentUiState.previousState == RoutineExecutionState.INITIAL_COUNTDOWN &&
            _uiState.value.estado == RoutineExecutionState.INITIAL_COUNTDOWN && // togglePause nos restauró a este estado
            currentUiState.tiempoRestante > 0) {
            Log.d(TAG, "Reanudando INITIAL_COUNTDOWN con ${currentUiState.tiempoRestante}s restantes.")
            tiempoDeCuentaAtras = currentUiState.tiempoRestante
        } else {
            Log.d(TAG, "Iniciando NUEVO INITIAL_COUNTDOWN.")
            tiempoDeCuentaAtras = tiempoCuentaAtrasInicialGlobal
            // Emitir sonido de inicio solo si es un inicio completamente nuevo de la cuenta atrás
            if (currentUiState.previousState != RoutineExecutionState.PAUSED) { // Evitar sonido si solo se reanuda
                viewModelScope.launch { _soundEvents.emit("start_sound") }
            }
        }

        _uiState.update {
            it.copy(
                estado = RoutineExecutionState.INITIAL_COUNTDOWN,
                tiempoRestante = tiempoDeCuentaAtras
            )
        }
        startActiveTimerForCurrentStep()
    }

    private suspend fun moveToNextRoutineStep() {
        // Es buena idea cancelar aquí para evitar que un temporizador anterior
        // llame a moveToNextRoutineStep de nuevo si esta instancia ya está en proceso.
        currentCountdownJob?.cancel()
        Log.d(TAG, "moveToNextRoutineStep: Job de cuenta atrás anterior cancelado (si existía).")

        val currentUiState = _uiState.value // Usar el estado más reciente
        val rutina = currentUiState.rutina ?: run {
            Log.e(TAG, "moveToNextRoutineStep: Error interno - Rutina no disponible.")
            finishRoutineWithError("Error interno: Rutina no disponible.")
            return
        }

        Log.i(TAG, "moveToNextRoutineStep: ================== INICIO ==================")
        Log.i(TAG, "moveToNextRoutineStep: Procesando desde ESTADO=${currentUiState.estado}, EjIdx=${currentUiState.indiceEjercicioActual}, Serie=${currentUiState.serieActualEjercicio}, Ronda=${currentUiState.rondaActual}, EjActual=${currentUiState.ejercicioActual?.nombre}")

        when (currentUiState.estado) {
            RoutineExecutionState.INITIAL_COUNTDOWN -> {
                Log.d(TAG, "INITIAL_COUNTDOWN finalizado. Preparando primer ejercicio.")
                if (rutina.ejercicios.isEmpty()) {
                    Log.w(TAG, "moveToNextRoutineStep: La rutina no tiene ejercicios.")
                    finishRoutineWithError("La rutina no tiene ejercicios.")
                    return
                }
                val primerEjercicio = rutina.ejercicios.first()
                Log.d(TAG, "moveToNextRoutineStep: Primer ejercicio a preparar: ${primerEjercicio.nombre}")
                _uiState.update {
                    it.copy(
                        ejercicioActual = primerEjercicio,
                        indiceEjercicioActual = 0,
                        serieActualEjercicio = 1,
                        rondaActual = 1 // Asegurar que la ronda es 1
                    )
                }
                Log.d(TAG, "moveToNextRoutineStep: Estado actualizado para primer ejercicio. Llamando a prepareAndStartExerciseStep.")
                prepareAndStartExerciseStep()
            }

            RoutineExecutionState.EXERCISE_ACTIVE -> {
                Log.d(TAG, "EXERCISE_ACTIVE finalizado/saltado.")
                val ejercicioTerminado = currentUiState.ejercicioActual
                    ?: rutina.ejercicios.getOrNull(currentUiState.indiceEjercicioActual)

                if (ejercicioTerminado == null) {
                    Log.e(TAG, "moveToNextRoutineStep: Error - Ejercicio actual no encontrado al finalizar EXERCISE_ACTIVE. Estado: $currentUiState")
                    finishRoutineWithError("Error: Ejercicio actual no encontrado al finalizar EXERCISE_ACTIVE.")
                    return
                }

                Log.d(TAG, "Ejercicio '${ejercicioTerminado.nombre}' (Serie ${currentUiState.serieActualEjercicio}/${ejercicioTerminado.numeroDeSeries}) finalizado.")

                // Siguiente serie del mismo ejercicio
                if (currentUiState.serieActualEjercicio < ejercicioTerminado.numeroDeSeries) {
                    Log.d(TAG, "Pasando a la siguiente serie del ejercicio '${ejercicioTerminado.nombre}'.")
                    if (ejercicioTerminado.descansoEntreSeriesSegundos > 0) {
                        Log.d(TAG, "Iniciando REST_BETWEEN_SETS de ${ejercicioTerminado.descansoEntreSeriesSegundos}s.")
                        viewModelScope.launch { _soundEvents.emit("rest_start") }
                        _uiState.update {
                            it.copy(
                                estado = RoutineExecutionState.REST_BETWEEN_SETS,
                                tiempoRestante = ejercicioTerminado.descansoEntreSeriesSegundos
                                // serieActualEjercicio se incrementa DESPUÉS del descanso
                            )
                        }
                        startActiveTimerForCurrentStep()
                    } else {
                        Log.d(TAG, "Sin descanso entre series. Iniciando siguiente serie directamente.")
                        _uiState.update {
                            // Incrementar serie y mantener el ejercicio actual.
                            // El estado se cambiará a EXERCISE_ACTIVE en prepareAndStartExerciseStep
                            it.copy(serieActualEjercicio = it.serieActualEjercicio + 1)
                        }
                        prepareAndStartExerciseStep()
                    }
                    // Siguiente ejercicio de la misma ronda
                } else if (currentUiState.indiceEjercicioActual < rutina.ejercicios.size - 1) {
                    Log.d(TAG, "Todas las series de '${ejercicioTerminado.nombre}' completadas. Pasando al siguiente ejercicio.")
                    if (ejercicioTerminado.descansoDespuesEjercicioSegundos > 0) {
                        Log.d(TAG, "Iniciando REST_BETWEEN_EXERCISES de ${ejercicioTerminado.descansoDespuesEjercicioSegundos}s.")
                        viewModelScope.launch { _soundEvents.emit("rest_start") }
                        _uiState.update {
                            it.copy(
                                estado = RoutineExecutionState.REST_BETWEEN_EXERCISES,
                                tiempoRestante = ejercicioTerminado.descansoDespuesEjercicioSegundos
                                // indice y serie se actualizan DESPUÉS del descanso
                            )
                        }
                        startActiveTimerForCurrentStep()
                    } else {
                        Log.d(TAG, "Sin descanso después de ejercicio. Iniciando siguiente ejercicio directamente.")
                        val nuevoIndice = currentUiState.indiceEjercicioActual + 1
                        val proximoEjercicio = rutina.ejercicios.getOrNull(nuevoIndice)
                        if (proximoEjercicio == null) { // Doble check, aunque la condición del if ya debería cubrirlo
                            Log.e(TAG, "moveToNextRoutineStep: Error lógico - Próximo ejercicio nulo aunque el índice es válido.")
                            finishRoutineWithError("Error al pasar al siguiente ejercicio.")
                            return
                        }
                        _uiState.update {
                            it.copy(
                                indiceEjercicioActual = nuevoIndice,
                                serieActualEjercicio = 1, // Resetear serie para el nuevo ejercicio
                                ejercicioActual = proximoEjercicio
                            )
                        }
                        prepareAndStartExerciseStep()
                    }
                    // Siguiente ronda
                } else if (currentUiState.rondaActual < rutina.numeroDeRondas) {
                    Log.d(TAG, "Último ejercicio de la ronda ${currentUiState.rondaActual} completado. Pasando a la siguiente ronda.")
                    if (rutina.descansoEntreRondasSegundos > 0) {
                        Log.d(TAG, "Iniciando REST_BETWEEN_ROUNDS de ${rutina.descansoEntreRondasSegundos}s.")
                        viewModelScope.launch { _soundEvents.emit("rest_start") }
                        _uiState.update {
                            it.copy(
                                estado = RoutineExecutionState.REST_BETWEEN_ROUNDS,
                                tiempoRestante = rutina.descansoEntreRondasSegundos
                                // ronda, indice y serie se actualizan DESPUÉS del descanso
                            )
                        }
                        startActiveTimerForCurrentStep()
                    } else {
                        Log.d(TAG, "Sin descanso entre rondas. Iniciando siguiente ronda directamente.")
                        val primerEjercicioSiguienteRonda = rutina.ejercicios.firstOrNull()
                        if (primerEjercicioSiguienteRonda == null) {
                            Log.e(TAG, "moveToNextRoutineStep: Error - No hay ejercicios para la siguiente ronda aunque hay más rondas.")
                            finishRoutineWithError("Error: No hay ejercicios para la siguiente ronda.")
                            return
                        }
                        _uiState.update {
                            it.copy(
                                rondaActual = it.rondaActual + 1,
                                indiceEjercicioActual = 0, // Resetear índice de ejercicio
                                serieActualEjercicio = 1, // Resetear serie
                                ejercicioActual = primerEjercicioSiguienteRonda
                            )
                        }
                        prepareAndStartExerciseStep()
                    }
                    // Fin de la rutina
                } else {
                    Log.i(TAG, "Todas las series, ejercicios y rondas completadas. Finalizando rutina.")
                    finishRoutine()
                }
            }

            RoutineExecutionState.REST_BETWEEN_SETS -> {
                Log.d(TAG, "REST_BETWEEN_SETS finalizado para ejercicio '${currentUiState.ejercicioActual?.nombre}'.")
                Log.d(TAG, "Intentando emitir sonido 'rest_end' y actualizar UI para la siguiente serie.")
                try {
                    // Asumiendo que _soundEvents.emit es suspend o seguro de llamar en una corrutina
                    viewModelScope.launch {
                        _soundEvents.emit("rest_end")
                        Log.d(TAG, "Sonido 'rest_end' emitido (o intento hecho).")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al emitir sonido 'rest_end' en REST_BETWEEN_SETS", e)
                    // Considerar si el flujo debe continuar o no en caso de error de sonido
                }

                val serieAnterior = currentUiState.serieActualEjercicio
                _uiState.update {
                    it.copy(
                        serieActualEjercicio = it.serieActualEjercicio + 1
                        // ejercicioActual sigue siendo el mismo
                        // estado se cambiará en prepareAndStartExerciseStep
                    )
                }
                Log.d(TAG, "Serie actualizada de $serieAnterior a ${_uiState.value.serieActualEjercicio}. Llamando a prepareAndStartExerciseStep AHORA.")
                prepareAndStartExerciseStep()
            }

            RoutineExecutionState.REST_BETWEEN_EXERCISES -> {
                Log.d(TAG, "REST_BETWEEN_EXERCISES finalizado.")
                viewModelScope.launch { _soundEvents.emit("rest_end") } // Asumir suspend o seguro en launch

                val nuevoIndice = currentUiState.indiceEjercicioActual + 1
                val proximoEjercicio = rutina.ejercicios.getOrNull(nuevoIndice)

                if (proximoEjercicio == null) {
                    Log.e(TAG, "moveToNextRoutineStep: Error - No se encontró el próximo ejercicio (índice: $nuevoIndice) después del descanso entre ejercicios.")
                    finishRoutineWithError("Error: No se encontró el próximo ejercicio después del descanso.")
                    return
                }
                Log.d(TAG, "Preparando próximo ejercicio: ${proximoEjercicio.nombre}")
                _uiState.update {
                    it.copy(
                        indiceEjercicioActual = nuevoIndice,
                        serieActualEjercicio = 1, // Resetear serie para el nuevo ejercicio
                        ejercicioActual = proximoEjercicio
                    )
                }
                Log.d(TAG, "Estado actualizado para próximo ejercicio. Llamando a prepareAndStartExerciseStep.")
                prepareAndStartExerciseStep()
            }

            RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                Log.d(TAG, "REST_BETWEEN_ROUNDS finalizado.")
                viewModelScope.launch { _soundEvents.emit("rest_end") } // Asumir suspend o seguro en launch

                val proximaRonda = currentUiState.rondaActual + 1
                // Doble comprobación, aunque la lógica en EXERCISE_ACTIVE ya debería haberlo manejado
                if (proximaRonda <= rutina.numeroDeRondas) {
                    val primerEjercicioDeRonda = rutina.ejercicios.firstOrNull()
                    if (primerEjercicioDeRonda == null) {
                        Log.e(TAG, "moveToNextRoutineStep: Error - No hay ejercicios para la siguiente ronda ($proximaRonda) aunque la ronda es válida.")
                        finishRoutineWithError("Error: No hay ejercicios para la siguiente ronda.")
                        return
                    }
                    Log.d(TAG, "Preparando primera serie de la ronda $proximaRonda, ejercicio: ${primerEjercicioDeRonda.nombre}")
                    _uiState.update {
                        it.copy(
                            rondaActual = proximaRonda,
                            indiceEjercicioActual = 0, // Resetear índice de ejercicio
                            serieActualEjercicio = 1, // Resetear serie
                            ejercicioActual = primerEjercicioDeRonda
                        )
                    }
                    Log.d(TAG, "Estado actualizado para próxima ronda. Llamando a prepareAndStartExerciseStep.")
                    prepareAndStartExerciseStep()
                } else {
                    Log.w(TAG, "Se intentó pasar a la siguiente ronda (${proximaRonda}) pero ya se completaron todas (${rutina.numeroDeRondas}). Esto no debería suceder si la lógica anterior es correcta. Finalizando.")
                    finishRoutine()
                }
            }
            RoutineExecutionState.PAUSED,
            RoutineExecutionState.IDLE,
            RoutineExecutionState.LOADING,
            RoutineExecutionState.FINISHED,
            RoutineExecutionState.ERROR -> {
                Log.w(TAG, "moveToNextRoutineStep llamado desde un estado no procesable o ya final: ${currentUiState.estado}. No se realiza ninguna acción.")
            }
        }
        Log.i(TAG, "moveToNextRoutineStep: =================== FIN ====================")
    }

    private fun prepareAndStartExerciseStep() {
        val currentStateBeforeUpdate = _uiState.value
        val currentEjercicio = currentStateBeforeUpdate.ejercicioActual

        Log.d(TAG, "prepareAndStartExerciseStep: ================== INICIO ==================")
        Log.d(TAG, "prepareAndStartExerciseStep: Estado ENTRADA: ${currentStateBeforeUpdate.estado}, Ej: ${currentEjercicio?.nombre}, Serie: ${currentStateBeforeUpdate.serieActualEjercicio}, TiempoRest: ${currentStateBeforeUpdate.tiempoRestante}, PrevSt: ${currentStateBeforeUpdate.previousState}")

        if (currentEjercicio == null) {
            Log.e(TAG, "prepareAndStartExerciseStep: Error - Ejercicio nulo. Estado de entrada: ${currentStateBeforeUpdate.estado}")
            finishRoutineWithError("Error: Ejercicio nulo al preparar el paso del ejercicio.")
            return
        }

        var tiempoParaEjercicio: Int
        var isResumingTimedExercise = false

        // Lógica para determinar el tiempo del ejercicio
        // Esta condición es para cuando se REANUDA un ejercicio por tiempo desde PAUSA
        if (currentStateBeforeUpdate.previousState == RoutineExecutionState.EXERCISE_ACTIVE &&
            _uiState.value.estado == RoutineExecutionState.EXERCISE_ACTIVE && // El estado actual ya es EXERCISE_ACTIVE (restaurado por togglePausa)
            currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0 &&
            currentStateBeforeUpdate.tiempoRestante > 0) {
            Log.d(TAG, "prepareAndStartExerciseStep: Reanudando ejercicio POR TIEMPO '${currentEjercicio.nombre}' con ${currentStateBeforeUpdate.tiempoRestante}s.")
            tiempoParaEjercicio = currentStateBeforeUpdate.tiempoRestante
            isResumingTimedExercise = true
        } else if (currentEjercicio.duracionSegundos > 0 && currentEjercicio.repeticiones <= 0) { // Ejercicio por tiempo (nuevo inicio)
            Log.d(TAG, "prepareAndStartExerciseStep: Iniciando NUEVO ejercicio POR TIEMPO '${currentEjercicio.nombre}'. Duración: ${currentEjercicio.duracionSegundos}s.")
            tiempoParaEjercicio = currentEjercicio.duracionSegundos
        } else { // Ejercicio por repeticiones
            Log.d(TAG, "prepareAndStartExerciseStep: Preparando ejercicio POR REPETICIONES '${currentEjercicio.nombre}'. Reps: ${currentEjercicio.repeticiones}.")
            tiempoParaEjercicio = 0
        }

        Log.d(TAG, "prepareAndStartExerciseStep: Calculado: tiempoParaEjercicio=$tiempoParaEjercicio, isResuming=$isResumingTimedExercise")

        _uiState.update { prevState ->
            Log.d(TAG, "prepareAndStartExerciseStep: DENTRO de _uiState.update: prevState.estado=${prevState.estado}, prevState.serie=${prevState.serieActualEjercicio}")
            Log.d(TAG, "prepareAndStartExerciseStep:                                 NUEVO estado=EXERCISE_ACTIVE, NUEVO tiempoRestante=$tiempoParaEjercicio")
            prevState.copy(
                estado = RoutineExecutionState.EXERCISE_ACTIVE,
                tiempoRestante = tiempoParaEjercicio,
                ejercicioActual = currentEjercicio, // Asegurar que es el que hemos estado evaluando
                // serieActualEjercicio ya debería estar correcta desde moveToNextRoutineStep o inicio de rutina
                previousState = RoutineExecutionState.IDLE // Limpiar previousState para la próxima pausa
            )
        }

        val stateAfterUpdate = _uiState.value
        Log.d(TAG, "prepareAndStartExerciseStep: Estado DESPUÉS de _uiState.update: ${stateAfterUpdate.estado}, Ej: ${stateAfterUpdate.ejercicioActual?.nombre}, Serie: ${stateAfterUpdate.serieActualEjercicio}, TiempoRest: ${stateAfterUpdate.tiempoRestante}")

        if (!isResumingTimedExercise) {
            Log.d(TAG, "prepareAndStartExerciseStep: Emitiendo sonido 'exercise_start'.")
            viewModelScope.launch { _soundEvents.emit("exercise_start") }
        } else {
            Log.d(TAG, "prepareAndStartExerciseStep: NO emitiendo sonido 'exercise_start' (reanudando).")
        }

        Log.d(TAG, "prepareAndStartExerciseStep: Llamando a startActiveTimerForCurrentStep().")
        Log.d(TAG, "prepareAndStartExerciseStep: =================== FIN ====================")
        startActiveTimerForCurrentStep()
    }

    fun togglePausa() {
        val currentStateValue = _uiState.value // Captura el estado en el momento de llamar a togglePausa

        if (currentStateValue.estado == RoutineExecutionState.PAUSED) {
            // REANUDAR
            // 1. Actualizamos el estado para salir de PAUSED y volver al previousState.
            //    El tiempoRestante que está en currentStateValue.tiempoRestante es el correcto de cuando se pausó.
            _uiState.update {
                it.copy(
                    estado = currentStateValue.previousState // Volver al estado anterior
                    // tiempoRestante ya es el correcto (el que se guardó al pausar)
                    // previousState se podría limpiar aquí para la siguiente pausa, o dejarlo como está.
                    // previousState = RoutineExecutionState.IDLE // Opcional
                )
            }
            // En este punto, _uiState.value.estado es el estado al que volvemos (e.g., EXERCISE_ACTIVE)
            // y _uiState.value.tiempoRestante es el tiempo que quedó.

            Log.d(TAG, "Reanudando desde PAUSA. Estado restaurado a: ${_uiState.value.estado}, Tiempo restante: ${_uiState.value.tiempoRestante}")

            // 2. Ahora llamamos a la función apropiada para reiniciar el temporizador/paso
            //    con el estado y tiempo restantes ACTUALES en _uiState.value.
            when (_uiState.value.estado) { // Usar el estado ACTUAL (que acabamos de restaurar)
                RoutineExecutionState.INITIAL_COUNTDOWN -> {
                    // startInitialCountdown configura estado y tiempo y llama a startActiveTimerForCurrentStep.
                    // Ya está diseñado para usar _uiState.value.tiempoRestante si venimos de pausa.
                    startInitialCountdown()
                }
                RoutineExecutionState.EXERCISE_ACTIVE -> {
                    // prepareAndStartExerciseStep configura estado y tiempo y llama a startActiveTimerForCurrentStep.
                    // Ya está diseñado para usar _uiState.value.tiempoRestante si venimos de pausa.
                    prepareAndStartExerciseStep()
                }
                RoutineExecutionState.REST_BETWEEN_SETS,
                RoutineExecutionState.REST_BETWEEN_EXERCISES,
                RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                    // Para los descansos, el estado y el tiempoRestante ya son correctos.
                    // Simplemente reiniciamos el temporizador con el tiempo restante.
                    startActiveTimerForCurrentStep()
                }
                else -> {
                    Log.d(TAG, "togglePausa: Estado restaurado ${_uiState.value.estado} no requiere reinicio de temporizador explícito o es un estado no activo/finalizado.")
                }
            }

        } else { // PAUSAR
            // Solo pausar si estamos en un estado activo
            if (currentStateValue.estado != RoutineExecutionState.IDLE &&
                currentStateValue.estado != RoutineExecutionState.LOADING &&
                currentStateValue.estado != RoutineExecutionState.FINISHED &&
                currentStateValue.estado != RoutineExecutionState.ERROR &&
                currentStateValue.estado != RoutineExecutionState.PAUSED) {

                currentCountdownJob?.cancel() // Cancelar el job del temporizador actual
                // Guardamos el estado actual (currentStateValue.estado) como previousState
                // y el tiempoRestante actual (currentStateValue.tiempoRestante) se queda como está en uiState.
                _uiState.update {
                    it.copy(
                        previousState = currentStateValue.estado, // Guardar el estado actual como previousState
                        estado = RoutineExecutionState.PAUSED
                        // El tiempoRestante de 'it' es el tiempo actual en el momento de la pausa
                    )
                }
                Log.d(TAG, "Rutina PAUSADA. Estado anterior guardado: ${currentStateValue.estado}, Tiempo restante guardado: ${_uiState.value.tiempoRestante}")
            } else {
                Log.d(TAG, "Intento de pausar desde un estado no pausable o ya pausado: ${currentStateValue.estado}")
            }
        }
    }

    suspend fun saltarSiguientePaso() {
        currentCountdownJob?.cancel() // Siempre cancelar el temporizador actual
        val currentState = _uiState.value
        Log.d(TAG, "saltarSiguientePaso: Intentando saltar desde ${currentState.estado}")

        // La lógica de saltar es esencialmente la misma que si el paso actual terminara naturalmente.
        // Así que, en la mayoría de los casos, simplemente llamamos a moveToNextRoutineStep.
        // Sin embargo, si estamos en un descanso, queremos emitir "rest_end" antes.

        when (currentState.estado) {
            RoutineExecutionState.INITIAL_COUNTDOWN,
            RoutineExecutionState.EXERCISE_ACTIVE -> {
                Log.d(TAG, "Saltando ${currentState.estado}. Dejando que moveToNextRoutineStep decida.")
                // No es necesario modificar el estado aquí, moveToNextRoutineStep lo hará.
                moveToNextRoutineStep()
            }
            RoutineExecutionState.REST_BETWEEN_SETS,
            RoutineExecutionState.REST_BETWEEN_EXERCISES,
            RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                Log.d(TAG, "Saltando ${currentState.estado}. Emitiendo rest_end y llamando a moveToNextRoutineStep.")
                _soundEvents.emit("rest_end")
                // No es necesario actualizar el estado aquí para indicar que el descanso terminó (como poner tiempoRestante=0),
                // moveToNextRoutineStep se encargará de la transición DESDE este estado de descanso.
                moveToNextRoutineStep()
            }
            RoutineExecutionState.PAUSED -> {
                Log.d(TAG, "Intentando saltar mientras está PAUSADO. Primero reanudando y luego saltando.")
                // Reanudar primero para que el estado sea el correcto, luego saltar.
                // Esto podría necesitar un pequeño delay o una forma de asegurar que el estado se actualice.
                // O mejor, el usuario debería reanudar primero y luego saltar.
                // Por simplicidad, asumimos que la UI no permite saltar directamente desde pausa,
                // o si lo hace, se encarga de reanudar primero.
                // Si quieres manejarlo aquí:
                // togglePausa() // Reanuda
                // viewModelScope.launch { delay(50); moveToNextRoutineStep() } // Un pequeño delay puede ser necesario
                // Pero es más limpio que la UI maneje esto.
                _uiState.update { it.copy(errorMessage = "Reanuda la rutina antes de saltar.") }
            }
            else -> {
                Log.d(TAG, "No se puede saltar desde el estado: ${currentState.estado} o no es necesario.")
            }
        }
    }


    fun reiniciarRutina() {
        // routineJob?.cancel() // Ya no se usa así
        sessionTimerJob?.cancel()
        currentCountdownJob?.cancel()
        val rutinaActual = _uiState.value.rutina
        _uiState.update {
            RoutineUiState( // Resetear a un estado inicial limpio pero manteniendo la rutina cargada
                rutina = rutinaActual,
                isLoading = false,
                estado = RoutineExecutionState.IDLE, // Esperar a que el usuario inicie
                // ejercicioActual = rutinaActual?.ejercicios?.firstOrNull(), // Se seteará en startRoutine
                // Los demás valores se resetean a default en startRoutine/startInitialCountdown
            )
        }
        Log.d(TAG, "Rutina reiniciada al estado IDLE.")
        // El usuario deberá presionar "Iniciar" de nuevo, lo cual llamará a startRoutine.
    }

    private fun finishRoutineWithError(errorMessage: String) {
        Log.e(TAG, "Error en la rutina: $errorMessage")
        sessionTimerJob?.cancel()
        currentCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                estado = RoutineExecutionState.ERROR,
                errorMessage = errorMessage,
                isLoading = false
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveRoutineProgress(
        userId: String,
        userProfile: UserProfile, // Asumiendo que UserProfile es nullable o tienes un valor por defecto
        rutinaId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUiValue = _uiState.value
        val completedRoutine = currentUiValue.rutina
        // Asegúrate de que rondasCompletadas refleje las rondas realmente finalizadas.
        // Si la rutina termina en la ronda X, se completaron X rondas.
        // Si se detiene a mitad de la ronda X, se completaron X-1 rondas.
        // Ajusta esta lógica según cómo quieras contar.
        val rondasCompletadas = if (currentUiValue.estado == RoutineExecutionState.FINISHED) {
            currentUiValue.rondaActual
        } else {
            maxOf(0, currentUiValue.rondaActual -1) // Si no terminó, es la ronda anterior
        }
        val tiempoTotalSegundos = currentUiValue.tiempoTotalSesionSegundos

        if (completedRoutine == null) {
            val errorMsg = "No hay rutina para guardar."
            _uiState.update { it.copy(errorMessage = errorMsg, isSavingProgress = false) }
            onError(errorMsg)
            return
        }
        if (userId.isBlank()) { // userProfile puede ser opcional dependiendo de tu lógica de guardado
            val errorMsg = "Datos de usuario incompletos para guardar."
            _uiState.update { it.copy(errorMessage = errorMsg, isSavingProgress = false) }
            onError(errorMsg)
            return
        }

        _uiState.update { it.copy(isSavingProgress = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Intentando guardar progreso para userID: $userId, rutina: ${completedRoutine.nombre}, rondas: $rondasCompletadas, tiempo: $tiempoTotalSegundos")

                guardarProgresoRutina( // Simulación
                    userIdAuth = userId,
                    rutinaRealizada = completedRoutine,
                    perfilUsuarioActual = userProfile, // Puede ser null si tu función lo maneja
                    rondasCompletadasEnSesion = rondasCompletadas,
                    tiempoTotalDeLaSesionSegundos = tiempoTotalSegundos,
                    onSuccess = {
                        Log.i(TAG, "Progreso de rutina guardado exitosamente para userID: $userId")
                        _uiState.update { it.copy(successMessage = "¡Progreso guardado!", isSavingProgress = false) }
                        onSuccess()
                    },
                    onError = { errorMsgDb ->
                        Log.e(TAG, "Error al guardar progreso de rutina para userID: $userId. Error: $errorMsgDb")
                        _uiState.update { it.copy(errorMessage = errorMsgDb, isSavingProgress = false) }
                        onError(errorMsgDb)
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
        // routineJob?.cancel() // No se usa un job global de rutina
        sessionTimerJob?.cancel()
        currentCountdownJob?.cancel()
        _uiState.update { it.copy(estado = RoutineExecutionState.FINISHED, tiempoRestante = 0) }
        _soundEvents.emit("routine_finish_sound") // Sonido al finalizar la rutina
        Log.d(TAG, "Rutina finalizada.")
        // La UI debería observar el estado FINISHED para navegar o mostrar resumen.
    }

    override fun onCleared() {
        super.onCleared()
        // routineJob?.cancel()
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