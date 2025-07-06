package com.jcmateus.kalisfit.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.repositories.AuthRepositoryImpl
import com.jcmateus.kalisfit.data.repositories.CartRepositoryImpl
import com.jcmateus.kalisfit.model.CartItem
import com.jcmateus.kalisfit.viewmodel.CartAction
import com.jcmateus.kalisfit.viewmodel.CartViewModel
import com.jcmateus.kalisfit.viewmodel.CartViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    cartRepository: CartRepositoryImpl = remember { CartRepositoryImpl(FirebaseFirestore.getInstance()) },
    authRepository: AuthRepositoryImpl = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }
) {
    val cartViewModelFactory = remember(cartRepository, authRepository) {
        CartViewModelFactory(cartRepository, authRepository)
    }
    val cartViewModel: CartViewModel = viewModel(factory = cartViewModelFactory)

    val cartUiState by cartViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { // Key puede ser Unit ya que el toastMessage flow no cambia
        cartViewModel.toastMessage.collect { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val subtotalCalculado = remember(cartUiState.cartItems) {
        // Asumiendo que priceValue es en la unidad mínima (ej. centavos)
        // y quieres mostrarlo en la unidad principal (ej. pesos).
        // Si priceValue ya está en la unidad principal como Double, puedes quitar / 100.0
        cartUiState.cartItems.sumOf { (it.priceValue / 100.0) * it.cantidad }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mi Carrito") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
                // colors = TopAppBarDefaults.topAppBarColors(...) // Opcional: para styling
            )
        },
        bottomBar = {
            // Usa cartUiState.cartItems para la condición
            if (cartUiState.cartItems.isNotEmpty()) {
                CartSummaryBottomBar(
                    subtotal = subtotalCalculado, // Usa el subtotal calculado
                    onCheckoutClicked = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Proceso de Checkout no implementado aún.")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                cartUiState.isLoading && cartUiState.cartItems.isEmpty() -> { // Mostrar carga solo si no hay items aún
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                cartUiState.error != null -> {
                    Text(
                        text = "Error: ${cartUiState.error}",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                // Usa cartUiState.cartItems para la condición
                cartUiState.cartItems.isEmpty() && !cartUiState.isLoading -> {
                    EmptyCartView(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    CartItemsList(
                        cartItems = cartUiState.cartItems, // Usa cartItems de tu UiState
                        onQuantityChanged = { cartItemId, newQuantity -> // cartItemId será productId aquí
                            cartViewModel.onAction(CartAction.UpdateQuantity(cartItemId, newQuantity))
                        },
                        onRemoveItem = { cartItemId -> // cartItemId será productId aquí
                            cartViewModel.onAction(CartAction.RemoveFromCart(cartItemId))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
@Composable
fun CartItemsList(
    cartItems: List<CartItem>,
    onQuantityChanged: (productId: String, newQuantity: Int) -> Unit,
    onRemoveItem: (productId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Usamos productId como key. Si productId + talla + color definen la unicidad,
        // podrías hacer: key = { "${it.productId}-${it.tallaSeleccionada}-${it.colorSeleccionado}" }
        items(cartItems, key = { it.productId }) { cartItem ->
            CartListItem(
                cartItem = cartItem,
                onQuantityChanged = { newQuantity ->
                    onQuantityChanged(cartItem.productId, newQuantity)
                },
                onRemoveItem = {
                    onRemoveItem(cartItem.productId)
                }
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    }
}
@Composable
fun CartListItem(
    cartItem: CartItem,
    onQuantityChanged: (newQuantity: Int) -> Unit,
    onRemoveItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember(cartItem.currencyCode) { // Recalcula si el currencyCode cambia
        NumberFormat.getCurrencyInstance().apply {
            // Intenta establecer la moneda directamente si es posible,
            // sino usa el Locale por defecto con el formateador.
            // Para "COP", un Locale como ("es", "CO") sería más robusto.
            try {
                currency = java.util.Currency.getInstance(cartItem.currencyCode)
            } catch (e: Exception) {
                // Fallback a un Locale común si el código no es reconocido directamente
                // o si quieres un formato específico.
                // Para COP, Locale("es", "CO") es bueno.
                val locale = java.util.Locale("es", "CO") // Ejemplo para COP
                val tempFormatter = NumberFormat.getCurrencyInstance(locale)
                currency = tempFormatter.currency
            }
        }
    }

    val itemPriceForDisplay = remember(cartItem.priceValue) {
        cartItem.priceValue / 100.0 // Asumiendo que es en centavos
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cartItem.imagenUrl) // USADO: cartItem.imagenUrl
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.ic_default_placeholder),
                error = painterResource(R.drawable.ic_error_placeholder),
                contentDescription = cartItem.nombre, // USADO: cartItem.nombre
                modifier = Modifier
                    .size(90.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.nombre, // USADO: cartItem.nombre
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (cartItem.tallaSeleccionada != null || cartItem.colorSeleccionado != null) {
                    Text(
                        text = listOfNotNull(cartItem.tallaSeleccionada, cartItem.colorSeleccionado).joinToString(" / "), // USADO: tallaSeleccionada, colorSeleccionado
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = currencyFormatter.format(itemPriceForDisplay), // USADO: priceValue (a través de itemPriceForDisplay)
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { if (cartItem.cantidad > 1) onQuantityChanged(cartItem.cantidad - 1) }, // USADO: cartItem.cantidad
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            enabled = cartItem.cantidad > 1, // USADO: cartItem.cantidad
                            shape = CircleShape
                        ) { Text("-", fontSize = 16.sp) }

                        Text(
                            text = "${cartItem.cantidad}", // USADO: cartItem.cantidad
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        OutlinedButton(
                            onClick = { onQuantityChanged(cartItem.cantidad + 1) }, // USADO: cartItem.cantidad
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape
                            // Considerar stock si lo añades a CartItem: enabled = cartItem.cantidad < cartItem.stockDisponible
                        ) { Text("+", fontSize = 16.sp) }
                    }

                    IconButton(onClick = onRemoveItem) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Eliminar del carrito",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun CartSummaryBottomBar(
    subtotal: Double,
    onCheckoutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(
        java.util.Locale(
            "es",
            "CO"
        )
    ) } // Asumiendo COP

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = currencyFormatter.format(subtotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCheckoutClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("PROCEDER AL PAGO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


@Composable
fun EmptyCartView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.RemoveShoppingCart,
            contentDescription = "Carrito vacío",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Tu carrito está vacío",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Parece que aún no has añadido productos a tu carrito.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}