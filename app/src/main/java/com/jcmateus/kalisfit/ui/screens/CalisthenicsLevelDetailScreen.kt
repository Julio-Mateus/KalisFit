package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.copy
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.CalisthenicsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
// CompositionLocal para pasar SnackbarHostState (definido en respuestas anteriores o en tu código)
val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalisthenicsLevelDetailScreen(
    navController: NavHostController,
    progressionId: String,
    levelId: String,
    viewModel: CalisthenicsViewModel = viewModel()
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(progressionId, levelId, viewModel) {
        viewModel.loadExerciseLevelDetails(progressionId, levelId)
        viewModel.loadCurrentUserProgressionStates() // Esencial para isUnlocked/isCompleted
    }

    val exerciseDetails: ExerciseLevel? by viewModel.exerciseLevelDetails.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val error: String? by viewModel.error.collectAsState()

    val isCompleted by remember(progressionId, levelId, viewModel.userProgressionStates) {
        derivedStateOf { viewModel.isLevelCompleted(progressionId, levelId) }
    }
    val isUnlocked by remember(progressionId, levelId, viewModel.userProgressionStates, viewModel.progressions) {
        derivedStateOf { viewModel.isLevelUnlocked(progressionId, levelId) }
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            viewModel.clearExerciseLevelDetails()
            viewModel.consumedNextLevelNavigation()
        }
    }

    LaunchedEffect(error, viewModel) {
        error?.let { errorMessage ->
            if (exerciseDetails != null || (isLoading && exerciseDetails == null)) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearError()
                }
            }
        }
    }

    LaunchedEffect(viewModel.nextLevelToNavigate, navController) {
        viewModel.nextLevelToNavigate.collectLatest { nextLevelPair ->
            nextLevelPair?.let { (nextProgId, nextLvlId) ->
                if (progressionId != nextProgId || levelId != nextLvlId) {
                    // Navega al siguiente nivel y elimina la pantalla actual del backstack
                    navController.navigate(Routes.calisthenicsLevelDetail(nextProgId, nextLvlId)) {
                        popUpTo(navController.currentDestination?.route ?: "") { inclusive = true }
                    }
                }
                viewModel.consumedNextLevelNavigation()
            }
        }
    }

    val topBarBackgroundColor by animateColorAsState(
        targetValue = if (scrollState.value > 200) MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Un color elevado
        else Color.Transparent, // Transparente inicialmente
        label = "TopBar Background Color Animation"
    )
    val topBarContentColor by animateColorAsState(
        targetValue = if (scrollState.value > 200) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface, // Puedes cambiar esto si la imagen de fondo es oscura
        label = "TopBar Content Color Animation"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        exerciseDetails?.name?.takeIf { it.isNotBlank() && it != stringResource(R.string.exercise_not_found_placeholder_name) }
                            ?: stringResource(R.string.title_calisthenics_level_detail_loading),
                        color = topBarContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back),
                            tint = topBarContentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBackgroundColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues -> // paddingValues aquí INCLUYE el espacio del TopAppBar si existe
        CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // El padding del Scaffold se aplica aquí para asegurar que los estados de error/carga
                    // no se solapen con un TopAppBar que podría no ser transparente.
                    // LevelDetailContent manejará su propio padding interno para el contenido principal.
                    .padding(if (exerciseDetails == null) paddingValues else PaddingValues(0.dp))
            ) {
                when {
                    isLoading && exerciseDetails == null && error == null -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    error != null && exerciseDetails == null -> {
                        ErrorStateView(
                            errorMessage = error ?: stringResource(R.string.unknown_error_occurred),
                            onRetry = {
                                viewModel.clearError()
                                viewModel.loadExerciseLevelDetails(progressionId, levelId)
                            }
                        )
                    }
                    exerciseDetails != null -> {
                        val currentDetails = exerciseDetails!!
                        if (currentDetails.name == stringResource(R.string.exercise_not_found_placeholder_name)) {
                            Text(
                                currentDetails.description,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                                    .padding(paddingValues) // Aplicar aquí también
                            )
                        } else {
                            LevelDetailContent(
                                currentDetails = currentDetails,
                                imageLoader = imageLoader,
                                isUnlocked = isUnlocked,
                                isCompleted = isCompleted,
                                isLoadingFromViewModel = isLoading,
                                onMarkAsCompleted = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.level_marked_completed_feedback, currentDetails.name),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    viewModel.markLevelAsCompletedAndPrepareNext(progressionId, levelId)
                                },
                                scrollState = scrollState,
                                // Pasamos el padding superior del Scaffold para que LevelDetailContent lo use
                                // para el espaciado inicial de su contenido si es necesario.
                                scaffoldTopPadding = paddingValues.calculateTopPadding()
                            )
                        }
                    }
                    else -> {
                        Text(
                            stringResource(R.string.info_no_data_available),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                                .padding(paddingValues) // Aplicar aquí también
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelDetailContent(
    currentDetails: ExerciseLevel,
    imageLoader: ImageLoader,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    isLoadingFromViewModel: Boolean,
    onMarkAsCompleted: () -> Unit,
    scrollState: ScrollState,
    scaffoldTopPadding: Dp // Padding superior del Scaffold
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHost.current

    val imageHeight = 300.dp
    val parallaxFactor = 0.3f
    val density = LocalDensity.current
    val imageHeightPx = with(density) { imageHeight.toPx() }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
        // El padding general se aplica dentro, después de la imagen.
    ) {
        // --- Sección de Imagen/Video (Hero) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .graphicsLayer {
                    // Parallax effect: Mueve la imagen más lento que el scroll
                    translationY = scrollState.value * parallaxFactor
                    // Efecto de desvanecimiento opcional al hacer scroll hacia arriba
                    alpha = 1f - (scrollState.value / (imageHeightPx * 1.2f)).coerceIn(0f, 1f)
                }
        ) {
            if (currentDetails.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentDetails.imageUrl)
                        .crossfade(true)
                        // Quita .placeholder() y .error() de aquí si vas a usar
                        // los parámetros de AsyncImage directamente.
                        // Si prefieres definirlos en el request, usa los IDs de drawable:
                        // .placeholder(R.drawable.ic_default_placeholder)
                        // .error(R.drawable.ic_error_placeholder)
                        .build(), // <- Esta llamada a .build() es correcta aquí.
                    imageLoader = imageLoader,
                    contentDescription = stringResource(R.string.desc_exercise_image, currentDetails.name),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    // Pasa los painters directamente a AsyncImage:
                    placeholder = painterResource(id = R.drawable.ic_default_placeholder),
                    error = painterResource(id = R.drawable.ic_error_placeholder)
                )
            } else if (currentDetails.videoUrl != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircleOutline,
                        contentDescription = stringResource(R.string.desc_exercise_video_placeholder),
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Overlay sutil para mejorar legibilidad del botón de video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            ),
                            startY = imageHeightPx / 2 // Empieza el gradiente a la mitad
                        )
                    )
            )

            currentDetails.videoUrl?.let { videoUriString ->
                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUriString))
                            ContextCompat.startActivity(context, intent, null)
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.error_opening_video_link),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.PlayCircleFilled, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_watch_video))
                }
            }
        }

        // --- Contenido Principal ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // El padding superior aquí es para el espacio DESPUÉS de la imagen hero,
                // no para evitar el TopAppBar, ya que la imagen está "encima" de este Column.
                .padding(top = 24.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = currentDetails.name,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentDetails.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            val hasTargets = currentDetails.targetSets != null || currentDetails.targetReps != null || currentDetails.targetHoldTime != null
            if (hasTargets) {
                Spacer(modifier = Modifier.height(32.dp))
                SectionTitle(title = stringResource(R.string.header_targets))
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, hoveredElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentDetails.targetSets?.let { DetailItem(label = stringResource(R.string.detail_label_sets), value = it) }
                        currentDetails.targetReps?.let { DetailItem(label = stringResource(R.string.detail_label_reps), value = it) }
                        currentDetails.targetHoldTime?.let { DetailItem(label = stringResource(R.string.detail_label_hold_time), value = it) }
                    }
                }
            }

            currentDetails.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(32.dp))
                SectionTitle(title = stringResource(R.string.detail_label_notes_tips))
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, hoveredElevation = 4.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isUnlocked && !isCompleted) {
                Button(
                    onClick = onMarkAsCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large, // O RoundedCornerShape(16.dp)
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 6.dp),
                    enabled = !isLoadingFromViewModel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.CheckCircleOutline, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_mark_as_completed), style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary))
                }
            } else if (isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) // Redondeado
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.level_already_completed),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        stringResource(R.string.level_already_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Spacer para asegurar que el último elemento no quede pegado al fondo
            // y que el contenido pueda scrollear por encima del scaffoldTopPadding si es necesario
            // (considerando el espacio del TopAppBar si no es transparente)
            Spacer(modifier = Modifier.height(scaffoldTopPadding + 32.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

@Composable
fun ErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier // Se pasa desde CalisthenicsLevelDetailScreen con el padding del Scaffold
) {
    Column(
        modifier = modifier // Este modifier ya debería tener el padding del Scaffold
            .fillMaxSize()
            .padding(16.dp), // Padding adicional interno si es necesario
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ReportProblem, // Icono más adecuado
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            stringResource(R.string.error_loading_details_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.button_retry))
        }
    }
}

@Composable
fun DetailItem(label: String, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let { nonEmptyValue ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, // Un poco más de énfasis
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(0.4f)
                    .padding(end = 8.dp) // Añadir padding al final del label
            )
            // No se necesita Spacer si el weight y el padding del label manejan el espacio
            Text(
                text = nonEmptyValue,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}