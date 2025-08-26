package com.jcmateus.kalisfit.ui.screens.routines

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.MyRoutinesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoutinesScreen(
    navController: NavHostController,
    viewModel: MyRoutinesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var routineToDeleteId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar Snackbar para errores
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage() // Limpiar el mensaje después de mostrarlo
        }
    }
    // Snackbar para mensajes de éxito
     LaunchedEffect(uiState.successMessage) {
         uiState.successMessage?.let {
             snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
             viewModel.clearSuccessMessage()
         }
     }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Rutinas") }, // Título más corto
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentUserId = uiState.currentUserId
                        if (currentUserId != null && currentUserId.isNotBlank()) {
                            navController.navigate(Routes.editRoutine(userId = currentUserId))
                        } else {
                            Log.w("MyRoutinesScreen", "Acción Crear: currentUserId es nulo.")
                            // El Snackbar ya manejará el error si el ViewModel lo setea
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear Rutina")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // Un color más neutro para el TopAppBar
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Fondo general
        ) {
            when {
                uiState.isLoading && uiState.routines.isEmpty() -> { // Mostrar loader solo si no hay datos aún
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.routines.isEmpty() && uiState.currentUserId != null && !uiState.isLoading -> {
                    EmptyState(
                        message = "Aún no tienes rutinas personalizadas.",
                        buttonText = "¡Crea tu primera rutina!",
                        onButtonClick = {
                            val currentUserId = uiState.currentUserId!!
                            navController.navigate(Routes.editRoutine(userId = currentUserId))
                        }
                    )
                }
                uiState.currentUserId == null && !uiState.isLoading -> {
                    EmptyState(message = "Por favor, inicia sesión para ver tus rutinas.")
                }
                else -> {
                    MyRoutinesList(
                        routines = uiState.routines,
                        onRoutineClick = { clickedRoutineId ->
                            val currentUserId = uiState.currentUserId
                            if (currentUserId != null && currentUserId.isNotBlank()) {
                                navController.navigate(
                                    Routes.routineDetail(
                                        routineId = clickedRoutineId,
                                        userId = currentUserId
                                    )
                                )
                            }
                        },
                        onEditRoutineClick = { routineIdToEdit ->
                            val currentUserId = uiState.currentUserId
                            if (currentUserId != null && currentUserId.isNotBlank()) {
                                navController.navigate(
                                    Routes.editRoutine(
                                        userId = currentUserId,
                                        customRoutineId = routineIdToEdit
                                    )
                                )
                            }
                        },
                        onDeleteRoutineClick = { routineId ->
                            routineToDeleteId = routineId
                            showDeleteDialog = true
                        }
                    )
                }
            }
            // Mostrar el loader encima de la lista si isLoading es true pero ya hay rutinas (ej: durante eliminación)
            if (uiState.isLoading && uiState.routines.isNotEmpty()) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showDeleteDialog && routineToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta rutina? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        routineToDeleteId?.let { viewModel.deleteRoutine(it) }
                        showDeleteDialog = false
                        routineToDeleteId = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyState(
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        buttonText?.let { text ->
            onButtonClick?.let { clickAction ->
                Button(
                    onClick = clickAction,
                    shape = RoundedCornerShape(50) // Botón más redondeado
                ) {
                    Text(text)
                }
            }
        }
    }
}
@Composable
fun MyRoutinesList(
    routines: List<UserCustomRoutine>,
    onRoutineClick: (String) -> Unit,
    onEditRoutineClick: (String) -> Unit,
    onDeleteRoutineClick: (String) -> Unit // Nuevo callback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp), // Más padding vertical
        verticalArrangement = Arrangement.spacedBy(16.dp) // Más espacio entre ítems
    ) {
        items(routines, key = { it.id }) { routine ->
            CustomRoutineItem(
                routine = routine,
                onClick = { onRoutineClick(routine.id) },
                onEditClick = { onEditRoutineClick(routine.id) },
                onDeleteClick = { onDeleteRoutineClick(routine.id) } // Pasar el callback
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRoutineItem(
    routine: UserCustomRoutine,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit // Nuevo callback
) {
    Card(
        onClick = onClick, // Hacer toda la tarjeta clickeable
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Un poco más de elevación
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), // Esquinas más redondeadas
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            // Contenedor de la imagen
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(routine.imagenUrl) // La URL de tu imagen de Firebase Storage
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagen de ${routine.nombrePersonalizado}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Altura fija para la imagen
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp
                        )
                    ), // Redondear solo esquinas superiores
                contentScale = ContentScale.Crop // Para que la imagen cubra el espacio
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                    Box( // Placeholder mientras carga o si hay error
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state is AsyncImagePainter.State.Loading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondaryContainer)
                        } else {
                            Icon(
                                Icons.Outlined.BrokenImage,
                                contentDescription = "Error al cargar imagen",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                } else {
                    SubcomposeAsyncImageContent() // Muestra la imagen una vez cargada
                }
            }

            // Contenido de texto y acciones
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    routine.nombrePersonalizado,
                    style = MaterialTheme.typography.titleLarge, // Título más grande
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (routine.descripcion?.isNotBlank() == true) {
                    Text(
                        routine.descripcion!!,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Rondas: ${routine.numeroDeRondas} | Ej: ${routine.ejercicios.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Row {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) { // Tamaño un poco menor
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Editar Rutina",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) { // Tamaño un poco menor
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Eliminar Rutina",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}