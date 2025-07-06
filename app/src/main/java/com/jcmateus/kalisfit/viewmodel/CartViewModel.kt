package com.jcmateus.kalisfit.viewmodel

import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.repositories.AuthRepository
import com.jcmateus.kalisfit.data.repositories.CartRepository
import com.jcmateus.kalisfit.model.CartItem
import com.jcmateus.kalisfit.model.Product
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estados para la UI del carrito
data class CartUiState(
    val cartItems: List<CartItem> = emptyList(),
    val itemCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastAddedProductId: String? = null, // Para saber si se añadió algo
    val lastRemovedProductId: String? = null // Para saber si se removió algo
)

// Eventos que la UI puede enviar al ViewModel
sealed class CartAction {
    data class AddToCart(
        val product: Product,
        val cantidad: Int,
        val talla: String? = null,
        val color: String? = null
    ) : CartAction()

    data class UpdateQuantity(val productId: String, val nuevaCantidad: Int) : CartAction()
    data class RemoveFromCart(val productId: String) : CartAction()
    object ClearCart : CartAction()
    object ClearError : CartAction() // Para limpiar mensajes de error
    object ClearLastActionResult : CartAction() // Para limpiar indicadores de acción
}

class CartViewModel(
    private val cartRepository: CartRepository,
    private val authRepository: AuthRepository // Inyecta el AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    // Para mensajes de una sola vez (ej. "Producto añadido")
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Observar el ID del usuario y cargar el carrito cuando cambie (o al inicio)
        authRepository.observeUserId()
            .flatMapLatest { userId ->
                _uiState.update { it.copy(isLoading = true) }
                if (userId != null) {
                    // Combinar los ítems del carrito y el conteo de ítems
                    combine(
                        cartRepository.getCartItems(userId),
                        cartRepository.getCartItemCount(userId)
                    ) { items, count ->
                        CartUiState(cartItems = items, itemCount = count, isLoading = false)
                    }
                } else {
                    // Usuario no logueado, carrito vacío
                    flowOf(CartUiState(isLoading = false))
                }
            }
            .catch { e ->
                _uiState.update { it.copy(error = "Error al cargar el carrito: ${e.localizedMessage}", isLoading = false) }
            }
            .onEach { newState -> _uiState.value = newState } // Actualiza el estado principal
            .launchIn(viewModelScope)
    }

    fun onAction(action: CartAction) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            viewModelScope.launch { _toastMessage.emit("Por favor, inicia sesión para usar el carrito.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) } // Indicar carga y limpiar error previo
            val result: Result<Unit> = when (action) {
                is CartAction.AddToCart -> cartRepository.addProductToCart(
                    currentUserId, action.product, action.cantidad, action.talla, action.color
                )
                is CartAction.UpdateQuantity -> cartRepository.updateCartItemQuantity(
                    currentUserId, action.productId, action.nuevaCantidad
                )
                is CartAction.RemoveFromCart -> cartRepository.removeCartItem(
                    currentUserId, action.productId
                )
                is CartAction.ClearCart -> cartRepository.clearCart(currentUserId)
                is CartAction.ClearError -> {
                    _uiState.update { it.copy(error = null, isLoading = false) }
                    return@launch
                }
                is CartAction.ClearLastActionResult -> {
                    _uiState.update { it.copy(lastAddedProductId = null, lastRemovedProductId = null, isLoading = false) }
                    return@launch
                }
            }

            result.fold(
                onSuccess = {
                    val successMessage = when (action) {
                        is CartAction.AddToCart -> "Producto '${action.product.nombre}' añadido al carrito."
                        is CartAction.UpdateQuantity -> "Cantidad actualizada."
                        is CartAction.RemoveFromCart -> "Producto eliminado del carrito."
                        is CartAction.ClearCart -> "Carrito vaciado."
                        else -> "Acción completada."
                    }
                    _toastMessage.emit(successMessage)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastAddedProductId = if (action is CartAction.AddToCart) action.product.id else null,
                            lastRemovedProductId = if (action is CartAction.RemoveFromCart) action.productId else null
                        )
                    }
                },
                onFailure = { e ->
                    _toastMessage.emit("Error: ${e.localizedMessage}")
                    _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
                }
            )
            // La actualización de la lista de ítems y el conteo vendrá del Flow en init {}
        }
    }
}