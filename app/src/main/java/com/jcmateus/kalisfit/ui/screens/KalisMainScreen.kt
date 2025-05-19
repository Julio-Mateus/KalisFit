package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.navigation.BottomNavItem
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class) // Necesario para ModalBottomSheet
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(mainNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    var showMoreOptionsSheet by remember { mutableStateOf(false) } // Para la Opción 1 (BottomSheet)

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Routines,
        BottomNavItem.Profile,
        BottomNavItem.History,
        BottomNavItem.MoreOptions // Añadido para la Opción 1
    )

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentBottomRoute = navBackStackEntry?.destination?.route

    // El topBarTitle ya no es necesario si no hay TopAppBar
    // val topBarTitle = when (currentBottomRoute) { ... }

    Scaffold(
        // topBar = { ... } // TopAppBar eliminada
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    // El ítem "Más" no debería estar "seleccionado" como una pantalla
                    val selected = if (item is BottomNavItem.MoreOptions) false else currentBottomRoute == item.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (item is BottomNavItem.MoreOptions) {
                                showMoreOptionsSheet = true // Abrir el BottomSheet
                            } else {
                                if (currentBottomRoute != item.route) {
                                    bottomNavController.navigate(item.route) {
                                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
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
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    mainNavController = mainNavController,
                    bottomNavController = bottomNavController
                )
            }
            composable(BottomNavItem.Routines.route) {
                RoutineExplorerScreen(navController = mainNavController)
            }
            composable(BottomNavItem.Profile.route) {
                // Si eliges la Opción 3 (poner Settings/Logout en ProfileScreen),
                // necesitarás pasarle el mainNavController:
                ProfileScreen()
            }
            composable(BottomNavItem.History.route) {
                HistorialScreen(navController = mainNavController)
            }
            // No se necesita un composable() para BottomNavItem.MoreOptions.route
            // ya que solo dispara el BottomSheet.
        }
    }

    // --- ModalBottomSheet para "Más Opciones" (Opción 1) ---
    if (showMoreOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreOptionsSheet = false },
            // sheetState = rememberModalBottomSheetState(), // Opcional para más control
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Puedes añadir un título si quieres
                Text(
                    text = stringResource(R.string.title_more_options), // ej: "Más Opciones"
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.desc_edit_profile)) }, // ej: "Editar Perfil"
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        mainNavController.navigate(Routes.EDIT_PROFILE)
                        showMoreOptionsSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_settings)) }, // ej: "Configuración"
                    leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    modifier = Modifier.clickable {
                        mainNavController.navigate(Routes.SETTINGS)
                        showMoreOptionsSheet = false
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_logout)) }, // ej: "Cerrar Sesión"
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    modifier = Modifier.clickable {
                        FirebaseAuth.getInstance().signOut()
                        mainNavController.navigate(Routes.LOGIN) {
                            popUpTo(mainNavController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                        showMoreOptionsSheet = false
                    }
                )
                Spacer(modifier = Modifier.height(16.dp)) // Espacio para evitar que el contenido se pegue al borde inferior del sheet
            }
        }
    }
}