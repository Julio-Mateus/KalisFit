package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import android.util.Log
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.compose.ui.input.key.type
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.EjercicioTodos
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.TipoDeEjercicio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.text.mapNotNull
import kotlin.text.uppercase

data class AllExercisesUiState(
    val isLoading: Boolean = true,
    val exercises: List<Ejercicio> = emptyList(), // Correcto: espera List<Ejercicio>
    val errorMessage: String? = null,
    val searchTerm: String = ""
)

class AllExercisesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AllExercisesUiState())
    val uiState: StateFlow<AllExercisesUiState> = _uiState.asStateFlow()


    private val db = Firebase.firestore
    init {
        // Cambia el nombre de la función a algo más descriptivo
        fetchExercisesFromFirestore()
    }
    // Renombrada y modificada para Firestore
    fun fetchExercisesFromFirestore() {
        _uiState.update { currentState ->
            currentState.copy(isLoading = true, exercises = emptyList(), errorMessage = null)
        }
        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("ejercicios_todos").get().await()

                val exercisesList: List<Ejercicio> = querySnapshot.documents.mapNotNull { document ->
                    val grupoMuscularStrings = document.get("grupoMuscular") as? List<String> ?: emptyList()
                    val lugarEntrenamientoStrings = document.get("lugarEntrenamiento") as? List<String> ?: emptyList()
                    val equipamientoNecesarioStrings = document.get("equipamientoNecesario") as? List<String> ?: emptyList() // Este es List<String> en EjercicioTodos y Ejercicio

                    // ---- IMPORTANTE: TipoDeEjercicio ----
                    // Tu JSON original y EjercicioTodos no tienen 'tipoEjercicio'.
                    // Por lo tanto, es probable que este campo NO esté en tus documentos iniciales de Firestore.
                    // Asignaremos un valor por defecto. Si más adelante añades este campo a Firestore,
                    // esta lógica intentará leerlo.
                    val tipoEjercicioString = document.getString("tipoEjercicio") // Probablemente será null
                    val tipoEjercicioEnum = tipoEjercicioString?.let { tipoEjercicioStringValue ->
                        try {
                            TipoDeEjercicio.valueOf(tipoEjercicioStringValue.uppercase()) // Asume que el string en Firestore coincide con el nombre del enum
                        } catch (e: IllegalArgumentException) {
                            Log.w("AllExercisesVM", "Valor de TipoDeEjercicio no reconocido en Firestore: $tipoEjercicioStringValue. Usando SIMPLE.")
                            TipoDeEjercicio.SIMPLE // Valor por defecto si el string no coincide
                        }
                    } ?: TipoDeEjercicio.SIMPLE // Valor por defecto si el campo no existe o es null

                    Ejercicio(
                        id = document.id, // El ID del documento de Firestore
                        nombre = document.getString("nombre") ?: "",
                        descripcion = document.getString("descripcion") ?: "",
                        imagenUrl = document.getString("imagenUrl"), // El JSON tenía esto

                        // ---- Campos que NO estaban en EjercicioTodos / JSON original ----
                        // Estos probablemente serán null o 0 si no los añadiste específicamente a tus documentos de Firestore.
                        imagenUrl1 = document.getString("imagenUrl1"), // Probablemente null
                        imagenUrl2 = document.getString("imagenUrl2"), // Probablemente null
                        videoUrl = document.getString("videoUrl"),     // El JSON original tenía esto. Asegúrate de que se subió.

                        // duracionSegundosOriginal y repeticionesOriginal:
                        // Tu JSON original NO los tenía. Tu modelo Ejercicio SÍ.
                        // Si NO están en Firestore, serán 0 y "0".
                        duracionSegundosOriginal = (document.getLong("duracionSegundosOriginal") ?: 0L).toInt(),
                        repeticionesOriginal = document.getString("repeticionesOriginal") ?: "0",

                        numeroDeSeries = (document.getLong("numeroDeSeries") ?: 1L).toInt(), // Probablemente 1 (valor por defecto)
                        descansoEntreSeriesSegundos = (document.getLong("descansoEntreSeriesSegundos") ?: 0L).toInt(),
                        descansoDespuesEjercicioSegundos = (document.getLong("descansoDespuesEjercicioSegundos") ?: 0L).toInt(),

                        grupoMuscular = grupoMuscularStrings.mapNotNull { GrupoMuscular.fromString(it) },
                        equipamientoNecesario = equipamientoNecesarioStrings, // Ya es List<String>
                        lugarEntrenamiento = lugarEntrenamientoStrings.mapNotNull { LugarEntrenamiento.fromString(it) },

                        orden = document.getDouble("orden") ?: 0.0, // Probablemente 0.0

                        // ---- CAMPOS PROCESADOS/DERIVADOS ----
                        // Estos se inicializan con valores por defecto porque es improbable
                        // que estén en los documentos de Firestore basados en tu JSON inicial.
                        tipoEjercicio = tipoEjercicioEnum, // Usará el valor por defecto SIMPLE si no está en Firestore
                        componentes = emptyList(),       // Los componentes no estaban en el JSON simple
                        notaTempo = document.getString("notaTempo"),       // Probablemente null

                        esUnilateral = document.getBoolean("esUnilateral") ?: false // Esto SÍ estaba en tu JSON
                    )
                }
                // --- FIN DEL MAPEO CRUCIAL ---

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        exercises = exercisesList,
                        errorMessage = null
                    )
                }
                Log.d("AllExercisesVM", "Ejercicios cargados desde FIRESTORE: ${exercisesList.size}")

            } catch (e: Exception) {
                Log.e("AllExercisesVM", "Error al cargar ejercicios desde FIRESTORE", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar desde Firestore: ${e.localizedMessage}" // Mensaje de error más específico
                    )
                }
            }
        }
    }

    fun updateSearchTerm(newTerm: String) {
        _uiState.update { currentState ->
            currentState.copy(searchTerm = newTerm)
        }
        // No necesitas llamar a _loadExercises() aquí explícitamente si
        // RenderExercisesContent ya filtra basado en uiState.searchTerm.
        // Si tu lógica de carga/filtrado es más compleja y depende de este
        // término para hacer una nueva query a la BD, entonces sí lo necesitarías.
    }
}