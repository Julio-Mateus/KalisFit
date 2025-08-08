package com.jcmateus.kalisfit.viewmodel


import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.getUserCustomRoutineById
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.UserCustomRoutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EditRoutineUiState(
    val isLoading: Boolean = false,
    val routineToEdit: UserCustomRoutine? = null, // La rutina que se está editando o creando
    val originalTemplateId: String? = null, // Si se basa en una plantilla
    val isNewRoutine: Boolean = true, // Para saber si es una creación o edición
    val availableExercises: List<Ejercicio> = emptyList(), // Para el selector de ejercicios
    val errorMessages: List<String> = emptyList(), // Para mostrar errores de validación, etc.
    val saveSuccess: Boolean = false // Para indicar que se guardó con éxito
)

class EditRoutineViewModel(
    private val savedStateHandle: SavedStateHandle,
    // Inyecta tu repositorio aquí cuando lo tengas
    // private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRoutineUiState())
    val uiState: StateFlow<EditRoutineUiState> = _uiState.asStateFlow()

    // Argumentos de navegación que esperaremos
    val templateRoutineIdArg: String? = savedStateHandle[EditRoutineArgs.TEMPLATE_ID_ARG]
    val customRoutineIdArg: String? = savedStateHandle[EditRoutineArgs.CUSTOM_ROUTINE_ID_ARG]
    val currentUserIdArg: String? = savedStateHandle[EditRoutineArgs.USER_ID_ARG] // ¡MUY IMPORTANTE!

    init {
        if (currentUserIdArg == null || currentUserIdArg.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessages = listOf("Error: Usuario no identificado. No se puede editar la rutina."),
                isLoading = false
            )
            // Podrías incluso tener un evento para forzar la salida de la pantalla si el userId es vital.
        } else {
            loadInitialRoutine()
        }
    }

    private fun loadInitialRoutine() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                when {
                    // Caso 1: Editando una rutina personalizada existente
                    customRoutineIdArg != null -> {
                        // ¡Asegúrate de que currentUserIdArg no sea nulo aquí también!
                        // Ya lo validas en init, pero es bueno ser defensivo.
                        if (currentUserIdArg == null) {
                            _uiState.value = _uiState.value.copy(
                                errorMessages = listOf("Error crítico: Falta ID de usuario para cargar rutina personalizada."),
                                isLoading = false
                            )
                            return@launch // Salir de la corrutina
                        }

                        // LLAMADA A LA NUEVA FUNCIÓN DE FIRESTORE UTILS
                        val existingCustomRoutine: UserCustomRoutine? =
                            getUserCustomRoutineById(currentUserIdArg, customRoutineIdArg)

                        if (existingCustomRoutine != null) {
                            _uiState.value = _uiState.value.copy(
                                routineToEdit = existingCustomRoutine,
                                isNewRoutine = false,
                                // ESTO AHORA DEBERÍA FUNCIONAR
                                originalTemplateId = existingCustomRoutine.originalTemplateId,
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessages = listOf("Rutina personalizada no encontrada o error al cargarla."),
                                isLoading = false
                            )
                        }
                    }
                    // Caso 2: Creando una nueva rutina basada en una plantilla
                    templateRoutineIdArg != null -> {
                        // Aquí usas getRutinaByIdFromFirestore, que devuelve Rutina (plantilla)
                        val templateRoutine = getRutinaByIdFromFirestore(templateRoutineIdArg)

                        if (templateRoutine != null) {
                            val newCustomRoutine = UserCustomRoutine(
                                id = UUID.randomUUID().toString(),
                                userId = currentUserIdArg!!, // Sabemos que no es nulo aquí
                                originalTemplateId = templateRoutine.id, // ID de la plantilla
                                nombrePersonalizado = templateRoutine.nombre,
                                // ... mapea el resto de los campos de templateRoutine a newCustomRoutine ...
                                descripcion = templateRoutine.descripcion,
                                imagenUrl = templateRoutine.imagenUrl,
                                ejercicios = templateRoutine.ejercicios.map { it.copy() }, // Copia profunda si es necesario
                                numeroDeRondas = templateRoutine.numeroDeRondas,
                                descansoEntreRondasSegundos = templateRoutine.descansoEntreRondasSegundos,
                                nivelRecomendado = templateRoutine.nivelRecomendado.toList(),
                                objetivos = templateRoutine.objetivos.toList(),
                                lugarEntrenamiento = templateRoutine.lugarEntrenamiento.toList(), // Convertir Enum a String para UserCustomRoutine si es necesario
                                fechaCreacion = Timestamp.now(),
                                fechaUltimaModificacion = Timestamp.now()
                            )
                            _uiState.value = _uiState.value.copy(
                                routineToEdit = newCustomRoutine,
                                isNewRoutine = true,
                                originalTemplateId = templateRoutine.id, // También se guarda en el UiState directamente
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessages = listOf("Plantilla de rutina no encontrada."),
                                isLoading = false
                            )
                        }
                    }
                    // Caso 3: Creando una rutina completamente nueva desde cero
                    else -> {
                        val blankRoutine = UserCustomRoutine(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserIdArg!!,
                            nombrePersonalizado = "Nueva Rutina",
                            // originalTemplateId será null por defecto aquí, lo cual es correcto
                            // ... otros campos con valores por defecto o vacíos ...
                            ejercicios = emptyList(),
                            fechaCreacion = Timestamp.now(),
                            fechaUltimaModificacion = Timestamp.now()
                        )
                        _uiState.value = _uiState.value.copy(
                            routineToEdit = blankRoutine,
                            isNewRoutine = true,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EditRoutineVM", "Excepción en loadInitialRoutine", e)
                _uiState.value = _uiState.value.copy(
                    errorMessages = listOf("Error al cargar la rutina: ${e.message}"),
                    isLoading = false
                )
            }
        }
    }

    fun onRoutineNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(
            routineToEdit = _uiState.value.routineToEdit?.copy(nombrePersonalizado = newName)
        )
    }

    // --- MÁS FUNCIONES DE MODIFICACIÓN VENDRÁN AQUÍ ---
    // onDescriptionChanged, onRoundsChanged, onAddExercise, onRemoveExercise, etc.

    fun saveRoutine() {
        val routineToSave = _uiState.value.routineToEdit ?: return
        if (currentUserIdArg == null) {
            _uiState.value = _uiState.value.copy(errorMessages = listOf("Error crítico: Falta ID de usuario al guardar."))
            return
        }

        // Validaciones básicas (ejemplo)
        if (routineToSave.nombrePersonalizado.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessages = listOf("El nombre de la rutina no puede estar vacío."))
            return
        }
        if (routineToSave.ejercicios.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessages = listOf("La rutina debe tener al menos un ejercicio."))
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessages = emptyList())
        viewModelScope.launch {
            try {
                // TODO: Llamar al repositorio para guardar/actualizar routineToSave
                // Ejemplo: routineRepository.saveUserCustomRoutine(routineToSave.copy(fechaUltimaModificacion = Timestamp.now()))
                Log.d("EditRoutineVM", "Simulando guardado de: ${routineToSave.nombrePersonalizado}")
                // Simulación de éxito
                delay(1000) // Simula llamada de red

                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessages = listOf("Error al guardar la rutina: ${e.message}"),
                    saveSuccess = false
                )
            }
        }
    }

    fun onSaveHandled() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

// Define estas constantes para los argumentos de navegación, idealmente en tu archivo Routes.kt o similar
object EditRoutineArgs {
    const val TEMPLATE_ID_ARG = "templateId"
    const val CUSTOM_ROUTINE_ID_ARG = "customRoutineId"
    const val USER_ID_ARG = "userId" // Asegúrate de pasar esto al navegar
}

