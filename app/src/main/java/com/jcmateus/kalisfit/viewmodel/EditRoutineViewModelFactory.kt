package com.jcmateus.kalisfit.viewmodel

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class EditRoutineViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val defaultArgs: Bundle? = null,
    // Puedes pasar instancias ya creadas si las tienes disponibles
    // o instanciarlas dentro de la factory como se muestra abajo.
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle // El SavedStateHandle es provisto por AbstractSavedStateViewModelFactory
    ): T {
        if (modelClass.isAssignableFrom(EditRoutineViewModel::class.java)) {
            Log.d("ViewModelFactory", "Instantiating EditRoutineViewModel (SIMPLIFIED for test)")
            // Pasamos el 'handle' provisto, y las instancias de storage y firestore
            return EditRoutineViewModel(handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}. Was expecting EditRoutineViewModel.")
    }
}