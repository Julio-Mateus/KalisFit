package com.jcmateus.kalisfit.ui.screens.auth_profile

import android.net.Uri
import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val focusManager = LocalFocusManager.current

    // --- Strings ---
    val profileUpdatedMessage = stringResource(R.string.profile_updated_success)
    val errorPrefixMessage = stringResource(R.string.error_prefix)
    val editProfileTitleText = stringResource(R.string.edit_profile_title)
    val descNavigateBackText = stringResource(R.string.desc_navigate_back)
    // Strings para ProfileSection y ModernEditableField (asegúrate de tenerlos)
    val personalInfoLabel = stringResource(R.string.personal_information_label)
    val physicalDetailsLabel = stringResource(R.string.physical_training_details_label)
    val trainingPreferencesLabel =
        stringResource(R.string.training_preferences_label) // NUEVO: para la sección
    val nameFieldLabel = stringResource(R.string.name_label)
    val ageFieldLabel = stringResource(R.string.age_label)
    val sexFieldLabel = stringResource(R.string.sex_label)
    val weightFieldLabel = stringResource(R.string.weight_kg_label) // Asumiendo que es "Peso (kg)"
    val heightFieldLabel =
        stringResource(R.string.height_cm_label) // Asumiendo que es "Altura (cm)"
    val levelFieldLabel = stringResource(R.string.level_label) // Ejemplo: "Nivel"
    val goalsFieldLabel = stringResource(R.string.goals_label) // Ejemplo: "Objeti
    val frequencyFieldLabel =
        stringResource(R.string.weekly_frequency_days_label) // Asumiendo que es "Frecuencia (días)"
    val trainingPlaceFieldLabel = stringResource(R.string.training_place_label)
    val notSetPlaceholderText = stringResource(R.string.not_set_placeholder) // Ej: "No establecido"
    val cancelButtonText = stringResource(R.string.cancel_button)
    val saveButtonText = stringResource(R.string.save_button)
    val selectedDescText = stringResource(R.string.selected_desc) // Para FilterChip
    // Strings para los títulos de los diálogos (asegúrate que estén en strings.xml)
    val selectAgeTitle = stringResource(R.string.select_age_title)
    val selectSexTitle = stringResource(R.string.select_sex_title)
    val selectWeightTitle = stringResource(R.string.select_weight_title)
    val selectHeightTitle = stringResource(R.string.select_height_title)
    val selectFrequencyTitle = stringResource(R.string.select_frequency_title)
    val selectLevelTitle = stringResource(R.string.select_level_title)
    // --- Estados del ViewModel ---
    val nombre by viewModel.editableNombre.collectAsState()
    val nivel by viewModel.editableNivel.collectAsState()
    val objetivos by viewModel.editableObjetivos.collectAsState()
    val peso by viewModel.editablePeso.collectAsState()
    val altura by viewModel.editableAltura.collectAsState()
    val edad by viewModel.editableEdad.collectAsState()
    val sexo by viewModel.editableSexo.collectAsState()
    val frecuencia by viewModel.editableFrecuenciaSemanal.collectAsState()
    val lugaresEntrenamientoSeleccionados by viewModel.editableLugarEntrenamiento.collectAsState()
    val userProfile by viewModel.user.collectAsState()
    val fotoUrlActualDelPerfil = userProfile?.fotoUrl
    // --- Estado local para la nueva imagen ---
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher =
        //rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // En algunos proveedores (u OEMs) no se concede permiso persistible.
                    // Aun así conservamos el URI para intentar la subida en esta sesión.
                }
            }
            newImageUri = uri
        }
    // --- Estado para el proceso de guardado ---
    val updateState by viewModel.updateState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    // --- Estados para controlar la visibilidad de los diálogos ---
    var showEdadPickerDialog by remember { mutableStateOf(false) }
    var showSexoPickerDialog by remember { mutableStateOf(false) }
    var showPesoPickerDialog by remember { mutableStateOf(false) }
    var showAlturaPickerDialog by remember { mutableStateOf(false) }
    var showFrecuenciaPickerDialog by remember { mutableStateOf(false) }
    var showNivelPickerDialog by remember { mutableStateOf(false) }
    // --- Efecto para manejar el resultado del guardado ---
    LaunchedEffect(updateState) {
        isLoading = updateState is UserProfileViewModel.UpdateProfileState.Loading
        when (val state = updateState) {
            is UserProfileViewModel.UpdateProfileState.Success -> {
                Toast.makeText(context, profileUpdatedMessage, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                viewModel.resetUpdateState()
            }

            is UserProfileViewModel.UpdateProfileState.Error -> {
                Toast.makeText(context, "$errorPrefixMessage: ${state.message}", Toast.LENGTH_LONG)
                    .show()
                viewModel.resetUpdateState()
            }

            else -> { /* Idle or Loading */
            }
        }
    }
    // --- Comprobación de si el usuario está logueado ---
    if (!viewModel.isUserLoggedIn()) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editProfileTitleText, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { if (!isLoading) navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, descNavigateBackText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp) // Reducido un poco el espacio entre secciones
        ) {

            ProfileImageSection(
                currentImageUrl = fotoUrlActualDelPerfil,
                newImageUri = newImageUri,
                //onImageClick = { if (!isLoading) imagePickerLauncher.launch("image/*") }
                onImageClick = { if (!isLoading) imagePickerLauncher.launch(arrayOf("image/*")) }
            )

            ProfileSection(title = personalInfoLabel) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { viewModel.onNombreChange(it) },
                    label = { Text(nameFieldLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                )
                Spacer(Modifier.height(8.dp))
                ModernEditableField(
                    label = ageFieldLabel,
                    value = if (edad.isNotEmpty()) edad else notSetPlaceholderText,
                    onClick = { if (!isLoading) showEdadPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                ModernEditableField(
                    label = sexFieldLabel,
                    value = if (sexo.isNotEmpty()) sexo else notSetPlaceholderText,
                    onClick = { if (!isLoading) showSexoPickerDialog = true }
                )
            }

            ProfileSection(title = physicalDetailsLabel) {
                ModernEditableField(
                    label = weightFieldLabel,
                    value = if (peso.isNotEmpty()) "$peso kg" else notSetPlaceholderText,
                    onClick = { if (!isLoading) showPesoPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                ModernEditableField(
                    label = heightFieldLabel,
                    value = if (altura.isNotEmpty()) "$altura cm" else notSetPlaceholderText,
                    onClick = { if (!isLoading) showAlturaPickerDialog = true }
                )
            }

            // --- NUEVA SECCIÓN para Nivel, Objetivos, Frecuencia y Lugar ---
            ProfileSection(title = trainingPreferencesLabel) {
                // Nivel
                ModernEditableField(
                    label = levelFieldLabel,
                    value = if (nivel.isNotEmpty()) nivel else notSetPlaceholderText,
                    onClick = { if (!isLoading) showNivelPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                // Objetivos (con FilterChips para selección múltiple)
                Text(
                    text = goalsFieldLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                )
                val objetivosPosibles = remember { // Opciones de objetivos
                    listOf(
                        "Perder Peso",
                        "Ganar Músculo",
                        "Mejorar Resistencia",
                        "Salud General",
                        "Flexibilidad",
                        "Rendimiento Deportivo"
                    )
                }
                FlowRow( // Usar FlowRow para que los chips se ajusten a múltiples líneas
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    objetivosPosibles.forEach { objetivoOpcion ->
                        val isSelected = objetivos.contains(objetivoOpcion)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isLoading) {
                                    viewModel.onObjetivoSelected(objetivoOpcion, !isSelected)
                                }
                            },
                            label = {
                                Text(
                                    objetivoOpcion,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            enabled = !isLoading,
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        selectedDescText,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                ),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                // ... otros colores si es necesario
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = isSelected,
                                enabled = !isLoading,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp
                ) // Más espacio después de los chips

                // Frecuencia Semanal
                ModernEditableField(
                    label = frequencyFieldLabel,
                    value = if (frecuencia.isNotEmpty()) "$frecuencia días" else notSetPlaceholderText,
                    onClick = { if (!isLoading) showFrecuenciaPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                // Lugar de entrenamiento (con FilterChips para selección múltiple)
                Text(
                    trainingPlaceFieldLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                )
                val lugaresPosibles = remember { // Opciones de lugares
                    listOf("Casa", "Gimnasio", "Exterior", "Parque de Calistenia")
                }
                FlowRow( // Usar FlowRow aquí también
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lugaresPosibles.forEach { lugarOpcion ->
                        val isSelected =
                            lugaresEntrenamientoSeleccionados.contains(lugarOpcion) // Usa el estado correcto
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isLoading) {
                                    viewModel.onLugarEntrenamientoSelected(
                                        lugarOpcion,
                                        !isSelected
                                    ) // Llama a la función correcta
                                }
                            },
                            label = {
                                Text(
                                    lugarOpcion,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            enabled = !isLoading,
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        selectedDescText,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                ),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                // ... otros colores
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = isSelected,
                                enabled = !isLoading,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }
            }


            Spacer(Modifier.height(16.dp))

            // --- Botones de Acción --- (Sin cambios, pero asegúrate que viewModel.saveUserProfile esté actualizado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { if (!isLoading) navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(cancelButtonText, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { if (!isLoading) viewModel.saveUserProfile(newImageUri) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(saveButtonText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    // --- Diálogos (sin cambios en su definición, solo asegúrate que estén) ---
    if (showEdadPickerDialog) {
        NumberPickerDialog(
            title = selectAgeTitle,
            initialValue = edad.toIntOrNull() ?: 25,
            range = 10..99,
            onDismissRequest = { showEdadPickerDialog = false },
            onConfirm = { nuevaEdad ->
                viewModel.onEdadChange(nuevaEdad.toString())
                showEdadPickerDialog = false
            }
        )
    }

    if (showSexoPickerDialog) {
        OptionsPickerDialog(
            title = selectSexTitle,
            options = listOf("Masculino", "Femenino", "Otro", "Prefiero no decirlo"),
            selectedOption = sexo.ifEmpty { "Prefiero no decirlo" },
            onDismissRequest = { showSexoPickerDialog = false },
            onConfirm = { selected ->
                viewModel.onSexoChange(selected)
                showSexoPickerDialog = false
            }
        )
    }

    if (showPesoPickerDialog) {
        NumberPickerDialog(
            title = selectWeightTitle,
            initialValue = peso.toIntOrNull() ?: 60,
            range = 30..200,
            unit = "kg",
            onDismissRequest = { showPesoPickerDialog = false },
            onConfirm = { nuevoPeso ->
                viewModel.onPesoChange(nuevoPeso.toString())
                showPesoPickerDialog = false
            }
        )
    }

    if (showAlturaPickerDialog) {
        NumberPickerDialog(
            title = selectHeightTitle,
            initialValue = altura.toIntOrNull() ?: 170,
            range = 100..250,
            unit = "cm",
            onDismissRequest = { showAlturaPickerDialog = false },
            onConfirm = { nuevaAltura ->
                viewModel.onAlturaChange(nuevaAltura.toString())
                showAlturaPickerDialog = false
            }
        )
    }

    if (showFrecuenciaPickerDialog) {
        NumberPickerDialog(
            title = selectFrequencyTitle,
            initialValue = frecuencia.toIntOrNull() ?: 3,
            range = 0..7,
            unit = "días", // O "días/semana"
            onDismissRequest = { showFrecuenciaPickerDialog = false },
            onConfirm = { nuevaFrecuencia ->
                viewModel.onFrecuenciaChange(nuevaFrecuencia.toString())
                showFrecuenciaPickerDialog = false
            }
        )
    }
    if (showNivelPickerDialog) {
        OptionsPickerDialog(
            title = selectLevelTitle,
            options = listOf(
                "Principiante",
                "Intermedio",
                "Avanzado",
                "Experto"
            ), // Define tus niveles
            selectedOption = nivel.ifEmpty { "Principiante" }, // Opción por defecto
            onDismissRequest = { showNivelPickerDialog = false },
            onConfirm = { selectedNivel ->
                viewModel.onNivelChange(selectedNivel)
                showNivelPickerDialog = false
            }
        )
    }
}

/**
 * Un Composable para la sección de la imagen de perfil con un estilo más moderno.
 */
@Composable
fun ProfileImageSection(
    currentImageUrl: String?,
    newImageUri: Uri?,
    onImageClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(150.dp)) {
            val imageToDisplay = newImageUri ?: if (currentImageUrl?.isNotBlank() == true) currentImageUrl else R.drawable.ic_default_avatar
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageToDisplay,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar)
                ),
                contentDescription = stringResource(R.string.profile_picture_desc),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape ),
                contentScale = ContentScale.Crop
            )
            // Botón flotante de edición sobre la imagen
            IconButton(
                onClick = onImageClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(8.dp)
            ){
                Icon(Icons.Filled.CameraAlt, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/**
 * Un Composable reutilizable para crear secciones de perfil con título.
 */
@Composable
fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(), // Título en mayúsculas para un look moderno
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.primary, // Color de acento para el título
            modifier = Modifier.padding(bottom = 16.dp) // Espacio debajo del título
        )
        // Card sutil para agrupar el contenido de la sección
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    2.dp
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Sin elevación propia si la superficie ya tiene
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre elementos dentro de la tarjeta
            ) {
                content()
            }
        }
    }
}

/**
 * Un Composable para mostrar un campo editable con un estilo moderno,
 * que abre un diálogo al ser clickeado.
 */
@Composable
fun ModernEditableField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = value, style = MaterialTheme.typography.bodyLarge,fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun NumberPickerDialog(
    title: String,
    initialValue: Int,
    range: IntRange,
    unit: String? = null, // Parámetro opcional para la unidad (ej: "kg", "cm")
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Aseguramos que el valor inicial esté dentro del rango permitido.
    val coercedInitialValue = initialValue.coerceIn(range)
    var selectedValue by remember(coercedInitialValue) { mutableStateOf(coercedInitialValue) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (unit != null) "$selectedValue $unit" else selectedValue.toString(),
                    style = MaterialTheme.typography.headlineMedium, // Un estilo más prominente para el valor
                    modifier = Modifier.padding(bottom = 24.dp) // Más espacio debajo del valor
                )
                Slider(
                    value = selectedValue.toFloat(),
                    onValueChange = { newValue ->
                        // Actualiza el valor solo cuando el thumb del slider se detiene para mejor rendimiento
                        // o puedes actualizarlo directamente si prefieres ver el cambio en tiempo real.
                        // Para este caso, actualizamos en tiempo real.
                        selectedValue = newValue.toInt().coerceIn(range)
                    },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first - 1).coerceAtLeast(0), // Para pasos discretos
                    modifier = Modifier.fillMaxWidth()
                )
                // Opcional: Mostrar los límites del rango
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = range.first.toString(), style = MaterialTheme.typography.bodySmall)
                    Text(text = range.last.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedValue) }) {
                Text(stringResource(R.string.accept_button)) // Necesitas R.string.accept_button
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel_button)) // Necesitas R.string.cancel_button
            }
        }
    )
}

// --- OptionsPickerDialog ---
@Composable
fun OptionsPickerDialog(
    title: String,
    options: List<String>,
    selectedOption: String, // El valor que está actualmente seleccionado
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var currentSelection by remember(selectedOption) { mutableStateOf(selectedOption) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            // Usamos Column con verticalScroll si la lista de opciones es larga
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentSelection = option
                            } // Actualiza la selección al hacer clic
                            .padding(
                                vertical = 12.dp,
                                horizontal = 8.dp
                            ), // Padding para cada opción
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == currentSelection),
                            onClick = { currentSelection = option } // RadioButton también actualiza
                        )
                        Spacer(Modifier.width(16.dp)) // Espacio entre RadioButton y el texto
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge // Estilo para el texto de la opción
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentSelection) }) {
                Text(stringResource(R.string.accept_button)) // Necesitas R.string.accept_button
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel_button)) // Necesitas R.string.cancel_button
            }
        }
    )
}