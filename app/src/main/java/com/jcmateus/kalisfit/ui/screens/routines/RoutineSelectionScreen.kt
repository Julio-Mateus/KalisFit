package com.jcmateus.kalisfit.ui.screens.routines

import android.icu.text.SimpleDateFormat
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.jcmateus.kalisfit.model.TipoDiaEntrenamiento
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import java.util.Date
import kotlin.text.take

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RoutineSelectionScreen(
    navController: NavHostController,
    dateInMillis: Long
) {
    val context = LocalContext.current
    val selectedDate = remember { Date(dateInMillis) } // La fecha para la que se elige rutina
    val dateFormatter = remember {
        SimpleDateFormat(
            "EEEE dd 'de' MMMM",
            Locale.getDefault()
        )
    }
    // Obtener rutinas generales y personalizadas del ViewModel
    // Obtener el NavBackStackEntry de la ruta MAIN_CONTENT.
    val mainContentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(Routes.MAIN_CONTENT)
    }
    val userViewModel: UserProfileViewModel = viewModel(viewModelStoreOwner = mainContentEntry)
    val recommendedRoutines by userViewModel.recommendedRoutines.collectAsState() // O todas las rutinas
    val userCustomRoutines by userViewModel.userCustomRoutines.collectAsState()
    // Podrías necesitar cargar todas las rutinas si `recommendedRoutines` es limitado
    // LaunchedEffect(Unit) { userViewModel.loadAllRoutinesIfNeeded() }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Rutinas Sugeridas", "Mis Rutinas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elegir Rutina para ${dateFormatter.format(selectedDate)}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Rutinas Sugeridas/Generales
                        if (recommendedRoutines.isEmpty()) {
                            item { Text("No hay rutinas sugeridas disponibles.") }
                        }
                        items(recommendedRoutines, key = { it.id }) { rutina ->
                            RoutineSelectionItemCard(
                                title = rutina.nombre,
                                subtitle = rutina.descripcion.take(100) + "...", // O algún detalle
                                imageUrl = rutina.imagenUrl,
                                onClick = {
                                    userViewModel.updateDayInWeeklyPlan(
                                        dateToUpdate = selectedDate,
                                        rutinaId = rutina.id, // o rutina.slug si es lo que usas
                                        rutinaNombre = rutina.nombre,
                                        esCustom = false,
                                        tipoDeDia = TipoDiaEntrenamiento.ENTRENAMIENTO.name
                                    )
                                    navController.popBackStack() // Volver a la pantalla anterior
                                    Toast.makeText(context, "${rutina.nombre} asignada", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    1 -> { // Mis Rutinas Personalizadas
                        if (userCustomRoutines.isEmpty()) {
                            item { Text("No tienes rutinas personalizadas guardadas.") }
                        }
                        items(userCustomRoutines, key = { it.id }) { customRutina ->
                            RoutineSelectionItemCard(
                                title = customRutina.nombrePersonalizado,
                                subtitle = "${customRutina.ejercicios.size} ejercicios",
                                imageUrl = customRutina.imagenUrl,
                                onClick = {
                                    userViewModel.updateDayInWeeklyPlan(
                                        dateToUpdate = selectedDate,
                                        rutinaId = customRutina.id,
                                        rutinaNombre = customRutina.nombrePersonalizado,
                                        esCustom = true,
                                        tipoDeDia = TipoDiaEntrenamiento.ENTRENAMIENTO.name
                                    )
                                    navController.popBackStack()
                                    Toast.makeText(context, "${customRutina.nombrePersonalizado} asignada", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                // Opción para marcar como día de descanso
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            userViewModel.updateDayInWeeklyPlan(
                                dateToUpdate = selectedDate,
                                rutinaId = null,
                                rutinaNombre = null,
                                esCustom = null,
                                tipoDeDia = TipoDiaEntrenamiento.DESCANSO.name
                            )
                            navController.popBackStack()
                            Toast.makeText(context, "Día marcado como descanso", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Marcar como Día de Descanso")
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineSelectionItemCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Seleccionar")
        }
    }
}
