package com.jcmateus.kalisfit.navigation


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jcmateus.kalisfit.ui.screens.CalisthenicsLevelDetailScreen
import com.jcmateus.kalisfit.ui.screens.CartScreen
import com.jcmateus.kalisfit.ui.screens.EditProfileScreen
import com.jcmateus.kalisfit.ui.screens.ForgotPasswordScreen
import com.jcmateus.kalisfit.ui.screens.HistorialScreen
import com.jcmateus.kalisfit.ui.screens.HomeScreen
import com.jcmateus.kalisfit.ui.screens.KalisMainScreen
import com.jcmateus.kalisfit.ui.screens.LoginScreen
import com.jcmateus.kalisfit.ui.screens.OnboardingScreen
import com.jcmateus.kalisfit.ui.screens.OnboardingSuccessScreen
import com.jcmateus.kalisfit.ui.screens.ProductDetailScreen
import com.jcmateus.kalisfit.ui.screens.ProfileScreen
import com.jcmateus.kalisfit.ui.screens.RegisterScreen
import com.jcmateus.kalisfit.ui.screens.RoutineExplorerScreen
import com.jcmateus.kalisfit.ui.screens.RoutineScreen
import com.jcmateus.kalisfit.ui.screens.RoutineSuccessScreen
import com.jcmateus.kalisfit.ui.screens.SettingsScreen
import com.jcmateus.kalisfit.ui.screens.SplashScreen
import com.jcmateus.kalisfit.ui.screens.StoicismContentScreen
import com.jcmateus.kalisfit.ui.screens.TipsScreen
import com.jcmateus.kalisfit.viewmodel.SettingsViewModel
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisNavGraph(navController: NavHostController, settingsViewModel: SettingsViewModel) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // --- Rutas de Autenticación y Onboarding ---
        composable(Routes.SPLASH) {
            SplashScreen(
                onUserLoggedIn = {
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onUserNotLoggedIn = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgot = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.ONBOARDING){popUpTo(Routes.LOGIN) { inclusive = false } } },// O popUpTo(Routes.REGISTER) { inclusive = true } si prefieres
                onNavigateToLogin = { navController.popBackStack(Routes.LOGIN, inclusive = false) }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBackToLogin = {
                navController.popBackStack(Routes.LOGIN, inclusive = false)
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                navController.navigate(Routes.ONBOARDING_SUCCESS) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING_SUCCESS) {
            OnboardingSuccessScreen(
                onContinue = {
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.LOGIN) { inclusive = true } // O a MAIN_CONTENT popUpTo ONBOARDING_SUCCESS
                    }
                })
        }

        // --- Contenedor Principal con Drawer, BottomNav y TopAppBar ---
        composable(Routes.MAIN_CONTENT) {
            // KalisMainScreen contiene el NavHost interno para las pestañas (Home, Calisthenics, Stoicism, Running)
            KalisMainScreen(mainNavController = navController)
        }

        // --- Rutas de Nivel Superior (Sin BottomNav) ---
        // Accesibles desde el Drawer, o navegación directa desde otras pantallas.

        composable(Routes.PROFILE_SCREEN) { // Usando tu nueva constante de Routes.kt
            ProfileScreen(navController = navController)
        }


        composable(
            route = "${Routes.ROUTINES_EXPLORER_SCREEN}?place={place}", //  <-- MODIFICACIÓN IMPORTANTE 1: Define el argumento en la plantilla de la ruta
            arguments = listOf(navArgument("place") {          //  <-- MODIFICACIÓN IMPORTANTE 2: Declara el argumento
                type = NavType.StringType
                nullable = true                                    // Es opcional
                defaultValue = null                                // Valor por defecto si no se pasa
            })
        ) { backStackEntry ->                                    // backStackEntry contiene los argumentos
            val placeArgument = backStackEntry.arguments?.getString("place") // <-- MODIFICACIÓN IMPORTANTE 3: Extrae el argumento

            RoutineExplorerScreen(
                navController = navController, // O el NavHostController relevante
                placeFilterArgument = placeArgument  // <-- MODIFICACIÓN IMPORTANTE 4: Pasa el argumento extraído
                // viewModel se obtendrá dentro de RoutineExplorerScreen usando viewModel()
            )
        }

        composable(
            route = Routes.ROUTINE_DETAIL_SCREEN, // Usando tu nueva constante
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId")
            if (routineId != null) {
                RoutineScreen(navController = navController, rutinaId = routineId)
            } else {
                // Manejo de error: argumento faltante
                Text("Error: Falta el ID de la rutina.")
                // O navController.popBackStack()
            }
        }
        composable(Routes.ROUTINE_SUCCESS_SCREEN) { // Usando tu nueva constante
            RoutineSuccessScreen(onFinish = {
                // Decide a dónde navegar después del éxito de una rutina.
                // Podría ser a la lista de rutinas, a home, o popBackStack.
                navController.popBackStack(Routes.MAIN_CONTENT, inclusive = false) // Ejemplo: Volver a main sin incluir la pantalla de éxito
                // o navController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) { popUpTo(Routes.MAIN_CONTENT) }
            })
        }

        composable(Routes.EDIT_PROFILE_SCREEN) { // Usando tu nueva constante
            EditProfileScreen(navController = navController)
        }

        composable(Routes.SETTINGS_SCREEN) { // Usando tu nueva constante
            SettingsScreen(navController = navController, settingsViewModel = settingsViewModel)
        }

        composable(Routes.ACTIVITY_HISTORY_SCREEN) {
            HistorialScreen(navController = navController) // Pasa navController si tu pantalla lo necesita
        }

        // Si StoicismContentScreen es una pantalla de nivel superior (ej. desde el Drawer)
        // y no solo una pestaña, su ruta se definiría aquí.
        // Por tu Routes.kt, parece que `STOICISM_TAB` es para la pestaña principal.
        // Si tuvieras una pantalla "Acerca del Estoicismo" o "Principios Clave"
        // accesible desde el drawer, la pondrías aquí con una ruta diferente.
        // Ejemplo:
        // composable("stoicism_deep_dive_screen") {
        //     StoicismDeepDiveScreen(mainNavController = navController)
        // }


        // --- Pantalla de Tips ---
        // (Asumo que tus Routes.kt tiene TIPS_SCREEN, no TIPS)
        if (Routes.TIPS_SCREEN.isNotBlank()) {
            composable(Routes.TIPS_SCREEN) {
                TipsScreen() // Asumiendo que no necesita navController o lo obtiene de un ViewModel
            }
        }

        // =======================================================================
        //          NUEVA RUTA: DETALLE DE NIVEL DE CALISTENIA
        // =======================================================================
        composable(
            route = Routes.CALISTHENICS_LEVEL_DETAIL_SCREEN, // La plantilla de la ruta
            arguments = listOf(
                navArgument("progressionId") { type = NavType.StringType },
                navArgument("levelId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val progressionId = backStackEntry.arguments?.getString("progressionId")
            val levelId = backStackEntry.arguments?.getString("levelId")

            if (progressionId != null && levelId != null) {
                // Una vez que crees CalisthenicsLevelDetailScreen.kt y la importes,
                // esta línea funcionará:
                CalisthenicsLevelDetailScreen(
                    navController = navController,
                    progressionId = progressionId,
                    levelId = levelId
                )
            } else {
                // Manejo de error si los argumentos no se pasan correctamente.
                // Esto no debería suceder si siempre usas la función helper Routes.calisthenicsLevelDetail()
                Text("Error: Faltan argumentos para el detalle del nivel de calistenia.")
                // Considera navController.popBackStack() para volver a la pantalla anterior.
            }
        }
        composable(
            route = Routes.PRODUCT_DETAIL_SCREEN, // Asumiendo que Routes.PRODUCT_DETAIL_SCREEN ahora es algo como "productDetail/{productId}"
            arguments = listOf(navArgument("productId") { type = NavType.StringType }) // <--- CAMBIO: "productId"
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") // <--- CAMBIO: obtener "productId"
            if (productId != null) {
                ProductDetailScreen(
                    navController = navController,
                    productId = productId, // <--- CAMBIO: pasar productId
                    // storeViewModel se inyectará por defecto o con la factory en ProductDetailScreen
                )
            } else {
                // Manejo de error: argumento faltante
                Text("Error: Falta el ID del producto.")
                // Considera navController.popBackStack() para volver
            }
        }
        composable(Routes.CART_SCREEN) { // Asumiendo que Routes.CART_SCREEN es "cart_screen" o similar
            CartScreen(
                navController = navController
                // Aquí también pasarías el CartRepository y AuthRepository si los
                // estás inyectando manualmente en lugar de usar viewModel() con una factory
                // que los obtiene de Firebase directamente dentro de CartScreen.
                // Por como está estructurado CartScreen ahora, no necesitas pasar los repos aquí.
            )
        }
    }
}