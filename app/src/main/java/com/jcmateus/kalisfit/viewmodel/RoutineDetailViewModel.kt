package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.getUserCustomRoutineById
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val userId: String?
    // StateFlows para la UI y eventos de navegación
    private val _uiState = MutableStateFlow(RoutineDetailUiState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()
    private val _navigateToEditRoutine = MutableStateFlow<UserCustomRoutine?>(null)
    val navigateToEditRoutine: StateFlow<UserCustomRoutine?> = _navigateToEditRoutine.asStateFlow()
    private val _startRoutineExecution = MutableStateFlow<String?>(null) // Debe ser String?
    val startRoutineExecution: StateFlow<String?> = _startRoutineExecution.asStateFlow()
    init {
        val idFromHandle: String? = savedStateHandle.get<String>(Routes.Args.ROUTINE_ID_ARG)
        // Obtener el userId, podría ser nulo si es una plantilla
        this.userId = savedStateHandle.get<String>(Routes.Args.USER_ID_ARG)

        if (idFromHandle != null && idFromHandle.isNotBlank()) {
            this.rutinaId = idFromHandle
            Log.d(TAG, "ViewModel inicializado. rutinaId: '${this.rutinaId}', userId: '${this.userId}'")
            loadRoutineDetails(this.rutinaId, this.userId)
        } else {
            val errorMsg = "Argumento '${Routes.Args.ROUTINE_ID_ARG}' no encontrado o inválido."
            Log.e(TAG, errorMsg)
            _uiState.value = RoutineDetailUiState(isLoading = false, errorMessage = "ID de rutina no válido.")
            throw IllegalArgumentException(errorMsg)
        }
    }
    fun addGlobalToMyRoutines(userId: String) {
        val globalRoutine = _uiState.value.rutina ?: return
        viewModelScope.launch {
            try {val myNewRoutine = UserCustomRoutine(
                id = UUID.randomUUID().toString(),
                userId = userId,
                nombrePersonalizado = globalRoutine.nombre,
                descripcion = globalRoutine.descripcion,
                imagenUrl = globalRoutine.imagenUrl,
                ejercicios = globalRoutine.ejercicios,
                numeroDeRondas = globalRoutine.numeroDeRondas,
                descansoEntreRondasSegundos = globalRoutine.descansoEntreRondasSegundos,
                originalTemplateId = globalRoutine.id
            )

                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .collection("customRoutines").document(myNewRoutine.id)
                    .set(myNewRoutine).await()

                // Aquí puedes actualizar un estado para mostrar un Toast de éxito
            } catch (e: Exception) {
                Log.e("Detail", "Error al clonar rutina", e)
            }
        }
    }
    private fun loadRoutineDetails(idRutina: String, currentUserId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            Log.d(TAG, "Cargando detalles para rutina ID: $idRutina, Usuario ID: $currentUserId")

            try {
                var rutinaCargada: Rutina? = null // Inicializar a null

                // 1. Si hay un userId, INTENTAR cargar como UserCustomRoutine primero
                if (currentUserId != null && currentUserId.isNotBlank()) {
                    Log.d(TAG, "Intentando cargar como UserCustomRoutine...")
                    val customRoutine = getUserCustomRoutineById(currentUserId, idRutina)
                    if (customRoutine != null) {
                        Log.d(TAG, "UserCustomRoutine '${customRoutine.nombrePersonalizado}' cargada.")
                        rutinaCargada = mapUserCustomRoutineToRutina(customRoutine)
                    } else {
                        Log.w(TAG, "No se encontró UserCustomRoutine con ID: $idRutina para usuario: $currentUserId. Se intentará como plantilla.")
                        // NO establezcas rutinaCargada a null aquí explícitamente, ya lo está.
                        // Simplemente deja que continúe para intentar como plantilla.
                    }
                }
                // 2. Si NO se cargó como custom (o no había userId), INTENTAR cargar como plantilla
                if (rutinaCargada == null) { // Solo intenta como plantilla si no se encontró como personalizada O si no había userId
                    Log.d(TAG, "Intentando cargar como Rutina de plantilla...")
                    val plantillaRutina = getRutinaByIdFromFirestore(idRutina) // Usa tu función de FirestoreUtils
                    if (plantillaRutina != null) {
                        Log.d(TAG, "Rutina de plantilla '${plantillaRutina.nombre}' cargada.")
                        rutinaCargada = plantillaRutina // Asigna directamente, ya es tipo Rutina
                    } else {
                        Log.w(TAG, "No se encontró Rutina de plantilla con ID: $idRutina.")
                        // rutinaCargada sigue siendo null
                    }
                }
                // 3. Evaluar el resultado final y actualizar la UI
                if (rutinaCargada != null) {
                    Log.d(TAG, "Rutina '${rutinaCargada.nombre}' preparada para UI con ${rutinaCargada.ejercicios.size} ejercicios.")
                    _uiState.value = RoutineDetailUiState(
                        rutina = rutinaCargada,
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    // Este log ahora será más preciso si ambas búsquedas fallan.
                    Log.w(TAG, "No se encontró la rutina con ID: $idRutina (ni como custom ni como plantilla).")
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
    private fun mapUserCustomRoutineToRutina(customRoutine: UserCustomRoutine): Rutina {
        Log.d(TAG, "Mapeando UserCustomRoutine (ID: ${customRoutine.id}) a Rutina UI.")
        return Rutina(
            id = customRoutine.id,
            slug = customRoutine.originalTemplateId ?: customRoutine.id, // O lo que tenga sentido para el slug
            nombre = customRoutine.nombrePersonalizado,
            descripcion = customRoutine.descripcion ?: customRoutine.descripcion ?: "",
            imagenUrl = customRoutine.imagenUrl,
            nivelRecomendado = customRoutine.nivelRecomendado,
            objetivos = customRoutine.objetivos,
            lugarEntrenamiento = customRoutine.lugarEntrenamiento, // Asumiendo que UserCustomRoutine tiene List<LugarEntrenamiento>
            ejercicios = customRoutine.ejercicios.map { ejercicioCustom ->
                // Mapear cada Ejercicio (de UserCustomRoutine.ejercicios)
                // al modelo de Ejercicio que espera tu Rutina UI.
                // Esta es una copia directa, ajusta si los modelos son diferentes.
                Ejercicio(
                    id = ejercicioCustom.id,
                    nombre = ejercicioCustom.nombre,
                    descripcion = ejercicioCustom.descripcion,
                    imagenUrl = ejercicioCustom.imagenUrl,
                    imagenUrl1 = ejercicioCustom.imagenUrl1,
                    imagenUrl2 = ejercicioCustom.imagenUrl2,
                    videoUrl = ejercicioCustom.videoUrl,
                    duracionSegundosOriginal = ejercicioCustom.duracionSegundosOriginal,
                    repeticionesOriginal = ejercicioCustom.repeticionesOriginal,
                    numeroDeSeries = ejercicioCustom.numeroDeSeries,
                    descansoEntreSeriesSegundos = ejercicioCustom.descansoEntreSeriesSegundos,
                    descansoDespuesEjercicioSegundos = ejercicioCustom.descansoDespuesEjercicioSegundos,
                    grupoMuscular = ejercicioCustom.grupoMuscular,
                    equipamientoNecesario = ejercicioCustom.equipamientoNecesario,
                    lugarEntrenamiento = ejercicioCustom.lugarEntrenamiento,
                    orden = ejercicioCustom.orden,
                    tipoEjercicio = ejercicioCustom.tipoEjercicio,
                    componentes = ejercicioCustom.componentes.map { compCustom ->
                        compCustom.copy() // Asumiendo que ComponenteEjercicio es el mismo modelo
                    },
                    notaTempo = ejercicioCustom.notaTempo,
                    esUnilateral = ejercicioCustom.esUnilateral
                )
            },
            numeroDeRondas = customRoutine.numeroDeRondas,
            descansoEntreRondasSegundos = customRoutine.descansoEntreRondasSegundos
        )
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
    fun onPersonalizarRutinaClicked() { // Ya no necesita userId como parámetro si lo tienes como propiedad
        val rutinaActual = _uiState.value.rutina
        if (rutinaActual == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No se puede personalizar, rutina no cargada.")
            Log.w(TAG, "Intento de personalizar rutina nula.")
            return
        }

        // Usa el this.userId obtenido en el init
        if (this.userId == null || this.userId.isBlank()) {
            // Este caso es más complejo: si estamos viendo una plantilla, ¿cómo personalizamos?
            // El userId para la NUEVA rutina personalizada vendría del usuario actualmente logueado.
            // Necesitarías una forma de obtener el ID del usuario autenticado globalmente
            // (e.g., desde un AuthViewModel o similar)
            Log.w(TAG, "Se intentó personalizar, pero no hay un userId asociado a esta rutina (podría ser plantilla) o el usuario actual no está claro.")
            _uiState.value = _uiState.value.copy(errorMessage = "Inicia sesión para personalizar o selecciona una rutina de usuario.")
            return
        }

        Log.d(TAG, "Iniciando personalización para rutina ID: ${rutinaActual.id} por usuario ${this.userId}")

        // La lógica para crear UserCustomRoutine a partir de la 'rutinaActual' (que es tipo Rutina)
        // Si rutinaActual vino de una UserCustomRoutine, su ID es el customRoutineId.
        // Si rutinaActual vino de una plantilla, su ID es el templateId.

        val nuevaRutinaParaPersonalizar = UserCustomRoutine(
            id = UUID.randomUUID().toString(), // ID para la nueva copia personalizada
            userId = this.userId, // El usuario que está personalizando
            nombrePersonalizado = rutinaActual.nombre, // Empieza con el nombre actual
            descripcion = rutinaActual.descripcion,
            imagenUrl = rutinaActual.imagenUrl,
            ejercicios = rutinaActual.ejercicios.map { ejercicioOriginal ->
                ejercicioOriginal.copy( // Asegúrate que Ejercicio es tu modelo completo
                    componentes = ejercicioOriginal.componentes.map { componente -> componente.copy() }
                )
            },
            numeroDeRondas = rutinaActual.numeroDeRondas,
            descansoEntreRondasSegundos = rutinaActual.descansoEntreRondasSegundos,
            nivelRecomendado = rutinaActual.nivelRecomendado.toList(),
            objetivos = rutinaActual.objetivos.toList(),
            lugarEntrenamiento = rutinaActual.lugarEntrenamiento.toList(),
            originalTemplateId = if (rutinaActual.id != this.rutinaId && this.userId != null) rutinaActual.id else this.rutinaId, // Si el id de la rutina en UI es diferente al que vino por args (y hay user) era una plantilla
            // ^ Esta lógica de originalTemplateId puede necesitar ajuste fino.
            // Si this.userId es nulo, significa que estamos viendo una plantilla, entonces rutinaActual.id ES el templateId.
            // Si this.userId no es nulo, significa que estamos viendo una customRoutine, entonces this.rutinaId ES el customRoutineId.
            //   En este caso, UserCustomRoutine ya debería tener su propio originalTemplateId si fue creada desde una plantilla.
            //   La clave es si la 'rutinaActual' es la plantilla original o ya una UserCustomRoutine.
            //   Si la rutina actual ya es una UserCustomRoutine, su ID es el custom ID.
            //   Si `rutinaActual` proviene de una plantilla, `rutinaActual.id` es el ID de la plantilla.

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
            Log.d(TAG, "Refrescando detalles para rutinaId: $rutinaId, userId: $userId")
            loadRoutineDetails(rutinaId, userId)
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
