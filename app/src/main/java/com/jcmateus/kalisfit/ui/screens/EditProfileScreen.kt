package com.jcmateus.kalisfit.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.viewmodel.UserProfile

@Composable
fun EditProfileScreen(
    user: UserProfile,
    onProfileUpdated: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var nombre by remember { mutableStateOf(user.nombre) }
    var peso by remember { mutableStateOf(user.peso.toString()) }
    var altura by remember { mutableStateOf(user.altura.toString()) }
    var edad by remember { mutableStateOf(user.edad.toString()) }
    var sexo by remember { mutableStateOf(user.sexo) }
    var frecuencia by remember { mutableStateOf(user.frecuenciaSemanal.toString()) }
    var lugar by remember { mutableStateOf(user.lugarEntrenamiento) }

    val sexos = listOf("Masculino", "Femenino", "Otro")
    val lugares = listOf("Casa", "Gimnasio", "Exterior")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Editar perfil", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
        OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") })
        OutlinedTextField(value = altura, onValueChange = { altura = it }, label = { Text("Altura (cm)") })
        OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") })
        OutlinedTextField(value = frecuencia, onValueChange = { frecuencia = it }, label = { Text("Frecuencia semanal") })

        Text("Sexo", style = MaterialTheme.typography.titleSmall)
        sexos.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = sexo == it, onClick = { sexo = it })
                Text(it)
            }
        }

        Text("Lugar de entrenamiento", style = MaterialTheme.typography.titleSmall)
        lugares.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = lugar == it, onClick = { lugar = it })
                Text(it)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val actualizacion = mutableMapOf<String, Any>(
                        "nombre" to nombre,
                        "peso" to (peso.toFloatOrNull() ?: 0f),
                        "altura" to (altura.toFloatOrNull() ?: 0f),
                        "edad" to (edad.toIntOrNull() ?: 0),
                        "sexo" to sexo,
                        "frecuenciaSemanal" to (frecuencia.toIntOrNull() ?: 3),
                        "lugarEntrenamiento" to lugar
                    )

                    firestore.collection("users").document(uid)
                        .update(actualizacion)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                            onProfileUpdated()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar cambios")
            }
        }
    }
}
