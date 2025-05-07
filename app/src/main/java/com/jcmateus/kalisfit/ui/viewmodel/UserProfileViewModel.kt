package com.jcmateus.kalisfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class UserProfile(
    val nombre: String = "",
    val email: String = "",
    val fechaRegistro: String = "",
    val nivel: String = "",
    val objetivos: List<String> = emptyList()
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user

    fun loadUserProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre") ?: ""
                val email = doc.getString("email") ?: ""
                val fechaMillis = doc.getLong("fechaRegistro") ?: 0L
                val fechaFormateada = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(fechaMillis))
                val nivel = doc.getString("nivel") ?: ""
                val objetivos = doc.get("objetivos") as? List<String> ?: emptyList()
                _user.value = UserProfile(nombre, email, fechaFormateada, nivel, objetivos)
            }
    }
}