package com.jcmateus.kalisfit.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.values
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme
import com.jcmateus.kalisfit.viewmodel.AppTheme
import com.jcmateus.kalisfit.viewmodel.SettingsViewModel
import kotlin.text.uppercase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = viewModel() // Inyecta o crea el ViewModel
) {
    val context = LocalContext.current

    // Recolecta los estados del ViewModel
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val currentAppTheme by settingsViewModel.appTheme.collectAsState()
    val currentWeightUnit by settingsViewModel.weightUnit.collectAsState()
    val appVersion = settingsViewModel.appVersion

    var showThemeDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Para que sea desplazable si hay muchas opciones
                .padding(horizontal = 16.dp)
        ) {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_general))
            SwitchSettingItem(
                title = stringResource(R.string.settings_enable_notifications),
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
            )
            ClickableSettingItem(
                title = stringResource(R.string.settings_app_theme),
                icon = when(currentAppTheme) {
                    AppTheme.LIGHT -> Icons.Default.Brightness7
                    AppTheme.DARK -> Icons.Default.Brightness4
                    AppTheme.SYSTEM -> Icons.Default.SystemUpdateAlt
                },
                currentValue = currentAppTheme.name.replaceFirstChar { it.titlecase() },
                onClick = { showThemeDialog = true }
            )

            SettingsSectionTitle(title = stringResource(R.string.settings_section_units))
            ClickableSettingItem(
                title = stringResource(R.string.settings_weight_units),
                icon = Icons.Default.Scale,
                currentValue = currentWeightUnit.uppercase(),
                onClick = { showUnitDialog = true }
            )

            SettingsSectionTitle(title = stringResource(R.string.settings_section_legal))
            InfoSettingItem(
                title = stringResource(R.string.settings_privacy_policy),
                icon = Icons.Default.Policy,
                onClick = {
                    // Reemplaza con tu URL real
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/privacy"))
                    context.startActivity(intent)
                }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_terms_of_service),
                icon = Icons.Default.Info, // O algún icono más específico
                onClick = {
                    // Reemplaza con tu URL real
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/terms"))
                    context.startActivity(intent)
                }
            )

            SettingsSectionTitle(title = stringResource(R.string.settings_section_about))
            SettingItem(
                icon = Icons.Default.Info,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_app_version), style = MaterialTheme.typography.bodyLarge)
                        Text(appVersion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp)) // Espacio al final
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
                // Aquí deberías tener la lógica para aplicar el tema globalmente
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
}


@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector? = null,
    iconContentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp) // Altura mínima típica para items de lista
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp + 16.dp)) // Para alinear con items que sí tienen icono
        }
        content()
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}


@Composable
fun SwitchSettingItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingItem(
        icon = icon,
        iconContentDescription = title,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(
            1f
        ))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
fun ClickableSettingItem(
    title: String,
    icon: ImageVector,
    currentValue: String,
    onClick: () -> Unit
) {
    SettingItem(
        icon = icon,
        iconContentDescription = title,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(currentValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InfoSettingItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SettingItem(
        icon = icon,
        iconContentDescription = title,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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


fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "N/A"
    }.toString()
}


@Preview(showBackground = true, name = "Settings Screen Light")
@Composable
fun SettingsScreenPreview() {
    KalisFitTheme(darkTheme = false) {
        // Obtén el contexto local y trata de castearlo (puede no ser ideal para previews)
        val context = LocalContext.current
        val application = context.applicationContext as Application // ¡Cuidado aquí en previews!
        SettingsScreen(
            navController = rememberNavController(),
            settingsViewModel = SettingsViewModel(application) // Pasa la application
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen Dark")
@Composable
fun SettingsScreenDarkPreview() {
    KalisFitTheme(darkTheme = true) {
        val context = LocalContext.current
        val application = context.applicationContext as Application // ¡Cuidado aquí en previews!
        SettingsScreen(
            navController = rememberNavController(),
            settingsViewModel = SettingsViewModel(application) // Pasa la application
        )
    }
}