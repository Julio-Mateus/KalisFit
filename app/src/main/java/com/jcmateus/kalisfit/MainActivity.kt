package com.jcmateus.kalisfit

import android.Manifest
import android.content.Intent
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
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.navigation.Routes
import kotlin.getValue
import com.jcmateus.kalisfit.viewmodel.AuthViewModel


class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var navController: NavHostController

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            settingsViewModel.refreshNotificationPermissionStatus()
            if (isGranted) {
                Log.d(
                    "NotificationPermission",
                    "Permiso POST_NOTIFICATIONS concedido desde MainActivity."
                )
            } else {
                Log.d(
                    "NotificationPermission",
                    "Permiso POST_NOTIFICATIONS denegado desde MainActivity."
                )
            }
        }

    internal fun askNotificationPermissionInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NotificationPermission", "Permiso POST_NOTIFICATIONS ya está concedido.")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("NotificationPermission", "Mostrando rationale para POST_NOTIFICATIONS.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    Log.d("NotificationPermission", "Solicitando permiso POST_NOTIFICATIONS.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(
                "NotificationPermission",
                "No se requiere solicitud en tiempo de ejecución para POST_NOTIFICATIONS (API < 33)."
            )
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.refreshNotificationPermissionStatus()
        intent?.let { handleIntentExtras(it, true) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        intent?.let { handleIntentExtras(it, false) }

        setContent {
            navController = rememberNavController() // Inicializa la propiedad de la clase

            val currentAppTheme by settingsViewModel.appTheme.collectAsState()
            val useDarkTheme = when (currentAppTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            KalisFitTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false
            ) {
                Surface {
                    KalisNavGraph(
                        navController = navController, // Pasa la propiedad de la clase
                        settingsViewModel = settingsViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP) // onNewIntent(Intent) está disponible desde API 1, pero
    // para ser más precisos con las versiones recientes de ComponentActivity y sus dependencias.
    // En la práctica, suele funcionar sin esta anotación específica si tus minSdk es razonable.
    // Si tienes problemas de compatibilidad, revisa la documentación de la versión exacta de androidx.activity:activity-ktx que usas.
    // Generalmente no se necesita la anotación @RequiresApi para este método tan fundamental.
    override fun onNewIntent(intent: Intent) { // <-- Cambiado Intent? a Intent
        super.onNewIntent(intent)
        // Ahora 'intent' es no-nullable aquí, así que podemos pasarlo directamente.
        // También actualizamos el intent de la actividad para que onResume pueda acceder al más reciente.
        setIntent(intent) // MUY IMPORTANTE: actualiza el intent que la actividad retornará con getIntent()
        handleIntentExtras(intent, false)
    }

    private fun handleIntentExtras(intent: Intent, fromOnResume: Boolean = false) {
        val routineIdToNavigate = intent.getStringExtra("NAVIGATE_TO_ROUTINE_ID")

        if (!routineIdToNavigate.isNullOrBlank()) {
            Log.d(
                "MainActivity",
                "Recibido intent para navegar a rutina ID: $routineIdToNavigate. Desde onResume: $fromOnResume"
            )

            if (::navController.isInitialized) {
                // USAREMOS TU FUNCIÓN Routes.routineDetail()
                // Al abrir desde una notificación, generalmente no tenemos un contexto de usuario específico
                // para el deep link, a menos que la notificación sea PARA un usuario.
                // Si no se necesita un userId específico aquí, pasamos null.
                // Tu función routineDetail ya maneja el caso de userId nulo.
                val route = Routes.routineDetail(routineId = routineIdToNavigate, userId = null)

                Log.d("MainActivity", "Navegando a ruta: $route")

                navController.navigate(route) {
                    launchSingleTop = true
                }

                val isLaunchedFromHistory =
                    intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
                if (!isLaunchedFromHistory) {
                    intent.removeExtra("NAVIGATE_TO_ROUTINE_ID")
                    Log.d("MainActivity", "Extra NAVIGATE_TO_ROUTINE_ID removido del intent.")
                } else {
                    Log.d(
                        "MainActivity",
                        "Extra NAVIGATE_TO_ROUTINE_ID NO removido, intent desde historial."
                    )
                }

            } else {
                Log.w(
                    "MainActivity",
                    "NavController no inicializado al intentar manejar extras del intent."
                )
            }
        }
    }
}
