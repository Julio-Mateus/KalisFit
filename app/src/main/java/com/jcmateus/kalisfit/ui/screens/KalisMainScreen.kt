package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Necesario para ModalBottomSheet
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(mainNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val mainNavBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentMainRoute = mainNavBackStackEntry?.destination?.route

    // ACTUALIZADA la lista de bottomNavItems
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Routines,
        BottomNavItem.Running, // Nuevo ítem
        BottomNavItem.History
    )

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentBottomRoute = navBackStackEntry?.destination?.route

    // Título para la TopAppBar basado en la ruta actual del BottomNav
    val topBarTitle = when (currentBottomRoute) {
        BottomNavItem.Home.route -> stringResource(R.string.title_home) // Asume que tienes R.string.title_home
        BottomNavItem.Routines.route -> stringResource(R.string.title_routines_explorer) // Asume R.string.title_routines_explorer
        BottomNavItem.Running.route -> stringResource(R.string.title_running) // NECESITARÁS AÑADIR R.string.title_running
        BottomNavItem.History.route -> stringResource(R.string.title_history) // Asume R.string.title_history
        else -> stringResource(R.string.app_name) // Título por defecto
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.app_name), // Título del Drawer
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                // Ítem de Perfil en el Drawer
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.title_profile)) }, // NECESITARÁS R.string.title_profile
                    label = { Text(stringResource(R.string.title_profile)) },
                    selected = false, // Puedes gestionar la selección si es necesario
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.PROFILE) // Asegúrate que Routes.PROFILE está definido
                    }
                )

                // Nuevas Secciones en el Drawer
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.SelfImprovement, contentDescription = stringResource(R.string.drawer_stoicism)) },
                    label = { Text(stringResource(R.string.drawer_stoicism)) },
                    selected = currentMainRoute == Routes.STOICISM_CONTENT, // Para la selección
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.STOICISM_CONTENT) {
                            mainNavController.navigate(Routes.STOICISM_CONTENT)
                        }
                    }
                )
                // Aquí podrías añadir "Running Dashboard" si quieres una pantalla de running más completa accesible desde el drawer
                // además de la pestaña de running.

                // Podrías añadir aquí las categorías de exploración (Gimnasio, Calistenia)
                Text(
                    "Explorar por Tipo", // Placeholder, usa stringResource
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Gimnasio") }, // Placeholder, usa stringResource
                    selected = false,
                    onClick = { /* TODO: Navegar a rutinas filtradas por Gimnasio */ }
                )
                NavigationDrawerItem(
                    label = { Text("Calistenia") }, // Placeholder, usa stringResource
                    selected = false,
                    onClick = { /* TODO: Navegar a rutinas filtradas por Calistenia */ }
                )

                Spacer(Modifier.weight(1f)) // Empuja los items de abajo hacia el final
                Divider()
                Spacer(Modifier.height(8.dp))

                // Opciones que estaban en el ModalBottomSheet ahora en el Drawer
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.desc_edit_profile)) },
                    label = { Text(stringResource(R.string.desc_edit_profile)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.EDIT_PROFILE)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.menu_settings)) },
                    label = { Text(stringResource(R.string.menu_settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.SETTINGS)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.menu_logout)) },
                    label = { Text(stringResource(R.string.menu_logout)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut()
                        mainNavController.navigate(Routes.LOGIN) {
                            popUpTo(mainNavController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.desc_open_drawer) // NECESITARÁS R.string.desc_open_drawer
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors( // Opcional: para darle estilo
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentBottomRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentBottomRoute != item.route) {
                                    bottomNavController.navigate(item.route) {
                                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
            // ACTUALIZADO el NavHost interno
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
                // NUEVO composable para la pestaña Running
                composable(BottomNavItem.Running.route) {
                    // Aquí va tu pantalla para la pestaña de Running.
                    // Por ahora, un placeholder:
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pantalla de Running (Pestaña)")
                    }
                    // Cuando tengas la pantalla:
                    // RunningTabScreen(navController = mainNavController)
                }
                // composable para ProfileScreen ya no se accede desde el BottomNav
                // composable(BottomNavItem.Profile.route) { ... }
                composable(BottomNavItem.History.route) {
                    HistorialScreen(navController = mainNavController)
                }
            }
        }
    }

    // --- ModalBottomSheet para "Más Opciones" HA SIDO ELIMINADO ---
    // if (showMoreOptionsSheet) { ... }
}