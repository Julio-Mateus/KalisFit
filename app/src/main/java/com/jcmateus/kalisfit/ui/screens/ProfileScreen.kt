package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
//import com.google.type.Date
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.navigation.Routes
import kotlin.text.format
import java.text.SimpleDateFormat
import java.util.Date // Asegúrate que es java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    navController: NavHostController // Añadido para navegación
) {
    // Usar la función de extensión viewModel() para obtener la instancia del ViewModel
    val viewModel: UserProfileViewModel = viewModel()
    val userState = viewModel.user.collectAsState()
    val user = userState.value

    // LaunchedEffect para cargar el perfil una vez cuando el composable entra en la composición.
    // userState.value como key asegura que si el usuario cambia (ej. logout/login), se recargue.
    LaunchedEffect(key1 = userState.value?.uid) { // O usa Unit si solo cargas al inicio
        viewModel.loadUserProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
        // .padding(16.dp) // El padding se puede aplicar más específicamente dentro
    ) {
        user?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize() // Para que el scroll funcione en toda la pantalla
                    .verticalScroll(rememberScrollState()) // Añadir scroll si el contenido puede ser largo
                    .padding(16.dp), // Padding general para el contenido
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagen de perfil
                val painter = rememberAsyncImagePainter(
                    model = it.fotoUrl.ifBlank { R.drawable.ic_default_avatar }
                )
                Image(
                    painter = painter,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(120.dp) // Un poco más grande para destacar
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Text(it.nombre, style = MaterialTheme.typography.headlineMedium)
                // Es mejor no mostrar el email públicamente siempre, pero si es tu diseño, está bien.
                // Text(it.email, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))
                Divider(thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) // Un poco más de espacio
                ) {
                    ProfileInfoRow(label = "Nivel", value = it.nivel.ifBlank { "No especificado" })
                    ProfileInfoRow(label = "Objetivos", value = it.objetivos.joinToString(", ").ifBlank { "No especificados" })
                    ProfileInfoRow(label = "Peso", value = if (it.peso > 0f) "${it.peso} kg" else "No especificado")
                    ProfileInfoRow(label = "Altura", value = if (it.altura > 0f) "${it.altura} cm" else "No especificado")
                    ProfileInfoRow(label = "Edad", value = if (it.edad > 0) "${it.edad} años" else "No especificado")
                    ProfileInfoRow(label = "Sexo", value = it.sexo.ifBlank { "No especificado" })
                    ProfileInfoRow(label = "Frecuencia semanal", value = "${it.frecuenciaSemanal} días")
                    ProfileInfoRow(label = "Entrenamiento en", value = it.lugarEntrenamiento.joinToString(", ").ifBlank { "No especificado" })
                    val fechaRegistroFormateada = remember(it.fechaRegistro) { // remember para eficiencia
                        it.fechaRegistro?.toDate()?.let { date: Date ->
                            // Elige el formato que prefieras
                            val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy",
                                Locale.getDefault())
                            // val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            sdf.format(date)
                        } ?: "No disponible" // Valor si fechaRegistro es null
                    }
                    ProfileInfoRow(label = "Registrado el", value = fechaRegistroFormateada)
                }

                Spacer(modifier = Modifier.weight(1f)) // Empuja el botón hacia abajo si hay espacio

                Button(
                    onClick = {
                        navController.navigate(Routes.EDIT_PROFILE_SCREEN)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar Perfil")
                }
                Spacer(modifier = Modifier.height(16.dp)) // Espacio al final
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            // Podrías añadir un mensaje si la carga falla o si el usuario es null después de un tiempo
        }
    }
}

// ProfileInfoRow y ProfileSection se mantienen como están, están bien.
// Asegúrate que estén en el mismo archivo o accesibles.
@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // Buen añadido para alinear
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge, // labelLarge puede ser más apropiado
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface // Color estándar para el texto del cuerpo
        )
    }
}

// ProfileSection no se está usando en el ProfileScreen que me mostraste,
// pero si la usas en otro lado, está bien.
@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp), // Considera vertical = 8.dp
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}




