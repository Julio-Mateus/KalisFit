package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RoutineExplorerViewModelFactory(
    private val userProfileViewModel: UserProfileViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineExplorerViewModel(userProfileViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}