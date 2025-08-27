package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.widget.Toast
import androidx.compose.animation.core.copy
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.DiaDeEntrenamientoPlanificado
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.TipoDiaEntrenamiento
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.RoutineExplorerViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import java.util.Date
/*
// --- COMPOSABLES AUXILIARES (sin cambios si ya estaban bien) ---
@Composable
fun LoadingIndicator(text: String) { // Tu Composable local
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator() // El de Material 3
            Spacer(modifier = Modifier.height(8.dp))
            Text(text)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) { // Tu Composable local
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

 */

@Composable
fun NoDataCard(message: String) { // Tu Composable local
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(message, modifier = Modifier
            .padding(16.dp)
            .align(Alignment.CenterHorizontally))
    }
}
// --- FIN COMPOSABLES AUXILIARES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    navController: NavHostController,
) {
    val mainContentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(Routes.MAIN_CONTENT)
    }
    val userViewModel: UserProfileViewModel = viewModel(viewModelStoreOwner = mainContentEntry)
    val routineExplorerViewModel: RoutineExplorerViewModel = viewModel(viewModelStoreOwner = mainContentEntry)

    val planSemanal by userViewModel.planSemanal.collectAsState()
    val isLoadingPlan by userViewModel.isLoadingPlanSemanal.collectAsState()
    val errorPlan by userViewModel.planSemanalErrorMessage.collectAsState()

    val hoyCalendar = Calendar.getInstance()
    val context = LocalContext.current

    val dayDateFormatter = remember { SimpleDateFormat("EEE dd MMM", Locale.getDefault()) }

    var showReminderDialog by remember { mutableStateOf(false) }
    var routineInfoForDialog by remember { mutableStateOf<Triple<String, String, Date>?>(null) }

    // No necesitas este if (isLoadingPlan) { CircularProgressIndicator() } aquí,
    // el 'when' block ya maneja los estados de carga.

    // El planErrorMessage?.let ya no es necesario aquí si el when lo maneja.
    // Quítalo o asegúrate de que no sea redundante con el ErrorState del 'when'.
    // errorPlan?.let { /* ... */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Plan Semanal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { userViewModel.regenerateWeeklyPlan() },
                        enabled = !isLoadingPlan
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Regenerar Plan Semanal")
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
            isLoadingPlan && planSemanal == null -> {
                // Usando tu LoadingIndicator local. Si quieres el de M3, usa el path completo.
                LoadingIndicator(text = "Cargando plan semanal...")
            }
            errorPlan != null && planSemanal == null -> {
                // Usando tu ErrorState local
                ErrorState(message = errorPlan ?: "Error al cargar el plan.") {
                    userViewModel.loadPlanSemanalActual()
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

                        DayPlanCard( // Asumiendo que DayPlanCard está en el mismo paquete o importado
                            dia = diaPlanificado,
                            dateFormatter = dayDateFormatter,
                            isToday = esHoy,
                            onClick = {
                                if (diaPlanificado.tipoDeDia != TipoDiaEntrenamiento.DESCANSO.name && !diaPlanificado.completada) {
                                    val fechaMillis = diaPlanificado.fecha.toDate().time
                                    navController.navigate(Routes.selectRoutineForDate(fechaMillis))
                                }
                            },
                            onScheduleReminderClick = { rutinaId, rutinaNombre, fechaProgramada ->
                                routineInfoForDialog = Triple(rutinaId, rutinaNombre, fechaProgramada)
                                showReminderDialog = true
                            }
                        )
                    }
                }
            }
            else -> {
                com.jcmateus.kalisfit.ui.screens.NoDataCard(message = "No hay plan semanal disponible.")
            }
        }
        if (showReminderDialog && routineInfoForDialog != null) {
            val (rutinaId, rutinaNombre, fechaDelDia) = routineInfoForDialog!! // Non-null assert es seguro por la condición
            ScheduleWeeklyReminderDialog( // Asumiendo que ScheduleWeeklyReminderDialog está en el mismo paquete o importado
                //routineId = rutinaId,
                routineName = rutinaNombre,
                dayDate = fechaDelDia,
                onDismiss = { showReminderDialog = false }, // *** CORREGIDO: Pasar onDismiss ***
                onSchedule = { timeInMillis, isRepeating, intervalMillis ->
                    val rutinaParaRecordatorio = Rutina( // *** ASEGÚRATE DE IMPORTAR Rutina ***
                        id = rutinaId,
                        nombre = rutinaNombre
                        // Los demás campos usarán sus valores por defecto si están definidos en la data class Rutina
                    )

                    routineExplorerViewModel.scheduleRoutineReminder( // *** ASEGÚRATE DE IMPORTAR RoutineExplorerViewModel ***
                        rutina = rutinaParaRecordatorio,
                        timeInMillis = timeInMillis,
                        isRepeating = isRepeating,
                        intervalMillis = intervalMillis
                    )
                    showReminderDialog = false
                    Toast.makeText(context, "Recordatorio para '$rutinaNombre' programado.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanCard(
    dia: DiaDeEntrenamientoPlanificado,
    dateFormatter: SimpleDateFormat,
    isToday: Boolean,
    onClick: () -> Unit,
    // --- NUEVO CALLBACK ---
    onScheduleReminderClick: (rutinaId: String, rutinaNombre: String, fechaProgramada: Date) -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = when {
            dia.completada -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    )
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        onClick = {
            // Solo permitir clic para seleccionar rutina si no es día de descanso y no está completada
            if (dia.tipoDeDia != TipoDiaEntrenamiento.DESCANSO.name && !dia.completada) {
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box( /* ... Icono del tipo de día ... */
                modifier = Modifier
                    .size(48.dp)
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
                        dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> Icons.Filled.SelfImprovement
                        dia.rutinaIdAsignada != null -> Icons.Filled.FitnessCenter
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
                    text = dateFormatter.format(dia.fecha.toDate()).capitalizeWords(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                val displayText = when {
                    dia.completada && dia.nombreRutinaAsignada != null -> "Completado: ${dia.nombreRutinaAsignada}"
                    dia.completada -> "Día Completado"
                    dia.tipoDeDia == TipoDiaEntrenamiento.DESCANSO.name -> "Descanso Programado"
                    dia.rutinaIdAsignada != null -> dia.nombreRutinaAsignada!!
                    else -> "Toca planificar"
                }
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- Icono de acción a la derecha ---
            if (dia.rutinaIdAsignada != null && dia.tipoDeDia != TipoDiaEntrenamiento.DESCANSO.name && !dia.completada) {
                // Si hay una rutina asignada y no es descanso ni está completada, mostrar icono de campana
                IconButton(
                    onClick = {
                        onScheduleReminderClick(
                            dia.rutinaIdAsignada!!, // Sabemos que no es null por la condición
                            dia.nombreRutinaAsignada ?: "Rutina Planificada", // Nombre por defecto
                            dia.fecha.toDate()
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Programar recordatorio",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (dia.tipoDeDia != TipoDiaEntrenamiento.DESCANSO.name && !dia.completada) {
                // Si no hay rutina asignada (se puede planificar) Y no es descanso ni está completada
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Seleccionar rutina",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            // No mostrar ningún icono de acción si es día de descanso o ya está completada
        }
    }
}
// --- NUEVO: Diálogo específico para WeeklyPlanScreen ---
@SuppressLint("DefaultLocale")
@Composable
fun ScheduleWeeklyReminderDialog(
    routineName: String, // routineId no es necesario para mostrar el diálogo
    dayDate: Date,
    onDismiss: () -> Unit,
    onSchedule: (timeInMillis: Long, isRepeating: Boolean, intervalMillis: Long?) -> Unit
) {
    val context = LocalContext.current
    // Usaremos el Calendar de ICU si tu minSdk es 24+, sino java.util.Calendar
    val initialCalendar = Calendar.getInstance().apply { time = dayDate }
    var selectedHour by remember { mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCalendar.get(Calendar.MINUTE)) }
    val isRepeating = false // Como discutimos, para el plan semanal, suele ser no repetitivo
    val intervalMillis: Long? = null
    // El TimePickerDialog nativo generalmente respeta el tema del sistema/app.
    // No necesitas aplicar temas manualmente al TimePickerDialog en sí.
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            selectedHour = hour
            selectedMinute = minute
        },
        selectedHour,
        selectedMinute,
        true // Formato 24 horas
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        // --- COLORES Y ESTILO DEL DIÁLOGO ---
        containerColor = MaterialTheme.colorScheme.surface, // Fondo del diálogo
        titleContentColor = MaterialTheme.colorScheme.onSurface, // Color del título
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant, // Color del texto principal

        title = {
            Text(
                text = "Recordatorio para '$routineName'",
                style = MaterialTheme.typography.headlineSmall, // Un estilo más prominente
                color = MaterialTheme.colorScheme.primary // Usa tu color primario para el título
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(), // Para permitir centrar el contenido interno
                horizontalAlignment = Alignment.CenterHorizontally // Centrar el contenido de la columna
            ) {
                Text(
                    text = "Día: ${SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(dayDate)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp) // Espacio debajo de la fecha
                )

                // --- BOTÓN DE SELECCIONAR HORA MEJORADO ---
                Button(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f) // Que no ocupe todo el ancho, pero sí una buena parte
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // Botón primario
                        contentColor = MaterialTheme.colorScheme.onPrimary  // Texto sobre el botón primario
                    )
                ) {
                    Text(
                        text = "Hora: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        style = MaterialTheme.typography.labelLarge // Un poco más grande para el botón
                    )
                }
                // Aquí podrías añadir un checkbox si quieres permitir "repetir este recordatorio para este día de la semana"
                // Pero para un plan semanal, un recordatorio único para la fecha/hora seleccionada suele ser suficiente.
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scheduleCalendar = Calendar.getInstance().apply {
                        time = dayDate // Empezar con la fecha del día
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (System.currentTimeMillis() >= scheduleCalendar.timeInMillis) {
                        Toast.makeText(context, "Por favor, selecciona una hora futura.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    onSchedule(scheduleCalendar.timeInMillis, isRepeating, intervalMillis)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary, // Botón de acción secundario
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton( // Usar OutlinedButton para "Cancelar" es un patrón común
                onClick = onDismiss,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline) // Color del borde
                )
            ) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.primary // Color del texto para el botón de cancelar
                )
            }
        }
    )
}
// Helper para capitalizar (puedes ponerlo en un archivo de Utils)
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }
