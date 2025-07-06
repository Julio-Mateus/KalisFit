package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jcmateus.kalisfit.data.repositories.StoreRepository
import com.jcmateus.kalisfit.data.repositories.StoreRepositoryImpl
import kotlin.getValue


// Esta Factory es necesaria porque StoreViewModel tiene una dependencia (StoreRepository)
// en su constructor.
class StoreViewModelFactory : ViewModelProvider.Factory {

    // Creamos la instancia del repositorio aquí mismo.
    // En una app más grande o con inyección de dependencias, esto se manejaría de forma diferente.
    private val storeRepository: StoreRepository by lazy {
        StoreRepositoryImpl()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            // Pasamos la instancia del repositorio al constructor del ViewModel
            return StoreViewModel(storeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}