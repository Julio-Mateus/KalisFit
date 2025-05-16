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
fun KalisMainScreen(mainNavController: NavHostController) { // Este es el navController de KalisNavGraph
    val bottomNavController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Routines,
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
            navController = bottomNavController, // Este NavHost usa el bottomNavController
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                // PASA AMBOS NavControllers a HomeScreen
                HomeScreen(
                    mainNavController = mainNavController, // Para navegación de nivel superior
                    bottomNavController = bottomNavController  // Para cambiar pestañas del BottomNav
                )
            }

            composable(BottomNavItem.Routines.route) {
                RoutineExplorerScreen(navController = mainNavController) // mainNavController es correcto si RoutineExplorerScreen navega a "${Routes.ROUTINE}/{rutinaId}"
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        mainNavController.navigate(Routes.LOGIN) { // Usa mainNavController
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onEditProfile = {
                        mainNavController.navigate("edit_profile") // Usa mainNavController (esta ruta SÍ existe en KalisNavGraph)
                    }
                )
            }

            composable(BottomNavItem.History.route) {
                HistorialScreen(navController = mainNavController) // mainNavController es correcto si HistorialScreen navega a "${Routes.ROUTINE}/{rutinaId}"
            }
        }
    }
}
