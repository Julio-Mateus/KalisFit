package com.jcmateus.kalisfit.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
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
import com.jcmateus.kalisfit.model.UserProgressionState


@Composable
fun CalisthenicsProgressionScreen(
    mainNavController: NavHostController,
    modifier: Modifier = Modifier,
    calisthenicsViewModel: CalisthenicsViewModel = viewModel()
) {
    // Observar los estados del ViewModel
    val progressions by calisthenicsViewModel.progressions.collectAsState()
    val expandedProgressionId by calisthenicsViewModel.expandedProgressionId.collectAsState()
    val isLoading by calisthenicsViewModel.isLoading.collectAsState()
    val errorMessage by calisthenicsViewModel.error.collectAsState()

    // *** MODIFICACIÓN CLAVE: Observar userProgressionStates ***
    val userProgressionStatesMap by calisthenicsViewModel.userProgressionStates.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                calisthenicsViewModel.clearError()
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when {
            isLoading && progressions.isEmpty() -> { // Mostrar cargando solo si las progresiones también están vacías inicialmente
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            progressions.isEmpty() && !isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.calisthenics_no_progressions_found))
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        //.padding(horizontal = 16.dp, vertical = 8.dp),
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(progressions, key = { it.id }) { progression ->
                        // *** MODIFICACIÓN: Obtener el estado de esta progresión específica ***
                        val currentProgressionState = userProgressionStatesMap[progression.id]

                        ProgressionCard(
                            progression = progression,
                            isExpanded = expandedProgressionId == progression.id,
                            // *** MODIFICACIÓN: Pasar el estado de la progresión ***
                            userProgressionState = currentProgressionState,
                            onHeaderClick = {
                                calisthenicsViewModel.onProgressionHeaderClick(progression.id)
                            },
                            onLevelClick = { levelId, progId -> // Nombre del parámetro corregido a progId
                                Log.d("NavigationDebug", "ProgID: $progId, LevelID: $levelId")
                                mainNavController.navigate(
                                    Routes.calisthenicsLevelDetail(
                                        progId,
                                        levelId
                                    )
                                )
                            },
                            viewModel = calisthenicsViewModel // Se sigue pasando por si se necesita para otras cosas
                            // o si prefieres llamar a los métodos para isUnlocked/isCompleted.
                            // Lo importante es que ProgressionCard se recomponga
                            // cuando `userProgressionState` cambie.
                        )
                    }
                }
                // Mostrar un indicador de carga más sutil si ya hay datos pero se está actualizando algo más
                if (isLoading && progressions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
    userProgressionState: UserProgressionState?,
    onHeaderClick: () -> Unit,
    onLevelClick: (levelId: String, progressionId: String) -> Unit,
    viewModel: CalisthenicsViewModel
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
            }
            .build()
    }

    // Determinar si la progresión entera está completada
    val isEntireProgressionCompleted = remember(progression.levels, userProgressionState) {
        isProgressionCompleted(progression, userProgressionState)
    }

    // Modificador condicional para la tarjeta
    val cardModifier = if (isEntireProgressionCompleted) {
        Modifier
            .fillMaxWidth()
            .border( // Borde distintivo si está completada
                2.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), // Color primario con algo de transparencia
                MaterialTheme.shapes.medium
            )
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onHeaderClick,
        colors = if (isEntireProgressionCompleted) { // Colores condicionales para la tarjeta
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), // Fondo sutilmente tintado
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Asegura buen contraste para el contenido
            )
        } else {
            CardDefaults.cardColors() // Colores por defecto
        }
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
                    modifier = Modifier.weight(1f) // Ocupa el espacio disponible menos el icono de expandir
                ) {
                    // Contenedor para la imagen y el badge de logro
                    Box(
                        modifier = Modifier
                            .size(100.dp) // Tamaño del contenedor de la imagen
                            .clip(MaterialTheme.shapes.medium) // Redondear esquinas si la imagen no lo hace
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
                                modifier = Modifier.matchParentSize(), // La imagen llena el Box
                                contentScale = ContentScale.Crop // o ContentScale.Fit, según preferencia
                            )
                        }

                        // Badge de "Logro/Progresión Completada"
                        if (isEntireProgressionCompleted) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium, // Icono de logro
                                contentDescription = stringResource(R.string.progression_completed_achievement), // Definir en strings.xml
                                tint = MaterialTheme.colorScheme.primary, // Color del icono
                                modifier = Modifier
                                    .align(Alignment.TopEnd) // Posiciona en la esquina superior derecha del Box
                                    .padding(6.dp) // Espacio desde los bordes del Box
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), // Fondo semi-transparente para legibilidad
                                        CircleShape // Forma circular para el fondo del badge
                                    )
                                    .padding(5.dp) // Padding interno para el icono dentro del círculo
                                    .size(20.dp) // Tamaño del icono
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(
                            1f,
                            fill = false
                        )
                    ) { // No llenar excesivamente si el texto es corto
                        Text(
                            text = progression.name,
                            style = MaterialTheme.typography.titleLarge,
                            // Color del título cambia si la progresión está completada
                            color = if (isEntireProgressionCompleted) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                        progression.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // Color estándar para descripción
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Icono para expandir/colapsar
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.calisthenics_collapse) else stringResource(
                        R.string.calisthenics_expand
                    ),
                    modifier = Modifier.size(30.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Log.d(
                    "ProgressionCard",
                    "Progression ${progression.name} expanded. Levels: ${progression.levels.size}. UserState: $userProgressionState. IsEntirelyCompleted: $isEntireProgressionCompleted"
                )
                if (progression.levels.isEmpty()) {
                    Log.w(
                        "ProgressionCard",
                        "Warning: Progression ${progression.name} has no levels to display."
                    )
                    Text(
                        text = stringResource(R.string.calisthenics_no_levels_in_progression),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        val sortedLevels = remember(progression.levels) {
                            progression.levels.sortedBy { it.order }
                        }

                        sortedLevels.forEachIndexed { index, level ->
                            // El estado del nivel se obtiene del ViewModel, que ya tiene la lógica
                            // para determinar si está completado o desbloqueado basado en userProgressionState.
                            val isCompleted = viewModel.isLevelCompleted(progression.id, level.id)
                            val isUnlocked = viewModel.isLevelUnlocked(progression.id, level.id)

                            LevelItem( // Asumo que LevelItem está definido en otro lugar o más abajo en el mismo archivo
                                level = level,
                                isFirst = index == 0,
                                isLast = index == sortedLevels.size - 1,
                                isCompleted = isCompleted,
                                isUnlocked = isUnlocked,
                                onClick = {
                                    if (isUnlocked) {
                                        onLevelClick(level.id, progression.id)
                                    } else {
                                        Log.d(
                                            "ProgressionCard",
                                            "Level ${level.name} is locked. Click ignored."
                                        )
                                        // Considerar mostrar un Snackbar o Toast aquí
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper para determinar si una progresión está completamente finalizada
fun isProgressionCompleted(
    progression: Progression,
    userProgressionState: UserProgressionState?
): Boolean {
    if (userProgressionState == null || progression.levels.isEmpty()) {
        return false
    }
    // Todos los IDs de nivel de la progresión deben estar en los completedLevelIds del usuario
    return progression.levels.all { level ->
        userProgressionState.completedLevelIds.contains(level.id)
    }
}

@Composable
fun LevelItem(
    level: ExerciseLevel, // Asegúrate que ExerciseLevel tiene name, description, targetSets, etc.
    isFirst: Boolean,
    isLast: Boolean,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    val alpha = if (isUnlocked || isCompleted) 1f else 0.5f
    // val iconColor = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(top = if (isFirst) 12.dp else 0.dp) // Añade padding superior solo al primer item
    ) {
        Column { // Envolver en Column para el Divider
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
                            .size(72.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(level.name, style = MaterialTheme.typography.titleMedium)
                    level.description.takeIf { !it.isNullOrBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        level.targetSets?.let { ChipInfo("Sets: $it") }
                        level.targetReps?.let { ChipInfo("Reps: $it") }
                        level.targetHoldTime?.let { ChipInfo("Aguante: $it") } // Asumo que tienes R.string.target_hold_time_compact
                    }
                }
                // Icono de estado
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.level_completed_desc), // Define este string
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isUnlocked) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.desc_view_level_details), // Define este string
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.level_locked_desc), // Define este string
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (!isLast) {
                Divider(
                    modifier = Modifier.padding(
                        start = if (level.imageUrl != null) (72.dp + 16.dp + 16.dp) else 16.dp,
                        end = 16.dp
                    )
                )
            }
        }
    }
}

@Composable
fun ChipInfo(text: String) {
    Surface(
        shape = CircleShape, // O RoundedCornerShape(percent = 50)
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
