package com.jcmateus.kalisfit.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.AppTheme
import com.jcmateus.kalisfit.viewmodel.SettingsViewModel
import kotlin.text.uppercase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val currentAppTheme by settingsViewModel.appTheme.collectAsState()
    val currentWeightUnit by settingsViewModel.weightUnit.collectAsState()
    val appVersionName = remember { getAppVersionName(context) }
    val appVersionCode = remember { getAppVersionCode(context) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showOpenSourceLicensesDialog by remember { mutableStateOf(false) }

    val appNameString = stringResource(id = R.string.app_name)
    val playStoreLink = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val shareBodyString = stringResource(R.string.settings_share_app_text, appNameString, playStoreLink)
    val shareUsingString = stringResource(R.string.settings_share_using) // Para el chooser
    val finalSupportSubject = stringResource(R.string.settings_support_email_subject, appNameString)



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.desc_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Sutil elevación
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp) // Espacio inferior para que el último elemento no quede pegado
        ) {
            // Sección General
            SettingsSectionTitle(title = stringResource(R.string.settings_section_general))
            SwitchSettingItem(
                title = stringResource(R.string.settings_enable_notifications),
                description = stringResource(R.string.settings_enable_notifications_desc),
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
            )
            ClickableSettingItem(
                title = stringResource(R.string.settings_app_theme),
                icon = when (currentAppTheme) {
                    AppTheme.LIGHT -> Icons.Default.Brightness7
                    AppTheme.DARK -> Icons.Default.Brightness4
                    AppTheme.SYSTEM -> Icons.Default.BrightnessAuto // Icono más adecuado
                },
                currentValue = currentAppTheme.name.replaceFirstChar { it.titlecase() },
                onClick = { showThemeDialog = true }
            )

            // Sección Unidades
            SettingsSectionTitle(title = stringResource(R.string.settings_section_units))
            ClickableSettingItem(
                title = stringResource(R.string.settings_weight_units),
                icon = Icons.Default.FitnessCenter, // Icono más temático para fitness
                currentValue = currentWeightUnit.uppercase(),
                onClick = { showUnitDialog = true }
            )

            // Sección Soporte y Feedback
            SettingsSectionTitle(title = stringResource(R.string.settings_section_support))
            InfoSettingItem(
                title = stringResource(R.string.settings_rate_app),
                icon = Icons.Default.StarRate,
                onClick = {
                    val packageName = context.packageName
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_share_app),
                icon = Icons.Default.Share,
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, appNameString)
                        putExtra(Intent.EXTRA_TEXT, shareBodyString)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, shareUsingString))
                }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_contact_support),
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // Solo apps de email
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("kalisfit8@gmail.com")) // Reemplaza con tu email
                        putExtra(Intent.EXTRA_SUBJECT, finalSupportSubject)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // Manejar el caso de que no haya cliente de email
                    }
                }
            )
            // Sección Legal
            SettingsSectionTitle(title = stringResource(R.string.settings_section_legal))
            InfoSettingItem(
                title = stringResource(R.string.settings_privacy_policy),
                icon = Icons.Default.Policy,
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://terminoskalisfit.blogspot.com/p/politica-de-privacidad-kalisfit.html")))
                }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_terms_of_service),
                icon = Icons.AutoMirrored.Filled.Notes, // Un icono como "documento"
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://terminoskalisfit.blogspot.com/p/terminos-y-condiciones-de-uso-kalisfit.html")))
                }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_open_source_licenses),
                icon = Icons.Default.Code,
                onClick = {
                    // Idealmente, esto abriría una pantalla o diálogo dentro de la app
                    // que liste las licencias. La librería "AboutLibraries" es genial para esto.
                    // Por ahora, un diálogo placeholder:
                    showOpenSourceLicensesDialog = true
                }
            )

            // Sección Acerca de
            SettingsSectionTitle(title = stringResource(R.string.settings_section_about))
            AboutAppItem(
                versionName = appVersionName,
                versionCode = appVersionCode
            )
            // Puedes añadir un item simple para "Desarrollado por" si lo deseas
            SettingItem(
                icon = Icons.Default.Business, // O un icono de persona si es individual
                content = {
                    Column {
                        Text(
                            stringResource(R.string.settings_developed_by),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "JcMateus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )


            Spacer(Modifier.height(32.dp)) // Más espacio al final
        }
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_select_theme_dialog_title),
            options = AppTheme.values().map { it.name.replaceFirstChar { char -> char.titlecase() } to it },
            selectedOption = currentAppTheme,
            onDismiss = { showThemeDialog = false },
            onConfirm = { theme ->
                settingsViewModel.setAppTheme(theme)
                showThemeDialog = false
            }
        )
    }

    if (showUnitDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_select_unit_dialog_title),
            options = listOf("kg", "lbs").map { it.uppercase() to it },
            selectedOption = currentWeightUnit,
            onDismiss = { showUnitDialog = false },
            onConfirm = { unit ->
                settingsViewModel.setWeightUnit(unit)
                showUnitDialog = false
            }
        )
    }

    if (showOpenSourceLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showOpenSourceLicensesDialog = false },
            title = { Text(stringResource(R.string.settings_open_source_licenses)) },
            text = { Text(stringResource(R.string.settings_open_source_licenses_placeholder)) },
            confirmButton = {
                TextButton(onClick = { showOpenSourceLicensesDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

// -- Composable para "Acerca de" más elaborado --
@Composable
fun AboutAppItem(versionName: String, versionCode: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo de la app (opcional)
        Image(
            painter = painterResource(id = R.drawable.ic_logo2), // Reemplaza con tu logo real
            contentDescription = stringResource(R.string.app_logo_desc),
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(id = R.string.app_name), // Asume que tienes app_name en strings.xml
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.settings_app_version_formatted, versionName, versionCode.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    ListDivider()
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(), // Un estilo común es usar mayúsculas para títulos de sección
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp), // Más pequeño, con espaciado
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector? = null,
    iconContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val itemModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 16.dp, vertical = 12.dp) // Ajusta el padding
        .heightIn(min = 60.dp) // Un poco más de altura mínima

    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                modifier = Modifier
                    .size(32.dp) // Tamaño estándar para iconos en listas
                    .padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Si no hay icono, añade un espacio para mantener la alineación del texto
            Spacer(Modifier.width(24.dp + 16.dp))
        }
        content()
    }
    ListDivider() // Usar un divisor más estándar o personalizado
}


