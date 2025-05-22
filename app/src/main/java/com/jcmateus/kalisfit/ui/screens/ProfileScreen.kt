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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar
@Composable
fun ProfileScreen(
    navController: NavHostController
) {
    val viewModel: UserProfileViewModel = viewModel()
    val userState = viewModel.user.collectAsState()
    val user = userState.value

    LaunchedEffect(key1 = userState.value?.uid) {
        viewModel.loadUserProfile()
    }

    // ------ INICIO DE LA MODIFICACIÓN ------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Usuario") }, // O usa stringResource
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Opcional: personaliza colores
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding -> // El contenido de tu pantalla va aquí, usando innerPadding

        // El Box original ahora usa el padding del Scaffold
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplicar el padding del Scaffold aquí
        ) {
            user?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp), // Este padding es interno al contenido, después del padding del Scaffold
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val painter = rememberAsyncImagePainter(
                        model = it.fotoUrl.ifBlank { R.drawable.ic_default_avatar }
                    )
                    Image(
                        painter = painter,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Text(it.nombre, style = MaterialTheme.typography.headlineMedium)

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileInfoRow(label = "Nivel", value = it.nivel.ifBlank { "No especificado" })
                        ProfileInfoRow(label = "Objetivos", value = it.objetivos.joinToString(", ").ifBlank { "No especificados" })
                        ProfileInfoRow(label = "Peso", value = if (it.peso > 0f) "${it.peso} kg" else "No especificado")
                        ProfileInfoRow(label = "Altura", value = if (it.altura > 0f) "${it.altura} cm" else "No especificado")
                        ProfileInfoRow(label = "Edad", value = if (it.edad > 0) "${it.edad} años" else "No especificado")
                        ProfileInfoRow(label = "Sexo", value = it.sexo.ifBlank { "No especificado" })
                        ProfileInfoRow(label = "Frecuencia semanal", value = "${it.frecuenciaSemanal} días")
                        ProfileInfoRow(label = "Entrenamiento en", value = it.lugarEntrenamiento.joinToString(", ").ifBlank { "No especificado" })
                        val fechaRegistroFormateada = remember(it.fechaRegistro) {
                            it.fechaRegistro?.toDate()?.let { date: Date ->
                                val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
                                sdf.format(date)
                            } ?: "No disponible"
                        }
                        ProfileInfoRow(label = "Registrado el", value = fechaRegistroFormateada)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            navController.navigate(Routes.EDIT_PROFILE_SCREEN)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar Perfil")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } ?: Box(
                Modifier
                    .fillMaxSize(), // Este Box también debe estar dentro del contenedor que tiene el padding de Scaffold
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    // ------ FIN DE LA MODIFICACIÓN ------
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}


