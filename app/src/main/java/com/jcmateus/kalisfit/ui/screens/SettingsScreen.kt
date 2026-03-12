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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val notificationsEnabled by settingsViewModel.userNotificationsPreference.collectAsState()
    val voiceCoachEnabled by settingsViewModel.voiceCoachEnabled.collectAsState()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()
    val currentAppTheme by settingsViewModel.appTheme.collectAsState()
    val currentWeightUnit by settingsViewModel.weightUnit.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    
    val appNameString = stringResource(R.string.app_name)
    val playStoreLink = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val shareBody = stringResource(R.string.settings_share_app_text, appNameString, playStoreLink)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_general))
            
            SwitchSettingItem(
                title = stringResource(R.string.settings_enable_notifications),
                description = stringResource(R.string.settings_enable_notifications_desc),
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { settingsViewModel.setUserNotificationsPreference(it) }
            )

            SettingsSectionTitle(title = "Entrenamiento")
            SwitchSettingItem(
                title = "Entrenador de Voz",
                description = "Instrucciones por voz durante la rutina",
                icon = Icons.Default.RecordVoiceOver,
                checked = voiceCoachEnabled,
                onCheckedChange = { settingsViewModel.setVoiceCoachEnabled(it) }
            )
            SwitchSettingItem(
                title = "Vibración",
                description = "Avisos táctiles al cambiar de serie",
                icon = Icons.Default.Vibration,
                checked = vibrationEnabled,
                onCheckedChange = { settingsViewModel.setVibrationEnabled(it) }
            )

            SettingsSectionTitle(title = "Personalización")
            ClickableSettingItem(
                title = stringResource(R.string.settings_app_theme),
                icon = Icons.Default.Palette,
                currentValue = currentAppTheme.name,
                onClick = { showThemeDialog = true }
            )
            ClickableSettingItem(
                title = stringResource(R.string.settings_weight_units),
                icon = Icons.Default.FitnessCenter,
                currentValue = currentWeightUnit.uppercase(),
                onClick = { showUnitDialog = true }
            )
            
            SettingsSectionTitle(title = stringResource(R.string.settings_section_support))
            InfoSettingItem(
                title = stringResource(R.string.settings_share_app),
                icon = Icons.Default.Share,
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareBody)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_contact_support),
                icon = Icons.Default.Email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:kalisfit8@gmail.com"))
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }
            )

            SettingsSectionTitle(title = stringResource(R.string.settings_section_legal))
            InfoSettingItem(
                title = stringResource(R.string.settings_privacy_policy),
                icon = Icons.Default.Policy,
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://terminoskalisfit.blogspot.com/p/politica-de-privacidad-kalisfit.html"))) }
            )
            InfoSettingItem(
                title = stringResource(R.string.settings_terms_of_service),
                icon = Icons.AutoMirrored.Filled.Notes,
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://terminoskalisfit.blogspot.com/p/terminos-y-condiciones-de-uso-kalisfit.html"))) }
            )
            
            AboutAppItem()
        }
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "Seleccionar Tema",
            options = AppTheme.entries.map { it.name to it },
            selectedOption = currentAppTheme,
            onDismiss = { showThemeDialog = false },
            onConfirm = { settingsViewModel.setAppTheme(it); showThemeDialog = false }
        )
    }

    if (showUnitDialog) {
        SingleChoiceDialog(
            title = "Seleccionar Unidad",
            options = listOf("kg" to "kg", "lbs" to "lbs"),
            selectedOption = currentWeightUnit,
            onDismiss = { showUnitDialog = false },
            onConfirm = { settingsViewModel.setWeightUnit(it); showUnitDialog = false }
        )
    }
}

@Composable
fun AboutAppItem() {
    Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_logo2),
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text("KalisFit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Versión 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SwitchSettingItem(title: String, description: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ClickableSettingItem(title: String, icon: ImageVector, currentValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(currentValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun InfoSettingItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.Launch, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun <T> SingleChoiceDialog(title: String, options: List<Pair<String, T>>, selectedOption: T, onDismiss: () -> Unit, onConfirm: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (text, value) ->
                    Row(Modifier.fillMaxWidth().clickable { onConfirm(value) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = value == selectedOption, onClick = { onConfirm(value) })
                        Spacer(Modifier.width(8.dp))
                        Text(text)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
