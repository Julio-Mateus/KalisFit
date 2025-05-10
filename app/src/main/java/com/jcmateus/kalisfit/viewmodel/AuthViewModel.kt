package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class AuthViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(true, null)
                else onResult(false, task.exception?.message)
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
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid ?: ""
                    val userData = mapOf(
                        "uid" to uid,
                        "nombre" to name,
                        "email" to email,
                        "nivel" to nivel,
                        "objetivos" to objetivos,
                        "fechaRegistro" to System.currentTimeMillis()
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)

        docRef.get().addOnSuccessListener {
            if (!it.exists()) {
                val userData = mapOf(
                    "uid" to uid,
                    "nombre" to nombre,
                    "email" to email,
                    "fechaRegistro" to System.currentTimeMillis()
                )
                docRef.set(userData).addOnSuccessListener { onFinish() }
            } else {
                onFinish()
            }
        }
    }

    fun updateProfileAfterRegister(
        nivel: String,
        objetivos: List<String>,
        peso: Float,
        altura: Float,
        edad: Int,
        sexo: String,
        frecuenciaSemanal: Int,
        lugarEntrenamiento: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "nivel" to nivel,
                    "objetivos" to objetivos,
                    "peso" to peso,
                    "altura" to altura,
                    "edad" to edad,
                    "sexo" to sexo,
                    "frecuenciaSemanal" to frecuenciaSemanal,
                    "lugarEntrenamiento" to lugarEntrenamiento
                )
            )
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }
}