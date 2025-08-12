package com.jcmateus.kalisfit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.navigation.Routes
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

    val notAvailableText = stringResource(R.string.not_available)

    // Cargar perfil cuando el UID cambie (o inicialmente si ya está disponible)
    LaunchedEffect(key1 = Unit) { // O usa userState.value?.uid si quieres recargar si el uid cambia
        viewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                // Considera añadir scrollBehavior si el contenido es muy largo
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.EDIT_PROFILE_SCREEN) },
                icon = { Icon(Icons.Filled.Edit, contentDescription = "Editar Perfil") },
                text = { Text("Editar Perfil") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        floatingActionButtonPosition = FabPosition.Center // O FabPosition.End
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // Un fondo sutil para toda la pantalla
        ) {
            if (user == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 24.dp), // Mayor padding vertical
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Sección de Avatar y Nombre ---
                    val avatarPainter = rememberAsyncImagePainter(
                        model = user.fotoUrl.ifBlank { R.drawable.ic_default_avatar },
                        // Opcional: placeholder y error drawables
                        // placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
                        // error = painterResource(id = R.drawable.ic_avatar_error)
                    )
                    Image(
                        painter = avatarPainter,
                        contentDescription = stringResource(R.string.profile_picture_desc),
                        modifier = Modifier
                            .size(140.dp) // Un poco más grande
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), // Borde opcional
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.nombre.ifBlank { stringResource(R.string.name_not_specified) },
                        style = MaterialTheme.typography.headlineSmall, // Un poco más pequeño que headlineMedium pero aún prominente
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Nivel: ${user.nivel.ifBlank { stringResource(R.string.level_not_specified) }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Sección de Información Personal ---
                    ProfileSectionCard(title = stringResource(R.string.personal_information)) {
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.FitnessCenter,
                            label = stringResource(R.string.goals),
                            value = user.objetivos.joinToString(", ").ifBlank { stringResource(R.string.not_specified) }
                        )
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.MonitorWeight,
                            label = stringResource(R.string.weight),
                            value = if (user.peso > 0f) "${user.peso} kg" else stringResource(R.string.not_specified)
                        )
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.Height,
                            label = stringResource(R.string.height),
                            value = if (user.altura > 0f) "${user.altura} cm" else stringResource(R.string.not_specified)
                        )
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.Cake, // O Icons.Filled.Person
                            label = stringResource(R.string.age),
                            value = if (user.edad > 0) "${user.edad} ${stringResource(R.string.years_old)}" else stringResource(R.string.not_specified)
                        )
                        ProfileInfoRowWithIcon(
                            icon = if (user.sexo.equals("Masculino", true)) Icons.Filled.Male else if (user.sexo.equals("Femenino", true)) Icons.Filled.Female else Icons.Filled.Wc,
                            label = stringResource(R.string.gender),
                            value = user.sexo.ifBlank { stringResource(R.string.not_specified) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Sección de Entrenamiento ---
                    ProfileSectionCard(title = stringResource(R.string.training_details)) {
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.EventRepeat,
                            label = stringResource(R.string.weekly_frequency),
                            value = "${user.frecuenciaSemanal} ${stringResource(R.string.days_per_week)}"
                        )
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.Place,
                            label = stringResource(R.string.training_location),
                            value = user.lugarEntrenamiento.joinToString(", ").ifBlank { stringResource(R.string.not_specified) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Sección de Cuenta ---
                    ProfileSectionCard(title = stringResource(R.string.account_info)) {
                        val fechaRegistroFormateada = remember(user.fechaRegistro, notAvailableText) { // Añadir notAvailableText como key si su valor pudiera cambiar y afectar el remember
                            user.fechaRegistro?.toDate()?.let { date: Date ->
                                val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
                                sdf.format(date)
                            } ?: notAvailableText // Usar la variable que contiene la string
                        }
                        ProfileInfoRowWithIcon(
                            icon = Icons.Filled.DateRange,
                            label = stringResource(R.string.registration_date),
                            value = fechaRegistroFormateada
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp)) // Espacio para que el FAB no tape el último elemento
                }
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), // Esquinas más redondeadas
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Fondo de la tarjeta
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
fun ProfileInfoRowWithIcon(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.secondary // Color del icono
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacio entre icono y texto
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label, // Para accesibilidad
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) { // Columna para que la etiqueta y el valor puedan estar uno encima del otro si son largos, o lado a lado.
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge, // O bodyMedium
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp // Mejorar legibilidad si el texto es largo
            )
        }
    }
}



