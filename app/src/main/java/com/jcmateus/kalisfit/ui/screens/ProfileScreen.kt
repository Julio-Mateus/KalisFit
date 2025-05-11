package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    val viewModel = remember { UserProfileViewModel() }
    val userState = viewModel.user.collectAsState()
    val user = userState.value

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        user?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text("${it.nombre}", style = MaterialTheme.typography.headlineMedium)
                Text("${it.email}", style = MaterialTheme.typography.bodyMedium)

                Divider(thickness = 1.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileInfoRow(label = "Nivel", value = it.nivel)
                    ProfileInfoRow(label = "Objetivos", value = it.objetivos.joinToString(", "))
                    ProfileInfoRow(label = "Peso", value = "${it.peso} kg")
                    ProfileInfoRow(label = "Altura", value = "${it.altura} cm")
                    ProfileInfoRow(label = "Edad", value = "${it.edad} años")
                    ProfileInfoRow(label = "Sexo", value = it.sexo)
                    ProfileInfoRow(label = "Frecuencia semanal", value = "${it.frecuenciaSemanal} días")
                    ProfileInfoRow(label = "Entrenamiento en", value = it.lugarEntrenamiento)
                    ProfileInfoRow(label = "Registrado el", value = it.fechaRegistro)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onEditProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar perfil")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar perfil")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}



