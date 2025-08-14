package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineExplorerScreen(
    navController: NavController,
    placeFilterArgument: String?,
    levelFilterArgument: String?,
    routineExplorerViewModel: RoutineExplorerViewModel = viewModel(factory = RoutineExplorerViewModelFactory()),
    authViewModel: AuthViewModel = viewModel()
) {
    val rutinas: List<Rutina> by routineExplorerViewModel.rutinasFiltradas.collectAsState()
    val isLoading by routineExplorerViewModel.isLoading.collectAsState()
    val errorMessage by routineExplorerViewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(placeFilterArgument, routineExplorerViewModel) {
        if (placeFilterArgument != null) {
            try {
                val lugarEnum = LugarEntrenamiento.valueOf(placeFilterArgument.uppercase())
                routineExplorerViewModel.setLugarFilter(lugarEnum)
            } catch (e: IllegalArgumentException) {
                routineExplorerViewModel.setLugarFilter(null)
            }
        } else {
            if (routineExplorerViewModel.selectedLugar.value != null) {
                routineExplorerViewModel.setLugarFilter(null)
            }
        }
    }
    LaunchedEffect(levelFilterArgument, routineExplorerViewModel) {
        // Asume que levelFilterArgument puede ser null directamente
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
                .padding(innerPadding)
                .fillMaxSize()
                //.background(MaterialTheme.colorScheme.surface) // Fondo general de la pantalla
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column( // Para centrar icono y texto
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline, // Un icono de error
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                if (rutinas.isEmpty()) {
                    Column( // Para centrar icono y texto
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff, // Un icono más descriptivo
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (placeFilterArgument != null ||
                                routineExplorerViewModel.selectedNivel.value != null ||
                                routineExplorerViewModel.selectedGrupoMuscular.value != null)
                                "No hay rutinas que coincidan con tu búsqueda."
                            else
                                "Aún no hay rutinas aquí. ¡Vuelve pronto!",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleMedium, // Un poco más grande
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = rutinas, key = { it.id }) { rutina ->
                            RutinaCardEnhanced(rutina = rutina, onClick = { // Usamos la nueva tarjeta
                                val userIdForNavigation: String? = currentUser?.uid
                                navController.navigate(Routes.routineDetail(rutina.id, userIdForNavigation))
                            })
                        }
                    }
                }
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