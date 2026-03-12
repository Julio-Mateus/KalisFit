package com.jcmateus.kalisfit.ui.screens.routines

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.jcmateus.kalisfit.model.Ejercicio
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineDetailViewModel

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    navController: NavHostController,
    currentUserId: String?,
) {
    val viewModel: RoutineDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(uiState.rutina?.nombre ?: "", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val uid = currentUserId ?: ""
                        uiState.rutina?.id?.let { id ->
                            navController.navigate(Routes.editRoutine(userId = uid, templateId = id))
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Personalizar")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onIniciarRutinaClicked() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("EMPEZAR", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            uiState.rutina?.let { rutina ->
                RoutineDetailContent(rutina)
            }
            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    val routineIdToExecute by viewModel.startRoutineExecution.collectAsState()
    LaunchedEffect(routineIdToExecute) {
        routineIdToExecute?.let { id ->
            navController.navigate(Routes.startRoutineExecution(id))
            viewModel.onRutinaExecutionStarted()
        }
    }
}

@Composable
fun RoutineDetailContent(rutina: Rutina) {
    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            AsyncImage(
                model = rutina.imagenUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Text(rutina.descripcion, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoIcon(Icons.Default.Timer, "${rutina.ejercicios.size * 5} min")
                    InfoIcon(Icons.Default.FitnessCenter, "${rutina.ejercicios.size} ej.")
                    InfoIcon(Icons.Default.Star, rutina.nivelRecomendado.firstOrNull() ?: "Media")
                }
            }
        }
        item {
            Text("PLAN DE ENTRENAMIENTO", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        itemsIndexed(rutina.ejercicios) { index, ej ->
            ExerciseDetailItem(ej, index == rutina.ejercicios.size - 1)
        }
    }
}

@Composable
fun InfoIcon(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExerciseDetailItem(ej: Ejercicio, isLast: Boolean) {
    Row(modifier = Modifier.padding(horizontal = 20.dp).height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            if (!isLast) Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        }
        Card(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = ej.imagenUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(ej.nombre, fontWeight = FontWeight.Bold)
                    Text("${ej.numeroDeSeries} series x ${ej.repeticionesOriginal}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
