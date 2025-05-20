package com.jcmateus.kalisfit.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.data.obtenerRutinas
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp // <--- AÑADE ESTA LÍNEA

// Tu data class UserProfile se mantiene igual
data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val fechaRegistro: Timestamp? = null,
    val nivel: String = "",
    val objetivos: List<String> = emptyList(),
    val peso: Float = 0f,
    val altura: Float = 0f,
    val edad: Int = 0,
    val sexo: String = "",
    val frecuenciaSemanal: Int = 3,
    val lugarEntrenamiento: List<String> = emptyList(),
    val insignias: List<String> = emptyList(),
    val rutinasCompletadas: Int = 0,
    val progresoActual: String = "",
    val fotoUrl: String = ""
)

class UserProfileViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance() // <<--- AÑADIR INSTANCIA DE STORAGE
) : ViewModel() {

    // --- ESTADOS EXISTENTES ---
    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow() // Asegúrate de usar asStateFlow()

    private val _isLoadingUser = MutableStateFlow(false)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    private val _userErrorMessage = MutableStateFlow<String?>(null)
    val userErrorMessage: StateFlow<String?> = _userErrorMessage.asStateFlow()

    private val _recommendedRoutines = MutableStateFlow<List<Rutina>>(emptyList())
    val recommendedRoutines: StateFlow<List<Rutina>> = _recommendedRoutines.asStateFlow()

    private val _routinesErrorMessage = MutableStateFlow<String?>(null)
    val routinesErrorMessage: StateFlow<String?> = _routinesErrorMessage.asStateFlow()

    private val _isLoadingRoutines = MutableStateFlow(false)
    val isLoadingRoutines: StateFlow<Boolean> = _isLoadingRoutines.asStateFlow()

    // --- NUEVOS ESTADOS PARA LOS CAMPOS DEL FORMULARIO DE EDICIÓN ---
    private val _editableNombre = MutableStateFlow("")
    val editableNombre: StateFlow<String> = _editableNombre.asStateFlow()

    private val _editablePeso = MutableStateFlow("")
    val editablePeso: StateFlow<String> = _editablePeso.asStateFlow()

    private val _editableAltura = MutableStateFlow("")
    val editableAltura: StateFlow<String> = _editableAltura.asStateFlow()

    private val _editableEdad = MutableStateFlow("")
    val editableEdad: StateFlow<String> = _editableEdad.asStateFlow()

    private val _editableSexo = MutableStateFlow("")
    val editableSexo: StateFlow<String> = _editableSexo.asStateFlow()

    private val _editableFrecuenciaSemanal = MutableStateFlow("")
    val editableFrecuenciaSemanal: StateFlow<String> = _editableFrecuenciaSemanal.asStateFlow()

    // Para lugarEntrenamiento, dado que en UserProfile es List<String>
    // y en la UI de edición podrías manejarlo como una selección múltiple o un String simple.
    // Si la edición solo permite seleccionar UNO, un String está bien.
    // Si permite MÚLTIPLES, necesitarás un List<String> aquí y adaptar la UI.
    // Por simplicidad para el ejemplo, asumiré que en la edición se maneja como un String (el primero o el más relevante).
    private val _editableLugarEntrenamiento = MutableStateFlow("")
    val editableLugarEntrenamiento: StateFlow<String> = _editableLugarEntrenamiento.asStateFlow()

    // fotoUrl ya existe en tu UserProfile, pero podríamos tener uno para la URI de la nueva imagen si es necesario
    // Sin embargo, es más simple pasar la URI directamente a saveUserProfile.
    // El _user.value.fotoUrl será la fuente de la foto actual.

    // --- NUEVO ESTADO PARA LA OPERACIÓN DE ACTUALIZACIÓN/GUARDADO ---
    sealed class UpdateProfileState {
        object Idle : UpdateProfileState()
        object Loading : UpdateProfileState()
        object Success : UpdateProfileState()
        data class Error(val message: String) : UpdateProfileState()
    }

    private val _updateState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateState: StateFlow<UpdateProfileState> = _updateState.asStateFlow()


    companion object {
        private const val TAG = "UserProfileViewModel"
    }

    init {
        // Cargar el perfil del usuario cuando el ViewModel se inicializa.
        // Esto también poblará los campos editables.
        if (firebaseAuth.currentUser != null) {
            loadUserProfile()
        } else {
            // Manejar caso donde no hay usuario al inicio (opcional, podrías solo cargar bajo demanda)
            _userErrorMessage.value = "Usuario no autenticado al iniciar ViewModel."
            _isLoadingUser.value = false
        }
    }

    // --- NUEVAS FUNCIONES PARA ACTUALIZAR LOS STATEFLOWS EDITABLES DESDE LA UI ---
    fun onNombreChange(newName: String) {
        _editableNombre.value = newName
    }

    fun onPesoChange(newPeso: String) {
        _editablePeso.value = newPeso
    }

    fun onAlturaChange(newAltura: String) {
        _editableAltura.value = newAltura
    }

    fun onEdadChange(newEdad: String) {
        _editableEdad.value = newEdad
    }

    fun onSexoChange(newSexo: String) {
        _editableSexo.value = newSexo
    }

    fun onFrecuenciaChange(newFrecuencia: String) {
        _editableFrecuenciaSemanal.value = newFrecuencia
    }

    fun onLugarEntrenamientoChange(newLugar: String) {
        _editableLugarEntrenamiento.value = newLugar
    }


    fun loadUserProfile() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            _userErrorMessage.value = "Usuario no autenticado."
            _isLoadingUser.value = false
            _user.value = null
            // Limpiar también los campos editables
            clearEditableFields()
            return
        }

        _isLoadingUser.value = true
        _userErrorMessage.value = null

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val userProfile = doc.toObject(UserProfile::class.java)?.copy()

                    _user.value = userProfile
                    _isLoadingUser.value = false
                    _userErrorMessage.value = null

                    // <<--- POBLAR CAMPOS EDITABLES --- >>
                    userProfile?.let {
                        _editableNombre.value = it.nombre
                        _editablePeso.value = it.peso.takeIf { p -> p > 0f }?.toString() ?: ""
                        _editableAltura.value = it.altura.takeIf { a -> a > 0f }?.toString() ?: ""
                        _editableEdad.value = it.edad.takeIf { e -> e > 0 }?.toString() ?: ""
                        _editableSexo.value = it.sexo
                        _editableFrecuenciaSemanal.value =
                            it.frecuenciaSemanal.takeIf { f -> f > 0 }?.toString()
                                ?: "3" // Default si es 0
                        // Para lugarEntrenamiento, si es una lista, toma el primero o un string vacío.
                        // Ajusta esto según cómo quieras que se edite.
                        _editableLugarEntrenamiento.value =
                            it.lugarEntrenamiento.firstOrNull() ?: ""
                        // fotoUrl ya está en _user.value.fotoUrl
                    }

                    loadRecommendedRoutines(userProfile)
                } else {
                    Log.e(TAG, "El documento del usuario no existe para UID: $uid")
                    _user.value = null
                    _isLoadingUser.value = false
                    _userErrorMessage.value = "No se encontró el perfil del usuario."
                    clearEditableFields()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar el perfil de usuario", exception)
                _user.value = null
                _isLoadingUser.value = false
                _userErrorMessage.value =
                    "Error al cargar el perfil: ${exception.localizedMessage ?: "Error desconocido"}"
                clearEditableFields()
            }
    }

    private fun clearEditableFields() {
        _editableNombre.value = ""
        _editablePeso.value = ""
        _editableAltura.value = ""
        _editableEdad.value = ""
        _editableSexo.value = ""
        _editableFrecuenciaSemanal.value = ""
        _editableLugarEntrenamiento.value = ""
    }

    // --- NUEVA FUNCIÓN PARA GUARDAR EL PERFIL ---
    fun saveUserProfile(newImageUri: Uri?) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _updateState.value = UpdateProfileState.Error("Usuario no autenticado.")
            return
        }

        _updateState.value = UpdateProfileState.Loading
        viewModelScope.launch {
            try {
                var finalImageUrl = _user.value?.fotoUrl ?: ""

                if (newImageUri != null) {
                    val storageRef =
                        storage.reference.child("fotos_perfil/$currentUserId/${System.currentTimeMillis()}.jpg")
                    storageRef.putFile(newImageUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }

                val updatedPeso = _editablePeso.value.toFloatOrNull() ?: _user.value?.peso ?: 0f
                val updatedAltura =
                    _editableAltura.value.toFloatOrNull() ?: _user.value?.altura ?: 0f
                val updatedEdad = _editableEdad.value.toIntOrNull() ?: _user.value?.edad ?: 0
                val updatedFrecuencia =
                    _editableFrecuenciaSemanal.value.toIntOrNull() ?: _user.value?.frecuenciaSemanal
                    ?: 3

                // Preparar los datos para actualizar. Solo actualiza los campos que son editables.
                // Los campos como email, fechaRegistro, nivel, objetivos, insignias, rutinasCompletadas, progresoActual
                // no se están editando en este flujo simple, así que no se incluyen para update.
                // Si tuvieras campos como 'objetivos' o 'nivel' editables, los añadirías aquí.
                val profileDataToUpdate = mapOf(
                    "nombre" to _editableNombre.value,
                    "peso" to updatedPeso,
                    "altura" to updatedAltura,
                    "edad" to updatedEdad,
                    "sexo" to _editableSexo.value,
                    "frecuenciaSemanal" to updatedFrecuencia,
                    // Para 'lugarEntrenamiento', si en Firestore es List<String>, debes guardar una lista.
                    // Si el _editableLugarEntrenamiento solo tiene un valor, lo pones en una lista.
                    "lugarEntrenamiento" to if (_editableLugarEntrenamiento.value.isNotBlank()) listOf(
                        _editableLugarEntrenamiento.value
                    ) else emptyList<String>(),
                    "fotoUrl" to finalImageUrl
                    // NO incluyas campos que no se editan o que se manejan por separado (ej. 'nivel' si se calcula).
                )

                firestore.collection("users").document(currentUserId)
                    .update(profileDataToUpdate)
                    .await()

                // Refrescar el perfil del usuario local después de guardar para que la UI (ProfileScreen y EditProfileScreen si sigue visible)
                // refleje los cambios inmediatamente.
                loadUserProfile() // Esto recargará _user y también los _editable* fields.

                _updateState.value = UpdateProfileState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar el perfil del usuario", e)
                _updateState.value =
                    UpdateProfileState.Error(e.message ?: "Error desconocido al guardar.")
            }
        }
    }

    // --- NUEVA FUNCIÓN PARA RESETEAR EL ESTADO DE ACTUALIZACIÓN ---
    fun resetUpdateState() {
        _updateState.value = UpdateProfileState.Idle
    }


    // --- FUNCIONES EXISTENTES (loadRecommendedRoutines, refreshRecommendations) ---
    // Se mantienen como están, pero asegúrate de que usen _user.value correctamente.

    fun loadRecommendedRoutines(currentUserProfile: UserProfile?) { // Cambiado el nombre del parámetro para claridad
        val profileToUse =
            currentUserProfile ?: _user.value // Usar el perfil pasado o el del StateFlow
        if (profileToUse == null) {
            Log.w(TAG, "Intento de cargar rutinas recomendadas sin perfil de usuario.")
            _routinesErrorMessage.value = "Perfil de usuario no disponible para recomendar rutinas."
            _isLoadingRoutines.value = false
            _recommendedRoutines.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoadingRoutines.value = true
            _routinesErrorMessage.value = null
            _recommendedRoutines.value = emptyList() // Limpiar antes de cargar nuevas

            val userLocationEnums: List<LugarEntrenamiento>? = profileToUse.lugarEntrenamiento
                .mapNotNull { lugarString ->
                    try {
                        // LugarEntrenamiento.valueOf(lugarString.trim().uppercase(Locale.ROOT)) // Opción más simple si los nombres coinciden y son enums
                        LugarEntrenamiento.entries.firstOrNull { enumEntry -> // Opción más robusta a mayúsculas/minúsculas
                            enumEntry.name.equals(lugarString.trim(), ignoreCase = true)
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.w(
                            TAG,
                            "Lugar de entrenamiento del usuario '$lugarString' no es un enum LugarEntrenamiento válido."
                        )
                        null
                    }
                }
                .takeIf { it.isNotEmpty() }

            Log.d(
                TAG,
                "Cargando rutinas recomendadas para: Nivel='${profileToUse.nivel}', " +
                        "Objetivos='${profileToUse.objetivos.joinToString()}', " +
                        "Lugares Enum='${userLocationEnums?.joinToString { it.name } ?: "Ninguno/Vacío"}'"
            )

            // Asumo que obtenerRutinas es una función suspend o usa callbacks como la tienes
            obtenerRutinas(
                nivel = profileToUse.nivel.takeIf { it.isNotBlank() },
                objetivos = profileToUse.objetivos.takeIf { it.isNotEmpty() },
                lugaresEntrenamiento = userLocationEnums,
                onResult = { rutinasList ->
                    _recommendedRoutines.value =
                        rutinasList.take(5) // Tomar solo las primeras 5 o según necesites
                    _isLoadingRoutines.value = false
                    if (rutinasList.isEmpty()) {
                        Log.d(TAG, "No se encontraron rutinas para los criterios dados.")
                        _routinesErrorMessage.value =
                            "No se encontraron rutinas con tus preferencias."
                    } else {
                        _routinesErrorMessage.value =
                            null // Limpiar error si se encontraron rutinas
                    }
                },
                onError = { errorMsg ->
                    _recommendedRoutines.value = emptyList()
                    _routinesErrorMessage.value = errorMsg
                    _isLoadingRoutines.value = false
                    Log.e(TAG, "Error al cargar rutinas recomendadas: $errorMsg")
                }
            )
        }
    }

    fun refreshRecommendations() {
        // loadRecommendedRoutines tomará el usuario de _user.value si se le pasa null.
        loadRecommendedRoutines(null)
    }

    fun isUserLoggedIn(): Boolean { // Añadir esta función de ayuda
        return firebaseAuth.currentUser != null
    }
}