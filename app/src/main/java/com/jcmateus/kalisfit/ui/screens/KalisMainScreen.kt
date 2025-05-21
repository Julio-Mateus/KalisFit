package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
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

    // Para determinar la ruta actual del mainNavController (usado para seleccionar ítems del Drawer)
    val mainNavBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentMainRoute = mainNavBackStackEntry?.destination?.route

    // --- Lista de ítems para el BottomNavigationBar ---
    // Utiliza los objetos definidos en BottomNavItem.kt
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calisthenics, // NUEVO
        BottomNavItem.Stoicism,     // NUEVO
        BottomNavItem.Running
    )

    // Para determinar la ruta actual del bottomNavController (usado para títulos y selección en BottomNav)
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentBottomRoute = navBackStackEntry?.destination?.route

    // --- Título para la TopAppBar basado en la ruta actual del BottomNav ---
    val topBarTitle = when (currentBottomRoute) {
        BottomNavItem.Home.route -> stringResource(R.string.title_home)
        BottomNavItem.Calisthenics.route -> stringResource(R.string.title_calisthenics) // NECESITARÁS R.string.title_calisthenics
        BottomNavItem.Stoicism.route -> stringResource(R.string.title_stoicism)         // NECESITARÁS R.string.title_stoicism
        BottomNavItem.Running.route -> stringResource(R.string.title_running)
        else -> stringResource(R.string.app_name) // Título por defecto
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Encabezado del Drawer
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                // --- Ítems del Navigation Drawer ---

                // Perfil
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.drawer_profile)) }, // NECESITARÁS R.string.drawer_profile
                    label = { Text(stringResource(R.string.drawer_profile)) },
                    selected = currentMainRoute == Routes.PROFILE_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.PROFILE_SCREEN)
                        }
                    }
                )

                // Explorar Rutinas (antes una pestaña)
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = stringResource(R.string.drawer_routines_explorer)) }, // NECESITARÁS R.string.drawer_routines_explorer
                    label = { Text(stringResource(R.string.drawer_routines_explorer)) },
                    selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.ROUTINES_EXPLORER_SCREEN) {
                            mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN)
                        }
                    }
                )

                // Historial de Actividad (antes una pestaña)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = stringResource(R.string.drawer_activity_history)) }, // NECESITARÁS R.string.drawer_activity_history
                    label = { Text(stringResource(R.string.drawer_activity_history)) },
                    selected = currentMainRoute == Routes.ACTIVITY_HISTORY_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.ACTIVITY_HISTORY_SCREEN) {
                            mainNavController.navigate(Routes.ACTIVITY_HISTORY_SCREEN)
                        }
                    }
                )

                // El ítem de "Estoicismo" se elimina del Drawer si ahora es una pestaña principal
                // a menos que quieras un enlace a una sección introductoria diferente aquí.

                // Spacer(Modifier.height(8.dp))
                // Divider() // Opcional: separador antes de filtros
                // Spacer(Modifier.height(8.dp))

                // Filtros dentro de "Explorar Rutinas" (o secciones dedicadas)
                Text(
                    stringResource(R.string.drawer_explore_by_type), // NECESITARÁS R.string.drawer_explore_by_type
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = stringResource(R.string.drawer_gym_routines)) }, // Opcional, o sin icono
                    label = { Text(stringResource(R.string.drawer_gym_routines)) }, // NECESITARÁS R.string.drawer_gym_routines
                    selected = false, // La selección sería más compleja si filtra la pantalla ROUTINES_EXPLORER_SCREEN
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Navegar a ROUTINES_EXPLORER_SCREEN con un argumento para filtrar por "Gimnasio"
                        // mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?category=gym")
                        // O si tienes una ruta dedicada:
                        // mainNavController.navigate(Routes.GYM_ROUTINES_FILTER_SCREEN)
                    }
                )
                // Aquí podrías añadir un filtro para "Rutinas de Calistenia" si es diferente de la pestaña principal de progresiones
                // NavigationDrawerItem(
                // icon = { Icon(Icons.Filled.SelfImprovement, contentDescription = "Rutinas de Calistenia") }, // Placeholder
                // label = { Text("Rutinas de Calistenia") }, // Placeholder
                // selected = false,
                // onClick = {
                // scope.launch { drawerState.close() }
                // TODO: Navegar a ROUTINES_EXPLORER_SCREEN con filtro "calistenia" o a una ruta dedicada
                // mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?category=calisthenics")
                // }
                // )

                // Empuja los ítems de abajo hacia el final
                Spacer(Modifier.weight(1f))
                Divider()
                Spacer(Modifier.height(8.dp))

                // Editar Perfil
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.desc_edit_profile)) },
                    label = { Text(stringResource(R.string.desc_edit_profile)) },
                    selected = currentMainRoute == Routes.EDIT_PROFILE_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.EDIT_PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.EDIT_PROFILE_SCREEN)
                        }
                    }
                )
                // Configuración
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.menu_settings)) },
                    label = { Text(stringResource(R.string.menu_settings)) },
                    selected = currentMainRoute == Routes.SETTINGS_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.SETTINGS_SCREEN) {
                            mainNavController.navigate(Routes.SETTINGS_SCREEN)
                        }
                    }
                )
                // Cerrar Sesión
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.menu_logout)) },
                    label = { Text(stringResource(R.string.menu_logout)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut()
                        mainNavController.navigate(Routes.LOGIN) { // Asegúrate que Routes.LOGIN es correcto
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
                    colors = TopAppBarDefaults.topAppBarColors(
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
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelResource)) },
                            label = { Text(stringResource(item.labelResource)) } // Usando labelResource
                        )
                    }
                }
            }
        ) { innerPadding ->
            // --- NavHost INTERNO para las pestañas del BottomNavigationBar ---
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Home.route, // Ruta de inicio del BottomNav
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route) { // Usa .route de BottomNavItem
                    HomeScreen(
                        mainNavController = mainNavController,
                        bottomNavController = bottomNavController // Pasa si es necesario para sub-navegación dentro de Home
                    )
                }
                // NUEVA PESTAÑA: CALISTENIA
                composable(BottomNavItem.Calisthenics.route) { // Usa .route
                    // Aquí va tu pantalla de progresiones de Calistenia
                    CalisthenicsProgressionScreen(mainNavController = mainNavController)
                }
                // NUEVA PESTAÑA: ESTOICISMO
                composable(BottomNavItem.Stoicism.route) { // Usa .route
                    // Aquí va tu pantalla de contenido de Estoicismo
                    StoicismContentScreen(mainNavController = mainNavController) // Crea esta pantalla
                }
                // PESTAÑA: RUNNING
                composable(BottomNavItem.Running.route) { // Usa .route
                    RunningTabScreen(navController = mainNavController) // Asumo que mainNavController es el correcto aquí
                }

                // Las rutas de Routines y History ya NO se definen aquí,
                // se acceden desde el Drawer y se gestionan por el mainNavController.
            }
        }
    }
}