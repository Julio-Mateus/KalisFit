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
import com.jcmateus.kalisfit.ui.screens.StoicismContentScreen
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
                navController.navigate(Routes.MAIN_CONTENT) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING_SUCCESS) {
            OnboardingSuccessScreen(
                onContinue = {
                    navController.navigate(Routes.MAIN_CONTENT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                })
        }

        // --- Contenedor Principal con Drawer, BottomNav y TopAppBar ---
        composable(Routes.MAIN_CONTENT) {
            KalisMainScreen(mainNavController = navController)
        }

        // --- Rutas de Nivel Superior (Sin BottomNav) ---
        // Accesibles desde el Drawer, TopAppBar, o desde otras pantallas usando mainNavController.

        // NUEVA RUTA PARA LA PANTALLA DE PERFIL COMPLETA
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController) // Asumiendo que ProfileScreen no necesita directamente el navController para navegación interna compleja
            // o que usa un ViewModel para manejar acciones que podrían navegar.
            // Si ProfileScreen necesita navegar a otros destinos del mainNavController, pásaselo:
            // ProfileScreen(navController = navController)
        }

        composable(
            route = Routes.ROUTINE_DETAIL,
            arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rutinaId = backStackEntry.arguments?.getString("rutinaId")
            if (rutinaId != null) {
                RoutineScreen(navController = navController, rutinaId = rutinaId)
            } else {
                // Manejo de error o volver atrás
            }
        }
        composable(Routes.ROUTINE_SUCCESS) {
            RoutineSuccessScreen(onFinish = {
                navController.navigate(Routes.MAIN_CONTENT) {
                    popUpTo(Routes.MAIN_CONTENT) { inclusive = true }
                }
            })
        }

        composable(Routes.EDIT_PROFILE) {
            // EditProfileScreen puede obtener el UserProfileViewModel internamente
            // o podrías instanciarlo aquí si es compartido de forma específica
            EditProfileScreen(navController = navController) // Asumiendo que toma el UserProfile de un ViewModel interno
            // o que ya no pasas 'user = UserProfile()' directamente.
            // Si aún necesitas pasar 'user', asegúrate que UserProfile() sea el valor correcto
            // o que lo obtengas de un ViewModel aquí.
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.STOICISM_CONTENT) {
            StoicismContentScreen(mainNavController = navController) // Pasas el navController del KalisNavGraph
        }

        // --- Pantalla de Tips (si aún la usas) ---
        if (Routes.TIPS.isNotBlank()) { // Para evitar error si comentas la ruta TIPS en Routes.kt
            composable(Routes.TIPS) {
                TipsScreen() // Asumiendo que no necesita navController
            }
        }


        // NOTA: Las rutas para las PESTAÑAS (HOME_TAB, ROUTINES_TAB, etc.) NO se definen aquí
        // porque son manejadas por el NavHost INTERNO dentro de KalisMainScreen.
        // Este KalisNavGraph (con mainNavController) solo necesita saber cómo llegar a Routes.MAIN_CONTENT,
        // y a las pantallas de nivel superior como Routes.PROFILE, Routes.SETTINGS, etc.
    }
}
