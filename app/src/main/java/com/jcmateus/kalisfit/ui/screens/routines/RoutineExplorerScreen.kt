package com.jcmateus.kalisfit.ui.screens.routines

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.AuthViewModel
import com.jcmateus.kalisfit.viewmodel.RoutineExplorerViewModel
import com.jcmateus.kalisfit.viewmodel.RoutineExplorerViewModelFactory

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterialApi::class
)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun RoutineExplorerScreen(
    navController: NavController,
    placeFilterArgument: String?,
    levelFilterArgument: String?,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val factory = RoutineExplorerViewModelFactory(application)
    val viewModel: RoutineExplorerViewModel = viewModel(factory = factory)

    val rutinas by viewModel.rutinasFiltradas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val searchTerm by viewModel.searchTerm.collectAsState()
    val isSearchVisible by viewModel.isSearchAndFilterUiVisible.collectAsState()
    val selectedNivel by viewModel.selectedNivel.collectAsState()
    val selectedLugar by viewModel.selectedLugar.collectAsState()
    // 1. Estado de Pull to Refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.refreshRutinas() }
    )
    LaunchedEffect(placeFilterArgument) {
        placeFilterArgument?.let {
            try {
                val lugar = LugarEntrenamiento.valueOf(it.uppercase())
                viewModel.setLugarFilter(lugar)
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(levelFilterArgument) {
        levelFilterArgument?.let { viewModel.setNivelFilter(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Explorar", fontWeight = FontWeight.Black)
                        Text(
                            "Encuentra tu rutina ideal",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearchAndFilterUiVisibility() }) {
                        Icon(
                            if (isSearchVisible) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                            null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState) // 2. Añadir el gesto
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SearchAndFilterSection(
                        searchTerm = searchTerm,
                        onSearchChange = { viewModel.setSearchTerm(it) },
                        selectedNivel = selectedNivel,
                        onNivelSelected = { viewModel.setNivelFilter(it) },
                        selectedLugar = selectedLugar,
                        onLugarSelected = { viewModel.setLugarFilter(it) },
                        onClear = { viewModel.clearFilters() }
                    )
                }
                when {
                    // Caso 1 : Está cargando por primera vez y no hay datos en caché
                    isLoading && rutinas.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                            CircularProgressIndicator()
                        }
                    }
                    // Caso 2: Hay un error (ej. problema serio con Firestore)
                    errorMessage != null && rutinas.isEmpty() -> {
                        ErrorState(msg = errorMessage!!)
                    }
                    // Caso 3: No hay rutinas (ya sea por internet o por filtros)
                    rutinas.isEmpty() -> {
                        // Si no hay filtros puestos, es probable que sea error de conexión
                        if (searchTerm.isEmpty() && selectedNivel == null && selectedLugar == null){
                            EmptyStateView(message = "No pudimos cargar las rutinas. Revisa tu conexión.")
                        } else {
                            EmptyExplorerState()
                        }
                    }
                    // Caso 4: TOdo bien, mostramos la lista
                    else -> {
                        RoutineList(
                            rutinas = rutinas,
                            onRoutineClick = { rutina ->
                                navController.navigate(
                                    Routes.routineDetail(rutina.id, currentUser?.uid)
                                )
                            }
                        )
                    }
                }
            }
            // 4. El indicador visual de carga arriba
            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
@Composable
fun RoutineList(
    rutinas: List<Rutina>,
    onRoutineClick: (Rutina) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(rutinas, key = { it.id }) { rutina ->
            ExplorerRoutineCard(rutina) {
                onRoutineClick(rutina)
            }
        }
    }
}
@Composable
fun EmptyStateView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.WifiOff, // Un icono que represente la falta de red
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSection(
    searchTerm: String,
    onSearchChange: (String) -> Unit,
    selectedNivel: String?,
    onNivelSelected: (String?) -> Unit,
    selectedLugar: LugarEntrenamiento?,
    onLugarSelected: (LugarEntrenamiento?) -> Unit,
    onClear: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchTerm,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar rutinas...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Niveles
            listOf("Principiante", "Intermedio", "Avanzado").forEach { nivel ->
                FilterChip(
                    selected = selectedNivel == nivel,
                    onClick = { onNivelSelected(if (selectedNivel == nivel) null else nivel) },
                    label = { Text(nivel) }
                )
            }
            // Lugares
            LugarEntrenamiento.entries.forEach { lugar ->
                FilterChip(
                    selected = selectedLugar == lugar,
                    onClick = { onLugarSelected(if (selectedLugar == lugar) null else lugar) },
                    label = { Text(lugar.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }

            if (selectedNivel != null || selectedLugar != null || searchTerm.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Limpiar", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ExplorerRoutineCard(rutina: Rutina, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = rutina.imagenUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                                startY = 0f,
                                endY = 100f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(
                        rutina.nombre,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bolt,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            rutina.nivelRecomendado.firstOrNull() ?: "General",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.Filled.Timer,
                            null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${rutina.ejercicios.size * 5} min",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Filled.Add,
                        null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyExplorerState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No encontramos rutinas con esos filtros",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(msg: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("Error: $msg", color = MaterialTheme.colorScheme.error)
    }
}
