package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Define una clase para el estado de la UI de esta pantalla
// Define una clase para el estado de la UI de esta pantalla
data class RoutineDetailUiState(
    val rutina: Rutina? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class RoutineDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "RoutineDetailViewModel"
    private val rutinaId: String // Declaración sin inicialización inmediata

    // StateFlows para la UI y eventos de navegación
    private val _uiState = MutableStateFlow(RoutineDetailUiState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private val _navigateToEditRoutine = MutableStateFlow<UserCustomRoutine?>(null)
    val navigateToEditRoutine: StateFlow<UserCustomRoutine?> = _navigateToEditRoutine.asStateFlow()

    private val _startRoutineExecution = MutableStateFlow<String?>(null) // Debe ser String?
    val startRoutineExecution: StateFlow<String?> = _startRoutineExecution.asStateFlow()

    init {
        // Obtener el ID de la rutina desde SavedStateHandle
        val idFromHandle: String? = savedStateHandle.get<String>(Routes.Args.ROUTINE_ID_ARG) // Usa la constante correcta

        if (idFromHandle != null && idFromHandle.isNotBlank()) {
            this.rutinaId = idFromHandle
            Log.d(TAG, "ViewModel inicializado. rutinaId obtenido: '$idFromHandle'")
            loadRoutineDetails(this.rutinaId) // Llama a loadRoutineDetails con el ID asignado
        } else {
            val errorMessage = "Argumento '${Routes.Args.ROUTINE_ID_ARG}' no encontrado o inválido en SavedStateHandle para RoutineDetailViewModel"
            Log.e(TAG, errorMessage)
            _uiState.value = RoutineDetailUiState(
                isLoading = false,
                errorMessage = "ID de rutina no válido." // Mensaje amigable para el usuario
            )
            // Considera el manejo de errores: Si quieres que la app crashee, mantén el throw.
            // Si prefieres que el ViewModel exista en un estado de error, puedes quitar el throw.
            // Sin embargo, este ViewModel depende críticamente del rutinaId.
            throw IllegalArgumentException(errorMessage)
        }
    }

    // ELIMINA EL SEGUNDO BLOQUE init DESDE AQUÍ
    /*
    init {
        // ... contenido del segundo bloque init que estaba causando problemas ...
    }
    */
    // HASTA AQUÍ

    private fun loadRoutineDetails(idRutina: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            Log.d(TAG, "Cargando detalles para la rutina ID: $idRutina")

            try {
                val rutinaCargada = getRutinaByIdFromFirestore(idRutina)

                if (rutinaCargada != null) {
                    Log.d(TAG, "Rutina '${rutinaCargada.nombre}' cargada con ${rutinaCargada.ejercicios.size} ejercicios.")
                    _uiState.value = RoutineDetailUiState(
                        rutina = rutinaCargada,
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    Log.w(TAG, "No se encontró la rutina con ID: $idRutina o falló la carga.")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        rutina = null,
                        errorMessage = "Rutina no encontrada."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al cargar detalles de la rutina con ID: $idRutina", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    rutina = null,
                    errorMessage = "Error al cargar la rutina: ${e.localizedMessage}"
                )
            }
        }
    }

    fun onIniciarRutinaClicked() {
        _uiState.value.rutina?.let { rutinaActual ->
            Log.d(TAG, "Solicitando inicio de rutina con ID: ${rutinaActual.id}")
            _startRoutineExecution.value = rutinaActual.id // Emitir SOLO el ID
        }
    }

    fun onRutinaExecutionStarted() {
        _startRoutineExecution.value = null
    }

    fun onPersonalizarRutinaClicked(userId: String) {
        val plantillaOriginal = _uiState.value.rutina
        if (plantillaOriginal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No se puede personalizar, rutina no cargada.")
            Log.w(TAG, "Intento de personalizar rutina nula.")
            return
        }

        if (userId.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Debes iniciar sesión para personalizar.")
            Log.w(TAG, "Usuario no autenticado, no se puede personalizar rutina.")
            return
        }

        Log.d(TAG, "Iniciando personalización para la rutina plantilla ID: ${plantillaOriginal.id} por usuario $userId")

        val nuevaRutinaParaPersonalizar = UserCustomRoutine(
            id = UUID.randomUUID().toString(), // Asegúrate de que UserCustomRoutine tenga 'id' como var si lo asignas aquí
            userId = userId,
            nombrePersonalizado = plantillaOriginal.nombre,
            descripcion = plantillaOriginal.descripcion,
            imagenUrl = plantillaOriginal.imagenUrl,
            ejercicios = plantillaOriginal.ejercicios.map { ejercicioOriginal ->
                // Realiza una copia profunda si es necesario
                ejercicioOriginal.copy(
                    componentes = ejercicioOriginal.componentes.map { componente -> componente.copy() }
                )
            },
            numeroDeRondas = plantillaOriginal.numeroDeRondas,
            descansoEntreRondasSegundos = plantillaOriginal.descansoEntreRondasSegundos,
            nivelRecomendado = plantillaOriginal.nivelRecomendado.toList(),
            objetivos = plantillaOriginal.objetivos.toList(),
            lugarEntrenamiento = plantillaOriginal.lugarEntrenamiento.toList(), // Asegúrate de que los tipos coincidan
            originalTemplateId = plantillaOriginal.id,
            fechaCreacion = Timestamp.now(),
            fechaUltimaModificacion = Timestamp.now()
        )

        _navigateToEditRoutine.value = nuevaRutinaParaPersonalizar
        Log.d(TAG, "UserCustomRoutine creada para edición: ${nuevaRutinaParaPersonalizar.nombrePersonalizado}")
    }

    fun onNavigationToEditRoutineDone() {
        _navigateToEditRoutine.value = null
    }


    fun refreshRoutineDetails() {
        // Si llegamos aquí, y el init se completó sin lanzar una excepción,
        // rutinaId ya está inicializado y no está en blanco.
        // La comprobación isNotBlank() es una salvaguarda adicional,
        // pero teóricamente no es estrictamente necesaria debido a la lógica del init.
        if (rutinaId.isNotBlank()) {
            Log.d(TAG, "Refrescando detalles para rutinaId: $rutinaId")
            loadRoutineDetails(rutinaId)
        } else {
            // Este caso solo ocurriría si modificaras el init para NO lanzar una excepción
            // y permitieras que rutinaId se quede vacío o nulo.
            Log.w(TAG, "Intento de refrescar detalles pero rutinaId está en blanco. Esto no debería ocurrir si el init lanzó excepción por ID inválido.")
            _uiState.value = _uiState.value.copy(
                isLoading = false, // Podrías querer mantener isLoading como estaba o ponerlo en false
                errorMessage = "No se puede refrescar: ID de rutina no disponible."
            )
        }
    }
}
