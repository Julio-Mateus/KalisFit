package com.jcmateus.kalisfit.ui.screens.auth_profile


import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.AuthViewModel

// Definiciones de Enum (idealmente en tu ViewModel o un archivo de modelos)
// Define tus Enums aquí o asegúrate de que estén importados correctamente
enum class NivelExperiencia(val displayName: String) {
    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado")
}

enum class Sexo(val displayName: String) {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro")
}

enum class LugarEntrenamiento(val displayName: String) {
    CASA("Casa"),
    GIMNASIO("Gimnasio"),
    EXTERIOR("Exterior"),
    CALISTENIA("Calistenia")
}

enum class ObjetivoEntrenamiento(val displayName: String) {
    FUERZA("Fuerza"),
    RESISTENCIA("Resistencia"),
    HIPERTROFIA("Hipertrofia"),
    BIENESTAR_MENTAL("Bienestar mental"),
    PERDIDA_PESO("Pérdida de Peso")
}

data class QuickProfilePreset(
    val title: String,
    val subtitle: String,
    val edad: Float,
    val peso: Float,
    val altura: Float,
    val frecuencia: Float,
    val objetivos: List<ObjetivoEntrenamiento>,
    val lugares: List<LugarEntrenamiento>
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    authViewModel: AuthViewModel = viewModel(), // Inyecta o crea tu ViewModel
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val niveles = remember { NivelExperiencia.values().toList() }
    val sexos = remember { Sexo.values().toList() }
    val lugares = remember { LugarEntrenamiento.values().toList() }
    val objetivosDisponibles = remember { ObjetivoEntrenamiento.values().toList() }

    var nivelSeleccionado by remember { mutableStateOf<NivelExperiencia?>(null) }
    var sexoSeleccionado by remember { mutableStateOf<Sexo?>(null) }
    val lugaresSeleccionados = remember { mutableStateListOf<LugarEntrenamiento>() }
    val objetivosSeleccionados = remember { mutableStateListOf<ObjetivoEntrenamiento>() }

    var edadState by remember { mutableFloatStateOf(25f) }
    var pesoState by remember { mutableFloatStateOf(70f) }
    var alturaState by remember { mutableFloatStateOf(170f) }
    var frecuenciaState by remember { mutableFloatStateOf(3f) }
    var currentStep by remember { mutableStateOf(0) }
    var selectedPresetTitle by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val profilePresets = remember {
        listOf(
            QuickProfilePreset(
                title = "Equilibrado",
                subtitle = "Objetivo de fuerza y resistencia",
                edad = 25f,
                peso = 70f,
                altura = 170f,
                frecuencia = 3f,
                objetivos = listOf(
                    ObjetivoEntrenamiento.BIENESTAR_MENTAL,
                    ObjetivoEntrenamiento.RESISTENCIA
                ),
                lugares = listOf(LugarEntrenamiento.CASA)
            ),
            QuickProfilePreset(
                title = "Ganancia muscular",
                subtitle = "Más fuerza e hipertrofia",
                edad = 24f,
                peso = 72f,
                altura = 172f,
                frecuencia = 5f,
                objetivos = listOf(ObjetivoEntrenamiento.FUERZA, ObjetivoEntrenamiento.HIPERTROFIA),
                lugares = listOf(LugarEntrenamiento.GIMNASIO)
            ),
            QuickProfilePreset(
                title = "Definición",
                subtitle = "Resistencia + pérdida de peso",
                edad = 27f,
                peso = 74f,
                altura = 171f,
                frecuencia = 4f,
                objetivos = listOf(
                    ObjetivoEntrenamiento.RESISTENCIA,
                    ObjetivoEntrenamiento.PERDIDA_PESO
                ),
                lugares = listOf(LugarEntrenamiento.CASA, LugarEntrenamiento.EXTERIOR)
            )
        )
    }
    val stepIsValid = when (currentStep) {
        0 -> nivelSeleccionado != null && sexoSeleccionado != null
        1 -> objetivosSeleccionados.isNotEmpty() && lugaresSeleccionados.isNotEmpty()
        else -> true
    }

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { if (!isLoading) currentStep -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Atras")
                        }
                    }
                    Button(
                        onClick = {
                            if (!stepIsValid) {
                                Toast.makeText(
                                    context,
                                    "Complete este paso para continiar",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            if (currentStep < 2) {
                                currentStep += 1
                            } else {
                                // Lógica de guardado..
                                isLoading = true
                                authViewModel.updateProfileAfterRegister(
                                    nivel = nivelSeleccionado!!.displayName,
                                    objetivos = objetivosSeleccionados.map { it.displayName },
                                    peso = pesoState,
                                    altura = alturaState,
                                    edad = edadState.toInt(),
                                    sexo = sexoSeleccionado!!.displayName,
                                    frecuenciaSemanal = frecuenciaState.toInt(),
                                    lugarEntrenamiento = lugaresSeleccionados.map { it.displayName },
                                    onResult = { success, message ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Perfil actualizado con éxito",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onFinish()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Error al actualizar: ${message ?: "Desconocido"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isLoading && stepIsValid,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (currentStep < 2) "Siguiente" else "Comenzar")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Indicador de Progreso Moderno
            StepProgressIndicator(currentStep = currentStep, totalSteps = 3)

            Spacer(modifier = Modifier.height(24.dp))
            // Lottie y Títulos
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
            LottieAnimation(
                composition = composition,
                iterations = Int.MAX_VALUE,
                modifier = Modifier.height(160.dp)
            )

            Text(
                text = "¡Bienvenido a KalisFit!", // Considera usar stringResource
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configura tu perfil en 3 pasos rápidos",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "Paso ${currentStep + 1} de 3",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.animateContentSize()) {
                when (currentStep) {
                    0 -> {
                        OnboardingSectionCard(title = "Hazlo rápido ⚡") {
                            Text(
                                text = "Elige una sugerencia y autocompleta la mayoría de campos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                profilePresets.forEach { preset ->
                                    StyledFilterChip(
                                        text = preset.title,
                                        selected = selectedPresetTitle == preset.title, // Ahora sí cambia de color
                                        onSelectedChange = {
                                            selectedPresetTitle =
                                                preset.title // Guardamos cuál se seleccionó
                                            // Solo actualizamos edad/peso/altura si el usuario NO los ha movido (están en default)
                                            if (edadState == 25f) edadState = preset.edad
                                            if (pesoState == 70f) pesoState = preset.peso
                                            if (alturaState == 170f) alturaState = preset.altura
                                            frecuenciaState = preset.frecuencia
                                            objetivosSeleccionados.clear()
                                            objetivosSeleccionados.addAll(preset.objetivos)
                                            lugaresSeleccionados.clear()
                                            lugaresSeleccionados.addAll(preset.lugares)

                                            Toast.makeText(
                                                context,
                                                "Sugerencia aplicada: ${preset.title}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }

                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        OnboardingSectionCard(title = "Tu base") {
                            SliderInputField(
                                label = "Edad",
                                value = edadState,
                                onValueChange = { edadState = it },
                                valueRange = 12f..80f,
                                steps = 67,
                                displayValueSuffix = " años",
                                icon = Icons.Filled.Cake
                            )
                            SliderInputField(
                                label = "Peso",
                                value = pesoState,
                                onValueChange = { pesoState = it },
                                valueRange = 30f..200f,
                                steps = 169,
                                displayValueSuffix = " kg",
                                icon = Icons.Filled.MonitorWeight
                            )
                            SliderInputField(
                                label = "Altura",
                                value = alturaState,
                                onValueChange = { alturaState = it },
                                valueRange = 120f..220f,
                                steps = 99,
                                displayValueSuffix = " cm",
                                icon = Icons.Filled.Height
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OnboardingSectionCard(title = "Tu nivel y sexo") {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                niveles.forEachIndexed { index, nivel ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = niveles.size
                                        ),
                                        onClick = { nivelSeleccionado = nivel },
                                        selected = (nivel == nivelSeleccionado),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(nivel.displayName)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                sexos.forEachIndexed { index, sexo ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = sexos.size
                                        ),
                                        onClick = { sexoSeleccionado = sexo },
                                        selected = (sexo == sexoSeleccionado),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(sexo.displayName)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        OnboardingSectionCard(title = "¿Qué quieres lograr?") {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                objetivosDisponibles.forEach { objetivo ->
                                    StyledFilterChip(
                                        text = objetivo.displayName,
                                        selected = objetivo in objetivosSeleccionados,
                                        onSelectedChange = {
                                            if (objetivo in objetivosSeleccionados) objetivosSeleccionados.remove(
                                                objetivo
                                            )
                                            else objetivosSeleccionados.add(objetivo)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OnboardingSectionCard(title = "¿Dónde sueles entrenar?") { // Considera usar stringResource
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                lugares.forEach { lugar ->
                                    StyledFilterChip(
                                        text = lugar.displayName,
                                        selected = lugar in lugaresSeleccionados,
                                        onSelectedChange = {
                                            if (lugar in lugaresSeleccionados) lugaresSeleccionados.remove(
                                                lugar
                                            )
                                            else lugaresSeleccionados.add(lugar)
                                        },
                                        leadingIcon = { // Ejemplo de iconos para lugares
                                            when (lugar) {
                                                LugarEntrenamiento.CASA -> Icon(
                                                    Icons.Filled.Home,
                                                    null,
                                                    tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                                                )

                                                LugarEntrenamiento.GIMNASIO -> Icon(
                                                    Icons.Filled.FitnessCenter,
                                                    null,
                                                    tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                                                )

                                                LugarEntrenamiento.EXTERIOR -> Icon(
                                                    Icons.Filled.Terrain,
                                                    null,
                                                    tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                                                )

                                                LugarEntrenamiento.CALISTENIA -> Icon(
                                                    Icons.Filled.SportsGymnastics,
                                                    null,
                                                    tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OnboardingSectionCard(title = "Frecuencia semanal") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 4, 5, 6).forEach { day ->
                                    StyledFilterChip(
                                        text = "$day días",
                                        selected = frecuenciaState.toInt() == day,
                                        onSelectedChange = { frecuenciaState = day.toFloat() },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.EventRepeat,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { frecuenciaState = 3f }) {
                                Text("Sugerir para mi: 3 días")
                            }
                        }
                    }

                    else -> {
                        OnboardingSectionCard(title = "Resumen") {
                            Text("Nivel: ${nivelSeleccionado?.displayName ?: "_"}")
                            Text("Sexo: ${sexoSeleccionado?.displayName ?: "_"}")
                            Text("Edad/Peso/Altura: ${edadState.toInt()} años ° ${pesoState.toInt()} kg ° ${alturaState.toInt()} cm")
                            Text("Frecuencia: ${frecuenciaState.toInt()} días")
                            Text("Objetivos: ${objetivosSeleccionados.joinToString { it.displayName }}")
                            Text("Lugares: ${lugaresSeleccionados.joinToString { it.displayName }}")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
@Composable
fun OnboardingSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                2.dp
            ) // Eleva ligeramente el color de la superficie
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ) // Aumentar padding vertical
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Alinear título a la izquierda
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary, // Usar color primario para el título
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SliderInputField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValueSuffix: String = "",
    icon: ImageVector? = null,
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) { // Aumentar padding vertical
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Text(
                text = "$label:",
                style = MaterialTheme.typography.titleSmall, // Etiqueta un poco más pequeña
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f) // Permitir que la etiqueta ocupe espacio
            )
            Text(
                text = "${value.toInt()}$displayValueSuffix",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), // Valor más prominente
                color = MaterialTheme.colorScheme.primary, // Destacar el valor
                textAlign = TextAlign.End // Alinear valor a la derecha
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant, // Más sutil para la pista inactiva
                activeTickColor = MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.5f
                ), // Ticks más sutiles
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.3f
                )
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledFilterChip(
    text: String,
    selected: Boolean,
    onSelectedChange: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange() },
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (selected && leadingIcon == null) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Seleccionado",
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            leadingIcon
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outline.copy(
                alpha = 0.7f
            )
        ),
        modifier = Modifier.height(40.dp)
    )
}

@Composable
fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep

            val color = when {
                isCurrent -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            // El paso actual es un poco más ancho para dar jerarquía
            val weight = if (isCurrent) 1.5f else 1f

            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .animateContentSize()
            )
        }
    }
}

