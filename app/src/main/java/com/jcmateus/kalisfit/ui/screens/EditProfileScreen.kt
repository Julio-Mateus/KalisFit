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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: UserProfileViewModel = viewModel() // Inyectar/Obtener el ViewModel
) {
    val context = LocalContext.current

    // Observar los ESTADOS EDITABLES del ViewModel
    val nombre by viewModel.editableNombre.collectAsState() // Antes: viewModel.nombre
    val peso by viewModel.editablePeso.collectAsState()     // Antes: viewModel.peso
    val altura by viewModel.editableAltura.collectAsState() // Antes: viewModel.altura
    val edad by viewModel.editableEdad.collectAsState()     // Antes: viewModel.edad
    val sexo by viewModel.editableSexo.collectAsState()     // Antes: viewModel.sexo
    val frecuencia by viewModel.editableFrecuenciaSemanal.collectAsState() // Antes: viewModel.frecuenciaSemanal
    val lugar by viewModel.editableLugarEntrenamiento.collectAsState()
    val userProfile by viewModel.user.collectAsState() // Observa el UserProfile completo
    val fotoUrlActualDelPerfil = userProfile?.fotoUrl

    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        newImageUri = uri
        // Opcional: podrías tener viewModel.setPreviewImageUri(uri) si quieres lógica compleja de previsualización
    }

    val updateState by viewModel.updateState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }


    // Ya no necesitas cargar el perfil explícitamente aquí si el ViewModel lo hace en init
    // o si tienes un método específico que llamas, pero `loadUserProfile` en init es común.
    // LaunchedEffect(Unit) {
    // viewModel.loadUserProfile() // Asegúrate que esto se llama si no está en init o si necesitas recargar
    // }


    LaunchedEffect(updateState) {
        isLoading = updateState is UserProfileViewModel.UpdateProfileState.Loading
        when (val state = updateState) { // Renombrar para evitar conflicto con la variable externa
            is UserProfileViewModel.UpdateProfileState.Success -> {
                Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                viewModel.resetUpdateState()
            }
            is UserProfileViewModel.UpdateProfileState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetUpdateState()
            }
            UserProfileViewModel.UpdateProfileState.Idle -> { /* No hacer nada */ }
            UserProfileViewModel.UpdateProfileState.Loading -> { /* isLoading se encarga */ }
        }
    }

    // Comprobación de si el usuario está logueado
    if (!viewModel.isUserLoggedIn()) {
        // Si el usuario no está logueado, navega a Login.
        // Esto es una forma simple; una app más compleja podría tener un flujo de autenticación más robusto
        // gestionado a un nivel superior o por el propio ViewModel emitiendo un evento de navegación.
        LaunchedEffect(Unit) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true } // Limpiar toda la pila y ir a Login
            }
        }
        return // No renderizar el resto de la UI si no hay usuario
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Editar perfil", style = MaterialTheme.typography.headlineLarge)

        Box(contentAlignment = Alignment.Center) {
            val urlDelPerfil = fotoUrlActualDelPerfil // Variable local estable
            val imageToDisplay = newImageUri ?: if (urlDelPerfil?.isNotBlank() == true) urlDelPerfil else R.drawable.ic_default_avatar
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageToDisplay,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar)
                ),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { launcher.launch("image/*") },
                contentScale = ContentScale.Crop
            )
        }
        Text("Toca para cambiar foto", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(value = nombre, onValueChange = { viewModel.onNombreChange(it) }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = edad, onValueChange = { viewModel.onEdadChange(it) }, label = { Text("Edad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        // Para 'sexo', considera un DropdownMenu o similar para mejor UX
        OutlinedTextField(value = sexo, onValueChange = { viewModel.onSexoChange(it) }, label = { Text("Sexo") }, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(value = peso, onValueChange = { viewModel.onPesoChange(it) }, label = { Text("Peso (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = altura, onValueChange = { viewModel.onAlturaChange(it) }, label = { Text("Altura (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = frecuencia, onValueChange = { viewModel.onFrecuenciaChange(it) }, label = { Text("Frecuencia semanal (días)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        Text("Lugar de entrenamiento", style = MaterialTheme.typography.titleMedium)
        val lugaresPosibles = listOf("Casa", "Gimnasio", "Exterior") // Podría venir del ViewModel
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            lugaresPosibles.forEach { lugarOpcion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.onLugarEntrenamientoChange(lugarOpcion) }
                ) {
                    RadioButton(
                        selected = lugar == lugarOpcion,
                        onClick = { viewModel.onLugarEntrenamientoChange(lugarOpcion) }
                    )
                    Text(lugarOpcion, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    viewModel.saveUserProfile(newImageUri)
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}
