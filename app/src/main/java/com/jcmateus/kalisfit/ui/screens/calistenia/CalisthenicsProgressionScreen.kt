package com.jcmateus.kalisfit.ui.screens.calistenia

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.CalisthenicsViewModel
import kotlinx.coroutines.launch
import com.jcmateus.kalisfit.model.UserProgressionState


@Composable
fun CalisthenicsProgressionScreen(
    mainNavController: NavHostController,
    modifier: Modifier = Modifier,
    calisthenicsViewModel: CalisthenicsViewModel = viewModel()
) {
    val progressions by calisthenicsViewModel.filteredProgressions.collectAsState()
    val expandedProgressionId by calisthenicsViewModel.expandedProgressionId.collectAsState()
    val isLoading by calisthenicsViewModel.isLoading.collectAsState()
    val errorMessage by calisthenicsViewModel.error.collectAsState()
    val userProgressionStatesMap by calisthenicsViewModel.userProgressionStates.collectAsState()
    
    val selectedCategory by calisthenicsViewModel.selectedCategory.collectAsState()
    val selectedDifficulty by calisthenicsViewModel.selectedDifficulty.collectAsState()

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

    Column(modifier = modifier.fillMaxSize()) {
        // --- Header Seccion ---
        Text(
            text = "Explorar Skills",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // --- Filtros Modernos ---
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { calisthenicsViewModel.selectCategory(it) }
        )
        
        DifficultyFilterRow(
            selectedDifficulty = selectedDifficulty,
            onDifficultySelected = { calisthenicsViewModel.selectDifficulty(it) }
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading && progressions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                progressions.isEmpty() && !isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.calisthenics_no_progressions_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(progressions, key = { it.id }) { progression ->
                            val currentProgressionState = userProgressionStatesMap[progression.id]

                            ProgressionCard(
                                progression = progression,
                                isExpanded = expandedProgressionId == progression.id,
                                userProgressionState = currentProgressionState,
                                onHeaderClick = {
                                    calisthenicsViewModel.onProgressionHeaderClick(progression.id)
                                },
                                onLevelClick = { levelId, progId ->
                                    mainNavController.navigate(Routes.calisthenicsLevelDetail(progId, levelId))
                                },
                                viewModel = calisthenicsViewModel
                            )
                        }
                    }
                    if (isLoading && progressions.isNotEmpty()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterRow(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    val categories = listOf("Todos", "Empuje", "Tracción", "Core", "Estáticos", "Piernas")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        items(categories) { cat ->
            val isSelected = (cat == "Todos" && selectedCategory == null) || (cat == selectedCategory)
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(if (cat == "Todos") null else cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant, enabled = true, selected = isSelected)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyFilterRow(
    selectedDifficulty: String?,
    onDifficultySelected: (String?) -> Unit
) {
    val difficulties = listOf("Todo Nivel", "Principiante", "Intermedio", "Avanzado")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        items(difficulties) { diff ->
            val isSelected = (diff == "Todo Nivel" && selectedDifficulty == null) || (diff == selectedDifficulty)
            FilterChip(
                selected = isSelected,
                onClick = { onDifficultySelected(if (diff == "Todo Nivel") null else diff) },
                label = { Text(diff) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )
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
        ImageLoader.Builder(context).components { add(GifDecoder.Factory()) }.build()
    }

    val isEntireProgressionCompleted = remember(progression.levels, userProgressionState) {
        isProgressionCompleted(progression, userProgressionState)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        onClick = onHeaderClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isEntireProgressionCompleted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f) 
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    progression.iconUrl?.let { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl).crossfade(true).build(),
                            imageLoader = imageLoader,
                            contentDescription = progression.name,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isEntireProgressionCompleted) {
                        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center).size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DifficultyBadge(progression.difficulty)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = progression.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = progression.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    progression.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                if (progression.levels.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calisthenics_no_levels_in_progression),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        progression.levels.sortedBy { it.order }.forEachIndexed { index, level ->
                            val isCompleted = viewModel.isLevelCompleted(progression.id, level.id)
                            val isUnlocked = viewModel.isLevelUnlocked(progression.id, level.id)

                            LevelItem(
                                level = level,
                                isFirst = index == 0,
                                isLast = index == progression.levels.size - 1,
                                isCompleted = isCompleted,
                                isUnlocked = isUnlocked,
                                onClick = { if (isUnlocked) onLevelClick(level.id, progression.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: String) {
    val color = when (difficulty) {
        "Principiante" -> Color(0xFF4CAF50)
        "Intermedio" -> Color(0xFFFFC107)
        "Avanzado" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = difficulty.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = color
        )
    }
}

fun isProgressionCompleted(progression: Progression, userProgressionState: UserProgressionState?): Boolean {
    if (userProgressionState == null || progression.levels.isEmpty()) return false
    return progression.levels.all { userProgressionState.completedLevelIds.contains(it.id) }
}

@Composable
fun LevelItem(
    level: ExerciseLevel,
    isFirst: Boolean,
    isLast: Boolean,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (isUnlocked) 1f else 0.5f
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(alpha).clickable(enabled = isUnlocked, onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(
                if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )) {
                if (isCompleted) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                } else {
                    Text(
                        text = "${level.order + 1}",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(level.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    level.targetSets?.let { ChipInfo("Sets: $it") }
                    level.targetReps?.let { ChipInfo("Reps: $it") }
                }
            }
            if (!isUnlocked) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ChipInfo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}