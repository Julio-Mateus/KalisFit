package com.jcmateus.kalisfit.ui.screens

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
import androidx.compose.material3.MaterialTheme
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
import com.jcmateus.kalisfit.viewmodel.AuthViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val viewModel = remember { AuthViewModel() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var nivel by remember { mutableStateOf("") }
    val niveles = listOf("Principiante", "Intermedio", "Avanzado")

    val objetivosDisponibles = listOf("Fuerza", "Resistencia", "Masa muscular", "Bienestar mental")
    val objetivosSeleccionados = remember { mutableStateListOf<String>() }

    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Tu nivel de experiencia", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        niveles.forEach {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nivel = it }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = nivel == it, onClick = { nivel = it })
                Text(it)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("¿Qué objetivos tienes?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        objetivosDisponibles.forEach { objetivo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (objetivosSeleccionados.contains(objetivo))
                            objetivosSeleccionados.remove(objetivo)
                        else
                            objetivosSeleccionados.add(objetivo)
                    }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = objetivosSeleccionados.contains(objetivo),
                    onCheckedChange = {
                        if (it) objetivosSeleccionados.add(objetivo)
                        else objetivosSeleccionados.remove(objetivo)
                    }
                )
                Text(objetivo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = altura, onValueChange = { altura = it }, label = { Text("Altura (cm)") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = sexo, onValueChange = { sexo = it }, label = { Text("Sexo") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = frecuencia, onValueChange = { frecuencia = it }, label = { Text("Días de entrenamiento por semana") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = lugar, onValueChange = { lugar = it }, label = { Text("Lugar de entrenamiento") })

        Spacer(modifier = Modifier.height(32.dp))

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
                    lugar
                ) { success, message ->
                    if (success) onFinish()
                    else Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                }
            },
            enabled = nivel.isNotEmpty() && objetivosSeleccionados.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar")
        }
    }
}
