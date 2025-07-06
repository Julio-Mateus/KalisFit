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
    // En init o donde configures listeners
    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }
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
                        "fechaRegistro" to Timestamp.now() // <--- CAMBIO AQUÍ
                        // Puedes añadir otros campos de UserProfile con valores iniciales/por defecto aquí
                        // si tu data class UserProfile los tiene y quieres que se creen al registrarse.
                        // Por ejemplo:
                        // "peso" to 0f,
                        // "altura" to 0f,
                        // "edad" to 0,
                        // "sexo" to "",
                        // "frecuenciaSemanal" to 3, // O un valor por defecto
                        // "lugarEntrenamiento" to emptyList<String>(),
                        // "insignias" to emptyList<String>(),
                        // "rutinasCompletadas" to 0,
                        // "progresoActual" to "",
                        // "fotoUrl" to ""
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w("AuthViewModel", "saveUserIfNew: UID es nulo, no se puede guardar.")
            onFinish() // Llama a onFinish para no bloquear el flujo, pero registra el problema.
            return
        }
        val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)

        docRef.get().addOnSuccessListener { documentSnapshot -> // Cambiado 'it' por 'documentSnapshot' para claridad
            if (!documentSnapshot.exists()) {
                val userData = mapOf(
                    "uid" to uid,
                    "nombre" to nombre,
                    "email" to email,
                    "fechaRegistro" to Timestamp.now() // <--- CAMBIO AQUÍ
                    // Similar a register, considera añadir otros campos de UserProfile con valores iniciales
                    // si esta función puede ser la primera vez que se crea el perfil.
                )
                docRef.set(userData)
                    .addOnSuccessListener {
                        Log.d("AuthViewModel", "Nuevo usuario guardado desde saveUserIfNew.")
                        onFinish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthViewModel", "Error al guardar nuevo usuario en saveUserIfNew: ${e.message}", e)
                        onFinish() // Llama a onFinish incluso si hay error para desbloquear
                    }
            } else {
                Log.d("AuthViewModel", "El usuario ya existe, no se guardó desde saveUserIfNew.")
                onFinish()
            }
        }.addOnFailureListener { e ->
            Log.e("AuthViewModel", "Error al obtener documento en saveUserIfNew: ${e.message}", e)
            onFinish() // Llama a onFinish en caso de error de lectura
        }
    }


    fun updateProfileAfterRegister(
        nivel: String,
        objetivos: List<String>, // Ya es List<String>, ¡bien!
        peso: Float,
        altura: Float,
        edad: Int,
        sexo: String,
        frecuenciaSemanal: Int,
        lugarEntrenamiento: List<String>, // <--- CAMBIADO DE String A List<String>
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            onResult(false, "Usuario no autenticado.") // Añadir mensaje de error y return
            return
        }

        // Usar .update() es bueno si el documento ya existe y solo quieres añadir/modificar estos campos.
        // Si 'register' ya creó el documento con algunos datos y esto es para completar,
        // .update() es más seguro que .set() sin merge, para no borrar campos existentes accidentalmente.
        // Si 'register' NO crea el documento del usuario en la colección 'users' y esta es la primera escritura,
        // entonces .set(..., SetOptions.merge()) o simplemente .set(...) si sabes que no hay nada antes, sería la opción.
        // Dado que 'register' SÍ crea el documento, .update() está bien o .set(..., SetOptions.merge()).
        // Voy a asumir que quieres actualizar/añadir estos campos, así que .update() es correcto.

        val profileUpdates = mapOf(
            "nivel" to nivel,
            "objetivos" to objetivos, // Firebase maneja List<String> como un Array
            "peso" to peso,
            "altura" to altura,
            "edad" to edad,
            "sexo" to sexo,
            "frecuenciaSemanal" to frecuenciaSemanal,
            "lugarEntrenamiento" to lugarEntrenamiento // Firebase lo guardará como un Array
            // NO añadas fechaRegistro aquí si ya la estableciste en la función `register`
            // o si solo quieres actualizarla en momentos específicos.
            // Si esta función también puede ser llamada para un usuario que se registró
            // pero no completó el onboarding inmediatamente, y quieres que la fecha de "completado de perfil"
            // se actualice, podrías añadir:
            // "fechaCompletadoPerfil" to FieldValue.serverTimestamp() // O System.currentTimeMillis()
        )

        firestore.collection("users").document(uid)
            .update(profileUpdates) // .update() es correcto si el doc ya existe por la función register
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Perfil actualizado después del registro.")
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Error al actualizar el perfil después del registro", e)
                onResult(false, e.message)
            }
    }
}