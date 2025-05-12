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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.navigation.BottomNavItem
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(mainNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Tips,
        BottomNavItem.Profile,
        BottomNavItem.History
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = bottomNavController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController, // Este sigue usando el controlador local para la barra inferior
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ¡CORRECCIÓN! Pasa el mainNavController a HomeScreen
            composable(BottomNavItem.Home.route) { HomeScreen(navController = mainNavController) }

            composable(BottomNavItem.Tips.route) { TipsScreen() } // TipsScreen no necesita mainNavController si no navega fuera del NavHost anidado

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        // Usa el mainNavController para ir al Login de nivel superior
                        mainNavController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onEditProfile = {
                        // Usa el mainNavController para ir a 'edit_profile' de nivel superior
                        mainNavController.navigate("edit_profile")
                    }
                )
            }

            // Si EditProfileScreen se mantiene en el NavHost principal (como parece en KalisNavGraph):
            // No definas 'edit_profile' aquí. Ya está en el NavHost principal.
            // Si EditProfileScreen estuviera en este NavHost anidado, usaría bottomNavController.

            composable(BottomNavItem.History.route) {
                // HistorialScreen podría necesitar mainNavController si navega a la pantalla de Rutina
                HistorialScreen(navController = mainNavController)
            }
        }
    }
}
