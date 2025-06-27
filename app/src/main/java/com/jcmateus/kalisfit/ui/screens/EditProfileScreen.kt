package com.jcmateus.kalisfit.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Necesario para TopAppBar
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: UserProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val profileUpdatedSuccessMessage = stringResource(R.string.profile_updated_success)
    val errorPrefixMessage = stringResource(R.string.error_prefix)

    // Recolectar estados del ViewModel
    val nombre by viewModel.editableNombre.collectAsState()
    val peso by viewModel.editablePeso.collectAsState()
    val altura by viewModel.editableAltura.collectAsState()
    val edad by viewModel.editableEdad.collectAsState()
    val sexo by viewModel.editableSexo.collectAsState()
    val frecuencia by viewModel.editableFrecuenciaSemanal.collectAsState()
    val lugarEntrenamiento by viewModel.editableLugarEntrenamiento.collectAsState() // Renombrado para claridad
    val userProfile by viewModel.user.collectAsState()
    val fotoUrlActualDelPerfil = userProfile?.fotoUrl

    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        newImageUri = uri
    }

    val updateState by viewModel.updateState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }

    // Efectos para manejar el estado de actualización y la navegación
    LaunchedEffect(updateState) {
        isLoading = updateState is UserProfileViewModel.UpdateProfileState.Loading
        when (val state = updateState) {
            is UserProfileViewModel.UpdateProfileState.Success -> {
                Toast.makeText(context, profileUpdatedSuccessMessage, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                viewModel.resetUpdateState()
            }
            is UserProfileViewModel.UpdateProfileState.Error -> {
                Toast.makeText(context, "$errorPrefixMessage: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetUpdateState()
            }
            UserProfileViewModel.UpdateProfileState.Idle -> Unit
            UserProfileViewModel.UpdateProfileState.Loading -> Unit
        }
    }

    if (!viewModel.isUserLoggedIn()) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator() // Muestra un loader si no está logueado mientras navega
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile_title), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { if (!isLoading) navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp), // Padding general
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Sección de Foto de Perfil
            ProfileImageSection(
                currentImageUrl = fotoUrlActualDelPerfil,
                newImageUri = newImageUri,
                onImageClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Sección de Información Personal
            ProfileSectionCard(title = stringResource(R.string.personal_information_label)) {
                ModernOutlinedTextField(
                    value = nombre,
                    onValueChange = { viewModel.onNombreChange(it) },
                    label = stringResource(R.string.name_label),
                    leadingIcon = Icons.Filled.PersonOutline,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = edad,
                        onValueChange = { viewModel.onEdadChange(it) },
                        label = stringResource(R.string.age_label),
                        leadingIcon = Icons.Filled.Cake,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    ModernOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = sexo, // Considera un Dropdown o Chips si tienes pocas opciones predefinidas
                        onValueChange = { viewModel.onSexoChange(it) },
                        label = stringResource(R.string.sex_label),
                        leadingIcon = Icons.Filled.Wc,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sección de Detalles Físicos y de Entrenamiento
            ProfileSectionCard(title = stringResource(R.string.physical_training_details_label)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = peso,
                        onValueChange = { viewModel.onPesoChange(it) },
                        label = stringResource(R.string.weight_kg_label),
                        leadingIcon = Icons.Filled.MonitorWeight,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    ModernOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = altura,
                        onValueChange = { viewModel.onAlturaChange(it) },
                        label = stringResource(R.string.height_cm_label),
                        leadingIcon = Icons.Filled.Height,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                ModernOutlinedTextField(
                    value = frecuencia,
                    onValueChange = { viewModel.onFrecuenciaChange(it) },
                    label = stringResource(R.string.weekly_frequency_days_label),
                    leadingIcon = Icons.Filled.EventRepeat,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done // O Next si hay más campos abajo
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.training_place_label),
                    style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                val lugaresPosibles = listOf("Casa", "Gimnasio", "Exterior", "Calistenia") // Asegúrate que coincidan con tus enums/constantes si los tienes
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lugaresPosibles.forEach { lugarOpcion ->
                        StyledFilterChip( // Reutiliza el StyledFilterChip de OnboardingScreen si es similar
                            text = lugarOpcion,
                            selected = lugarEntrenamiento == lugarOpcion, // Asumiendo que `lugarEntrenamiento` es un solo String
                            onSelectedChange = { viewModel.onLugarEntrenamientoChange(lugarOpcion) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botones de Acción
            ActionButtons(
                isLoading = isLoading,
                onCancel = { navController.popBackStack() },
                onSave = { viewModel.saveUserProfile(newImageUri) }
            )
            Spacer(modifier = Modifier.height(16.dp)) // Espacio al final para scroll
        }
    }
}

@Composable
fun ProfileImageSection(
    currentImageUrl: String?,
    newImageUri: Uri?,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val imageToDisplay = newImageUri ?: if (currentImageUrl?.isNotBlank() == true) currentImageUrl else R.drawable.ic_default_avatar
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageToDisplay,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar),
                    contentScale = ContentScale.Crop // Asegura que la imagen llene el círculo
                ),
                contentDescription = stringResource(R.string.profile_picture_desc),
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape) // Fondo sutil
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) // Borde
                    .clickable(onClick = onImageClick),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onImageClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.change_photo_desc),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.tap_to_change_photo_label),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
fun ModernOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), // Esquinas más redondeadas
        colors = OutlinedTextFieldDefaults.colors( // Ajustar colores si es necesario
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        enabled = enabled
    )
}
@Composable
fun ActionButtons(
    isLoading: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            enabled = !isLoading,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.cancel_button), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            enabled = !isLoading,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(stringResource(R.string.save_button), fontWeight = FontWeight.Medium)
            }
        }
    }
}
