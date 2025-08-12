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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.semantics.disabled
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

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar
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
    val nameFieldLabel = stringResource(R.string.name_label)
    val ageFieldLabel = stringResource(R.string.age_label)
    val sexFieldLabel = stringResource(R.string.sex_label)
    val weightFieldLabel = stringResource(R.string.weight_kg_label) // Asumiendo que es "Peso (kg)"
    val heightFieldLabel = stringResource(R.string.height_cm_label) // Asumiendo que es "Altura (cm)"
    val frequencyFieldLabel = stringResource(R.string.weekly_frequency_days_label) // Asumiendo que es "Frecuencia (días)"
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

    // --- Estados del ViewModel ---
    val nombre by viewModel.editableNombre.collectAsState()
    val peso by viewModel.editablePeso.collectAsState()
    val altura by viewModel.editableAltura.collectAsState()
    val edad by viewModel.editableEdad.collectAsState()
    val sexo by viewModel.editableSexo.collectAsState()
    val frecuencia by viewModel.editableFrecuenciaSemanal.collectAsState()
    val lugar by viewModel.editableLugarEntrenamiento.collectAsState()
    val userProfile by viewModel.user.collectAsState()
    val fotoUrlActualDelPerfil = userProfile?.fotoUrl

    // --- Estado local para la nueva imagen ---
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                Toast.makeText(context, "$errorPrefixMessage: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetUpdateState()
            }
            else -> { /* Idle or Loading */ }
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
            verticalArrangement = Arrangement.spacedBy(28.dp) // Espacio entre secciones
        ) {

            // --- Sección de Imagen de Perfil ---
            ProfileImageSection(
                currentImageUrl = fotoUrlActualDelPerfil,
                newImageUri = newImageUri,
                onImageClick = { if(!isLoading) imagePickerLauncher.launch("image/*") }
            )
            // --- Sección de Información Personal ---
            ProfileSection(title = personalInfoLabel) {
                // Nombre (TextField estándar dentro de la sección)
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

                Spacer(Modifier.height(8.dp)) // Espacio antes del primer ModernEditableField
                // o usa el `verticalArrangement.spacedBy` del Column en ProfileSection

                // Edad
                ModernEditableField(
                    label = ageFieldLabel,
                    value = if (edad.isNotEmpty()) edad else notSetPlaceholderText,
                    onClick = { if(!isLoading) showEdadPickerDialog = true }
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp) // Divisor más sutil

                // Sexo
                ModernEditableField(
                    label = sexFieldLabel,
                    value = if (sexo.isNotEmpty()) sexo else notSetPlaceholderText,
                    onClick = { if(!isLoading) showSexoPickerDialog = true }
                )
            }


            // --- Sección de Detalles Físicos y de Entrenamiento ---
            ProfileSection(title = physicalDetailsLabel) {
                // Peso
                ModernEditableField(
                    label = weightFieldLabel,
                    value = if (peso.isNotEmpty()) "$peso kg" else notSetPlaceholderText,
                    onClick = { if(!isLoading) showPesoPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                // Altura
                ModernEditableField(
                    label = heightFieldLabel,
                    value = if (altura.isNotEmpty()) "$altura cm" else notSetPlaceholderText,
                    onClick = { if(!isLoading) showAlturaPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                // Frecuencia Semanal
                ModernEditableField(
                    label = frequencyFieldLabel,
                    value = if (frecuencia.isNotEmpty()) "$frecuencia días" else notSetPlaceholderText,
                    onClick = { if(!isLoading) showFrecuenciaPickerDialog = true }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                // Lugar de entrenamiento (con FilterChips)
                Text(
                    trainingPlaceFieldLabel,
                    style = MaterialTheme.typography.labelMedium, // Consistente con ModernEditableField
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp) // Ajuste de padding
                )
                val lugaresPosibles = listOf("Casa", "Gimnasio", "Exterior", "Calistenia")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()), // Permite scroll si los chips no caben
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    lugaresPosibles.forEach { lugarOpcion ->
                        val isSelected = lugar == lugarOpcion
                        val currentChipIsEnabled = !isLoading // Renombrado para evitar conflicto con el 'enabled' del FilterChip

                        FilterChip(
                            selected = isSelected,
                            onClick = { if (currentChipIsEnabled) viewModel.onLugarEntrenamientoChange(lugarOpcion) },
                            label = { Text(lugarOpcion, style = MaterialTheme.typography.labelLarge) },
                            enabled = currentChipIsEnabled, // Usar la variable local
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = selectedDescText, // Asegúrate de tener este string
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                // Opcional: definir colores para estado deshabilitado
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Alfa estándar para contenido deshabilitado// Alfa estándar para contenido deshabilitado                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = isSelected, // Pasar el estado de selección
                                enabled = currentChipIsEnabled, // Pasar el estado de habilitación
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp,
                                // Opcional: definir bordes para estado deshabilitado
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                disabledSelectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) // Un poco más tenue
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp)) // Espacio antes de los botones

            // --- Botones de Acción ---
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
                    border = BorderStroke(1.dp, if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline)
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
                    } else {
                        Text(saveButtonText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp)) // Espacio al final del scroll
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
}

/**
 * Un Composable para la sección de la imagen de perfil con un estilo más moderno.
 */
@Composable
fun ProfileImageSection(
    currentImageUrl: String?,
    newImageUri: Uri?,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp) // Tamaño generoso
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)) // Fondo sutil
                .clickable(onClick = onImageClick)
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape), // Borde de acento
            contentAlignment = Alignment.Center
        ) {
            val imageToDisplay = newImageUri ?: if (currentImageUrl?.isNotBlank() == true) currentImageUrl else R.drawable.ic_default_avatar
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageToDisplay,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar)
                ),
                contentDescription = stringResource(R.string.profile_picture_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Icono de cámara superpuesto
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp) // Ajusta el padding para la posición
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = stringResource(R.string.tap_to_change_photo_label),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
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
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.primary, // Color de acento para el título
            modifier = Modifier.padding(bottom = 16.dp) // Espacio debajo del título
        )
        // Card sutil para agrupar el contenido de la sección
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) // Bordes redondeados para el área clickeable
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp), // Padding interno generoso
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = if (value == stringResource(R.string.not_set_placeholder)) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        Icon(
            imageVector = Icons.Outlined.Edit, // Icono más sutil
            contentDescription = stringResource(R.string.desc_edit_field), // Necesitarás este string
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
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
                            .clickable { currentSelection = option } // Actualiza la selección al hacer clic
                            .padding(vertical = 12.dp, horizontal = 8.dp), // Padding para cada opción
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