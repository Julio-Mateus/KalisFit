package com.jcmateus.kalisfit.data.repositories

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface AuthRepository {
    fun getCurrentUserId(): String? // Sincrónico, puede ser null si no está logueado
    fun observeUserId(): Flow<String?> // Un Flow para observar cambios en el estado de autenticación
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun observeUserId(): Flow<String?> = callbackFlow {
        // Inicialmente envía el estado actual
        trySend(firebaseAuth.currentUser?.uid)

        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        // Cuando el Flow se cierra, removemos el listener
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }
}