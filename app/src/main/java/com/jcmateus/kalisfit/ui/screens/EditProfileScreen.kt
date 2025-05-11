package com.jcmateus.kalisfit.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.UserProfile

@Composable
fun EditProfileScreen(
    user: UserProfile,
    onProfileUpdated: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var nombre by remember { mutableStateOf(user.nombre) }
    var peso by remember { mutableStateOf(user.peso.takeIf { it > 0f }?.toString() ?: "") }
    var altura by remember { mutableStateOf(user.altura.takeIf { it > 0f }?.toString() ?: "") }
    var edad by remember { mutableStateOf(user.edad.takeIf { it > 0 }?.toString() ?: "") }
    var sexo by remember { mutableStateOf(user.sexo) }
    var frecuencia by remember { mutableStateOf(user.frecuenciaSemanal.toString()) }
    var lugar by remember { mutableStateOf(user.lugarEntrenamiento.firstOrNull() ?: "") }
    var fotoUrl by remember { mutableStateOf(user.fotoUrl) }

    val sexos = listOf("Masculino", "Femenino", "Otro")
    val lugares = listOf("Casa", "Gimnasio", "Exterior")

    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        newImageUri = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Editar perfil", style = MaterialTheme.typography.headlineLarge)
        // Imagen de perfil
        Box(contentAlignment = Alignment.Center) {
            val painter = rememberAsyncImagePainter(
                model = newImageUri ?: fotoUrl.ifBlank { R.drawable.ic_default_avatar },
                placeholder = painterResource(R.drawable.ic_default_avatar)
            )
            Image(
                painter = painter,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { launcher.launch("image/*") }
            )
        }
        Text("Toca para cambiar foto", style = MaterialTheme.typography.bodySmall)
        // ➤ Datos personales
        Text("Datos personales", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sexo, onValueChange = { sexo = it }, label = { Text("Sexo") }, modifier = Modifier.fillMaxWidth())

        // ➤ Datos físicos
        Text("Datos físicos", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = altura, onValueChange = { altura = it }, label = { Text("Altura (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        // ➤ Datos de entrenamiento
        Text("Entrenamiento", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = frecuencia, onValueChange = { frecuencia = it }, label = { Text("Frecuencia semanal (días)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        // Radios para lugar de entrenamiento
        Text("Lugar de entrenamiento", style = MaterialTheme.typography.bodyLarge)
        lugares.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = lugar == it, onClick = { lugar = it })
                Text(it)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Acciones
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    fun actualizarFirestore(finalUrl: String = fotoUrl) {
                        val actualizacion = mutableMapOf<String, Any>(
                            "nombre" to nombre,
                            "peso" to (peso.toFloatOrNull() ?: 0f),
                            "altura" to (altura.toFloatOrNull() ?: 0f),
                            "edad" to (edad.toIntOrNull() ?: 0),
                            "sexo" to sexo,
                            "frecuenciaSemanal" to (frecuencia.toIntOrNull() ?: 3),
                            "lugarEntrenamiento" to lugar,
                            "fotoUrl" to finalUrl
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
                    }

                    if (newImageUri != null) {
                        val ref = storage.reference.child("fotos_perfil/$uid.jpg")
                        ref.putFile(newImageUri!!).continueWithTask {
                            if (!it.isSuccessful) throw it.exception!!
                            ref.downloadUrl
                        }.addOnSuccessListener { uri ->
                            actualizarFirestore(uri.toString())
                        }.addOnFailureListener {
                            Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        actualizarFirestore()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
        }
    }
}
