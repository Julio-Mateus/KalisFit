package com.jcmateus.kalisfit.data.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.KalisFitApplication
import com.jcmateus.kalisfit.model.Product
import com.jcmateus.kalisfit.util.FirestoreCollections
import com.jcmateus.kalisfit.util.FirestoreFields
import com.jcmateus.kalisfit.util.FirestoreQueryDirections
import kotlinx.coroutines.tasks.await

class StoreRepositoryImpl : StoreRepository {

    // Obteniendo la instancia de Firestore desde tu clase Application
    private val firestore: FirebaseFirestore = KalisFitApplication.FirestoreInstance.instance

    override suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val querySnapshot = firestore.collection(FirestoreCollections.PRODUCTS)
                .whereEqualTo(FirestoreFields.DISPONIBLE, true) // Solo productos disponibles
                .orderBy(FirestoreFields.FECHA_CREACION, FirestoreQueryDirections.DESCENDING) // Más nuevos primero
                .get()
                .await() // Usar await de kotlinx-coroutines-play-services para coroutines

            val products = querySnapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Log.e("StoreRepositoryImpl", "Error fetching all products", e)
            Result.failure(e)
        }
    }

    // --- NUEVA IMPLEMENTACIÓN PARA OBTENER PRODUCTO POR ID ---
    override suspend fun getProductById(productId: String): Result<Product?> {
        return try {
            Log.d("StoreRepositoryImpl", "Intentando obtener producto por ID: $productId")
            val documentSnapshot = firestore.collection(FirestoreCollections.PRODUCTS)
                .document(productId) // Utiliza el ID del documento para la búsqueda directa
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val product = documentSnapshot.toObject(Product::class.java)
                Log.d("StoreRepositoryImpl", "Producto encontrado con ID '$productId': ${product?.nombre}")
                Result.success(product)
            } else {
                Log.d("StoreRepositoryImpl", "Producto con ID '$productId' no encontrado en Firestore.")
                Result.success(null) // Producto no encontrado
            }
        } catch (e: Exception) {
            Log.e("StoreRepositoryImpl", "Error al obtener producto por ID: $productId", e)
            Result.failure(e)
        }
    }

    // --- FUNCIÓN ANTERIOR (getProductBySlug) COMENTADA ---
    // Ya que no estás usando slugs en tus datos actuales, esta función no es necesaria.
    // Puedes eliminarla completamente si lo prefieres.
    /*
    override suspend fun getProductBySlug(productSlug: String): Result<Product?> {
        return try {
            val querySnapshot = firestore.collection(FirestoreCollections.PRODUCTS)
                .whereEqualTo(FirestoreFields.SLUG, productSlug)
                .whereEqualTo(FirestoreFields.DISPONIBLE, true) // Opcional
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Result.success(null)
            } else {
                val product = querySnapshot.documents.firstOrNull()?.toObject(Product::class.java)
                Result.success(product)
            }
        } catch (e: Exception) {
            Log.e("StoreRepositoryImpl", "Error fetching product by slug: $productSlug", e)
            Result.failure(e)
        }
    }
    */

    override suspend fun getFeaturedProducts(): Result<List<Product>> {
        return try {
            val querySnapshot = firestore.collection(FirestoreCollections.PRODUCTS)
                .whereEqualTo(FirestoreFields.DISPONIBLE, true)
                .whereEqualTo(FirestoreFields.DESTACADO, true) // Filtrar por el campo 'destacado'
                .orderBy(FirestoreFields.FECHA_CREACION, FirestoreQueryDirections.DESCENDING) // Opcional: ordenar destacados
                .limit(10) // Limitar el número de productos destacados a mostrar
                .get()
                .await()

            val products = querySnapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Log.e("StoreRepositoryImpl", "Error fetching featured products", e)
            Result.failure(e)
        }
    }

    override suspend fun getProductsByCategory(category: String): Result<List<Product>> {
        return try {
            val querySnapshot = firestore.collection(FirestoreCollections.PRODUCTS)
                .whereEqualTo(FirestoreFields.DISPONIBLE, true)
                .whereArrayContains(FirestoreFields.CATEGORIAS, category) // Para campos array como 'categorias'
                .orderBy(FirestoreFields.FECHA_CREACION, FirestoreQueryDirections.DESCENDING)
                .get()
                .await()

            val products = querySnapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Log.e("StoreRepositoryImpl", "Error fetching products for category: $category", e)
            Result.failure(e)
        }
    }
}