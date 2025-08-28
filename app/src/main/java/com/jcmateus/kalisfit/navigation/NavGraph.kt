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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jcmateus.kalisfit.ui.screens.AllExercisesScreen
import com.jcmateus.kalisfit.ui.screens.calistenia.CalisthenicsLevelDetailScreen
import com.jcmateus.kalisfit.ui.screens.store.CartScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.EditProfileScreen
import com.jcmateus.kalisfit.ui.screens.routines.EditRoutineScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.ForgotPasswordScreen
import com.jcmateus.kalisfit.ui.screens.historial.HistorialScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.LoginScreen
import com.jcmateus.kalisfit.ui.screens.routines.MyRoutinesScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.OnboardingScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.OnboardingSuccessScreen
import com.jcmateus.kalisfit.ui.screens.store.ProductDetailScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.ProfileScreen
import com.jcmateus.kalisfit.ui.screens.auth_profile.RegisterScreen
import com.jcmateus.kalisfit.ui.screens.routines.RoutineDetailScreen
import com.jcmateus.kalisfit.ui.screens.routines.RoutineExplorerScreen
import com.jcmateus.kalisfit.ui.screens.routines.RoutineScreen
import com.jcmateus.kalisfit.ui.screens.routines.RoutineSelectionScreen
import com.jcmateus.kalisfit.ui.screens.routines.RoutineSuccessScreen
import com.jcmateus.kalisfit.ui.screens.SettingsScreen
import com.jcmateus.kalisfit.ui.screens.SplashScreen
import com.jcmateus.kalisfit.ui.screens.TipsScreen
import com.jcmateus.kalisfit.ui.screens.WeeklyPlanScreen
import com.jcmateus.kalisfit.viewmodel.AuthViewModel
import com.jcmateus.kalisfit.viewmodel.EditRoutineViewModel
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
                    navController.navigate(Routes.REGISTER) {
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
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
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
            route = Routes.ROUTINE_DETAIL_SCREEN, // Ahora es "$ROUTINE_DETAIL_PREFIX/{routineId}?userId={userId}"
            arguments = listOf(
                navArgument(Routes.Args.ROUTINE_ID_ARG) {
                    type = NavType.StringType
                    // nullable = false // Es parte de la ruta, no puede ser nulo para que la ruta coincida
                },
                navArgument(Routes.Args.USER_ID_ARG) { // DECLARAR EL NUEVO ARGUMENTO
                    type = NavType.StringType
                    nullable = true // Es un query parameter opcional, así que puede ser nulo
                    defaultValue = null // Explícito que si no se pasa, es null
                }
            )
        ) { backStackEntry ->
            // El ViewModel ahora tomará routineId y userId del SavedStateHandle automáticamente.
            // No necesitas extraerlos aquí para pasarlos explícitamente al ViewModel.
            Log.d(
                "NavGraph_RoutineDetail",
                "Navegando a RoutineDetail. ROUTINE_ID_ARG: ${backStackEntry.arguments?.getString(Routes.Args.ROUTINE_ID_ARG)}, " +
                        "USER_ID_ARG: ${backStackEntry.arguments?.getString(Routes.Args.USER_ID_ARG)}" // Loguea el userId también
            )
            // Necesitas pasar el ID del usuario AUTENTICADO a RoutineDetailScreen para acciones
            // como "personalizar esta rutina PARA MÍ".
            val currentUserFromAuth by authViewModel.currentUser.collectAsState()
            val loggedInUserId: String? = currentUserFromAuth?.uid
            // Ya NO necesitas pasar `routineId` ni `userIdFromNav` a `RoutineDetailScreen`
            // si el ViewModel los toma del SavedStateHandle.
            // Solo pasas el `loggedInUserId` si la pantalla lo necesita para alguna lógica de UI
            // o para pasar a funciones del ViewModel que requieran el usuario *actual*.
            RoutineDetailScreen(
                navController = navController,
                // routineId = routineIdFromArgs, // Ya no es necesario si el VM usa SavedStateHandle
                currentUserId = loggedInUserId // Pasa el ID del usuario autenticado actualmente
                // para acciones específicas del usuario (ej. personalizar)
            )
        }
        composable(
            route = Routes.ROUTINE_EXECUTION_SCREEN, // Esta constante ahora es "$ROUTINE_EXECUTION_PREFIX?routineId={routineId}&customRoutineId={customRoutineId}"
            arguments = listOf(
                navArgument(Routes.Args.ROUTINE_ID_ARG) { // Argumento para el ID de la rutina base/plantilla
                    type = NavType.StringType
                    nullable = true       // Es un parámetro de consulta, puede ser nulo
                    defaultValue = null   // Valor por defecto si no se pasa
                },
                navArgument(Routes.Args.CUSTOM_ROUTINE_ID_ARG) { // NUEVO: Argumento para el ID de la rutina personalizada
                    type = NavType.StringType
                    nullable = true       // Es un parámetro de consulta, puede ser nulo
                    defaultValue = null   // Valor por defecto si no se pasa
                }
            )
        ) { backStackEntry ->
            val rutinaId = backStackEntry.arguments?.getString(Routes.Args.ROUTINE_ID_ARG)
            val customRutinaId = backStackEntry.arguments?.getString(Routes.Args.CUSTOM_ROUTINE_ID_ARG) // Obtener el customRoutineId

            RoutineScreen(
                navController = navController,
                rutinaId = rutinaId,
                customRutinaId = customRutinaId // Pasarlo a tu RoutineScreen
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
                navController = navController
            )
        }
        composable(Routes.ROUTINE_SUCCESS_SCREEN) {
            RoutineSuccessScreen(onFinish = {
                navController.navigate(Routes.MAIN_CONTENT) {
                    // Limpia TODO el backstack hasta el inicio del grafo actual
                    // y asegúrate de que MAIN_CONTENT sea la única instancia.
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true // Evita múltiples copias de MAIN_CONTENT
                }
            })
        }
        composable(Routes.MY_CUSTOM_ROUTINES_SCREEN) {
            // Asumiendo que has creado MyRoutinesScreen.kt como se discutió
            MyRoutinesScreen(navController = navController)
        }
        composable(
            route = Routes.ALL_EXERCISES_SCREEN, // Ahora es "all_exercises_screen/{isSelectingForRoutine}"
            arguments = listOf(
                navArgument(Routes.Args.IS_SELECTING_FOR_ROUTINE_ARG) { // <--- Usa la constante de Args
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isSelecting = backStackEntry.arguments?.getBoolean(Routes.Args.IS_SELECTING_FOR_ROUTINE_ARG) ?: false
            AllExercisesScreen(
                navController = navController,
                isSelectingForRoutine = isSelecting
            )
        }
        composable(Routes.WEEKLY_PLAN_SCREEN) {
            // Asumiendo que WeeklyPlanScreen.kt existe y está importada
            // El UserProfileViewModel se inyectará por defecto o con la factory en WeeklyPlanScreen
            WeeklyPlanScreen(navController = navController)
        }
        composable(
            route = Routes.ROUTINE_SELECTION_SCREEN_ROUTE_TEMPLATE, // Usa la plantilla de la ruta
            arguments = listOf(
                navArgument(Routes.Args.DATE_IN_MILLIS_ARG) { // Usa la constante del argumento
                    type = NavType.LongType
                    // defaultValue no es necesario aquí si la ruta siempre lo incluye
                }
            )
        ) { backStackEntry ->
            val dateInMillis = backStackEntry.arguments?.getLong(Routes.Args.DATE_IN_MILLIS_ARG)

            if (dateInMillis != null) {
                // Asumiendo que RoutineSelectionScreen.kt existe y está importada
                // El UserProfileViewModel (o el ViewModel que necesite RoutineSelectionScreen)
                // se inyectará por defecto o con la factory dentro de RoutineSelectionScreen
                RoutineSelectionScreen(
                    navController = navController,
                    dateInMillis = dateInMillis
                    // Aquí pasarías cualquier ViewModel que RoutineSelectionScreen necesite,
                    // por ejemplo, si necesita acceder al UserProfileViewModel para
                    // obtener información del usuario o actualizar el plan:
                    // userViewModel = viewModel() // O el viewModel específico
                )
            } else {
                // Manejo de error si el argumento no se pasa correctamente.
                // Esto no debería suceder si siempre navegas usando Routes.selectRoutineForDate().
                Text("Error: Faltan argumentos para la selección de rutina.")
                // Considera navController.popBackStack() para volver a la pantalla anterior.
            }
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