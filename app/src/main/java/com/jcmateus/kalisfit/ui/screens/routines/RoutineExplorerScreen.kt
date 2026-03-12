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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    LaunchedEffect(placeFilterArgument) {
        placeFilterArgument?.let {
            try {
                val lugar = LugarEntrenamiento.valueOf(it.uppercase())
                viewModel.setLugarFilter(lugar)
            } catch (e: Exception) {}
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
                        Text("Encuentra tu rutina ideal", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearchAndFilterUiVisibility() }) {
                        Icon(if (isSearchVisible) Icons.Filled.FilterListOff else Icons.Filled.FilterList, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

            if (isLoading && rutinas.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (errorMessage != null) {
                ErrorState(errorMessage!!)
            } else if (rutinas.isEmpty()) {
                EmptyExplorerState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(rutinas, key = { it.id }) { rutina ->
                        ExplorerRoutineCard(rutina) {
                            navController.navigate(Routes.routineDetail(rutina.id, currentUser?.uid))
                        }
                    }
                }
            }
        }
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
        
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
    }
}

@Composable
fun ExplorerRoutineCard(rutina: Rutina, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.height(200.dp)) {
            AsyncImage(
                model = rutina.imagenUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(rutina.nombre, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rutina.nivelRecomendado.firstOrNull() ?: "General", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.Timer, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${rutina.ejercicios.size * 5} min", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun EmptyExplorerState() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.SearchOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Text("No encontramos lo que buscas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ErrorState(msg: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("Error: $msg", color = MaterialTheme.colorScheme.error)
    }
}
