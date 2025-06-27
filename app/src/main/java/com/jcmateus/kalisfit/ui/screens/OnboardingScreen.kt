package com.jcmateus.kalisfit.ui.screens


import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
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


@RequiresApi(Build.VERSION_CODES.O) // Requerido por alguna parte de tu código original, verifica si aún es necesario
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

    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
        LottieAnimation(
            composition = composition,
            iterations = 1,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(180.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Personaliza tu Viaje Fitness", // Considera usar stringResource
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OnboardingSectionCard(title = "Cuéntanos sobre ti") { // Considera usar stringResource
            Text("Tu nivel de experiencia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth()) // Considera usar stringResource
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                niveles.forEachIndexed { index, nivel ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = niveles.size),
                        onClick = { nivelSeleccionado = nivel },
                        selected = nivelSeleccionado == nivel,
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(nivel.displayName)
                    }
                }
            }

            Text("Sexo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth()) // Considera usar stringResource
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                sexos.forEachIndexed { index, sexoOp ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = sexos.size),
                        onClick = { sexoSeleccionado = sexoOp },
                        selected = sexoSeleccionado == sexoOp,
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(sexoOp.displayName)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingSectionCard(title = "Tus Detalles Físicos") { // Considera usar stringResource
            SliderInputField(
                label = "Edad", // Considera usar stringResource
                value = edadState,
                onValueChange = { edadState = it },
                valueRange = 12f..100f,
                steps = 87,
                displayValueSuffix = " años", // Considera usar stringResource
                icon = Icons.Filled.Cake
            )
            SliderInputField(
                label = "Peso", // Considera usar stringResource
                value = pesoState,
                onValueChange = { pesoState = it },
                valueRange = 30f..200f,
                steps = 169,
                displayValueSuffix = " kg",
                icon = Icons.Filled.MonitorWeight
            )
            SliderInputField(
                label = "Altura", // Considera usar stringResource
                value = alturaState,
                onValueChange = { alturaState = it },
                valueRange = 100f..250f,
                steps = 149,
                displayValueSuffix = " cm",
                icon = Icons.Filled.Height
            )
            SliderInputField(
                label = "Días de entreno/semana", // Considera usar stringResource
                value = frecuenciaState,
                onValueChange = { frecuenciaState = it },
                valueRange = 1f..7f,
                steps = 5,
                displayValueSuffix = " días", // Considera usar stringResource
                icon = Icons.Filled.EventRepeat
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingSectionCard(title = "¿Qué quieres lograr?") { // Considera usar stringResource
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
                            if (objetivo in objetivosSeleccionados) objetivosSeleccionados.remove(objetivo)
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
                            if (lugar in lugaresSeleccionados) lugaresSeleccionados.remove(lugar)
                            else lugaresSeleccionados.add(lugar)
                        },
                        leadingIcon = { // Ejemplo de iconos para lugares
                            when (lugar) {
                                LugarEntrenamiento.CASA -> Icon(Icons.Filled.Home, null, tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)
                                LugarEntrenamiento.GIMNASIO -> Icon(Icons.Filled.FitnessCenter, null, tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)
                                LugarEntrenamiento.EXTERIOR -> Icon(Icons.Filled.Terrain, null, tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)
                                LugarEntrenamiento.CALISTENIA -> Icon(Icons.Filled.SportsGymnastics, null, tint = if (lugar in lugaresSeleccionados) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = {
                if (nivelSeleccionado == null || sexoSeleccionado == null || objetivosSeleccionados.isEmpty() || lugaresSeleccionados.isEmpty()) {
                    Toast.makeText(context, "Por favor, completa todas las selecciones", Toast.LENGTH_LONG).show() // Considera usar stringResource
                    return@Button
                }
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
                            Toast.makeText(context, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show() // Considera usar stringResource
                            onFinish()
                        } else {
                            Toast.makeText(context, "Error al actualizar: ${message ?: "Desconocido"}", Toast.LENGTH_LONG).show() // Considera usar stringResource
                        }
                    }
                )
            },
            enabled = nivelSeleccionado != null && sexoSeleccionado != null && objetivosSeleccionados.isNotEmpty() && lugaresSeleccionados.isNotEmpty() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Guardar y Continuar", style = MaterialTheme.typography.titleMedium) // Considera usar stringResource
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun OnboardingSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) // Eleva ligeramente el color de la superficie
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp) // Aumentar padding vertical
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Alinear título a la izquierda
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary, // Usar color primario para el título
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), // Valor más prominente
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
                activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), // Ticks más sutiles
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        ),
        modifier = Modifier.height(40.dp)
    )
}
