package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.repositories.StoreRepository
import com.jcmateus.kalisfit.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- Estados de la UI ---

// Estado para la lista de productos (ej. pantalla principal de la tienda)
sealed class ProductListUiState {
    object Loading : ProductListUiState()
    data class Success(val products: List<Product>) : ProductListUiState()
    data class Error(val message: String) : ProductListUiState()
    object Empty : ProductListUiState() // Para cuando la lista de productos está vacía
}

// Estado para la vista de detalle de un producto
sealed class ProductDetailUiState {
    object Loading : ProductDetailUiState()
    data class Success(val product: Product) : ProductDetailUiState()
    object NotFound : ProductDetailUiState() // Producto no encontrado o no disponible
    data class Error(val message: String) : ProductDetailUiState()
}

// Estado para la lista de productos destacados
sealed class FeaturedProductsUiState {
    object Loading : FeaturedProductsUiState()
    data class Success(val products: List<Product>) : FeaturedProductsUiState()
    data class Error(val message: String) : FeaturedProductsUiState()
    object Empty : FeaturedProductsUiState()
}

// Estado para la lista de productos por categoría
sealed class CategoryProductsUiState {
    object Loading : CategoryProductsUiState()
    data class Success(val products: List<Product>, val category: String) : CategoryProductsUiState()
    data class Error(val message: String, val category: String) : CategoryProductsUiState()
    object Empty : CategoryProductsUiState()
}


class StoreViewModel(
    private val storeRepository: StoreRepository // El repositorio se inyecta vía constructor
) : ViewModel() {

    // --- StateFlows para la lista de todos los productos ---
    private val _productListState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val productListState: StateFlow<ProductListUiState> = _productListState.asStateFlow()

    // --- StateFlows para el detalle de un producto ---
    private val _productDetailState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val productDetailState: StateFlow<ProductDetailUiState> = _productDetailState.asStateFlow()

    // --- StateFlows para productos destacados ---
    private val _featuredProductsState = MutableStateFlow<FeaturedProductsUiState>(FeaturedProductsUiState.Loading)
    val featuredProductsState: StateFlow<FeaturedProductsUiState> = _featuredProductsState.asStateFlow()

    // --- StateFlows para productos por categoría ---
    private val _categoryProductsState = MutableStateFlow<CategoryProductsUiState>(CategoryProductsUiState.Loading)
    val categoryProductsState: StateFlow<CategoryProductsUiState> = _categoryProductsState.asStateFlow()

    init {
        loadAllProducts()
        // loadFeaturedProducts() // Opcional
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            _productListState.value = ProductListUiState.Loading
            storeRepository.getAllProducts()
                .onSuccess { products ->
                    _productListState.value = if (products.isEmpty()) {
                        ProductListUiState.Empty
                    } else {
                        ProductListUiState.Success(products)
                    }
                }
                .onFailure { exception ->
                    _productListState.value = ProductListUiState.Error(exception.message ?: "Error desconocido al cargar productos.")
                }
        }
    }

    // --- NUEVA FUNCIÓN PARA CARGAR PRODUCTO POR ID ---
    fun loadProductById(productId: String) {
        viewModelScope.launch {
            _productDetailState.value = ProductDetailUiState.Loading
            storeRepository.getProductById(productId) // Llama a la nueva función del repositorio
                .onSuccess { product ->
                    if (product != null) {
                        _productDetailState.value = ProductDetailUiState.Success(product)
                    } else {
                        _productDetailState.value = ProductDetailUiState.NotFound
                    }
                }
                .onFailure { exception ->
                    _productDetailState.value = ProductDetailUiState.Error(exception.message ?: "Error al cargar detalle del producto.")
                }
        }
    }

    // --- FUNCIÓN ANTERIOR (loadProductBySlug) ---
    // Puedes eliminar esta función si ya no vas a usar slugs para cargar detalles,
    // o mantenerla si planeas añadir slugs más adelante y quieres tener ambas opciones.
    // Por ahora, como no tienes slugs en tus datos, esta función no será útil.
    /*
    fun loadProductBySlug(slug: String) {
        viewModelScope.launch {
            _productDetailState.value = ProductDetailUiState.Loading
            storeRepository.getProductBySlug(slug)
                .onSuccess { product ->
                    if (product != null) {
                        _productDetailState.value = ProductDetailUiState.Success(product)
                    } else {
                        _productDetailState.value = ProductDetailUiState.NotFound
                    }
                }
                .onFailure { exception ->
                    _productDetailState.value = ProductDetailUiState.Error(exception.message ?: "Error al cargar detalle del producto.")
                }
        }
    }
    */

    fun loadFeaturedProducts() {
        viewModelScope.launch {
            _featuredProductsState.value = FeaturedProductsUiState.Loading
            storeRepository.getFeaturedProducts()
                .onSuccess { products ->
                    _featuredProductsState.value = if (products.isEmpty()) {
                        FeaturedProductsUiState.Empty
                    } else {
                        FeaturedProductsUiState.Success(products)
                    }
                }
                .onFailure { exception ->
                    _featuredProductsState.value = FeaturedProductsUiState.Error(exception.message ?: "Error al cargar productos destacados.")
                }
        }
    }

    fun loadProductsByCategory(category: String) {
        viewModelScope.launch {
            _categoryProductsState.value = CategoryProductsUiState.Loading
            storeRepository.getProductsByCategory(category)
                .onSuccess { products ->
                    _categoryProductsState.value = if (products.isEmpty()) {
                        CategoryProductsUiState.Empty
                    } else {
                        CategoryProductsUiState.Success(products, category)
                    }
                }
                .onFailure { exception ->
                    _categoryProductsState.value = CategoryProductsUiState.Error(
                        exception.message ?: "Error al cargar productos de la categoría: $category.",
                        category
                    )
                }
        }
    }
}