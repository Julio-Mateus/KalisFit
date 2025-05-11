package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun loadUserProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre") ?: ""
                val email = doc.getString("email") ?: ""
                val nivel = doc.getString("nivel") ?: ""
                val objetivos = doc.get("objetivos") as? List<String> ?: emptyList()
                val fechaMillis = doc.getLong("fechaRegistro") ?: 0L
                val fechaFormateada =
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(fechaMillis))
                val peso = doc.getDouble("peso")?.toFloat() ?: 0f
                val altura = doc.getDouble("altura")?.toFloat() ?: 0f
                val edad = doc.getLong("edad")?.toInt() ?: 0
                val sexo = doc.getString("sexo") ?: ""
                val frecuenciaSemanal = doc.getLong("frecuenciaSemanal")?.toInt() ?: 3
                val lugarEntrenamiento = doc.getString("lugarEntrenamiento") ?: ""
                val insignias = doc.get("insignias") as? List<String> ?: emptyList()
                val rutinasCompletadas = doc.getLong("rutinasCompletadas")?.toInt() ?: 0
                val progresoActual = doc.getString("progresoActual") ?: ""

                _user.value = UserProfile(
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
                    lugarEntrenamiento = listOf(lugarEntrenamiento),
                    insignias = emptyList(),
                    rutinasCompletadas = 0,
                    progresoActual = ""
                )
            }
    }
}