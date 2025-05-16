package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RoutineExplorerViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineExplorerViewModel() as T // Llama al constructor sin argumentos
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}