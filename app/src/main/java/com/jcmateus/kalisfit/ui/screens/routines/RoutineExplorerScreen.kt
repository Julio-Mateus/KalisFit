package com.jcmateus.kalisfit.ui.screens.routines

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // ExperimentalLayoutApi para FlowRow
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun RoutineExplorerScreen(
    navController: NavController,
    placeFilterArgument: String?,
    levelFilterArgument: String?,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val filterAllOptionString = stringResource(R.string.filter_all_option)
    val application = LocalContext.current.applicationContext as Application
    val factory = RoutineExplorerViewModelFactory(application)
    val routineExplorerViewModel: RoutineExplorerViewModel = viewModel(factory = factory)
    val rutinas: List<Rutina> by routineExplorerViewModel.rutinasFiltradas.collectAsState()
    val isLoading by routineExplorerViewModel.isLoading.collectAsState()
    val errorMessage by routineExplorerViewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    // Estados de la UI para la búsqueda y filtros desde el ViewModel
    val searchTerm by routineExplorerViewModel.searchTerm.collectAsState()
    val isSearchAndFilterUiVisible by routineExplorerViewModel.isSearchAndFilterUiVisible.collectAsState()
    val selectedNivelVM by routineExplorerViewModel.selectedNivel.collectAsState() // Renombrar para evitar conflicto con argumento
    val selectedLugarVM by routineExplorerViewModel.selectedLugar.collectAsState()
    val selectedGrupoMuscularVM by routineExplorerViewModel.selectedGrupoMuscular.collectAsState()
    // LaunchedEffects para los argumentos de navegación
    LaunchedEffect(placeFilterArgument, routineExplorerViewModel) {
        if (placeFilterArgument != null) {
            try {
                val lugarEnum = LugarEntrenamiento.valueOf(placeFilterArgument.uppercase())
                // Solo aplicar si es diferente al estado actual del VM para evitar bucles si se navega de nuevo
                if (routineExplorerViewModel.selectedLugar.value != lugarEnum) {
                    routineExplorerViewModel.setLugarFilter(lugarEnum)
                }
            } catch (e: IllegalArgumentException) {
                if (routineExplorerViewModel.selectedLugar.value != null) {
                    routineExplorerViewModel.setLugarFilter(null)
                }
            }
        } else {
            // Si el argumento es nulo, pero el VM tiene un filtro de lugar (quizás de una acción anterior),
            // no lo limpiamos automáticamente aquí a menos que esa sea la intención deseada.
            // La UI de filtros permitirá al usuario limpiarlo.
            // Si vienes de otra pantalla sin filtro de lugar, los filtros existentes en el VM persistirán.
        }
    }
    LaunchedEffect(levelFilterArgument, routineExplorerViewModel) {
        if (routineExplorerViewModel.selectedNivel.value != levelFilterArgument) {
            routineExplorerViewModel.setNivelFilter(levelFilterArgument)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorar Rutinas") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back)
                        )
                    }
                },
                actions = {
                    // Opcional: Mostrar un botón de limpiar en la TopAppBar si la UI de búsqueda está visible
                    if (isSearchAndFilterUiVisible) {
                        IconButton(onClick = { routineExplorerViewModel.clearFilters() }) {
                            Icon(
                                Icons.Filled.ClearAll,
                                contentDescription = stringResource(R.string.clear_all_filters_desc)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplicar el padding del Scaffold aquí
        ) {
            // Contenido principal de la pantalla (tu Column)
            Column(
                modifier = Modifier.fillMaxSize() // La Column ocupa todo el Box
            ) {
                // --- Sección de Búsqueda y Filtros (Animada) ---
                AnimatedVisibility(
                    visible = isSearchAndFilterUiVisible,
                    enter = fadeIn() + expandVertically(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut() + shrinkVertically(animationSpec = tween(durationMillis = 300))
                ) {
                    SearchAndFilterSection(
                        searchTerm = searchTerm,
                        onSearchTermChange = { routineExplorerViewModel.setSearchTerm(it) },
                        selectedNivel = selectedNivelVM,
                        onNivelSelected = { nivelSeleccionado ->
                            routineExplorerViewModel.setNivelFilter(nivelSeleccionado)
                        },
                        selectedLugar = selectedLugarVM,
                        onLugarSelected = { lugarSeleccionado ->
                            routineExplorerViewModel.setLugarFilter(lugarSeleccionado)
                        },
                        selectedGrupoMuscular = selectedGrupoMuscularVM,
                        onGrupoMuscularSelected = { grupoSeleccionado ->
                            routineExplorerViewModel.setGrupoMuscularFilter(grupoSeleccionado)
                        },
                        onClearFiltersClick = { routineExplorerViewModel.clearFilters() }
                    )
                }
                // --- Contenido Principal (Lista de rutinas o mensajes) ---
                Box(
                    modifier = Modifier
                        .weight(1f) // Ocupa el espacio restante
                        .fillMaxSize()
                ) {
                    if (isLoading && rutinas.isEmpty()) { // Mostrar loading solo si no hay datos previos
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Error: $errorMessage",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        if (rutinas.isEmpty()) {
                            val noResultsText = when {
                                searchTerm.isNotBlank() || selectedNivelVM != null || selectedLugarVM != null || selectedGrupoMuscularVM != null -> stringResource(
                                    R.string.no_routines_match_search_filters
                                )

                                placeFilterArgument != null || levelFilterArgument != null -> stringResource(
                                    R.string.no_routines_match_initial_filters
                                )

                                else -> stringResource(R.string.no_routines_yet)
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.SearchOff,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = noResultsText,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items = rutinas, key = { it.id }) { rutina ->
                                    RutinaCardEnhanced(rutina = rutina, onClick = {
                                        val userIdForNavigation: String? = currentUser?.uid
                                        navController.navigate(
                                            Routes.routineDetail(
                                                rutina.id,
                                                userIdForNavigation
                                            )
                                        )
                                    })
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { routineExplorerViewModel.toggleSearchAndFilterUiVisibility() },
                modifier = Modifier
                    .align(Alignment.TopEnd) // <-- ALINEACIÓN A LA ESQUINA SUPERIOR DERECHA
                    .padding(16.dp),         // <-- Padding para separarlo de los bordes
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (isSearchAndFilterUiVisible) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (isSearchAndFilterUiVisible) stringResource(R.string.close_search_desc) else stringResource(R.string.open_search_filters_desc)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // ExperimentalLayoutApi si usas FlowRow de foundation
@Composable
fun SearchAndFilterSection(
    searchTerm: String,
    onSearchTermChange: (String) -> Unit,
    selectedNivel: String?,
    onNivelSelected: (String?) -> Unit,
    selectedLugar: LugarEntrenamiento?,
    onLugarSelected: (LugarEntrenamiento?) -> Unit,
    selectedGrupoMuscular: String?,
    onGrupoMuscularSelected: (String?) -> Unit,
    onClearFiltersClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current // Para obtener strings
    val filterAllOptionString = stringResource(R.string.filter_all_option)
    // Opciones para los filtros
    val nivelesPosibles = remember { listOf(context.getString(R.string.filter_all_option)) + listOf("Principiante", "Intermedio", "Avanzado", "Experto") }
    val lugaresPosibles =
        remember { listOf<LugarEntrenamiento?>(null) + LugarEntrenamiento.entries.toList() } // null representa "Todos"
    val gruposMuscularesPosibles = remember { // Define tus grupos musculares
        listOf(context.getString(R.string.filter_all_option), "Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Abdomen", "Full Body")
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Para que los filtros puedan scrollear si son muchos
        ) {
            // Barra de Búsqueda
            OutlinedTextField(
                value = searchTerm,
                onValueChange = onSearchTermChange,
                label = { Text(stringResource(R.string.search_routines_hint)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_icon_desc)) },
                trailingIcon = {
                    if (searchTerm.isNotBlank()) {
                        IconButton(onClick = { onSearchTermChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search_desc))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.filter_by_label), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            // Filtro de Nivel
            Text(stringResource(R.string.level_label_filter), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                nivelesPosibles.forEach { nivelOption -> // Renombrar para claridad
                    val isSelected = nivelOption.equals(selectedNivel, ignoreCase = true) || (selectedNivel == null && nivelOption == filterAllOptionString)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (nivelOption == filterAllOptionString) {
                                onNivelSelected(null) // Pasar null si "Todos" es seleccionado
                            } else {
                                onNivelSelected(nivelOption)
                            }
                        },
                        label = { Text(nivelOption) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Filtro de Lugar
            Text(stringResource(R.string.training_place_label_filter), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                lugaresPosibles.forEach { lugarOption ->
                    val isSelected = lugarOption == selectedLugar
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLugarSelected(lugarOption) }, // El ViewModel ya maneja null para "Todos" aquí
                        label = { Text(lugarOption?.toString()?.replaceFirstChar { it.titlecase(java.util.Locale.getDefault()) } ?: filterAllOptionString) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Filtro de Grupo Muscular
            Text(stringResource(R.string.muscle_group_label_filter), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                gruposMuscularesPosibles.forEach { grupoOption ->
                    val isSelected = grupoOption.equals(selectedGrupoMuscular, ignoreCase = true) || (selectedGrupoMuscular == null && grupoOption == filterAllOptionString)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (grupoOption == filterAllOptionString) {
                                onGrupoMuscularSelected(null) // Pasar null si "Todos" es seleccionado
                            } else {
                                onGrupoMuscularSelected(grupoOption)
                            }
                        },
                        label = { Text(grupoOption) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClearFiltersClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.clear_all_button))
            }
        }
    }
}
@Composable
fun RutinaCardEnhanced(rutina: Rutina, onClick: () -> Unit) {
    Log.d("RutinaCardEnhanced", "ID Rutina: ${rutina.id}, Nombre: ${rutina.nombre}, ImageURL: ${rutina.imagenUrl}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column { // Quitamos el padding aquí para que la imagen ocupe todo el ancho
            // --- INICIO: Mostrar Imagen de la Rutina ---
            if (!rutina.imagenUrl.isNullOrBlank()) {
                Log.d("RutinaCardEnhanced", "Mostrando AsyncImage para: ${rutina.imagenUrl}")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(rutina.imagenUrl)
                        .crossfade(true) // Opcional: para una carga más suave
                        // .placeholder(R.drawable.placeholder_image) // Opcional: imagen mientras carga
                        // .error(R.drawable.error_image) // Opcional: imagen si hay error
                        .build(),
                    contentDescription = "Imagen de la rutina: ${rutina.nombre}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp) // O una altura fija: .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop // O ContentScale.Fit, según prefieras
                )
            }else {
                // ----> INICIO DEBUG <----
                Log.d("RutinaCardEnhanced", "ImageURL es nula o vacía para la rutina: ${rutina.nombre}")
                // ----> FIN DEBUG <----
            }
            // --- FIN: Mostrar Imagen de la Rutina ---

            // Contenido de texto con padding
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = rutina.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (rutina.descripcion.isNotBlank()) {
                    Text(
                        text = rutina.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                RutinaDetailItemWithIcon(
                    icon = Icons.Filled.Star,
                    label = "Nivel",
                    value = rutina.nivelRecomendado.joinToString(" / ")
                )
                RutinaDetailItemWithIcon(
                    icon = Icons.Filled.CheckCircle,
                    label = "Objetivos",
                    value = rutina.objetivos.joinToString(", ")
                )
                RutinaDetailItemWithIcon(
                    icon = determinePlaceIcon(rutina.lugarEntrenamiento.firstOrNull()?.toString()), // Asegúrate que lugarEntrenamiento es String o conviértelo
                    label = "Lugares",
                    value = rutina.lugarEntrenamiento.joinToString(", ") { it.toString() } // Si es una lista de Enums
                )
            }
        }
    }
}
@Composable
fun RutinaDetailItemWithIcon(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label, // Para accesibilidad
            modifier = Modifier.size(18.dp), // Tamaño del icono
            tint = MaterialTheme.colorScheme.primary // Color del icono
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelLarge, // Un poco más grande para la etiqueta
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, // Para que no se desborde si el valor es muy largo
            overflow = TextOverflow.Ellipsis
        )
    }
}
// Helper para determinar el icono del lugar
@Composable
fun determinePlaceIcon(lugarNombre: String?): ImageVector {
    return when (lugarNombre?.uppercase()) { // Hacemos uppercase para comparar con los enums
        LugarEntrenamiento.CASA.name -> Icons.Filled.Home
        LugarEntrenamiento.GIMNASIO.name -> Icons.Filled.FitnessCenter
        LugarEntrenamiento.EXTERIOR.name -> Icons.Filled.Park // O Terrain
        LugarEntrenamiento.CALISTENIA.name -> Icons.Filled.SelfImprovement
        else -> Icons.Filled.Place // Icono por defecto
    }
}