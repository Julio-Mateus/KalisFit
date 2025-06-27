package com.jcmateus.kalisfit.viewmodel

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
//import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp // <--- AÑADE ESTA LÍNEA
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.UserActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

// --- Data Class para el Resumen Semanal del HomeScreen ---
data class ResumenSemanal(
    val rutinasCompletadas: Int = 0,
    val tiempoTotalEntrenadoSegundos: Int = 0,
    // Podrías añadir más campos como calorías o días activos si los calculas
)

// --- Sealed class para la Última Actividad del HomeScreen ---
sealed class LastActivityItem {
    data class Routine(val progreso: ProgresoRutina) : LastActivityItem()
    data class FreeActivity(val activity: UserActivity) : LastActivityItem()
    object None : LastActivityItem()
    object Loading : LastActivityItem()
}


class UserProfileViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    // --- Estados del Perfil del Usuario ---
    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()

    private val _isLoadingUser = MutableStateFlow(false)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    private val _userErrorMessage = MutableStateFlow<String?>(null)
    val userErrorMessage: StateFlow<String?> = _userErrorMessage.asStateFlow()

    // --- Estados para Rutinas Recomendadas ---
    private val _recommendedRoutines = MutableStateFlow<List<Rutina>>(emptyList())
    val recommendedRoutines: StateFlow<List<Rutina>> = _recommendedRoutines.asStateFlow()

    private val _routinesErrorMessage = MutableStateFlow<String?>(null)
    val routinesErrorMessage: StateFlow<String?> = _routinesErrorMessage.asStateFlow()

    private val _isLoadingRoutines = MutableStateFlow(false)
    val isLoadingRoutines: StateFlow<Boolean> = _isLoadingRoutines.asStateFlow()

    // --- Estados para los Campos Editables del Perfil ---
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

    private val _editableLugarEntrenamiento = MutableStateFlow("") // Asume edición de un solo lugar
    val editableLugarEntrenamiento: StateFlow<String> = _editableLugarEntrenamiento.asStateFlow()

    // --- Estado para la Operación de Actualización/Guardado del Perfil ---
    sealed class UpdateProfileState {
        object Idle : UpdateProfileState()
        object Loading : UpdateProfileState()
        object Success : UpdateProfileState()
        data class Error(val message: String) : UpdateProfileState()
    }

    private val _updateState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateState: StateFlow<UpdateProfileState> = _updateState.asStateFlow()

    // --- Estados para los Datos del HomeScreen ---
    private val _homeScreenSummary = MutableStateFlow<ResumenSemanal?>(null)
    val homeScreenSummary: StateFlow<ResumenSemanal?> = _homeScreenSummary.asStateFlow()

    private val _lastActivity = MutableStateFlow<LastActivityItem>(LastActivityItem.Loading)
    val lastActivity: StateFlow<LastActivityItem> = _lastActivity.asStateFlow()

    private val _isLoadingHomeScreenData = MutableStateFlow(false)
    val isLoadingHomeScreenData: StateFlow<Boolean> = _isLoadingHomeScreenData.asStateFlow()

    private val _homeScreenErrorMessage = MutableStateFlow<String?>(null)
    val homeScreenErrorMessage: StateFlow<String?> = _homeScreenErrorMessage.asStateFlow()


    companion object {
        private const val TAG = "UserProfileViewModel"
        // Formateador de fecha para mostrar en la UI si es necesario
        // Asegúrate que tus modelos (ProgresoRutina.fecha, UserActivity.timestamp) se manejen adecuadamente
        // para la visualización. ProgresoRutina.fecha es Timestamp, UserActivity.timestamp es Date?
        @SuppressLint("SimpleDateFormat")
        val displayDateFormatter = SimpleDateFormat("EEE, d MMM yyyy HH:mm",
            Locale.getDefault())

        fun formatFirebaseTimestampForDisplay(timestamp: com.google.firebase.Timestamp?): String {
            return timestamp?.toDate()?.let { displayDateFormatter.format(it) } ?: "N/A"
        }
        fun formatDateForDisplay(date: Date?): String {
            return date?.let { displayDateFormatter.format(it) } ?: "N/A"
        }
    }

    init {
        if (firebaseAuth.currentUser != null) {
            loadUserProfile()
            loadHomeScreenData() // Cargar datos del home screen al iniciar si el usuario está logueado
        } else {
            _userErrorMessage.value = "Usuario no autenticado."
            _isLoadingUser.value = false
            _homeScreenErrorMessage.value = "Usuario no autenticado."
            _lastActivity.value = LastActivityItem.None
        }
    }

    // --- Funciones para Actualizar Campos Editables desde la UI ---
    fun onNombreChange(newName: String) { _editableNombre.value = newName }
    fun onPesoChange(newPeso: String) { _editablePeso.value = newPeso }
    fun onAlturaChange(newAltura: String) { _editableAltura.value = newAltura }
    fun onEdadChange(newEdad: String) { _editableEdad.value = newEdad }
    fun onSexoChange(newSexo: String) { _editableSexo.value = newSexo }
    fun onFrecuenciaChange(newFrecuencia: String) { _editableFrecuenciaSemanal.value = newFrecuencia }
    fun onLugarEntrenamientoChange(newLugar: String) { _editableLugarEntrenamiento.value = newLugar }

    // --- Carga y Gestión del Perfil del Usuario ---
    fun loadUserProfile() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            _userErrorMessage.value = "Usuario no autenticado."
            _isLoadingUser.value = false
            _user.value = null
            clearEditableFields()
            // También podría ser útil limpiar datos del home screen si el usuario se desloguea
            _homeScreenSummary.value = null
            _lastActivity.value = LastActivityItem.None
            _recommendedRoutines.value = emptyList()
            return
        }

        _isLoadingUser.value = true
        _userErrorMessage.value = null

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val userProfile = doc.toObject(UserProfile::class.java) // No es necesario .copy() aquí
                    _user.value = userProfile
                    populateEditableFields(userProfile)
                    loadRecommendedRoutines(userProfile) // Cargar recomendaciones después del perfil
                } else {
                    Log.w(TAG, "El documento del usuario no existe para UID: $uid")
                    _user.value = null
                    _userErrorMessage.value = "No se encontró el perfil del usuario."
                    clearEditableFields()
                }
                _isLoadingUser.value = false
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar el perfil de usuario", exception)
                _user.value = null
                _isLoadingUser.value = false
                _userErrorMessage.value = "Error al cargar el perfil: ${exception.localizedMessage}"
                clearEditableFields()
            }
    }

    private fun populateEditableFields(userProfile: UserProfile?) {
        userProfile?.let {
            _editableNombre.value = it.nombre
            _editablePeso.value = it.peso.takeIf { p -> p > 0f }?.toString() ?: ""
            _editableAltura.value = it.altura.takeIf { a -> a > 0f }?.toString() ?: ""
            _editableEdad.value = it.edad.takeIf { e -> e > 0 }?.toString() ?: ""
            _editableSexo.value = it.sexo
            _editableFrecuenciaSemanal.value = it.frecuenciaSemanal.takeIf { f -> f >= 0 }?.toString() ?: "3" // 0 puede ser válido
            _editableLugarEntrenamiento.value = it.lugarEntrenamiento.firstOrNull() ?: ""
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
                    val imageFileName = "${System.currentTimeMillis()}.jpg"
                    val storageRef = storage.reference.child("fotos_perfil/$currentUserId/$imageFileName")
                    storageRef.putFile(newImageUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }

                val profileDataToUpdate = mutableMapOf<String, Any>()
                profileDataToUpdate["nombre"] = _editableNombre.value
                profileDataToUpdate["peso"] = _editablePeso.value.toFloatOrNull() ?: _user.value?.peso ?: 0f
                profileDataToUpdate["altura"] = _editableAltura.value.toFloatOrNull() ?: _user.value?.altura ?: 0f
                profileDataToUpdate["edad"] = _editableEdad.value.toIntOrNull() ?: _user.value?.edad ?: 0
                profileDataToUpdate["sexo"] = _editableSexo.value
                profileDataToUpdate["frecuenciaSemanal"] = _editableFrecuenciaSemanal.value.toIntOrNull() ?: _user.value?.frecuenciaSemanal ?: 3
                profileDataToUpdate["lugarEntrenamiento"] = if (_editableLugarEntrenamiento.value.isNotBlank()) {
                    listOf(_editableLugarEntrenamiento.value)
                } else {
                    _user.value?.lugarEntrenamiento ?: emptyList() // Mantener original si no se edita y está vacío
                }
                if (finalImageUrl.isNotEmpty() || newImageUri != null) { // Actualizar solo si hay nueva imagen o ya existía y no se borró
                    profileDataToUpdate["fotoUrl"] = finalImageUrl
                }


                firestore.collection("users").document(currentUserId)
                    .update(profileDataToUpdate)
                    .await()

                loadUserProfile() // Recargar para reflejar cambios
                _updateState.value = UpdateProfileState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar el perfil del usuario", e)
                _updateState.value = UpdateProfileState.Error(e.message ?: "Error desconocido al guardar.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateProfileState.Idle
    }

    // --- Carga de Datos para HomeScreen ---
    fun loadHomeScreenData() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            _homeScreenErrorMessage.value = "Usuario no autenticado."
            _isLoadingHomeScreenData.value = false
            _lastActivity.value = LastActivityItem.None
            _homeScreenSummary.value = null
            return
        }

        _isLoadingHomeScreenData.value = true
        _homeScreenErrorMessage.value = null
        _lastActivity.value = LastActivityItem.Loading // Mostrar estado de carga para última actividad

        viewModelScope.launch {
            try {
                // Usar coroutineScope para lanzar tareas paralelas y esperar a que todas terminen
                // Esto puede hacer la carga un poco más rápida si las latencias de red lo permiten.
                coroutineScope {
                    // 1. Cargar Resumen Semanal de Rutinas (tarea asíncrona)
                    val resumenAsync = async {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        val unaSemanaAtrasTimestamp = Timestamp(calendar.time)

                        val rutinasQuerySnapshot = firestore.collection("users").document(uid)
                            .collection("progresoRutinas") // APUNTA A SUBCOLECCIÓN
                            // El filtro .whereEqualTo("userId", uid) es redundante si la regla de seguridad ya lo cubre por ruta,
                            // pero se mantiene si tu modelo ProgresoRutina tiene un campo 'userId'.
                            // Si ProgresoRutina NO tiene 'userId', elimina la siguiente línea:
                            .whereEqualTo("userId", uid)
                            .whereGreaterThanOrEqualTo("fecha", unaSemanaAtrasTimestamp)
                            .orderBy("fecha", Query.Direction.DESCENDING)
                            .get()
                            .await()

                        // Asegúrate de que ProgresoRutina tiene el campo `userId` si lo usas en la query
                        // o si filtras por él aquí abajo.
                        val rutinasSemanales = rutinasQuerySnapshot.toObjects(ProgresoRutina::class.java)
                        val tiempoTotalSemanasSegundos = rutinasSemanales.sumOf { it.tiempoTotalSesionSegundos }

                        ResumenSemanal(
                            rutinasCompletadas = rutinasSemanales.size,
                            tiempoTotalEntrenadoSegundos = tiempoTotalSemanasSegundos
                        )
                    }

                    // 2. Cargar Última Rutina (tarea asíncrona)
                    val ultimaRutinaAsync = async {
                        val ultimaRutinaQuery = firestore.collection("users").document(uid)
                            .collection("progresoRutinas") // APUNTA A SUBCOLECCIÓN
                            // Similar al comentario anterior sobre .whereEqualTo("userId", uid)
                            // Si ProgresoRutina NO tiene 'userId', elimina la siguiente línea:
                            .whereEqualTo("userId", uid)
                            .orderBy("fecha", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .await()
                        ultimaRutinaQuery.documents.firstOrNull()?.toObject(ProgresoRutina::class.java)
                    }

                    // 3. Cargar Última Actividad Libre (tarea asíncrona)
                    val ultimaActividadLibreAsync = async {
                        // *** IMPORTANTE: AJUSTA "activities" SI TU SUBCOLECCIÓN SE LLAMA "userActivities" ***
                        val ultimaActividadLibreQuery = firestore.collection("users").document(uid)
                            .collection("activities") // APUNTA A SUBCOLECCIÓN (ajusta si es "userActivities")
                            // Similar al comentario anterior sobre .whereEqualTo("userId", uid)
                            // Si UserActivity NO tiene 'userId', elimina la siguiente línea:
                            //.whereEqualTo("userId", uid)
                            // 'timestamp' es Date en UserActivity, pero Firestore lo maneja como Timestamp para la query
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .await()
                        ultimaActividadLibreQuery.documents.firstOrNull()?.toObject(UserActivity::class.java)
                    }

                    // Esperar resultados de todas las tareas asíncronas
                    val resumenSemanal = resumenAsync.await()
                    val ultimaRutina = ultimaRutinaAsync.await()
                    val ultimaActividadLibre = ultimaActividadLibreAsync.await()

                    // Actualizar los StateFlows con los resultados
                    _homeScreenSummary.value = resumenSemanal

                    // Determinar cuál es la más reciente
                    when {
                        ultimaRutina == null && ultimaActividadLibre == null -> {
                            _lastActivity.value = LastActivityItem.None
                        }
                        ultimaRutina != null && ultimaActividadLibre == null -> {
                            _lastActivity.value = LastActivityItem.Routine(ultimaRutina)
                        }
                        ultimaRutina == null && ultimaActividadLibre != null -> {
                            _lastActivity.value = LastActivityItem.FreeActivity(ultimaActividadLibre)
                        }
                        // Ambos no son null, necesitamos comparar fechas
                        ultimaRutina != null && ultimaActividadLibre != null -> {
                            // ultimaRutina.fecha es com.google.firebase.Timestamp
                            // ultimaActividadLibre.timestamp es java.util.Date?

                            val actividadLibreTimestamp = ultimaActividadLibre.timestamp?.let { Timestamp(it) }

                            if (actividadLibreTimestamp == null) {
                                // Si la actividad libre no tiene fecha (o es null), la rutina es la más reciente
                                _lastActivity.value = LastActivityItem.Routine(ultimaRutina)
                            } else {
                                // Ambas tienen fechas válidas para comparar
                                if (ultimaRutina.fecha > actividadLibreTimestamp) {
                                    _lastActivity.value = LastActivityItem.Routine(ultimaRutina)
                                } else {
                                    _lastActivity.value = LastActivityItem.FreeActivity(ultimaActividadLibre)
                                }
                            }
                        }
                    }
                    _homeScreenErrorMessage.value = null // Limpiar error si todo fue exitoso
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos del home screen", e)
                _homeScreenErrorMessage.value = "Error al cargar datos: ${e.localizedMessage}"
                _lastActivity.value = LastActivityItem.None // Resetear a un estado no-cargando/error
                _homeScreenSummary.value = null // Limpiar resumen en caso de error
            } finally {
                _isLoadingHomeScreenData.value = false
            }
        }
    }

    fun refreshHomeScreenData() {
        loadHomeScreenData()
    }

    // --- Carga de Rutinas Recomendadas ---
    fun loadRecommendedRoutines(currentUserProfile: UserProfile?) {
        val profileToUse = currentUserProfile ?: _user.value
        if (profileToUse == null || profileToUse.uid.isBlank()) { // uid check for safety
            _routinesErrorMessage.value = "Perfil de usuario no disponible."
            _isLoadingRoutines.value = false
            _recommendedRoutines.value = emptyList()
            return
        }

        _isLoadingRoutines.value = true
        _routinesErrorMessage.value = null
        _recommendedRoutines.value = emptyList()

        viewModelScope.launch {
            try {
                // Convertir lugares de entrenamiento del perfil (Strings) a Enums
                val userLocationEnums: List<LugarEntrenamiento> = profileToUse.lugarEntrenamiento
                    .mapNotNull { lugarString ->
                        try {
                            LugarEntrenamiento.entries.firstOrNull { it.name.equals(lugarString.trim(), ignoreCase = true) }
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Lugar de entrenamiento del usuario '$lugarString' no es un enum válido.")
                            null
                        }
                    }

                // Llamar a la función que obtiene las rutinas (idealmente en un Repositorio)
                // Esta es una función de ejemplo, su implementación real está fuera de este ViewModel
                val rutinasList = obtenerRutinasDesdeFirestore(
                    nivel = profileToUse.nivel.takeIf { it.isNotBlank() },
                    objetivos = profileToUse.objetivos.takeIf { it.isNotEmpty() },
                    lugaresEntrenamientoEnums = userLocationEnums.takeIf { it.isNotEmpty() }
                )

                _recommendedRoutines.value = rutinasList.take(5) // Tomar un número limitado
                if (rutinasList.isEmpty()) {
                    _routinesErrorMessage.value = "No se encontraron rutinas con tus preferencias."
                } else {
                    _routinesErrorMessage.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar rutinas recomendadas", e)
                _recommendedRoutines.value = emptyList()
                _routinesErrorMessage.value = "Error al cargar recomendaciones: ${e.localizedMessage}"
            } finally {
                _isLoadingRoutines.value = false
            }
        }
    }

    // Esta función debería idealmente estar en una clase Repositorio.
    // Aquí se incluye como ejemplo de cómo podría ser la lógica de consulta.
    private suspend fun obtenerRutinasDesdeFirestore(
        nivel: String?,
        objetivos: List<String>?,
        lugaresEntrenamientoEnums: List<LugarEntrenamiento>?
    ): List<Rutina> {
        try {
            var query: Query = firestore.collection("rutinas")
            var appliedServerFilter = false

            // Aplicar el filtro de NIVEL en el servidor si está presente
            if (nivel != null) {
                query = query.whereArrayContains("nivelRecomendado", nivel)
                appliedServerFilter = true
            }
            // Si no se filtró por nivel, intentar filtrar por OBJETIVOS en el servidor
            else if (objetivos != null && objetivos.isNotEmpty()) {
                query = query.whereArrayContainsAny("objetivos", objetivos.take(10)) // Firestore limita a 10 en 'array-contains-any'
                appliedServerFilter = true
            }
            // Si no se filtró por nivel ni objetivos, intentar filtrar por LUGAR en el servidor
            else if (lugaresEntrenamientoEnums != null && lugaresEntrenamientoEnums.isNotEmpty()) {
                val lugaresStr = lugaresEntrenamientoEnums.map { it.name }
                query = query.whereArrayContainsAny("lugarEntrenamiento", lugaresStr.take(10)) // Firestore limita a 10
                appliedServerFilter = true
            }

            val snapshot = query.limit(50).get().await() // Obtener un conjunto más grande si se filtra mucho en cliente
            var rutinasFetched = snapshot.toObjects(Rutina::class.java)

            // Filtrado en Cliente para los criterios NO aplicados en servidor
            if (nivel != null && appliedServerFilter && !query.toString().contains("nivelRecomendado")) { // Verificación torpe, mejorar
                rutinasFetched = rutinasFetched.filter { rutina -> rutina.nivelRecomendado.any { it.equals(nivel, ignoreCase = true) } }
            }
            // Si el filtro de nivel se aplicó en el servidor, ya no es necesario filtrarlo en cliente
            // a menos que la lógica del servidor haya sido "else if"

            // Filtrar por objetivos en cliente SI no se hizo en servidor O si se quiere una lógica más compleja (e.g., TODOS los objetivos)
            if (objetivos != null && objetivos.isNotEmpty()) {
                rutinasFetched = rutinasFetched.filter { rutina ->
                    // Ejemplo: si se requiere que la rutina tenga AL MENOS UNO de los objetivos del usuario
                    objetivos.any { objetivoUsuario -> rutina.objetivos.any { it.equals(objetivoUsuario, ignoreCase = true) } }
                    // O si se requiere que la rutina tenga TODOS los objetivos del usuario:
                    // objetivos.all { objetivoUsuario -> rutina.objetivos.any { it.equals(objetivoUsuario, ignoreCase = true) } }
                }
            }

            // Filtrar por lugar en cliente SI no se hizo en servidor
            if (lugaresEntrenamientoEnums != null && lugaresEntrenamientoEnums.isNotEmpty()) {
                val nombresLugaresUsuario = lugaresEntrenamientoEnums.map { it.name }
                rutinasFetched = rutinasFetched.filter { rutina ->
                    rutina.lugarEntrenamiento.any { lugarRutina -> // Asumiendo que rutina.lugarEntrenamiento es List<String>
                        nombresLugaresUsuario.any { it.equals(lugarRutina.toString(), ignoreCase = true) }
                    }
                }
            }

            return rutinasFetched
        } catch (e: Exception) {
            Log.e(TAG, "Error en obtenerRutinasDesdeFirestore: ${e.message}", e)
            throw e
        }
    }


    fun refreshRecommendations() {
        loadRecommendedRoutines(null) // Usa el perfil de usuario actual en _user.value
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    // Limpiar estados cuando el ViewModel se destruye (o el usuario se desloguea explícitamente)
    override fun onCleared() {
        super.onCleared()
        // Aquí podrías resetear estados si es necesario, aunque los StateFlows se manejarán
        // por el ciclo de vida de sus colectores.
        Log.d(TAG, "UserProfileViewModel onCleared")
    }
}