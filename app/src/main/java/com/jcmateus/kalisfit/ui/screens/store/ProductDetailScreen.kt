package com.jcmateus.kalisfit.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.repositories.AuthRepositoryImpl
import com.jcmateus.kalisfit.data.repositories.CartRepositoryImpl
import com.jcmateus.kalisfit.model.Product
import com.jcmateus.kalisfit.ui.screens.stoicism.SectionTitle
import com.jcmateus.kalisfit.viewmodel.CartAction
import com.jcmateus.kalisfit.viewmodel.CartViewModel
import com.jcmateus.kalisfit.viewmodel.CartViewModelFactory
import com.jcmateus.kalisfit.viewmodel.ProductDetailUiState
import com.jcmateus.kalisfit.viewmodel.StoreViewModel
import com.jcmateus.kalisfit.viewmodel.StoreViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavHostController,
    productId: String,
    storeViewModel: StoreViewModel = viewModel(factory = StoreViewModelFactory())
    // Aquí es donde instanciamos CartViewModel usando su factory
    // Necesitas una forma de obtener/crear cartRepository y authRepository
) {
    // ---- Instanciación de Dependencias para CartViewModelFactory ----
    // Opción 1: Crear nuevas instancias aquí (si no son singletons y no necesitas la misma instancia en otro lado)
    val cartRepository = remember { CartRepositoryImpl(FirebaseFirestore.getInstance()) }
    val authRepository = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }

    // Opción 2: Obtener de un CompositionLocal, o si las creaste en un Composable padre y las pasas como parámetros.
    // Ejemplo si las pasaras como parámetros (tendrías que añadir a la firma de ProductDetailScreen):
    // val cartRepository: CartRepository = passedCartRepository
    // val authRepository: AuthRepository = passedAuthRepository

    // Crear la factory para CartViewModel
    val cartViewModelFactory = remember(cartRepository, authRepository) {
        CartViewModelFactory(cartRepository, authRepository)
    }
    val cartViewModel: CartViewModel = viewModel(factory = cartViewModelFactory)
    // -----------------------------------------------------------------

    LaunchedEffect(productId) {
        storeViewModel.loadProductById(productId)
    }

    val productDetailState by storeViewModel.productDetailState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observar mensajes del CartViewModel para Snackbars
    LaunchedEffect(key1 = cartViewModel) {
        cartViewModel.toastMessage.collect { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { /* El título se puede dejar vacío */ },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = productDetailState) {
                is ProductDetailUiState.Loading -> CircularProgressIndicator()
                is ProductDetailUiState.Success -> {
                    ProductDetailsContent(
                        product = state.product,
                        scaffoldPadding = paddingValues,
                        onAddToCart = { product, cantidad, talla, color ->
                            cartViewModel.onAction(
                                CartAction.AddToCart(
                                    product = product,
                                    cantidad = cantidad,
                                    talla = talla,
                                    color = color
                                )
                            )
                        }
                    )
                }
                is ProductDetailUiState.NotFound -> Text(
                    "Producto no encontrado.",
                    modifier = Modifier.padding(paddingValues)
                )
                is ProductDetailUiState.Error -> Text(
                    "Error: ${state.message}",
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailsContent(
    product: Product,
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onAddToCart: (product: Product, cantidad: Int, talla: String?, color: String?) -> Unit // <--- NUEVO PARÁMETRO
) {
    var selectedTalla by remember(product.id) { mutableStateOf(product.tallasDisponibles.firstOrNull()) }
    var selectedColor by remember(product.id) { mutableStateOf(product.coloresDisponibles.firstOrNull()) }
    var cantidad by remember(product.id) { mutableStateOf(1) }


    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 0.dp)
            .padding(bottom = scaffoldPadding.calculateBottomPadding() + 16.dp + 56.dp) // Añadido espacio para el botón de abajo
    ) {
        // --- Sección de Imagen Principal ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imagenUrl)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.ic_default_placeholder), // Asegúrate que estos drawables existan
                error = painterResource(R.drawable.ic_error_placeholder),     // Asegúrate que estos drawables existan
                contentDescription = "Imagen de ${product.nombre}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // --- Contenido Principal (Nombre, Precio, Marca, etc.) ---
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
            if (product.marca?.isNotBlank() == true) { // Simplificado el chequeo de null y blank
                Text(
                    text = product.marca!!.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = product.nombre,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = product.priceDisplay,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // --- Disponibilidad y Envío ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val isAvailable = product.disponible && (product.stock == null || product.stock!! > 0)
                val stockStatus = when {
                    isAvailable -> "En Stock"
                    product.disponible && product.stock == 0 -> "Pocas Unidades (Consultar)"
                    else -> "No Disponible"
                }
                val stockColor = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Icon(
                    if (isAvailable) Icons.Filled.CheckCircleOutline else Icons.Filled.ErrorOutline,
                    contentDescription = "Disponibilidad",
                    tint = stockColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stockStatus, style = MaterialTheme.typography.bodyMedium, color = stockColor)

                if (product.envioGratis) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Filled.LocalShipping,
                        contentDescription = "Envío Gratis",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Envío Gratis",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }


            // --- Selección de Tallas (si aplica) ---
            if (product.tallasDisponibles.isNotEmpty()) {
                SectionTitle("Tallas Disponibles") // Asegúrate que SectionTitle esté definido
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    product.tallasDisponibles.forEach { talla ->
                        ChipSelector( // Asegúrate que ChipSelector esté definido
                            text = talla,
                            isSelected = talla == selectedTalla,
                            onClick = { selectedTalla = talla }
                        )
                    }
                }
            }

            // --- Selección de Colores (si aplica) ---
            if (product.coloresDisponibles.isNotEmpty()) {
                SectionTitle("Colores Disponibles") // Asegúrate que SectionTitle esté definido
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    product.coloresDisponibles.forEach { colorName ->
                        ChipSelector( // Asegúrate que ChipSelector esté definido
                            text = colorName,
                            isSelected = colorName == selectedColor,
                            onClick = { selectedColor = colorName }
                        )
                    }
                }
            }

            // --- Cantidad ---
            SectionTitle("Cantidad") // Asegúrate que SectionTitle esté definido
            QuantitySelector( // Asegúrate que QuantitySelector esté definido
                cantidad = cantidad,
                onIncrement = {
                    if (product.stock == null || cantidad < product.stock!!) {
                        cantidad++
                    }
                },
                onDecrement = {
                    if (cantidad > 1) {
                        cantidad--
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp),
                maxStock = product.stock
            )


            // --- Botón Principal de Acción (Añadir al Carrito) ---
            Button(
                onClick = {
                    // LLAMAR A LA LAMBDA PASADA COMO PARÁMETRO
                    onAddToCart(product, cantidad, selectedTalla, selectedColor)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp), // Este padding es dentro de la columna de contenido principal
                shape = MaterialTheme.shapes.medium,
                enabled = product.disponible && (product.stock == null || product.stock!! > 0)
            ) {
                Icon(
                    Icons.Filled.AddShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("AÑADIR AL CARRITO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }


            // --- Descripción del Producto ---
            if (product.descripcion?.isNotBlank() == true) {
                SectionTitle("Descripción del Producto")
                Text(
                    text = product.descripcion!!, // No necesitas !! si ya comprobaste con ?.isNotBlank()
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }


            // --- Especificaciones Adicionales ---
            val especificaciones = mutableListOf<Pair<String, String>>()
            product.material?.takeIf { it.isNotBlank() }?.let { especificaciones.add("Material" to it) }
            product.tipoProducto?.takeIf { it.isNotBlank() }?.let { especificaciones.add("Tipo" to it) }
            product.sku?.takeIf { it.isNotBlank() }?.let { especificaciones.add("SKU" to it) }


            if (especificaciones.isNotEmpty()) {
                SectionTitle("Especificaciones")
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    especificaciones.forEach { (label, value) ->
                        SpecificationRow(label = label, value = value) // Asegúrate que SpecificationRow esté definido
                    }
                }
            }

            // --- Categorías y Etiquetas ---
            val tags = (product.categorias + product.etiquetas).distinct().filter { it.isNotBlank() }
            if (tags.isNotEmpty()) {
                SectionTitle("Etiquetas")
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        SuggestionChip( // El SuggestionChip de Material 3
                            onClick = { /* TODO: Lógica para navegar o filtrar por este tag */ },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipSelector(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small, // Forma más rectangular para los chips
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, // Fondo transparente
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), // Un resaltado sutil
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SpecificationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

// --- Definición del QuantitySelector ---
@Composable
fun QuantitySelector(
    cantidad: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    maxStock: Int? = null // Opcional: para deshabilitar el botón de incremento si se alcanza el stock
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), // Borde sutil
                RoundedCornerShape(8.dp) // Esquinas redondeadas
            )
            .padding(horizontal = 4.dp, vertical = 2.dp) // Padding interno
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = cantidad > 1 // Deshabilitar si la cantidad es 1
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Reducir cantidad",
                tint = if (cantidad > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }

        Text(
            text = "$cantidad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp) // Espacio alrededor del número
        )

        IconButton(
            onClick = onIncrement,
            enabled = maxStock == null || cantidad < maxStock // Deshabilitar si se alcanza el stock máximo
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Aumentar cantidad",
                tint = if (maxStock == null || cantidad < maxStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

