package com.jcmateus.kalisfit.data.repositories

import com.jcmateus.kalisfit.model.Product


interface StoreRepository {

    /**
     * Obtiene todos los productos disponibles de la tienda, usualmente ordenados
     * por fecha de creación descendente (los más nuevos primero) o por nombre.
     * @return Result<List<Product>> Un objeto Result que contiene la lista de productos
     *         si la operación es exitosa, o una excepción si falla.
     */
    suspend fun getAllProducts(): Result<List<Product>>

    /**
     * Obtiene un producto específico por su ID.
     * @param productId El ID único del producto a buscar.
     * @return Result<Product?> Un objeto Result que contiene el producto si se encuentra,
     *         null si no, o una excepción si la operación falla.
     */
    suspend fun getProductById(productId: String): Result<Product?> // <--- DESCOMENTADA Y ES LA QUE USAREMOS

    /**
     * Obtiene un producto específico por su slug. (Opcional si no tienes slugs)
     * @param productSlug El slug único del producto a buscar.
     * @return Result<Product?> Un objeto Result que contiene el producto si se encuentra
     *         y está disponible, null si no se encuentra o no está disponible,
     *         o una excepción si la operación falla.
     */
    // suspend fun getProductBySlug(productSlug: String): Result<Product?> // <--- COMENTADA O ELIMINADA SI NO LA USAS

    /**
     * Obtiene una lista de productos destacados.
     * @return Result<List<Product>> Un objeto Result con la lista de productos destacados.
     */
    suspend fun getFeaturedProducts(): Result<List<Product>>

    /**
     * Obtiene productos por una categoría específica.
     * @param category La categoría por la cual filtrar los productos.
     * @return Result<List<Product>> Un objeto Result con la lista de productos de esa categoría.
     */
    suspend fun getProductsByCategory(category: String): Result<List<Product>>

    // Podrías añadir más funciones aquí en el futuro, como:
    // suspend fun searchProducts(query: String): Result<List<Product>>
}