@Composable
fun SwitchSettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingItem(
        icon = icon,
        iconContentDescription = title,
        // Hacemos que todo el item sea clickeable para cambiar el switch, mejora la accesibilidad
        onClick = { onCheckedChange(!checked) }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange, // El onClick del Row ya lo maneja, pero es bueno tenerlo también aquí por si acaso
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun ClickableSettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    currentValue: String,
    onClick: () -> Unit
) {
    SettingItem(
        icon = icon,
        iconContentDescription = title,
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                currentValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon( // Indicador de que es clickeable para más opciones
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null, // Decorativo
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoSettingItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SettingItem(
        icon = icon,
        iconContentDescription = title,
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        Icon( // Indicador de que navega o abre algo
            imageVector = Icons.AutoMirrored.Filled.Launch, // El icono "launch" es bueno para enlaces externos
            contentDescription = null, // Decorativo
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<String, T>>, // Par de (Texto a mostrar, Valor real)
    selectedOption: T,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit
) {
    var tempSelected by remember(selectedOption) { mutableStateOf(selectedOption) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (text, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { tempSelected = value }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (tempSelected == value),
                            onClick = { tempSelected = value }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelected) }) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.cancel))
            }
        }
    )
}
// Un divisor más delgado y sutil
@Composable
fun ListDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 16.dp + 32.dp + 16.dp) // Alineado con el inicio del texto
    )
}
// --- Funciones de utilidad para obtener la versión (las tenías fuera, pero pueden estar aquí o en un archivo utils) ---
fun getAppVersionName(context: Context): String {
    return try {
        getPackageInfo(context).versionName
    } catch (e: Exception) {
        "N/A"
    }.toString()
}

fun getAppVersionCode(context: Context): Int {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getPackageInfo(context).longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(context).versionCode
        }
    } catch (e: Exception) {
        0
    }
}

private fun getPackageInfo(context: Context): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
}
