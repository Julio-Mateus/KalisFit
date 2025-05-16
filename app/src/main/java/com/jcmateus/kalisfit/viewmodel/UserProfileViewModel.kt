package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.activity.result.launch
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isEmpty
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.data.obtenerRutinas
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val fechaRegistro: String = "",
    val nivel: String = "",
    val objetivos: List<String> = emptyList(),
    val peso: Float = 0f,             // en kilogramos
    val altura: Float = 0f,           // en centímetros
    val edad: Int = 0,
    val sexo: String = "",           // Masculino / Femenino / Otro
    val frecuenciaSemanal: Int = 3,   // días por semana
    val lugarEntrenamiento: List<String> = emptyList(), // Lista de Casa / Gimnasio / Exterior
    val insignias: List<String> = emptyList(), // Insignias ganadas por logros
    val rutinasCompletadas: Int = 0,  // Para evaluar si sube de nivel
    val progresoActual: String = "",  // Texto descriptivo del avance
    val fotoUrl: String = ""
)

class UserProfileViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user

    // --- NUEVOS ESTADOS PARA LA CARGA DEL PERFIL ---
    private val _isLoadingUser = MutableStateFlow(false) // Inicialmente no cargando
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser

    private val _userErrorMessage = MutableStateFlow<String?>(null)
    val userErrorMessage: StateFlow<String?> = _userErrorMessage
    // --- FIN DE NUEVOS ESTADOS ---

    private val _recommendedRoutines = MutableStateFlow<List<Rutina>>(emptyList())
    val recommendedRoutines: StateFlow<List<Rutina>> = _recommendedRoutines

    private val _routinesErrorMessage = MutableStateFlow<String?>(null)
    val routinesErrorMessage: StateFlow<String?> = _routinesErrorMessage

    private val _isLoadingRoutines = MutableStateFlow(false)
    val isLoadingRoutines: StateFlow<Boolean> = _isLoadingRoutines

    companion object {
        private const val TAG = "UserProfileViewModel"
    }

    fun loadUserProfile() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            _userErrorMessage.value = "Usuario no autenticado."
            _isLoadingUser.value = false // No hay usuario para cargar
            _user.value = null // Asegurar que el usuario esté nulo
            return
        }

        _isLoadingUser.value = true
        _userErrorMessage.value = null // Limpiar errores previos

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nombre = doc.getString("nombre") ?: ""
                    val email = doc.getString("email") ?: ""
                    // ... (resto de los campos como los tienes)
                    val nivel = doc.getString("nivel") ?: ""
                    val objetivos = doc.get("objetivos") as? List<String> ?: emptyList()
                    val fechaMillis = doc.getLong("fechaRegistro") ?: 0L
                    val fechaFormateada = if (fechaMillis > 0) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(fechaMillis))
                    } else {
                        "N/A"
                    }
                    val peso = doc.getDouble("peso")?.toFloat() ?: 0f
                    val altura = doc.getDouble("altura")?.toFloat() ?: 0f
                    val edad = doc.getLong("edad")?.toInt() ?: 0
                    val sexo = doc.getString("sexo") ?: ""
                    val frecuenciaSemanal = doc.getLong("frecuenciaSemanal")?.toInt() ?: 3
                    val lugarEntrenamientoStringsList = doc.get("lugarEntrenamiento") as? List<String> ?: emptyList()
                    val insignias = doc.get("insignias") as? List<String> ?: emptyList()
                    val rutinasCompletadas = doc.getLong("rutinasCompletadas")?.toInt() ?: 0
                    val progresoActual = doc.getString("progresoActual") ?: ""
                    // val fotoUrl = doc.getString("fotoUrl") ?: "" // Si la tienes

                    val userProfile = UserProfile(
                        uid = uid,
                        nombre = nombre,
                        email = email,
                        fechaRegistro = fechaFormateada,
                        nivel = nivel,
                        objetivos = objetivos,
                        peso = peso,
                        altura = altura,
                        edad = edad,
                        sexo = sexo,
                        frecuenciaSemanal = frecuenciaSemanal,
                        lugarEntrenamiento = lugarEntrenamientoStringsList,
                        insignias = insignias,
                        rutinasCompletadas = rutinasCompletadas,
                        progresoActual = progresoActual
                        // fotoUrl = fotoUrl
                    )
                    _user.value = userProfile
                    _isLoadingUser.value = false
                    _userErrorMessage.value = null // Éxito, sin error
                    loadRecommendedRoutines(userProfile) // Cargar rutinas después de cargar el perfil
                } else {
                    Log.e(TAG, "El documento del usuario no existe para UID: $uid")
                    _user.value = null
                    _isLoadingUser.value = false
                    _userErrorMessage.value = "No se encontró el perfil del usuario."
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar el perfil de usuario", exception)
                _user.value = null
                _isLoadingUser.value = false
                _userErrorMessage.value = "Error al cargar el perfil: ${exception.localizedMessage ?: "Error desconocido"}"
            }
    }


    fun loadRecommendedRoutines(currentUser: UserProfile?) {
        val profile = currentUser ?: _user.value
        if (profile == null) {
            Log.w(TAG, "Intento de cargar rutinas recomendadas sin perfil de usuario.")
            _routinesErrorMessage.value = "Perfil de usuario no disponible para recomendar rutinas."
            _isLoadingRoutines.value = false
            _recommendedRoutines.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoadingRoutines.value = true
            _routinesErrorMessage.value = null
            _recommendedRoutines.value = emptyList()

            val userLocationEnums: List<LugarEntrenamiento>? = profile.lugarEntrenamiento
                .mapNotNull { lugarString ->
                    try {
                        LugarEntrenamiento.entries.firstOrNull { enumEntry ->
                            enumEntry.name.equals(lugarString.trim(), ignoreCase = true)
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Lugar de entrenamiento del usuario '$lugarString' no es un enum LugarEntrenamiento válido.")
                        null
                    }
                }
                .takeIf { it.isNotEmpty() }

            Log.d(
                TAG,
                "Cargando rutinas recomendadas para: Nivel='${profile.nivel}', " +
                        "Objetivos='${profile.objetivos.joinToString()}', " +
                        "Lugares Enum='${userLocationEnums?.joinToString { it.name } ?: "Ninguno/Vacío"}'"
            )

            obtenerRutinas(
                nivel = profile.nivel.takeIf { it.isNotBlank() },
                objetivos = profile.objetivos.takeIf { it.isNotEmpty() },
                lugaresEntrenamiento = userLocationEnums,
                onResult = { rutinasList ->
                    _recommendedRoutines.value = rutinasList.take(5)
                    _isLoadingRoutines.value = false
                    if (rutinasList.isEmpty()) {
                        Log.d(TAG, "No se encontraron rutinas para los criterios dados.")
                        _routinesErrorMessage.value = "No se encontraron rutinas con tus preferencias."
                    } else {
                        _routinesErrorMessage.value = null // Limpiar error si se encontraron rutinas
                    }
                },
                onError = { errorMsg ->
                    _recommendedRoutines.value = emptyList() // Limpiar en caso de error
                    _routinesErrorMessage.value = errorMsg
                    _isLoadingRoutines.value = false
                    Log.e(TAG, "Error al cargar rutinas recomendadas: $errorMsg")
                }
            )
        }
    }

    fun refreshRecommendations() {
        // No es necesario pasar el usuario aquí si loadRecommendedRoutines ya lo toma de _user.value como fallback
        loadRecommendedRoutines(_user.value)
    }
}