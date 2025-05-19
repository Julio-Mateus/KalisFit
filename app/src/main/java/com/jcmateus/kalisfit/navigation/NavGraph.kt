package com.jcmateus.kalisfit.navigation


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jcmateus.kalisfit.ui.screens.EditProfileScreen
import com.jcmateus.kalisfit.ui.screens.ForgotPasswordScreen
import com.jcmateus.kalisfit.ui.screens.HomeScreen
import com.jcmateus.kalisfit.ui.screens.KalisMainScreen
import com.jcmateus.kalisfit.ui.screens.LoginScreen
import com.jcmateus.kalisfit.ui.screens.OnboardingScreen
import com.jcmateus.kalisfit.ui.screens.OnboardingSuccessScreen
import com.jcmateus.kalisfit.ui.screens.ProfileScreen
import com.jcmateus.kalisfit.ui.screens.RegisterScreen
import com.jcmateus.kalisfit.ui.screens.RoutineScreen
import com.jcmateus.kalisfit.ui.screens.RoutineSuccessScreen
import com.jcmateus.kalisfit.ui.screens.SettingsScreen
import com.jcmateus.kalisfit.ui.screens.SplashScreen
import com.jcmateus.kalisfit.ui.screens.TipsScreen
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // --- Rutas de Autenticación y Onboarding ---
        composable(Routes.SPLASH) {
            SplashScreen(
                onUserLoggedIn = {
                    navController.navigate(Routes.MAIN_CONTENT) { // Navega a la pantalla principal
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
                    navController.navigate(Routes.MAIN_CONTENT) { // Navega a la pantalla principal
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgot = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.ONBOARDING) },
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
                navController.navigate(Routes.MAIN_CONTENT) { // Navega a la pantalla principal
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING_SUCCESS) {
            OnboardingSuccessScreen(
                onContinue = {
                    navController.navigate(Routes.MAIN_CONTENT) { // Navega a la pantalla principal
                        // Limpia la pila hasta LOGIN para que al presionar "atrás"
                        // desde MAIN_CONTENT no vuelva al OnboardingSuccess
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                })
        }

        // --- Contenedor Principal con BottomNav y TopAppBar ---
        // Esta es la ruta que carga KalisMainScreen
        composable(Routes.MAIN_CONTENT) {
            KalisMainScreen(mainNavController = navController)
        }

        // --- Rutas de Nivel Superior (Sin BottomNav) ---
        // Estas son accesibles desde pantallas dentro de MAIN_CONTENT (usando el mainNavController)
        // o desde el TopAppBar.

        composable(
            route = Routes.ROUTINE_DETAIL, // Usar la constante definida
            arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rutinaId = backStackEntry.arguments?.getString("rutinaId")
            // Valida que rutinaId no sea null si es obligatorio
            if (rutinaId != null) {
                RoutineScreen(navController = navController, rutinaId = rutinaId)
            } else {
                // Manejar el caso de rutinaId nulo, quizás volviendo atrás o mostrando un error
                // navController.popBackStack()
                // Text("Error: ID de rutina no encontrado")
            }
        }
        composable(Routes.ROUTINE_SUCCESS) {
            RoutineSuccessScreen(onFinish = {
                navController.navigate(Routes.MAIN_CONTENT) {
                    // Limpia hasta MAIN_CONTENT para un reinicio limpio en la pantalla principal
                    popUpTo(Routes.MAIN_CONTENT) { inclusive = true }
                }
            })
        }

        composable(Routes.EDIT_PROFILE) {
            // EditProfileScreen debería obtener los datos del usuario a través de un ViewModel
            val userProfileViewModel: UserProfileViewModel =
                viewModel()
            EditProfileScreen(navController = navController, user = UserProfile())
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        // --- Pantalla de Tips ---
        // Evalúa el propósito de esta pantalla:
        // 1. ¿Es una pantalla para EXPLORAR una lista de tips? -> Usa Routes.TIPS_EXPLORER y crea TipsExplorerScreen.
        // 2. ¿Es una pantalla que muestra UN tip (ej. tip del día)? -> Podría no necesitar ser una ruta de nivel superior.
        //    Podría ser un diálogo, o contenido integrado en HomeScreen.
        // 3. Si es una pantalla simple a la que necesitas navegar directamente:
        composable(Routes.TIPS) { // Si mantienes esta ruta
            // SimpleTipsScreen(navController = navController) // O la pantalla que corresponda
            // Por ejemplo, si TipsScreen es solo una pantalla simple:
            TipsScreen() // Si no necesita navController para más navegación.
        }

        // NOTA: Las rutas como Routes.HOME y Routes.PROFILE (las antiguas) ya no se definen aquí
        // porque son manejadas por el NavHost INTERNO dentro de KalisMainScreen.
        // El KalisNavGraph solo necesita saber cómo llegar a Routes.MAIN_CONTENT.
    }
}
