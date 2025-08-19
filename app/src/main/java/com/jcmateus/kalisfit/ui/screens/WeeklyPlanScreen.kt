package com.jcmateus.kalisfit.ui.screens

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.DiaDeEntrenamientoPlanificado
import com.jcmateus.kalisfit.model.TipoDiaEntrenamiento
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    navController: NavHostController,
    userViewModel: UserProfileViewModel = viewModel()
) {
    val planSemanal by userViewModel.planSemanal.collectAsState()
    val isLoading by userViewModel.isLoadingPlanSemanal.collectAsState()
    val error by userViewModel.planSemanalErrorMessage.collectAsState()
    val hoyCalendar = Calendar.getInstance()
    val context = LocalContext.current
    val isLoadingPlan by userViewModel.isLoadingPlanSemanal.collectAsState()
    // Formateador para mostrar el día de la semana y la fecha
    val dayDateFormatter = remember { SimpleDateFormat("EEE dd MMM",
        Locale.getDefault()) }
    // Puedes observar isLoadingPlanSemanal para mostrar un indicador de carga
    if (isLoadingPlan) {
        CircularProgressIndicator()
    }

// Y planSemanalErrorMessage para mostrar errores
    val planErrorMessage by userViewModel.planSemanalErrorMessage.collectAsState()
    planErrorMessage?.let {
        Text("Error: $it", color = Color.Red)
        // Considera un botón para limpiar el mensaje de error o que se limpie solo
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Plan Semanal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                // Opcional: acción para regenerar el plan
                actions = {
                    IconButton(
                        onClick = {
                            userViewModel.regenerateWeeklyPlan()
                            // Opcional: Mostrar un Toast de inicio si quieres, aunque el estado de carga lo manejará
                            // Toast.makeText(context, "Regenerando plan...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isLoadingPlan // Deshabilitar mientras está cargando/regenerando
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh, // o el ícono que prefieras
                            contentDescription = "Regenerar Plan Semanal"
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
    ) { paddingValues ->
        when {
            isLoading && planSemanal == null -> {
                LoadingIndicator(text = "Cargando plan semanal...")
            }
            error != null && planSemanal == null -> {
                ErrorState(message = error ?: "Error al cargar el plan.") {
                    userViewModel.loadPlanSemanalActual() // Asume que esto recarga/crea
                }
            }
            planSemanal != null -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = planSemanal!!.diasPlanificados,
                        key = { diaPlanificado -> diaPlanificado.fecha.seconds }
                    ) { diaPlanificado ->
                        val diaCalendar = Calendar.getInstance().apply { time = diaPlanificado.fecha.toDate() }
                        val esHoy = hoyCalendar.get(Calendar.DAY_OF_YEAR) == diaCalendar.get(Calendar.DAY_OF_YEAR) &&
                                hoyCalendar.get(Calendar.YEAR) == diaCalendar.get(Calendar.YEAR)

                        DayPlanCard(
                            dia = diaPlanificado,
                            dateFormatter = dayDateFormatter,
                            isToday = esHoy, // <--- Pasarlo aquí
                            onClick = {
                                val fechaMillis = diaPlanificado.fecha.toDate().time
                                navController.navigate(Routes.selectRoutineForDate(fechaMillis))
                            }
                        )
                    }
                }
            }
            else -> {
                NoDataCard(message = "No se pudo cargar el plan semanal.")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanCard(
    dia: DiaDeEntrenamientoPlanificado,
    dateFormatter: SimpleDateFormat,
    isToday: Boolean, // Nuevo parámetro para saber si es el día actual
    onClick: () -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = when {
            dia.completada -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.secondaryContainer // Día de entrenamiento pendiente
        }
    )
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)), // Borde para el día actual
        shape = RoundedCornerShape(16.dp), // Un radio de esquina un poco más grande
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp) // Más padding vertical
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icono del tipo de día/estado a la izquierda
            Box(
                modifier = Modifier
                    .size(48.dp) // Un poco más grande
                    .clip(CircleShape)
                    .background(
                        when {
                            dia.completada -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> MaterialTheme.colorScheme.tertiaryContainer.copy(
                                alpha = 0.7f
                            )

                            dia.rutinaIdAsignada != null -> MaterialTheme.colorScheme.secondary.copy(
                                alpha = 0.2f
                            )

                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        dia.completada -> Icons.Filled.CheckCircle
                        dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> Icons.Filled.SelfImprovement // O un icono de "luna", "relax"
                        dia.rutinaIdAsignada != null -> Icons.Filled.FitnessCenter // Podría cambiar según el tipo de rutina
                        else -> Icons.Filled.PlaylistAdd
                    },
                    contentDescription = "Estado del día",
                    tint = when {
                        dia.completada -> MaterialTheme.colorScheme.primary
                        dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> MaterialTheme.colorScheme.onTertiaryContainer
                        dia.rutinaIdAsignada != null -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormatter.format(dia.fecha.toDate()).capitalizeWords(), // Capitalizar día y mes
                    style = MaterialTheme.typography.titleMedium, // Un poco más grande
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp)) // Más espacio

                val displayText = when {
                    dia.completada && dia.nombreRutinaAsignada != null -> "Completado: ${dia.nombreRutinaAsignada}"
                    dia.completada -> "Día Completado"
                    dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> "Descanso Programado"
                    dia.nombreRutinaAsignada != null -> dia.nombreRutinaAsignada!!
                    else -> "Toca planificar" // Texto más accionable
                }
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyLarge, // Un poco más grande
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icono de flecha o acción a la derecha
            if (dia.tipoDeDia != TipoDiaEntrenamiento.DESCANSO.name && !dia.completada) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Seleccionar rutina",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
// Helper para capitalizar (puedes ponerlo en un archivo de Utils)
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }
