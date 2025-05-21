package com.jcmateus.kalisfit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.viewmodel.CalisthenicsViewModel
import kotlinx.coroutines.launch


@Composable
fun CalisthenicsProgressionScreen(
    mainNavController: NavHostController,
    calisthenicsViewModel: CalisthenicsViewModel = viewModel() // Inyectar/obtener el ViewModel
) {
    // Observar los estados del ViewModel
    val progressions by calisthenicsViewModel.progressions.collectAsState()
    val expandedProgressionId by calisthenicsViewModel.expandedProgressionId.collectAsState()
    val isLoading by calisthenicsViewModel.isLoading.collectAsState()
    val errorMessage by calisthenicsViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Ya no necesitamos llamar a fetchCalisthenicsProgressions() aquí si se llama en el init del ViewModel
    // Si no, puedes usar un LaunchedEffect(Unit) { calisthenicsViewModel.fetchCalisthenicsProgressions() }

    // Mostrar Snackbar si hay un mensaje de error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                calisthenicsViewModel.clearError() // Limpiar el error después de mostrarlo
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplicar padding del Scaffold
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                progressions.isEmpty() && !isLoading -> { // Solo mostrar si no está cargando y está vacío
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.calisthenics_no_progressions_found))
                        // Opcional: Añadir un botón para reintentar
                        // Button(onClick = { calisthenicsViewModel.fetchCalisthenicsProgressions() }) {
                        // Text("Reintentar")
                        // }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(progressions, key = { it.id }) { progression ->
                            ProgressionCard(
                                progression = progression,
                                isExpanded = expandedProgressionId == progression.id,
                                onHeaderClick = {
                                    calisthenicsViewModel.onProgressionHeaderClick(progression.id)
                                },
                                onLevelClick = { levelId, progressionId -> // Pasar también progresión ID
                                    // TODO: Navegar a una pantalla de detalle del ejercicio o de registro
                                    println("Nivel clickeado: $levelId (Progresión ID: $progressionId, Nombre: ${progression.name})")
                                    // mainNavController.navigate("${Routes.CALISTHENICS_LEVEL_DETAIL_SCREEN}/$progressionId/$levelId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionCard(
    progression: Progression,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    onLevelClick: (levelId: String, progressionId: String) -> Unit // Modificado para pasar ambos IDs
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onHeaderClick
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Usar AsyncImage para el icono de la progresión desde URL
                    progression.iconUrl?.let { imageUrl -> // Renombré 'it' a 'imageUrl' para claridad
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl) // Usar la URL
                                .crossfade(true)
                                .placeholder(R.drawable.ic_default_placeholder) // Pasar el ID del recurso directamente
                                .error(R.drawable.ic_error_placeholder)         // Pasar el ID del recurso directamente
                                .build(),
                            contentDescription = progression.name, // Esto está bien aquí
                            modifier = Modifier.size(40.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) { // fill = false para que no empuje el icono de expandir
                        Text(
                            text = progression.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        progression.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.calisthenics_collapse) else stringResource(R.string.calisthenics_expand),
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    progression.levels.forEachIndexed { index, level ->
                        LevelItem(
                            level = level,
                            isFirst = index == 0,
                            isLast = index == progression.levels.size - 1,
                            onClick = { onLevelClick(level.id, progression.id) } // Pasar ambos IDs
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelItem(
    level: ExerciseLevel,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = if (isFirst) 12.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Usar AsyncImage para la imagen del nivel desde URL
            level.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_default_placeholder) // O una versión más pequeña si la tienes
                        .error(R.drawable.ic_error_placeholder)         // O una versión más pequeña si la tienes
                        .build(),
                    contentDescription = level.name, // USA EL NOMBRE DEL NIVEL para la descripción
                    modifier = Modifier
                        .size(60.dp) // El tamaño era 60.dp en tu código original para LevelItem
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop // Era Crop en tu código original para LevelItem
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(level.name, style = MaterialTheme.typography.titleMedium)
                level.description?.let { // Hacer la descripción opcional también
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    level.targetSets?.let {
                        Text("Series: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    level.targetReps?.let {
                        Text("Reps: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    level.targetHoldTime?.let {
                        Text("Aguante: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            // Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ver detalle") // Opcional
        }
    }
    if (!isLast) {
        Divider(modifier = Modifier.padding(start = if (level.imageUrl != null) 88.dp else 16.dp, end = 16.dp)) // Ajustar indentación del divisor
    }
}