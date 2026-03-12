package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class AuthViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        Log.d("KALISFIT_DEBUG", "--- INTENTO DE LOGIN ---")
        Log.d("KALISFIT_DEBUG", "Email: '$email', Password Length: ${password.length}")

        if (email.isBlank() || password.isBlank()) {
            Log.e("KALISFIT_DEBUG", "Error: Campos vacíos detectados antes de llamar a Firebase")
            onResult(false, "El correo y la contraseña no pueden estar vacíos.")
            return
        }

        try {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("KALISFIT_DEBUG", "Login Exitoso")
                        onResult(true, null)
                    } else {
                        Log.e("KALISFIT_DEBUG", "Login Fallido: ${task.exception?.message}")
                        onResult(false, task.exception?.message ?: "Error de autenticación")
                    }
                }
        } catch (e: Exception) {
            Log.e("KALISFIT_DEBUG", "Excepción atrapada en login: ${e.message}")
            onResult(false, e.message)
        }
    }

    fun register(
        email: String,
        password: String,
        name: String,
        nivel: String,
        objetivos: List<String>,
        onResult: (Boolean, String?) -> Unit
    ) {
        Log.d("KALISFIT_DEBUG", "--- INTENTO DE REGISTRO ---")
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            onResult(false, "Por favor completa todos los campos obligatorios.")
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid ?: ""
                    val userData = mapOf(
                        "uid" to uid,
                        "nombre" to name,
                        "email" to email.trim(),
                        "nivel" to nivel,
                        "objetivos" to objetivos,
                        "fechaRegistro" to Timestamp.now()
                    )
                    firestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { e -> onResult(false, e.message) }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun saveUserIfNew(nombre: String, email: String, onFinish: () -> Unit) {
        val uid = firebaseAuth.currentUser?.uid ?: return onFinish()
        val docRef = firestore.collection("users").document(uid)

        docRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                val userData = mapOf(
                    "uid" to uid,
                    "nombre" to nombre,
                    "email" to email.trim(),
                    "fechaRegistro" to Timestamp.now()
                )
                docRef.set(userData).addOnCompleteListener { onFinish() }
            } else {
                onFinish()
            }
        }.addOnFailureListener { onFinish() }
    }

    fun updateProfileAfterRegister(
        nivel: String,
        objetivos: List<String>,
        peso: Float,
        altura: Float,
        edad: Int,
        sexo: String,
        frecuenciaSemanal: Int,
        lugarEntrenamiento: List<String>,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            onResult(false, "Usuario no autenticado.")
            return
        }

        val profileUpdates = mapOf(
            "nivel" to nivel,
            "objetivos" to objetivos,
            "peso" to peso,
            "altura" to altura,
            "edad" to edad,
            "sexo" to sexo,
            "frecuenciaSemanal" to frecuenciaSemanal,
            "lugarEntrenamiento" to lugarEntrenamiento
        )

        firestore.collection("users").document(uid)
            .update(profileUpdates)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    // Funciones requeridas por la pantalla de Running
    fun updateLocationPermission(isGranted: Boolean) {}
    fun onSummaryDone() {}
}