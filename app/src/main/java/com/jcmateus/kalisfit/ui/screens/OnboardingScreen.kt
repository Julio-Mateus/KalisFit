package com.jcmateus.kalisfit.ui.screens


import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.AuthViewModel

// Definiciones de Enum (idealmente en tu ViewModel o un archivo de modelos)
enum class NivelExperiencia(val displayName: String) {
    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado")
}

enum class Sexo(val displayName: String) {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro") // Considera añadir "Otro" o "Prefiero no decirlo"
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
    PERDIDA_PESO("Pérdida de Peso") // Ejemplo de otro objetivo
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember { AuthViewModel() } // Asume la existencia de tu ViewModel
    val scrollState = rememberScrollState()

    // Usando Enums para las opciones
    val niveles = remember { NivelExperiencia.values().toList() }
    val sexos = remember { Sexo.values().toList() }
    val lugares = remember { LugarEntrenamiento.values().toList() }
    val objetivosDisponibles = remember { ObjetivoEntrenamiento.values().toList() }

    // Estados para las selecciones
    var nivelSeleccionado by remember { mutableStateOf<NivelExperiencia?>(null) }
    var sexoSeleccionado by remember { mutableStateOf<Sexo?>(null) }
    val lugaresSeleccionados = remember { mutableStateListOf<LugarEntrenamiento>() }
    val objetivosSeleccionados = remember { mutableStateListOf<ObjetivoEntrenamiento>() }

    // Estados para los campos de texto
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Encabezado Visual ---
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(180.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            "Personaliza tu Experiencia",
            style = MaterialTheme.typography.headlineMedium, // Un poco más grande
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Sección: Sobre ti ---
        SectionTitle1("Sobre ti")

        // Nivel
        Text("Tu nivel de experiencia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            niveles.forEach { nivel ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = niveles.indexOf(nivel), count = niveles.size),
                    onClick = { nivelSeleccionado = nivel },
                    selected = nivelSeleccionado == nivel
                ) {
                    Text(nivel.displayName)
                }
            }
        }

        // Sexo
        Text("Sexo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            sexos.forEach { sexoOp ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = sexos.indexOf(sexoOp), count = sexos.size),
                    onClick = { sexoSeleccionado = sexoOp },
                    selected = sexoSeleccionado == sexoOp
                ) {
                    Text(sexoOp.displayName)
                }
            }
        }


        // --- Sección: Detalles Físicos ---
        SectionTitle1("Detalles Físicos")
        CustomOutlinedTextField(
            value = peso,
            onValueChange = { peso = it.filter { char -> char.isDigit() } }, // Permitir solo dígitos
            label = "Peso (kg)",
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Filled.FitnessCenter
        )
        CustomOutlinedTextField(
            value = altura,
            onValueChange = { altura = it.filter { char -> char.isDigit() } },
            label = "Altura (cm)",
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Filled.Height // Icono para altura
        )
        CustomOutlinedTextField(
            value = edad,
            onValueChange = { edad = it.filter { char -> char.isDigit() } },
            label = "Edad",
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Filled.Cake // Icono para edad/cumpleaños
        )
        CustomOutlinedTextField(
            value = frecuencia,
            onValueChange = { frecuencia = it.filter { char -> char.isDigit() } },
            label = "Días de entreno/semana",
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Filled.EventRepeat, // Icono para frecuencia
            imeAction = ImeAction.Done // Último campo antes de selecciones
        )
        Spacer(modifier = Modifier.height(16.dp))


        // --- Sección: Tus Metas ---
        SectionTitle1("Tus Metas")
        Text("¿Qué quieres lograr?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio vertical entre filas de chips
        ) {
            objetivosDisponibles.forEach { objetivo ->
                CustomFilterChip(
                    text = objetivo.displayName,
                    selected = objetivo in objetivosSeleccionados,
                    onSelectedChange = {
                        if (objetivo in objetivosSeleccionados) objetivosSeleccionados.remove(objetivo)
                        else objetivosSeleccionados.add(objetivo)
                    }
                )
            }
        }

        // --- Sección: Lugar de Entrenamiento ---
        SectionTitle1("¿Dónde sueles entrenar?")
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lugares.forEach { lugar ->
                CustomFilterChip(
                    text = lugar.displayName,
                    selected = lugar in lugaresSeleccionados,
                    onSelectedChange = {
                        if (lugar in lugaresSeleccionados) lugaresSeleccionados.remove(lugar)
                        else lugaresSeleccionados.add(lugar)
                    },
                    // Opcional: Iconos para lugares
                    // leadingIcon = { Icon( /* icono específico para el lugar */ ) }
                )
            }
        }

        // Spacer para empujar el botón hacia abajo si hay espacio
        Spacer(modifier = Modifier.weight(1f))

        // --- Botón Continuar ---
        Button(
            onClick = {
                // Validación básica (puedes expandirla)
                if (nivelSeleccionado == null || sexoSeleccionado == null || objetivosSeleccionados.isEmpty() || peso.isBlank() || altura.isBlank() || edad.isBlank() || frecuencia.isBlank()) {
                    Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_LONG).show()
                    return@Button
                }

                val pesoF = peso.toFloatOrNull() ?: 0f
                val alturaF = altura.toFloatOrNull() ?: 0f
                val edadI = edad.toIntOrNull() ?: 0
                val frecuenciaI = frecuencia.toIntOrNull() ?: 3 // Un valor por defecto si es inválido

                viewModel.updateProfileAfterRegister(
                    nivel = nivelSeleccionado!!.displayName,
                    objetivos = objetivosSeleccionados.map { it.displayName },
                    peso = pesoF,
                    altura = alturaF,
                    edad = edadI,
                    sexo = sexoSeleccionado!!.displayName,
                    frecuenciaSemanal = frecuenciaI, // <--- CORREGIDO
                    lugarEntrenamiento = lugaresSeleccionados.map { it.displayName }, // <--- CORREGIDO
                    onResult = { success, message -> // Asegúrate de nombrar el último parámetro lambda si es necesario
                        if (success) {
                            onFinish()
                        } else {
                            Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            },
            enabled = nivelSeleccionado != null && sexoSeleccionado != null && objetivosSeleccionados.isNotEmpty() && lugaresSeleccionados.isNotEmpty() && peso.isNotBlank() && altura.isNotBlank() && edad.isNotBlank() && frecuencia.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp) // Botón un poco más alto
        ) {
            Text("Guardar y Continuar", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// --- Componentes Reutilizables ---

@Composable
fun SectionTitle1(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge, // Título de sección más grande
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 16.dp) // Más espacio vertical para títulos de sección
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFilterChip(
    text: String,
    selected: Boolean,
    onSelectedChange: () -> Unit, // Simplificado, ya que el estado se maneja fuera
    leadingIcon: @Composable (() -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange() },
        label = { Text(text) },
        leadingIcon = if (selected) {
            leadingIcon ?: { // Icono de check por defecto si está seleccionado y no se provee otro
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Seleccionado",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            leadingIcon // Muestra el icono provisto si no está seleccionado
        }
        // Puedes añadir colors = FilterChipDefaults.filterChipColors(...) para personalizar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = label) } },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Un poco más de espacio vertical
    )
}

