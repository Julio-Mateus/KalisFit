package com.jcmateus.kalisfit.ui.screens.routines

import android.icu.text.SimpleDateFormat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.jcmateus.kalisfit.model.TipoDiaEntrenamiento
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineSelectionScreen(
    navController: NavHostController,
    dateInMillis: Long
) {
    val context = LocalContext.current
    val selectedDate = remember { Date(dateInMillis) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()) }
    
    val mainContentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(Routes.MAIN_CONTENT)
    }
    val userViewModel: UserProfileViewModel = viewModel(viewModelStoreOwner = mainContentEntry)
    val recommendedRoutines by userViewModel.recommendedRoutines.collectAsState()
    val userCustomRoutines by userViewModel.userCustomRoutines.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PLANIFICAR DÍA", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(dateFormatter.format(selectedDate).uppercase(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("SUGERIDAS", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Star, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("MIS RUTINAS", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, null) }
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val listToDisplay = if (selectedTab == 0) recommendedRoutines else userCustomRoutines
                
                if (listToDisplay.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No hay rutinas en esta categoría", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                items(listToDisplay) { item ->
                    val title = if (selectedTab == 0) (item as com.jcmateus.kalisfit.model.Rutina).nombre else (item as com.jcmateus.kalisfit.model.UserCustomRoutine).nombrePersonalizado
                    val img = if (selectedTab == 0) (item as com.jcmateus.kalisfit.model.Rutina).imagenUrl else (item as com.jcmateus.kalisfit.model.UserCustomRoutine).imagenUrl
                    
                    SelectionRoutineCard(
                        title = title,
                        imageUrl = img,
                        onClick = {
                            userViewModel.updateDayInWeeklyPlan(
                                dateToUpdate = selectedDate,
                                rutinaId = if (selectedTab == 0) (item as com.jcmateus.kalisfit.model.Rutina).id else (item as com.jcmateus.kalisfit.model.UserCustomRoutine).id,
                                rutinaNombre = title,
                                esCustom = selectedTab == 1,
                                tipoDeDia = TipoDiaEntrenamiento.ENTRENAMIENTO.name
                            )
                            navController.popBackStack()
                            Toast.makeText(context, "Rutina asignada", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Botón de Descanso resaltado
            Surface(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
                onClick = {
                    userViewModel.updateDayInWeeklyPlan(selectedDate, null, null, null, TipoDiaEntrenamiento.DESCANSO.name)
                    navController.popBackStack()
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Hotel, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text("MARCAR COMO DÍA DE DESCANSO", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
fun SelectionRoutineCard(title: String, imageUrl: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
