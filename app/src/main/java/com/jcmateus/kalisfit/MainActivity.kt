package com.jcmateus.kalisfit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import com.jcmateus.kalisfit.navigation.KalisNavGraph
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme
import com.jcmateus.kalisfit.viewmodel.AppTheme
import com.jcmateus.kalisfit.viewmodel.SettingsViewModel
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    // --- INICIO: LÓGICA PARA PERMISO DE NOTIFICACIONES ---
    /**
     * Lanzador para la solicitud de permiso de notificaciones.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("NotificationPermission", "Permiso POST_NOTIFICATIONS concedido desde MainActivity.")
                // Aquí podrías, si quisieras, disparar alguna lógica si el permiso se concede
                // en este momento, pero generalmente la acción de programar notificaciones
                // se hará desde un ViewModel o una acción específica del usuario.
            } else {
                Log.d("NotificationPermission", "Permiso POST_NOTIFICATIONS denegado desde MainActivity.")
                // Considera mostrar un mensaje al usuario si el permiso es crucial
                // y es denegado, guiándole a la configuración de la app.
            }
        }

    /**
     * Verifica y solicita el permiso POST_NOTIFICATIONS si es necesario (para Android 13+).
     * Marcada como 'internal' para que pueda ser llamada desde otros archivos
     * dentro del mismo módulo (como tus pantallas Composable).
     */
    internal fun askNotificationPermissionInternal() {
        // Esta función solo es relevante para Android 13 (API 33, TIRAMISU) y versiones posteriores.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido por el usuario.
                    Log.d("NotificationPermission", "Permiso POST_NOTIFICATIONS ya está concedido (verificado desde MainActivity).")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario ha denegado el permiso previamente, pero no seleccionó "No volver a preguntar".
                    // Deberías mostrar una UI explicando por qué necesitas el permiso.
                    Log.d("NotificationPermission", "Mostrando rationale para POST_NOTIFICATIONS (desde MainActivity).")
                    // En una app real, aquí mostrarías un diálogo/Snackbar antes de lanzar.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // El permiso no ha sido solicitado antes o el usuario lo denegó
                    // y seleccionó "No volver a preguntar".
                    Log.d("NotificationPermission", "Solicitando permiso POST_NOTIFICATIONS (desde MainActivity).")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // En versiones anteriores a Android 13, el permiso se considera otorgado
            // si está en el Manifest.
            Log.d("NotificationPermission", "No se requiere solicitud en tiempo de ejecución para POST_NOTIFICATIONS (API < 33, verificado desde MainActivity).")
        }
    }
    // --- FIN: LÓGICA PARA PERMISO DE NOTIFICACIONES ---

    @RequiresApi(Build.VERSION_CODES.O) // Esta anotación ya la tenías
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NO LLAMAMOS a askNotificationPermissionInternal() aquí directamente.
        // Se llamará desde LoginScreen o KalisMainScreen usando LaunchedEffect.

        setContent {
            val currentAppTheme by settingsViewModel.appTheme.collectAsState()

            val useDarkTheme = when (currentAppTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }

            KalisFitTheme(
                darkTheme = useDarkTheme,
                dynamicColor = true
            ) {
                Surface {
                    val navController = rememberNavController()
                    KalisNavGraph(
                        navController = navController,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
