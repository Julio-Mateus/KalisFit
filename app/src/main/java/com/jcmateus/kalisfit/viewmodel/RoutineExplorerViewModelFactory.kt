package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RoutineExplorerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pasa la instancia de Application al constructor del ViewModel
            return RoutineExplorerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}