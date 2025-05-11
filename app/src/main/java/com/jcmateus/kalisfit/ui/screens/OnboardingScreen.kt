package com.jcmateus.kalisfit.ui.screens

import android.R.attr.enabled
import android.R.attr.type
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember { AuthViewModel() }
    val scrollState = rememberScrollState()

    val niveles = listOf("Principiante", "Intermedio", "Avanzado")
    val sexos = listOf("Masculino", "Femenino")
    val lugares = listOf("Casa", "Gimnasio", "Exterior")
    val objetivosDisponibles = listOf("Fuerza", "Resistencia", "Masa muscular", "Bienestar mental")

    var nivel by remember { mutableStateOf("") }
    var expandedNivel by remember { mutableStateOf(false) }

    var sexo by remember { mutableStateOf("") }
    var expandedSexo by remember { mutableStateOf(false) }

    val lugaresSeleccionados = remember { mutableStateListOf<String>() }
    val objetivosSeleccionados = remember { mutableStateListOf<String>() }

    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🎞️ Encabezado visual (puedes cambiar por imagen si no usas Lottie)
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Text("Personaliza tu experiencia", style = MaterialTheme.typography.headlineSmall)

        // Nivel
        ExposedDropdownMenuBox(
            expanded = expandedNivel,
            onExpandedChange = { expandedNivel = !expandedNivel }) {
            OutlinedTextField(
                value = nivel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Nivel") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNivel) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedNivel,
                onDismissRequest = { expandedNivel = false }) {
                niveles.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = {
                        nivel = it
                        expandedNivel = false
                    })
                }
            }
        }

        // Objetivos
        Text("Objetivos", style = MaterialTheme.typography.titleSmall)
        objetivosDisponibles.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = it in objetivosSeleccionados,
                    onCheckedChange = { selected ->
                        if (selected) objetivosSeleccionados.add(it) else objetivosSeleccionados.remove(
                            it
                        )
                    }
                )
                Text(it)
            }
        }

        // Datos físicos
        OutlinedTextField(
            value = peso,
            onValueChange = { peso = it },
            label = { Text("Peso (kg)") })
        OutlinedTextField(
            value = altura,
            onValueChange = { altura = it },
            label = { Text("Altura (cm)") })
        OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") })
        OutlinedTextField(
            value = frecuencia,
            onValueChange = { frecuencia = it },
            label = { Text("Frecuencia semanal") })

        // Sexo
        ExposedDropdownMenuBox(
            expanded = expandedSexo,
            onExpandedChange = { expandedSexo = !expandedSexo }) {
            OutlinedTextField(
                value = sexo,
                onValueChange = {},
                readOnly = true, // Es de solo lectura
                label = { Text("Sexo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSexo) },
                // *** ¡AQUÍ ESTÁ LA CORRECCIÓN! ***
                modifier = Modifier
                    .menuAnchor(
                        MenuAnchorType.PrimaryNotEditable,
                        true
                    ) // Usamos el tipo correcto y habilitamos el ancla
                    .fillMaxWidth() // Mantiene el ancho completo
            )
            // Este es el menú desplegable que aparece al hacer clic en el OutlinedTextField
            ExposedDropdownMenu(
                expanded = expandedSexo,
                onDismissRequest = { expandedSexo = false }) {
                sexos.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = {
                        sexo = it
                        expandedSexo = false // Cerrar el menú después de seleccionar
                    })
                }
            }
        }

        // Lugar entrenamiento
        Text("Lugar de entrenamiento", style = MaterialTheme.typography.titleSmall)
        lugares.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = it in lugaresSeleccionados,
                    onCheckedChange = { selected ->
                        if (selected) lugaresSeleccionados.add(it) else lugaresSeleccionados.remove(
                            it
                        )
                    }
                )
                Text(it)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón continuar
        Button(
            onClick = {
                val pesoF = peso.toFloatOrNull() ?: 0f
                val alturaF = altura.toFloatOrNull() ?: 0f
                val edadI = edad.toIntOrNull() ?: 0
                val frecuenciaI = frecuencia.toIntOrNull() ?: 3

                viewModel.updateProfileAfterRegister(
                    nivel,
                    objetivosSeleccionados.toList(),
                    pesoF,
                    alturaF,
                    edadI,
                    sexo,
                    frecuenciaI,
                    lugaresSeleccionados.toList().toString()
                ) { success, message ->
                    if (success) onFinish()
                    else Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                }
            },
            enabled = nivel.isNotEmpty() && objetivosSeleccionados.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar y continuar")
        }
    }
}

