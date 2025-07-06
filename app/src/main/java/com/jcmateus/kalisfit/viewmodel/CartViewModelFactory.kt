package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jcmateus.kalisfit.data.repositories.AuthRepository
import com.jcmateus.kalisfit.data.repositories.CartRepository

class CartViewModelFactory(
    private val cartRepository: CartRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(cartRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}