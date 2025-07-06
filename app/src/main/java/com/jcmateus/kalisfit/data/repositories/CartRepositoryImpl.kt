package com.jcmateus.kalisfit.data.repositories


import android.system.Os
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jcmateus.kalisfit.model.CartItem
import com.jcmateus.kalisfit.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


// Define un TAG constante para el logging
private const val TAG = "CartRepositoryImpl"

class CartRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CartRepository {

    private fun cartItemsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("cartItems")

    override fun getCartItems(userId: String): Flow<List<CartItem>> =
        callbackFlow {
            val listenerRegistration = cartItemsCollection(userId)
                .orderBy("addedAt")
                .addSnapshotListener { snapshot, error: FirebaseFirestoreException? -> // Especificar tipo de error
                    if (error != null) {
                        Log.w(TAG, "Error escuchando ítems del carrito", error) // Correcto uso de Log.w
                        close(error) // Cierra el Flow con el error de Firestore
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val cartItems = snapshot.toObjects(CartItem::class.java)
                        trySend(cartItems).isSuccess
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }

    override suspend fun addProductToCart(
        userId: String,
        product: Product,
        cantidad: Int,
        talla: String?,
        color: String?
    ): Result<Unit> {
        return try {
            val cartItemDocRef = cartItemsCollection(userId).document(product.id)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(cartItemDocRef)
                // Usar ?.toObject en lugar de .toObject para manejar el caso donde el documento no existe
                val existingCartItem = snapshot.toObject(CartItem::class.java)
                val currentQuantity = existingCartItem?.cantidad ?: 0
                val newQuantity = currentQuantity + cantidad

                if (newQuantity <= 0) {
                    transaction.delete(cartItemDocRef)
                } else {
                    val cartItem = CartItem(
                        productId = product.id,
                        nombre = product.nombre,
                        priceDisplay = product.priceDisplay,
                        priceValue = product.priceValue,
                        currencyCode = product.currencyCode,
                        imagenUrl = product.imagenUrl,
                        cantidad = newQuantity,
                        tallaSeleccionada = talla,
                        colorSeleccionado = color
                        // addedAt es manejado por @ServerTimestamp en el modelo
                    )
                    transaction.set(cartItemDocRef, cartItem)
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error añadiendo producto al carrito: ${product.id}", e) // Correcto uso de Log.e
            Result.failure(e)
        }
    }

    override suspend fun updateCartItemQuantity(
        userId: String,
        productId: String,
        nuevaCantidad: Int
    ): Result<Unit> {
        return try {
            val cartItemDocRef = cartItemsCollection(userId).document(productId)
            if (nuevaCantidad <= 0) {
                cartItemDocRef.delete().await()
            } else {
                cartItemDocRef.update(
                    mapOf(
                        "cantidad" to nuevaCantidad,
                        "addedAt" to FieldValue.serverTimestamp() // Usar FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando cantidad del ítem: $productId", e) // Correcto uso de Log.e
            Result.failure(e)
        }
    }

    override suspend fun removeCartItem(userId: String, productId: String): Result<Unit> {
        return try {
            cartItemsCollection(userId).document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando ítem del carrito: $productId", e) // Correcto uso de Log.e
            Result.failure(e)
        }
    }

    override suspend fun clearCart(userId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val snapshot = cartItemsCollection(userId).get().await()
            snapshot.documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando el carrito", e) // Correcto uso de Log.e
            Result.failure(e)
        }
    }

    override fun getCartItemCount(userId: String): Flow<Int> = callbackFlow {
        val listenerRegistration = cartItemsCollection(userId)
            .addSnapshotListener { snapshot, error: FirebaseFirestoreException? -> // Especificar tipo de error
                if (error != null) {
                    Log.w(TAG, "Error escuchando conteo de ítems", error) // Correcto uso de Log.w
                    trySend(0).isSuccess // Opcional: enviar 0 en error o close(error)
                    // close(error) // Si prefieres propagar el error y que el colector lo maneje
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    var totalCount = 0
                    snapshot.toObjects(CartItem::class.java).forEach { cartItem ->
                        totalCount += cartItem.cantidad
                    }
                    trySend(totalCount).isSuccess
                } else {
                    trySend(0).isSuccess
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
}