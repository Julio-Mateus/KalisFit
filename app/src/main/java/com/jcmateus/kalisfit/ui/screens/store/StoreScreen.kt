package com.jcmateus.kalisfit.ui.screens.store

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.Product
import com.jcmateus.kalisfit.viewmodel.ProductListUiState
import com.jcmateus.kalisfit.viewmodel.StoreViewModel
import com.jcmateus.kalisfit.viewmodel.StoreViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onProductClick: (productId: String) -> Unit, // <--- CALLBACK para notificar clic
    modifier: Modifier = Modifier,
    storeViewModel: StoreViewModel = viewModel(factory = StoreViewModelFactory())
) {
    val productListUiState by storeViewModel.productListState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = productListUiState) {
            is ProductListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ProductListUiState.Success -> {
                if (state.products.isNotEmpty()) {
                    ProductGrid(
                        products = state.products,
                        onProductClick = { productId ->
                            // Simplemente invoca el callback que se le pasó a StoreScreen
                            onProductClick(productId) // <--- USA EL CALLBACK
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // ProductListUiState.Empty maneja el caso de lista vacía
            }
            is ProductListUiState.Empty -> {
                Text(
                    text = "No hay productos disponibles en este momento.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is ProductListUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProductGrid(
    products: List<Product>,
    onProductClick: (productId: String) -> Unit, // <--- CAMBIO: Ahora espera productId
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(
            items = products,
            key = { product -> product.id } // Usar product.id como clave es correcto y ya estaba así
        ) { product ->
            ProductItemCard(
                product = product,
                onClick = {
                    // Ahora directamente usamos product.id, asumiendo que siempre está presente
                    // y es válido si el producto existe.
                    if (product.id.isNotBlank()) { // Buena práctica verificar, aunque debería ser siempre true
                        onProductClick(product.id) // <--- CAMBIO: Pasa product.id
                    } else {
                        // Este caso es menos probable si 'id' viene del JSON y es el ID del documento,
                        // pero es bueno tener un log por si acaso.
                        Log.e(
                            "ProductGrid",
                            "Error: Producto encontrado sin un ID válido. Nombre: ${product.nombre}"
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Mantenlo si Card usa APIs experimentales
@Composable
fun ProductItemCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // .aspectRatio(0.75f) // Ajusta para la proporción deseada de la tarjeta (ej. más alta que ancha)
            .clip(MaterialTheme.shapes.medium), // Usar formas del tema
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Color de fondo sutil
    ) {
        Column {
            // --- Sección de Imagen con Badges Superpuestos ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Imagen cuadrada para consistencia
                    .background(MaterialTheme.colorScheme.surface) // Fondo para la imagen si no carga
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imagenUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.ic_default_placeholder),
                    error = painterResource(R.drawable.ic_error_placeholder),
                    contentDescription = "Imagen de ${product.nombre}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Crop para llenar el espacio
                )

                // Badges/Etiquetas superpuestos
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (product.destacado) {
                        ProductBadge(
                            text = "DESTACADO",
                            icon = Icons.Filled.Star,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (product.envioGratis) {
                        ProductBadge(
                            text = "ENVÍO GRATIS",
                            icon = Icons.Filled.LocalShipping,
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                // Podrías añadir un gradiente sutil en la parte inferior de la imagen
                // para que el texto de abajo (si lo pones sobre la imagen) sea más legible.
            }

            // --- Sección de Información del Producto ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp) // Más padding
            ) {
                if (product.marca != null && product.marca!!.isNotBlank()) {
                    Text(
                        text = product.marca!!.uppercase(), // MARCA EN MAYÚSCULAS
                        style = MaterialTheme.typography.labelSmall, // Estilo pequeño para la marca
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = product.nombre,
                    style = MaterialTheme.typography.titleMedium, // Título un poco más grande
                    fontWeight = FontWeight.SemiBold, // Un poco más de peso
                    maxLines = 2, // Permitir dos líneas para el nombre
                    overflow = TextOverflow.Ellipsis, // Cortar si es muy largo
                    minLines = 2, // Para mantener la altura consistente si algunos nombres son cortos
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = product.priceDisplay,
                    style = MaterialTheme.typography.bodyLarge, // Precio prominente
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // Color primario para el precio
                )

                // Opcional: Mostrar stock de forma sutil si es bajo
                product.stock?.let { stock ->
                    if (stock > 0 && stock < 5) { // Ejemplo: si quedan menos de 5
                        Text(
                            text = "¡Solo quedan $stock!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error, // Color de advertencia
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (stock == 0 && !product.disponible) {
                        Text(
                            text = "AGOTADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductBadge(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                backgroundColor,
                RoundedCornerShape(percent = 50)
            ) // Forma de píldora
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(14.dp) // Icono pequeño
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall, // Texto pequeño para el badge
            fontWeight = FontWeight.Medium
        )
    }
}

