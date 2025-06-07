package com.jcmateus.kalisfit.ui.screens

import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.CalisthenicsViewModel
import kotlinx.coroutines.launch
import coil.decode.GifDecoder


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
                                onLevelClick = { levelId, progressionId ->
                                    Log.d("NavigationDebug", "ProgID: $progressionId, LevelID: $levelId")
                                    // La navegación solo debería ocurrir si el nivel está desbloqueado,
                                    // lo cual ya se maneja dentro de LevelItem.
                                    mainNavController.navigate(Routes.calisthenicsLevelDetail(progressionId, levelId))
                                },
                                viewModel = calisthenicsViewModel // <--- Pasar el ViewModel
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
    onLevelClick: (levelId: String, progressionId: String) -> Unit, // Modificado para pasar ambos IDs
    viewModel: CalisthenicsViewModel // <--- Añadir ViewModel como parámetro
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
        }
        .build()

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
                    progression.iconUrl?.let { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .placeholder(R.drawable.ic_default_placeholder)
                                .error(R.drawable.ic_error_placeholder)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = progression.name,
                            modifier = Modifier.size(100.dp), // Aumentado para mejor visualización
                            contentScale = ContentScale.Fit // Fit o Crop según preferencia
                        )
                        Spacer(Modifier.width(16.dp)) // Aumentado espacio
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = progression.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        progression.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium, // BodyMedium para mejor lectura
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, // Limitar líneas para evitar que sea muy largo
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.calisthenics_collapse) else stringResource(R.string.calisthenics_expand),
                    modifier = Modifier.size(30.dp) // Un poco más grande
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Log.d("ProgressionCard", "Progression ${progression.name} expanded. Levels count: ${progression.levels.size}")
                if (progression.levels.isEmpty()) {
                    Log.w("ProgressionCard", "Warning: Progression ${progression.name} has no levels to display.")
                    Text(
                        text = stringResource(R.string.calisthenics_no_levels_in_progression),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    // Asegúrate de que progression.levels esté ordenado por 'order'
                    // Esto ya debería venir así desde el ViewModel (gracias a FirestoreUtils)
                    progression.levels.forEachIndexed { index, level ->
                        Log.d("ProgressionCard", "Displaying level: ${level.name} (ID: ${level.id}, Order: ${level.order})")

                        // Obtener estado del nivel desde el ViewModel
                        val isCompleted = viewModel.isLevelCompleted(progression.id, level.id)
                        val isUnlocked = viewModel.isLevelUnlocked(progression.id, level.id)

                        LevelItem(
                            level = level,
                            isFirst = index == 0,
                            isLast = index == progression.levels.size - 1,
                            isCompleted = isCompleted, // <--- Pasar estado
                            isUnlocked = isUnlocked,   // <--- Pasar estado
                            onClick = {
                                if (isUnlocked) { // Solo permitir click si está desbloqueado
                                    onLevelClick(level.id, progression.id)
                                } else {
                                    // Opcional: Mostrar un Snackbar o mensaje indicando que está bloqueado
                                    // Log.d("ProgressionCard", "Level ${level.name} is locked.")
                                }
                            }
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
    isCompleted: Boolean, // <--- Nuevo parámetro
    isUnlocked: Boolean,  // <--- Nuevo parámetro
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components { add(GifDecoder.Factory()) }
        .build()

    // Atenuar visualmente si el nivel no está desbloqueado (y no está completado)
    val alpha = if (isUnlocked || isCompleted) 1f else 0.5f // Atenuar si está bloqueado
    val iconColor = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha) // Aplicar alpha para feedback visual de bloqueo
            .clickable(enabled = isUnlocked, onClick = onClick) // Clickable solo si está desbloqueado
            .padding(top = if (isFirst) 12.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            level.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_default_placeholder)
                        .error(R.drawable.ic_error_placeholder)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = level.name,
                    modifier = Modifier
                        .size(72.dp) // Tamaño ligeramente ajustado
                        .clip(MaterialTheme.shapes.medium), // Forma más redondeada
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(level.name, style = MaterialTheme.typography.titleMedium)
                // Descripción opcional del nivel
                level.description.takeIf { !it.isNullOrBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall, // Más pequeño para la descripción
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Mostrar metas de forma más compacta
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    level.targetSets?.let { ChipInfo("Sets: $it") }
                    level.targetReps?.let { ChipInfo("Reps: $it") }
                    level.targetHoldTime?.let { ChipInfo("Aguante: $it") }
                }
            }
            // Icono de estado (completado o flecha)
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.level_completed_desc),
                    tint = MaterialTheme.colorScheme.primary, // Color distintivo para completado
                    modifier = Modifier.size(24.dp)
                )
            } else if (isUnlocked) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.desc_view_level_details),
                    tint = MaterialTheme.colorScheme.primary // Opcional: hacerlo más prominente si está desbloqueado
                )
            } else {
                Icon( // Icono de bloqueo si no está desbloqueado ni completado
                    Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.level_locked_desc),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    if (!isLast) {
        Divider(
            modifier = Modifier.padding(
                start = if (level.imageUrl != null) (72.dp + 16.dp + 16.dp) else 16.dp, // (tamaño imagen + spacer + padding)
                end = 16.dp
            )
        )
    }
}

// Un pequeño Composable para mostrar la información en forma de Chip (opcional, para estética)
@Composable
fun ChipInfo(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall, // labelSmall para texto de chip
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}