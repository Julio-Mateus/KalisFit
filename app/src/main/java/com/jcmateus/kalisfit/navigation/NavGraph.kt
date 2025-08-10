package com.jcmateus.kalisfit.navigation


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.ui.screens.AllExercisesScreen
import com.jcmateus.kalisfit.ui.screens.CalisthenicsLevelDetailScreen
import com.jcmateus.kalisfit.ui.screens.CartScreen
import com.jcmateus.kalisfit.ui.screens.EditProfileScreen
import com.jcmateus.kalisfit.ui.screens.EditRoutineScreen
import com.jcmateus.kalisfit.ui.screens.ForgotPasswordScreen
import com.jcmateus.kalisfit.ui.screens.HistorialScreen
import com.jcmateus.kalisfit.ui.screens.KalisMainScreen
import com.jcmateus.kalisfit.ui.screens.LoginScreen
import com.jcmateus.kalisfit.ui.screens.MyRoutinesScreen
import com.jcmateus.kalisfit.ui.screens.OnboardingScreen
import com.jcmateus.kalisfit.ui.screens.OnboardingSuccessScreen
import com.jcmateus.kalisfit.ui.screens.ProductDetailScreen
import com.jcmateus.kalisfit.ui.screens.ProfileScreen
import com.jcmateus.kalisfit.ui.screens.RegisterScreen
import com.jcmateus.kalisfit.ui.screens.RoutineDetailScreen
import com.jcmateus.kalisfit.ui.screens.RoutineExplorerScreen
import com.jcmateus.kalisfit.ui.screens.RoutineSuccessScreen
import com.jcmateus.kalisfit.ui.screens.SettingsScreen
import com.jcmateus.kalisfit.ui.screens.SplashScreen
import com.jcmateus.kalisfit.ui.screens.TipsScreen
import com.jcmateus.kalisfit.viewmodel.AuthViewModel
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModel
import com.jcmateus.kalisfit.viewmodel.RoutineDetailViewModel
import com.jcmateus.kalisfit.viewmodel.SettingsViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisNavGraph(navController: NavHostController, settingsViewModel: SettingsViewModel, authViewModel: AuthViewModel) {
    // NavHost se queda, pero ahora le añadimos animaciones por defecto.
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        // Animaciones por defecto para TODAS las transiciones en este NavHost
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(500)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(500)
            )
        }
    ) {

        // --- Rutas de Autenticación y Onboarding ---
        composable(Routes.SPLASH, enterTransition = { null }, exitTransition = { null }) {
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
                onRegisterSuccess = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = false
                        }
                    }
                },// O popUpTo(Routes.REGISTER) { inclusive = true } si prefieres
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
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        } // O a MAIN_CONTENT popUpTo ONBOARDING_SUCCESS
                    }
                })
        }

        // --- Contenedor Principal con Drawer, BottomNav y TopAppBar ---
        composable(
            Routes.MAIN_CONTENT, // Al entrar al contenido principal, usemos un Fade para que se sienta como un "inicio"
            enterTransition = { fadeIn(animationSpec = tween(700)) },
            // Al salir (logout), también un fade out
            exitTransition = { fadeOut(animationSpec = tween(700)) }) {
            // KalisMainScreen contiene el NavHost interno para las pestañas (Home, Calisthenics, Stoicism, Running)
            KalisMainScreen(mainNavController = navController)
        }
        // --- Rutas de Nivel Superior (Sin BottomNav) ---
        // Accesibles desde el Drawer, o navegación directa desde otras pantallas.
        composable(Routes.PROFILE_SCREEN) { // Usando tu nueva constante de Routes.kt
            ProfileScreen(navController = navController)
        }
        composable(
            route = "${Routes.ROUTINES_EXPLORER_SCREEN}?place={place}&level={level}", // AÑADIR &level={level}
            arguments = listOf(
                navArgument("place") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("level") { // AÑADIR ESTE ARGUMENTO
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val placeArgument = backStackEntry.arguments?.getString("place")
            val levelArgument = backStackEntry.arguments?.getString("level") // OBTENER EL ARGUMENTO DE NIVEL

            RoutineExplorerScreen(
                navController = navController,
                placeFilterArgument = placeArgument,
                levelFilterArgument = levelArgument // PASAR EL ARGUMENTO DE NIVEL A LA PANTALLA
            )
        }
        composable(
            route = Routes.ROUTINE_DETAIL_SCREEN, // ej. "routine_detail/{routineId}"
            arguments = listOf(navArgument(Routes.Args.ROUTINE_ID_ARG) { // ej. "routineId"
                type = NavType.StringType
                // nullable = false // Por defecto es false
            })
        ) { backStackEntry ->
            // El backStackEntry todavía es útil para obtener argumentos si los necesitaras pasar
            // directamente al Composable, pero el ViewModel ahora los tomará del SavedStateHandle.
            Log.d(
                "NavGraph_RoutineDetail",
                "Navegando a RoutineDetail. Argumento: ${backStackEntry.arguments?.getString(Routes.Args.ROUTINE_ID_ARG)}"
            )

            val currentUserFromAuth by authViewModel.currentUser.collectAsState()
            val currentUserId: String? = currentUserFromAuth?.uid

            RoutineDetailScreen(
                navController = navController, // El navController principal de este NavHost
                // Ya NO pasas routineDetailViewModel aquí
                currentUserId = currentUserId, // Pasa esto si la pantalla aún lo necesita directamente
            )
        }
        composable(
            route = Routes.ROUTINE_EXECUTION_SCREEN, // Usando la nueva plantilla de Routes.kt
            arguments = listOf(
                navArgument(Routes.Args.ROUTINE_ID_ARG) { // El nombre del argumento DEBE COINCIDIR con el de la plantilla de ruta
                    type = NavType.StringType
                    nullable = true // Importante si RoutineScreen acepta un rutinaId: String?
                    // Si rutinaId en RoutineScreen NO es nullable, entonces aquí también debe ser nullable = false (o no especificarlo, ya que es el default)
                    // En tu caso, RoutineScreen tiene rutinaId: String?, entonces nullable = true es correcto.
                }
            )
        ) { backStackEntry ->
            val rutinaId = backStackEntry.arguments?.getString(Routes.Args.ROUTINE_ID_ARG)

            // Aquí instancias tu RoutineScreen
            // Asumiendo que RoutineScreen.kt ya está creado e importado
            com.jcmateus.kalisfit.ui.screens.RoutineScreen( // Asegúrate de que la importación sea correcta si está en otro paquete
                navController = navController,
                rutinaId = rutinaId
                // UserProfileViewModel se obtendrá con viewModel() dentro de RoutineScreen
            )
        }
        composable(
            route = Routes.EDIT_ROUTINE_SCREEN_ROUTE_TEMPLATE, // <--- Usa la plantilla de ruta completa
            arguments = listOf(
                navArgument(Routes.Args.USER_ID_ARG) { // <--- Usa Args para consistencia
                    type = NavType.StringType
                    // No se marca como nullable aquí porque la plantilla lo define como {userId},
                    // pero el ViewModel debería manejar si llega vacío o inválido.
                    // Si quieres que la ruta falle si no se pasa, NavType.StringType es suficiente.
                },
                navArgument(Routes.Args.TEMPLATE_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.Args.CUSTOM_ROUTINE_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            // Aquí el composable para EditRoutineScreen
            val editRoutineViewModel: EditRoutineViewModel = viewModel()
            EditRoutineScreen(
                navController = navController,
                viewModel = editRoutineViewModel
            )
        }
        composable(Routes.ROUTINE_SUCCESS_SCREEN) { // Usando tu nueva constante
            RoutineSuccessScreen(onFinish = {
                // Decide a dónde navegar después del éxito de una rutina.
                // Podría ser a la lista de rutinas, a home, o popBackStack.
                navController.popBackStack(
                    Routes.MAIN_CONTENT,
                    inclusive = false
                ) // Ejemplo: Volver a main sin incluir la pantalla de éxito
                // o navController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) { popUpTo(Routes.MAIN_CONTENT) }
            })
        }
        composable(Routes.MY_CUSTOM_ROUTINES_SCREEN) {
            // Asumiendo que has creado MyRoutinesScreen.kt como se discutió
            MyRoutinesScreen(navController = navController)
        }
        composable(Routes.ALL_EXERCISES_SCREEN) {
            // Necesitarás crear una pantalla AllExercisesScreen.kt
            // y su respectivo ViewModel si requiere lógica compleja.
            // Por ahora, un placeholder:
            AllExercisesScreen(navController = navController) // Asegúrate de crear este Composable
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
            arguments = listOf(navArgument("productId") {
                type = NavType.StringType
            }) // <--- CAMBIO: "productId"
        ) { backStackEntry ->
            val productId =
                backStackEntry.arguments?.getString("productId") // <--- CAMBIO: obtener "productId"
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