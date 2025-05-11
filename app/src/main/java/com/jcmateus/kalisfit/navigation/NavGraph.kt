package com.jcmateus.kalisfit.navigation


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.jcmateus.kalisfit.ui.screens.SplashScreen
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
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
        composable(Routes.PROFILE) {
            ProfileScreen(
                onLogout = { navController.navigate(Routes.LOGIN) },
                onEditProfile = { navController.navigate("edit_profile") }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(navController)
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.ONBOARDING_SUCCESS) {
            OnboardingSuccessScreen(
                onContinue = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBackToLogin = {
                navController.popBackStack(Routes.LOGIN, inclusive = false)
            })
        }

        composable("main") {
            KalisMainScreen()
        }

        composable(Routes.ROUTINE) {
            RoutineScreen(
                navController = navController,
                onRoutineComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ROUTINE) { inclusive = true }
                    }
                }
            )
        }
        composable("routine") {
            RoutineScreen(
                navController = navController,
                onRoutineComplete = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("routine") { inclusive = true }
                    }
                }
            )
        }
        composable("edit_profile") {
            val viewModel = remember { UserProfileViewModel() }
            val user = viewModel.user.collectAsState().value

            user?.let {
                EditProfileScreen(
                    user = it,
                    onProfileUpdated = {
                        navController.popBackStack() // volver al perfil
                        viewModel.loadUserProfile() // recargar datos
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }

    }
}
