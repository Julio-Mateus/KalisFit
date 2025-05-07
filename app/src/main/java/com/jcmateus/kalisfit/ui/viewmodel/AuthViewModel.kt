package com.jcmateus.kalisfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth, private val firestore: FirebaseFirestore
) : ViewModel() {

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
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
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
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
                firestore.collection("users").document(uid).set(userData).addOnSuccessListener {
                    onResult(true, null)
                }.addOnFailureListener { e ->
                    onResult(false, e.message)
                }
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun updateProfileAfterRegister(
        nivel: String,
        objetivos: List<String>,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update(mapOf("nivel" to nivel, "objetivos" to objetivos))
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

}