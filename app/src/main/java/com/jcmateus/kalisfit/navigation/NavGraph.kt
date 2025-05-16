package com.jcmateus.kalisfit.navigation


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import com.jcmateus.kalisfit.ui.screens.SplashScreen
import com.jcmateus.kalisfit.ui.screens.TipsScreen
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen( // Ya no se pasa navController
                onUserLoggedIn = {
                    navController.navigate("main") { // "main" o tu ruta principal
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
                    // Navega a "main" que contiene la barra inferior
                    navController.navigate("main") {
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
        // REMOVER: composable(Routes.PROFILE) { ... } // Esta ruta ahora solo debe existir en el NavHost anidado
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBackToLogin = {
                navController.popBackStack(Routes.LOGIN, inclusive = false)
            })
        }
        // REMOVER: composable(Routes.HOME) { ... } // Esta ruta ahora solo debe existir en el NavHost anidado

        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                // Después del onboarding, navega a "main"
                navController.navigate("main") {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.ONBOARDING_SUCCESS) {
            OnboardingSuccessScreen(
                onContinue = {
                    // Después del éxito del onboarding, navega a "main"
                    navController.navigate("main") {
                        // Podrías ajustar el popUpTo según tu flujo deseado
                        popUpTo(Routes.LOGIN) { inclusive = true } // Ejemplo: Limpia la pila hasta Login
                    }
                })
        }

        // Este es el destino que carga la pantalla con la barra inferior
        composable("main") {
            KalisMainScreen(mainNavController = navController)
        }

        // Rutas de nivel superior que NO deben tener la barra inferior
        // Estas rutas se navegan DESDE las pantallas dentro del NavHost anidado
        composable(
            route = "${Routes.ROUTINE}/{rutinaId}",
            arguments = listOf(navArgument("rutinaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rutinaId = backStackEntry.arguments?.getString("rutinaId")
            RoutineScreen(navController = navController, rutinaId = rutinaId)
        }
        composable(Routes.ROUTINE_SUCCESS) {
            RoutineSuccessScreen(onFinish = {
                // Al terminar la rutina, navega de vuelta a "main" (lo que te llevará a Home)
                navController.navigate("main") {
                    // Opcional: ajusta el popUpTo para eliminar las pantallas de rutina de la pila
                    // popUpTo("${Routes.ROUTINE}/{rutinaId}") { inclusive = true } // Esto podría ser complicado con el argumento
                    // Una opción más simple es popUpTo una ruta conocida antes de la rutina, si existe
                    // O simplemente limpiar hasta "main" para empezar de nuevo en Home
                    popUpTo("main") { inclusive = true }
                }
            })
        }

        // Asegúrate de tener la ruta 'edit_profile' definida aquí si se navega desde ProfileScreen
        composable("edit_profile") {
            // Tu composable EditProfileScreen
            EditProfileScreen(navController = navController, user = UserProfile()) // Pasa navController si necesitas navegar desde aquí
        }

        composable(Routes.TIPS) {
            TipsScreen() // Pasa navController si TipsScreen necesita navegar
        }
    }
}
