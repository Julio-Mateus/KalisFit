package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.navigation.BottomNavItem
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(navController: NavHostController = rememberNavController()) {
    // 🧭 Navegación inferior
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Tips,
        BottomNavItem.Profile,
        BottomNavItem.History
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination == item.route,
                        onClick = { navController.navigate(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 🏠 Pantalla de inicio
            composable(BottomNavItem.Home.route) { HomeScreen(navController) }
            // 🎯 Tips generales
            composable(BottomNavItem.Tips.route) { TipsScreen() }
            // 👤 Perfil del usuario
            composable(BottomNavItem.Profile.route) { ProfileScreen(onLogout = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }, onEditProfile = { navController.navigate("edit_profile") }) }
            composable("edit_profile") {
                val viewModel = remember { UserProfileViewModel() }
                val userState = viewModel.user.collectAsState()
                val user = userState.value

                LaunchedEffect(Unit) {
                    viewModel.loadUserProfile()
                }

                if (user != null) {
                    EditProfileScreen(
                        user = user,
                        onProfileUpdated = {
                            navController.popBackStack()
                            viewModel.loadUserProfile()
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    // Pantalla de carga mientras el perfil se recupera
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            // 🕓 Historial de rutinas
            composable(BottomNavItem.History.route) {
                HistorialScreen()
            }
            // 💪 Rutinas desde el Home
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
            composable("routine_success") {
                RoutineSuccessScreen(onFinish = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("routine_success") { inclusive = true }
                    }
                })
            }
        }
    }
}
