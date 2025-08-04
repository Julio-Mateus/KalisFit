package com.jcmateus.kalisfit.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.guardarProgresoRutina
import com.jcmateus.kalisfit.model.ComponenteEjercicio
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.TipoDeEjercicio
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
    val componenteEjercicioActual: ComponenteEjercicio? = null, // NUEVO
    val indiceComponenteActual: Int = -1, // NUEVO: -1 si no hay componente activo, 0+ si sí
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
    val userProfile: UserProfile? = null
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
    private var ladoAlternadoCompletadoParaSerieActual: Boolean = false
    private var timerJob: Job? = null
    private var initialCountdownJob: Job? = null
    fun exitAndCleanUpRoutine() {
        viewModelScope.launch {
            // Detener cualquier temporizador activo
            timerJob?.cancel()
            timerJob = null
            initialCountdownJob?.cancel()
            initialCountdownJob = null

            // Resetear el estado de la UI a sus valores iniciales
            _uiState.update {
                // Crea una nueva instancia de RoutineUiState con valores por defecto
                // o copia el estado actual y resetea campos específicos.
                // Es más seguro crear una nueva instancia limpia.
                RoutineUiState(
                    // Aquí puedes definir los valores por defecto que consideres.
                    // Por ejemplo, si tienes un constructor por defecto para RoutineUiState, úsalo.
                    // O especifica cada campo:
                    estado = RoutineExecutionState.IDLE,
                    rutina = null,
                    ejercicioActual = null,
                    indiceEjercicioActual = 0,
                    rondaActual = 1,
                    serieActualEjercicio = 1,
                    tiempoRestante = 0,
                    errorMessage = null,
                    successMessage = null,
                    showExitConfirmation = false, // Asegúrate de resetear esto también
                    userProfile = _uiState.value.userProfile, // Mantén el perfil si es necesario para iniciar otra rutina

                    // ... cualquier otro campo que necesite ser reseteado
                )
            }
            // Aquí podrías añadir lógica adicional si necesitas limpiar algo más,
            // como datos en un repositorio temporal o liberar otros recursos.
            // Por ejemplo, si tienes un SoundPlayer que necesita ser liberado:
            // soundPlayer.release()

            // Si quieres emitir un sonido o evento al salir, puedes hacerlo aquí
            // _soundEvents.emit("routine_exited_sound") // Ejemplo
        }
        // Log para depuración
        // Log.d("RoutineViewModel", "exitAndCleanUpRoutine: Rutina limpiada y estado reseteado.")
    }
    private fun startActiveTimerForCurrentStep() {
        currentCountdownJob?.cancel()
        val currentState = _uiState.value
        val currentEjercicio = currentState.ejercicioActual
        val currentComponente = currentState.componenteEjercicioActual

        if (currentState.estado == RoutineExecutionState.IDLE ||
            currentState.estado == RoutineExecutionState.LOADING ||
            currentState.estado == RoutineExecutionState.FINISHED ||
            currentState.estado == RoutineExecutionState.ERROR ||
            currentState.estado == RoutineExecutionState.PAUSED) {
            Log.d(TAG, "startActiveTimerForCurrentStep: No se inicia temporizador para estado ${currentState.estado}")
            return
        }

        var esPorRepeticionesPaso = false
        var duracionPasoSegundos = 0
        var nombrePasoLog: String? = "Paso desconocido"

        if (currentState.estado == RoutineExecutionState.EXERCISE_ACTIVE) {
            if (currentComponente != null) {
                // Métricas del componente
                esPorRepeticionesPaso = !currentComponente.repeticiones.isNullOrEmpty() && (currentComponente.duracionSegundos ?: 0) <= 0
                duracionPasoSegundos = currentComponente.duracionSegundos ?: 0
                nombrePasoLog = currentComponente.nombreEspecifico ?: "Componente sin nombre"
            } else if (currentEjercicio != null) {
                // Métricas del ejercicio principal
                esPorRepeticionesPaso = !currentEjercicio.repeticionesOriginal.isNullOrEmpty() && currentEjercicio.repeticionesOriginal != "0" && currentEjercicio.duracionSegundosOriginal <= 0
                duracionPasoSegundos = currentEjercicio.duracionSegundosOriginal
                nombrePasoLog = currentEjercicio.nombre
            }
        } else {
            // Para INITIAL_COUNTDOWN o DESCANSOS, siempre hay tiempo
            esPorRepeticionesPaso = false
            duracionPasoSegundos = currentState.tiempoRestante // Usamos el tiempo ya asignado
            nombrePasoLog = currentState.estado.name
        }


        if (currentState.estado == RoutineExecutionState.EXERCISE_ACTIVE && esPorRepeticionesPaso) {
            Log.d(TAG, "startActiveTimerForCurrentStep: Paso '$nombrePasoLog' (Ejercicio/Componente) por repeticiones. No se inicia cuenta atrás automática.")
            if (currentState.tiempoRestante != 0) {
                _uiState.update { it.copy(tiempoRestante = 0) }
            }
            return
        }

        if (currentState.tiempoRestante <= 0) {
            // Avanzar si el tiempo es 0 para ciertos estados, o ejercicio por tiempo de 0s
            if (currentState.estado == RoutineExecutionState.INITIAL_COUNTDOWN ||
                (currentState.estado == RoutineExecutionState.EXERCISE_ACTIVE && duracionPasoSegundos == 0 && !esPorRepeticionesPaso) ||
                currentState.estado.name.startsWith("REST")) {
                Log.d(TAG, "startActiveTimerForCurrentStep: Tiempo restante es 0 para $nombrePasoLog (${currentState.estado}), llamando a moveToNextRoutineStep.")
                viewModelScope.launch { moveToNextRoutineStep() }
                return
            } else {
                Log.d(TAG, "startActiveTimerForCurrentStep: Tiempo restante es 0 para $nombrePasoLog (${currentState.estado}), pero no es un caso para avanzar (duracionPaso: $duracionPasoSegundos, esReps: $esPorRepeticionesPaso). No se inicia temporizador.")
                return
            }
        }

        Log.d(TAG, "startActiveTimerForCurrentStep: Iniciando temporizador para $nombrePasoLog (${currentState.estado}) con ${currentState.tiempoRestante}s")

        currentCountdownJob = viewModelScope.launch {
            try {
                var internalRemainingTime = _uiState.value.tiempoRestante
                while (isActive && internalRemainingTime > 0 && _uiState.value.estado == currentState.estado) {
                    delay(1000)
                    if (_uiState.value.estado != RoutineExecutionState.PAUSED) {
                        internalRemainingTime--
                        _uiState.update { it.copy(tiempoRestante = internalRemainingTime) }

                        if (internalRemainingTime in 1..3 && _uiState.value.estado != RoutineExecutionState.EXERCISE_ACTIVE ) { // Solo beep para descansos/cuenta atrás
                            _soundEvents.emit("beep")
                        } else if (internalRemainingTime in 1..3 && _uiState.value.estado == RoutineExecutionState.EXERCISE_ACTIVE && duracionPasoSegundos > 0 && !esPorRepeticionesPaso) {
                            _soundEvents.emit("beep") // Beep para ejercicios por tiempo también
                        }
                    }
                }

                if (isActive && internalRemainingTime == 0 && _uiState.value.estado == currentState.estado && _uiState.value.estado != RoutineExecutionState.PAUSED) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Temporizador finalizado para $nombrePasoLog (${currentState.estado}). Llamando a moveToNextRoutineStep.")
                    moveToNextRoutineStep()
                } else if (!isActive) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Job cancelado para $nombrePasoLog (${currentState.estado})")
                } else if (_uiState.value.estado == RoutineExecutionState.PAUSED) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Pausado durante temporizador para $nombrePasoLog (${currentState.estado}). Tiempo restante ${internalRemainingTime}s.")
                } else if (_uiState.value.estado != currentState.estado) {
                    Log.d(TAG, "startActiveTimerForCurrentStep: Estado cambió de ${currentState.estado} a ${_uiState.value.estado} durante el temporizador para $nombrePasoLog.")
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "startActiveTimerForCurrentStep: Job de temporizador explícitamente cancelado para $nombrePasoLog (${currentState.estado})")
            }
        }
    }
    fun startRoutine(rutinaId: String, userProfile: UserProfile?) { // Asumiendo que necesitas userProfile
        if (userProfile == null) {
            _uiState.update { it.copy(errorMessage = "Perfil de usuario no disponible.", isLoading = false, estado = RoutineExecutionState.ERROR) }
            Log.w(TAG, "startRoutine: Perfil de usuario es nulo.")
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null, rutina = null) }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando rutina completa con ID: $rutinaId")
                val loadedRutina = getRutinaByIdFromFirestore(rutinaId) // Asegúrate que esta función parsea bien
                if (loadedRutina == null) {
                    _uiState.update { it.copy(errorMessage = "Rutina con ID $rutinaId no encontrada.", isLoading = false, estado = RoutineExecutionState.ERROR) }
                    Log.w(TAG, "Rutina con ID $rutinaId no encontrada.")
                } else {
                    _uiState.update {
                        it.copy(
                            rutina = loadedRutina,
                            isLoading = false,
                            rondaActual = 1,
                            indiceEjercicioActual = 0, // Se usará para coger el primer ejercicio
                            serieActualEjercicio = 1,
                            tiempoTotalSesionSegundos = 0,
                            previousState = RoutineExecutionState.IDLE, // Estado previo a iniciar
                            componenteEjercicioActual = null, // Resetear al iniciar rutina
                            indiceComponenteActual = -1,    // Resetear al iniciar rutina
                            showExitConfirmation = false,
                            userProfile = userProfile // Guardar perfil
                        )
                    }
                    // Resetear el estado de lado alternado para la nueva rutina
                    ladoAlternadoCompletadoParaSerieActual = false
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

        // Comprobar si estamos reanudando una cuenta atrás inicial que fue pausada
        if (currentUiState.previousState == RoutineExecutionState.INITIAL_COUNTDOWN &&
            _uiState.value.estado == RoutineExecutionState.INITIAL_COUNTDOWN && // Ya restaurado por togglePause
            currentUiState.tiempoRestante > 0) {
            Log.d(TAG, "Reanudando INITIAL_COUNTDOWN con ${currentUiState.tiempoRestante}s restantes.")
            tiempoDeCuentaAtras = currentUiState.tiempoRestante
            // No emitir sonido de inicio si estamos reanudando
        } else {
            Log.d(TAG, "Iniciando NUEVO INITIAL_COUNTDOWN.")
            tiempoDeCuentaAtras = tiempoCuentaAtrasInicialGlobal // Usa tu constante global
            // Solo emitir sonido de inicio si no es una reanudación desde pausa Y el estado anterior no era pausa
            if (currentUiState.previousState != RoutineExecutionState.PAUSED) {
                viewModelScope.launch { _soundEvents.emit("start_sound") }
            }
        }

        _uiState.update {
            it.copy(
                estado = RoutineExecutionState.INITIAL_COUNTDOWN,
                tiempoRestante = tiempoDeCuentaAtras
                // previousState ya está gestionado o se gestionará en togglePause
            )
        }
        startActiveTimerForCurrentStep()
    }
    private suspend fun moveToNextRoutineStep() {
        currentCountdownJob?.cancel()
        Log.d(TAG, "moveToNextRoutineStep: Job de cuenta atrás anterior cancelado.")

        val currentUiState = _uiState.value
        val rutina = currentUiState.rutina ?: run {
            Log.e(TAG, "moveToNextRoutineStep: Error interno - Rutina no disponible.")
            finishRoutineWithError("Error interno: Rutina no disponible.")
            return
        }
        var ejercicioActualLoop = currentUiState.ejercicioActual
        val componenteActualLoop = currentUiState.componenteEjercicioActual
        val indiceComponenteLoop = currentUiState.indiceComponenteActual

        Log.i(TAG, "moveToNextRoutineStep: ================== INICIO ==================")
        Log.i(TAG, "moveToNextRoutineStep: Procesando desde ESTADO=${currentUiState.estado}, EjIdx=${currentUiState.indiceEjercicioActual}, CompIdx=${indiceComponenteLoop}, Serie=${currentUiState.serieActualEjercicio}, Ronda=${currentUiState.rondaActual}")
        Log.i(TAG, "moveToNextRoutineStep: EjActual=${ejercicioActualLoop?.nombre}, CompActual=${componenteActualLoop?.nombreEspecifico}, LadoAlternadoCompletado=${ladoAlternadoCompletadoParaSerieActual}")

        when (currentUiState.estado) {
            RoutineExecutionState.INITIAL_COUNTDOWN -> {
                Log.d(TAG, "INITIAL_COUNTDOWN finalizado. Preparando primer ejercicio.")
                if (rutina.ejercicios.isEmpty()) {
                    Log.w(TAG, "moveToNextRoutineStep: La rutina no tiene ejercicios.")
                    finishRoutineWithError("La rutina no tiene ejercicios.")
                    return
                }
                val primerEjercicio = rutina.ejercicios.first()
                _uiState.update {
                    it.copy(
                        ejercicioActual = primerEjercicio,
                        indiceEjercicioActual = 0,
                        serieActualEjercicio = 1,
                        rondaActual = 1,
                        componenteEjercicioActual = null,
                        indiceComponenteActual = -1 // Asegurar reseteo para el primer ejercicio
                    )
                }
                ladoAlternadoCompletadoParaSerieActual = false // Reset para el primer ejercicio
                prepareAndStartExerciseStep()
            }

            RoutineExecutionState.EXERCISE_ACTIVE -> {
                Log.d(TAG, "EXERCISE_ACTIVE finalizado/saltado para Ej: ${ejercicioActualLoop?.nombre}, Comp: ${componenteActualLoop?.nombreEspecifico}.")
                ejercicioActualLoop = ejercicioActualLoop ?: rutina.ejercicios.getOrNull(currentUiState.indiceEjercicioActual)

                if (ejercicioActualLoop == null) {
                    Log.e(TAG, "moveToNextRoutineStep: Error - Ejercicio actual nulo al finalizar EXERCISE_ACTIVE. Estado: $currentUiState")
                    finishRoutineWithError("Error: Ejercicio actual no encontrado.")
                    return
                }

                var avanzarASiguienteFasePrincipal = false // Sea serie, ejercicio o ronda

                // 1. Manejo de POR_LADO_ALTERNADO
                if (ejercicioActualLoop.tipoEjercicio == TipoDeEjercicio.POR_LADO_ALTERNADO &&
                    ejercicioActualLoop.esUnilateral &&
                    !ladoAlternadoCompletadoParaSerieActual) {
                    Log.d(TAG, "moveToNextRoutineStep: Lado 1 de '${ejercicioActualLoop.nombre}' (POR_LADO_ALTERNADO) completado. Preparando lado 2.")
                    ladoAlternadoCompletadoParaSerieActual = true
                    // El ejercicio actual, serie, ronda, etc., NO cambian.
                    // indiceComponenteActual y componenteEjercicioActual también deberían ser los mismos (usualmente nulos para POR_LADO_ALTERNADO simple)
                    // Se re-prepara el MISMO paso de ejercicio.
                    prepareAndStartExerciseStep()
                    return // Fin de este moveToNextRoutineStep, el siguiente se manejará después del segundo lado.
                }
                // Si llegamos aquí para POR_LADO_ALTERNADO, el segundo lado (o el único si no es unilateral) se completó.

                // 2. Manejo de Componentes (SUPERSET_SEQUENCIAL, CIRCUITO_TEMPORIZADO)
                if ((ejercicioActualLoop.tipoEjercicio == TipoDeEjercicio.SUPERSET_SEQUENCIAL ||
                            ejercicioActualLoop.tipoEjercicio == TipoDeEjercicio.CIRCUITO_TEMPORIZADO) &&
                    ejercicioActualLoop.componentes.isNotEmpty()) {

                    // Si componenteActualLoop es null, significa que estamos empezando el primer componente (o hubo un error)
                    // Si indiceComponenteLoop es -1, también significa que es el primero.
                    val proximoIndiceComponente = if (indiceComponenteLoop == -1) 0 else indiceComponenteLoop + 1

                    if (proximoIndiceComponente < ejercicioActualLoop.componentes.size) {
                        Log.d(TAG, "Pasando al siguiente componente (índice $proximoIndiceComponente) de '${ejercicioActualLoop.nombre}'.")
                        _uiState.update { it.copy(
                            // ejercicioActual, serieActual, rondaActual se mantienen
                            indiceComponenteActual = proximoIndiceComponente // Preparamos el índice para el siguiente prepareAndStart
                            // componenteEjercicioActual se actualizará en prepareAndStartExerciseStep
                        )}
                        prepareAndStartExerciseStep()
                    } else {
                        Log.d(TAG, "Todos los componentes de '${ejercicioActualLoop.nombre}' completados para la serie actual.")
                        _uiState.update { it.copy(componenteEjercicioActual = null, indiceComponenteActual = -1) } // Limpiar
                        avanzarASiguienteFasePrincipal = true
                    }
                } else {
                    // Si no era un ejercicio con componentes, o ya se completaron todos.
                    Log.d(TAG, "Ejercicio SIMPLE o último componente de '${ejercicioActualLoop.nombre}' finalizado.")
                    avanzarASiguienteFasePrincipal = true
                }

                if (avanzarASiguienteFasePrincipal) {
                    Log.d(TAG, "Fase de ejercicio/componentes de '${ejercicioActualLoop.nombre}' (Serie ${currentUiState.serieActualEjercicio}/${ejercicioActualLoop.numeroDeSeries}) finalizada. Evaluando siguiente paso principal.")
                    ladoAlternadoCompletadoParaSerieActual = false // Reset para la próxima serie o el próximo ejercicio.

                    // Siguiente SERIE del mismo ejercicio
                    if (currentUiState.serieActualEjercicio < ejercicioActualLoop.numeroDeSeries) {
                        Log.d(TAG, "Pasando a la siguiente serie del ejercicio '${ejercicioActualLoop.nombre}'.")
                        if (ejercicioActualLoop.descansoEntreSeriesSegundos > 0) {
                            Log.d(TAG, "Iniciando REST_BETWEEN_SETS de ${ejercicioActualLoop.descansoEntreSeriesSegundos}s.")
                            viewModelScope.launch { _soundEvents.emit("rest_start") }
                            _uiState.update {
                                it.copy(
                                    estado = RoutineExecutionState.REST_BETWEEN_SETS,
                                    tiempoRestante = ejercicioActualLoop.descansoEntreSeriesSegundos
                                    // componente e índice se resetearán DESPUÉS del descanso, en el case de REST_BETWEEN_SETS
                                )
                            }
                            startActiveTimerForCurrentStep()
                        } else {
                            Log.d(TAG, "Sin descanso entre series. Iniciando siguiente serie directamente.")
                            _uiState.update {
                                it.copy(
                                    serieActualEjercicio = it.serieActualEjercicio + 1,
                                    componenteEjercicioActual = null, // Reset para nueva serie
                                    indiceComponenteActual = -1
                                )
                            }
                            prepareAndStartExerciseStep() // ladoAlternado ya reseteado
                        }
                    }
                    // Siguiente EJERCICIO de la misma ronda
                    else if (currentUiState.indiceEjercicioActual < rutina.ejercicios.size - 1) {
                        Log.d(TAG, "Todas las series de '${ejercicioActualLoop.nombre}' completadas. Pasando al siguiente ejercicio.")
                        if (ejercicioActualLoop.descansoDespuesEjercicioSegundos > 0) {
                            // LOGS ADICIONALES
                            Log.d(TAG, "DEBUG: Ejercicio actual: ${ejercicioActualLoop.nombre}")
                            Log.d(TAG, "DEBUG: descansoEntreSeriesSegundos REAL: ${ejercicioActualLoop.descansoEntreSeriesSegundos}")
                            Log.d(TAG, "DEBUG: descansoDespuesEjercicioSegundos REAL: ${ejercicioActualLoop.descansoDespuesEjercicioSegundos}")
                            Log.d(TAG, "DEBUG: Se usará para tiempoRestante (REST_BETWEEN_EXERCISES): ${ejercicioActualLoop.descansoDespuesEjercicioSegundos}")
                            // FIN LOGS ADICIONALES
                            Log.d(TAG, "Iniciando REST_BETWEEN_EXERCISES de ${ejercicioActualLoop.descansoDespuesEjercicioSegundos}s.")
                            viewModelScope.launch { _soundEvents.emit("rest_start") }
                            _uiState.update {
                                it.copy(
                                    estado = RoutineExecutionState.REST_BETWEEN_EXERCISES,
                                    tiempoRestante = ejercicioActualLoop.descansoDespuesEjercicioSegundos
                                )
                            }
                            startActiveTimerForCurrentStep()
                        } else {
                            Log.d(TAG, "Sin descanso después de ejercicio. Iniciando siguiente ejercicio directamente.")
                            // Avanzar al siguiente ejercicio (lógica ahora en el case de REST_BETWEEN_EXERCISES o aquí si no hay descanso)
                            val nuevoIndice = currentUiState.indiceEjercicioActual + 1
                            val proximoEjercicio = rutina.ejercicios.getOrNull(nuevoIndice) ?: run {
                                finishRoutineWithError("Error: Próximo ejercicio nulo.")
                                return
                            }
                            _uiState.update {
                                it.copy(
                                    indiceEjercicioActual = nuevoIndice,
                                    serieActualEjercicio = 1, // Reset serie
                                    ejercicioActual = proximoEjercicio,
                                    componenteEjercicioActual = null, // Reset componente
                                    indiceComponenteActual = -1
                                )
                            }
                            prepareAndStartExerciseStep() // ladoAlternado ya reseteado
                        }
                    }
                    // Siguiente RONDA
                    else if (currentUiState.rondaActual < rutina.numeroDeRondas) {
                        Log.d(TAG, "Último ejercicio de la ronda ${currentUiState.rondaActual} completado. Pasando a la siguiente ronda.")
                        if (rutina.descansoEntreRondasSegundos > 0) {
                            Log.d(TAG, "Iniciando REST_BETWEEN_ROUNDS de ${rutina.descansoEntreRondasSegundos}s.")
                            viewModelScope.launch { _soundEvents.emit("rest_start") }
                            _uiState.update {
                                it.copy(
                                    estado = RoutineExecutionState.REST_BETWEEN_ROUNDS,
                                    tiempoRestante = rutina.descansoEntreRondasSegundos
                                )
                            }
                            startActiveTimerForCurrentStep()
                        } else {
                            Log.d(TAG, "Sin descanso entre rondas. Iniciando siguiente ronda directamente.")
                            // Avanzar a la siguiente ronda (lógica ahora en el case de REST_BETWEEN_ROUNDS o aquí)
                            val primerEjercicioSiguienteRonda = rutina.ejercicios.firstOrNull() ?: run {
                                finishRoutineWithError("Error: No hay ejercicios para la siguiente ronda.")
                                return
                            }
                            _uiState.update {
                                it.copy(
                                    rondaActual = it.rondaActual + 1,
                                    indiceEjercicioActual = 0, // Reset
                                    serieActualEjercicio = 1, // Reset
                                    ejercicioActual = primerEjercicioSiguienteRonda,
                                    componenteEjercicioActual = null, // Reset
                                    indiceComponenteActual = -1
                                )
                            }
                            prepareAndStartExerciseStep() // ladoAlternado ya reseteado
                        }
                    }
                    // FIN de la rutina
                    else {
                        Log.i(TAG, "Todas las series, ejercicios y rondas completadas. Finalizando rutina.")
                        finishRoutine()
                    }
                }
            } // Fin de EXERCISE_ACTIVE

            RoutineExecutionState.REST_BETWEEN_SETS -> {
                Log.d(TAG, "REST_BETWEEN_SETS finalizado para ejercicio '${currentUiState.ejercicioActual?.nombre}'.")
                viewModelScope.launch { _soundEvents.emit("rest_end") }

                _uiState.update {
                    it.copy(
                        serieActualEjercicio = it.serieActualEjercicio + 1,
                        componenteEjercicioActual = null, // Reset para nueva serie
                        indiceComponenteActual = -1      // y su índice
                    )
                }
                ladoAlternadoCompletadoParaSerieActual = false // Reset para la nueva serie
                prepareAndStartExerciseStep()
            }

            RoutineExecutionState.REST_BETWEEN_EXERCISES -> {
                Log.d(TAG, "REST_BETWEEN_EXERCISES finalizado.")
                viewModelScope.launch { _soundEvents.emit("rest_end") }

                val nuevoIndice = currentUiState.indiceEjercicioActual + 1
                val proximoEjercicio = rutina.ejercicios.getOrNull(nuevoIndice) ?: run {
                    Log.e(TAG, "moveToNextRoutineStep: Error - No se encontró el próximo ejercicio (índice: $nuevoIndice) después de REST_BETWEEN_EXERCISES.")
                    finishRoutineWithError("Error: Próximo ejercicio no encontrado.")
                    return
                }
                _uiState.update {
                    it.copy(
                        indiceEjercicioActual = nuevoIndice,
                        serieActualEjercicio = 1, // Reset serie
                        ejercicioActual = proximoEjercicio,
                        componenteEjercicioActual = null, // Reset componente
                        indiceComponenteActual = -1
                    )
                }
                ladoAlternadoCompletadoParaSerieActual = false // Reset para el nuevo ejercicio
                prepareAndStartExerciseStep()
            }

            RoutineExecutionState.REST_BETWEEN_ROUNDS -> {
                Log.d(TAG, "REST_BETWEEN_ROUNDS finalizado.")
                viewModelScope.launch { _soundEvents.emit("rest_end") }

                val proximaRonda = currentUiState.rondaActual + 1
                // La condición de si hay más rondas ya se chequeó antes de entrar a este descanso.
                val primerEjercicioDeRonda = rutina.ejercicios.firstOrNull() ?: run {
                    Log.e(TAG, "moveToNextRoutineStep: Error - No hay ejercicios para la siguiente ronda ($proximaRonda) después de REST_BETWEEN_ROUNDS.")
                    finishRoutineWithError("Error: No hay ejercicios para la siguiente ronda.")
                    return
                }
                _uiState.update {
                    it.copy(
                        rondaActual = proximaRonda,
                        indiceEjercicioActual = 0, // Reset
                        serieActualEjercicio = 1, // Reset
                        ejercicioActual = primerEjercicioDeRonda,
                        componenteEjercicioActual = null, // Reset
                        indiceComponenteActual = -1
                    )
                }
                ladoAlternadoCompletadoParaSerieActual = false // Reset para la nueva ronda
                prepareAndStartExerciseStep()
            }
            RoutineExecutionState.PAUSED,
            RoutineExecutionState.IDLE,
            RoutineExecutionState.LOADING,
            RoutineExecutionState.FINISHED,
            RoutineExecutionState.ERROR -> {
                Log.w(TAG, "moveToNextRoutineStep llamado desde un estado no procesable o ya final: ${currentUiState.estado}.")
            }
        }
        Log.i(TAG, "moveToNextRoutineStep: =================== FIN ====================")
    }
    private fun prepareAndStartExerciseStep() {
        val stateBeforeCall = _uiState.value
        var ejercicioParaPreparar = stateBeforeCall.ejercicioActual

        Log.d(TAG, "prepareAndStartExerciseStep: ================== INICIO (Entrada) ==================")
        Log.d(TAG, "prepareAndStartExerciseStep: Estado ENTRADA: ${stateBeforeCall.estado}, Ej: ${ejercicioParaPreparar?.nombre}, Serie: ${stateBeforeCall.serieActualEjercicio}, CompIdxIndicado: ${stateBeforeCall.indiceComponenteActual}, LadoAlternado: $ladoAlternadoCompletadoParaSerieActual")

        if (ejercicioParaPreparar == null) {
            Log.e(TAG, "prepareAndStartExerciseStep: Error - Ejercicio nulo. Estado de entrada: ${stateBeforeCall.estado}")
            finishRoutineWithError("Error: Ejercicio nulo al preparar.")
            return
        }

        var tiempoParaPaso: Int
        var esPorRepeticionesPaso: Boolean
        var nombrePasoLog: String = ejercicioParaPreparar.nombre // Default
        var isResumingTimedPaso = false
        var componenteQueSePrepara: ComponenteEjercicio? = null
        var indiceComponenteASetearEnState = stateBeforeCall.indiceComponenteActual // Por defecto, el que ya está

        // A. Determinar si estamos REANUDANDO un paso específico por TIEMPO
        if (stateBeforeCall.previousState == RoutineExecutionState.EXERCISE_ACTIVE &&
            stateBeforeCall.estado == RoutineExecutionState.EXERCISE_ACTIVE && // Estado actual ya es EXERCISE_ACTIVE (restaurado por togglePausa)
            stateBeforeCall.tiempoRestante > 0) {

            val componentePausado = stateBeforeCall.componenteEjercicioActual
            if (componentePausado != null && (componentePausado.duracionSegundos ?: 0) > 0) {
                isResumingTimedPaso = true
                componenteQueSePrepara = componentePausado
                nombrePasoLog = componentePausado.nombreEspecifico ?: ejercicioParaPreparar.nombre
                Log.d(TAG, "prepareAndStartExerciseStep: Reanudando COMPONENTE POR TIEMPO '${nombrePasoLog}' con ${stateBeforeCall.tiempoRestante}s.")
            } else if (componentePausado == null && ejercicioParaPreparar.duracionSegundosOriginal > 0 && (ejercicioParaPreparar.repeticionesOriginal.isNullOrEmpty() || ejercicioParaPreparar.repeticionesOriginal == "0")) {
                isResumingTimedPaso = true
                nombrePasoLog = ejercicioParaPreparar.nombre
                Log.d(TAG, "prepareAndStartExerciseStep: Reanudando EJERCICIO PRINCIPAL POR TIEMPO '${nombrePasoLog}' con ${stateBeforeCall.tiempoRestante}s.")
            }
        }

        // B. Configurar el paso (nuevo o reanudado)
        if (isResumingTimedPaso) {
            tiempoParaPaso = stateBeforeCall.tiempoRestante
            esPorRepeticionesPaso = false // Si se reanuda, era por tiempo
            // componenteQueSePrepara ya está seteado si es un componente.
            // indiceComponenteASetearEnState se mantiene como el del componente pausado.
        } else {
            // Es un inicio NUEVO de un paso (ejercicio o componente)
            // Resetear previousState ya que no estamos reanudando
            _uiState.update { it.copy(previousState = RoutineExecutionState.IDLE) }

            // B1. Manejo de POR_LADO_ALTERNADO (tiene prioridad para mostrar el "lado")
            if (ejercicioParaPreparar.tipoEjercicio == TipoDeEjercicio.POR_LADO_ALTERNADO &&
                ejercicioParaPreparar.esUnilateral &&
                ladoAlternadoCompletadoParaSerieActual) {
                // Este es el SEGUNDO lado. La UI debe indicarlo.
                // Las métricas (tiempo/reps) son las mismas que el primer lado.
                Log.d(TAG, "prepareAndStartExerciseStep: Preparando SEGUNDO LADO de '${ejercicioParaPreparar.nombre}'.")
            }

            // B2. Determinar si es un COMPONENTE o el ejercicio principal
            if ((ejercicioParaPreparar.tipoEjercicio == TipoDeEjercicio.SUPERSET_SEQUENCIAL ||
                        ejercicioParaPreparar.tipoEjercicio == TipoDeEjercicio.CIRCUITO_TEMPORIZADO) &&
                ejercicioParaPreparar.componentes.isNotEmpty()) {

                // Si indiceComponenteActual es -1 (inicio de superset) o un índice válido.
                // moveToNextStep ya debería haber incrementado indiceComponenteActual para el siguiente.
                // Si venimos de un REST_BETWEEN_SETS, indiceComponenteActual debería ser -1, y aquí lo ponemos a 0.
                val indiceDelComponenteAUsar = if (stateBeforeCall.indiceComponenteActual == -1) 0 else stateBeforeCall.indiceComponenteActual

                componenteQueSePrepara = ejercicioParaPreparar.componentes.getOrNull(indiceDelComponenteAUsar)
                indiceComponenteASetearEnState = indiceDelComponenteAUsar // Aseguramos que el state refleje el componente actual

                if (componenteQueSePrepara != null) {
                    nombrePasoLog = componenteQueSePrepara.nombreEspecifico ?: ejercicioParaPreparar.nombre
                    if ((componenteQueSePrepara.duracionSegundos ?: 0) > 0) {
                        tiempoParaPaso = componenteQueSePrepara.duracionSegundos!!
                        esPorRepeticionesPaso = false
                    } else { // Por repeticiones
                        tiempoParaPaso = 0
                        esPorRepeticionesPaso = true
                    }
                    Log.d(TAG, "prepareAndStartExerciseStep: Preparando NUEVO COMPONENTE '${nombrePasoLog}' (Índice $indiceDelComponenteAUsar). Tiempo: $tiempoParaPaso, EsReps: $esPorRepeticionesPaso")
                } else { // No debería pasar si la lógica de moveToNext es correcta
                    Log.e(TAG, "prepareAndStartExerciseStep: Error - Se esperaba un componente para ${ejercicioParaPreparar.nombre} en índice $indiceDelComponenteAUsar pero no se encontró. Volviendo al ejercicio principal.")
                    // Fallback a ejercicio principal si el componente es nulo (error de lógica/datos)
                    tiempoParaPaso = ejercicioParaPreparar.duracionSegundosOriginal
                    esPorRepeticionesPaso = !ejercicioParaPreparar.repeticionesOriginal.isNullOrEmpty() && ejercicioParaPreparar.repeticionesOriginal != "0" && ejercicioParaPreparar.duracionSegundosOriginal <= 0
                    nombrePasoLog = ejercicioParaPreparar.nombre
                    componenteQueSePrepara = null // Asegurar que no hay componente
                    indiceComponenteASetearEnState = -1
                }
            } else {
                // Ejercicio SIMPLE, CON_TEMPO, POR_LADO_ALTERNADO (primer lado), COMBINADO_TEMPORIZADO
                // O un superset/circuito sin componentes definidos (tratar como simple)
                componenteQueSePrepara = null // No hay componente activo
                indiceComponenteASetearEnState = -1 // Resetear índice de componente

                if (ejercicioParaPreparar.duracionSegundosOriginal > 0) {
                    tiempoParaPaso = ejercicioParaPreparar.duracionSegundosOriginal
                    esPorRepeticionesPaso = false
                    if (!ejercicioParaPreparar.repeticionesOriginal.isNullOrEmpty() && ejercicioParaPreparar.repeticionesOriginal != "0") {
                        // Es un ejercicio con tiempo Y repeticiones (ej. AMRAP en X tiempo). La UI lo manejará.
                        // El temporizador correrá por `duracionSegundosOriginal`.
                        Log.d(TAG, "prepareAndStartExerciseStep: Ejercicio '${ejercicioParaPreparar.nombre}' con tiempo ($tiempoParaPaso s) Y repeticiones (${ejercicioParaPreparar.repeticionesOriginal}).")
                    } else {
                        Log.d(TAG, "prepareAndStartExerciseStep: Preparando ejercicio POR TIEMPO '${ejercicioParaPreparar.nombre}'. Duración: ${tiempoParaPaso}s.")
                    }
                } else { // Por repeticiones
                    tiempoParaPaso = 0
                    esPorRepeticionesPaso = true
                    Log.d(TAG, "prepareAndStartExerciseStep: Preparando ejercicio POR REPETICIONES '${ejercicioParaPreparar.nombre}'. Reps: ${ejercicioParaPreparar.repeticionesOriginal}.")
                }
                nombrePasoLog = ejercicioParaPreparar.nombre
            }
        }

        Log.d(TAG, "prepareAndStartExerciseStep: Final Calculado: nombrePaso=$nombrePasoLog, tiempoPaso=$tiempoParaPaso, esReps=$esPorRepeticionesPaso, isResuming=$isResumingTimedPaso, compAct=${componenteQueSePrepara?.nombreEspecifico}, indiceCompASetear=$indiceComponenteASetearEnState")

        _uiState.update {
            it.copy(
                estado = RoutineExecutionState.EXERCISE_ACTIVE,
                tiempoRestante = tiempoParaPaso,
                ejercicioActual = ejercicioParaPreparar, // Puede ser redundante si no cambió, pero asegura consistencia
                componenteEjercicioActual = componenteQueSePrepara,
                indiceComponenteActual = indiceComponenteASetearEnState
                // previousState se reseteó arriba si no es reanudación
            )
        }

        if (!isResumingTimedPaso) { // Solo emitir sonido de inicio si no estamos reanudando este mismo paso
            Log.d(TAG, "prepareAndStartExerciseStep: Emitiendo sonido 'exercise_start' para $nombrePasoLog.")
            viewModelScope.launch { _soundEvents.emit("exercise_start") }
        } else {
            Log.d(TAG, "prepareAndStartExerciseStep: NO emitiendo sonido 'exercise_start' (reanudando $nombrePasoLog).")
        }

        Log.d(TAG, "prepareAndStartExerciseStep: Llamando a startActiveTimerForCurrentStep().")
        Log.d(TAG, "prepareAndStartExerciseStep: =================== FIN (Salida) ====================")
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