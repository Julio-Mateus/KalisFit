package com.jcmateus.kalisfit.data.repositories

import com.jcmateus.kalisfit.model.CartItem
import com.jcmateus.kalisfit.model.Product
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    // Obtener todos los ítems del carrito del usuario actual como un Flow
    fun getCartItems(userId: String): Flow<List<CartItem>>

    // Añadir un producto al carrito o actualizar su cantidad si ya existe
    suspend fun addProductToCart(
        userId: String,
        product: Product, // Pasamos el producto completo para obtener sus detalles
        cantidad: Int,
        talla: String? = null,
        color: String? = null
    ): Result<Unit> // Result para manejar éxito/error

    // Actualizar la cantidad de un ítem en el carrito
    suspend fun updateCartItemQuantity(userId: String, productId: String, nuevaCantidad: Int): Result<Unit>

    // Eliminar un ítem del carrito
    suspend fun removeCartItem(userId: String, productId: String): Result<Unit>

    // Limpiar todo el carrito del usuario (ej. después de un pedido)
    suspend fun clearCart(userId: String): Result<Unit>

    // Obtener el número de ítems en el carrito (para el badge)
    fun getCartItemCount(userId: String): Flow<Int>
}