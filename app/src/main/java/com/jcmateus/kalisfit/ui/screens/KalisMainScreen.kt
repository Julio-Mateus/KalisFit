package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.MainActivity
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.repositories.AuthRepositoryImpl
import com.jcmateus.kalisfit.data.repositories.CartRepositoryImpl
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.navigation.BottomNavItem
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.CartViewModel
import com.jcmateus.kalisfit.viewmodel.CartViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(mainNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? MainActivity

    val cartRepository =
        remember { CartRepositoryImpl(FirebaseFirestore.getInstance()) } // O como lo obtengas
    val authRepository =
        remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }    // O como lo obtengas
    val cartViewModelFactory = remember(cartRepository, authRepository) {
        CartViewModelFactory(cartRepository, authRepository)
    }
    val cartViewModel: CartViewModel = viewModel(factory = cartViewModelFactory)
    val cartUiState by cartViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = activity) {
        activity?.askNotificationPermissionInternal()
    }

    val mainNavBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentMainRoute = mainNavBackStackEntry?.destination?.route
    val currentPlaceFilterArgument = mainNavBackStackEntry?.arguments?.getString("place")

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calisthenics,
        BottomNavItem.Stoicism,
        BottomNavItem.Running,
        BottomNavItem.Store
    )

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentBottomRoute = navBackStackEntry?.destination?.route

    val topBarTitle = when (currentBottomRoute) {
        BottomNavItem.Home.route -> stringResource(R.string.title_home)
        BottomNavItem.Calisthenics.route -> stringResource(R.string.title_calisthenics)
        BottomNavItem.Stoicism.route -> stringResource(R.string.title_stoicism)
        BottomNavItem.Running.route -> stringResource(R.string.title_running)
        BottomNavItem.Store.route -> stringResource(R.string.title_store)
        else -> stringResource(R.string.app_name)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // El contenido del ModalDrawerSheet es en sí mismo un @Composable lambda
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant
                // Si surfaceVariant no te gusta, prueba con MaterialTheme.colorScheme.surface
                // o si tienes una versión de M3 más nueva (1.1+), prueba:
                // drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                // --- Encabezado del Drawer ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.height(8.dp))

                // --- Ítems Principales ---
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.drawer_profile)) },
                    label = { Text(stringResource(R.string.drawer_profile)) },
                    selected = currentMainRoute == Routes.PROFILE_SCREEN && currentPlaceFilterArgument == null,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.PROFILE_SCREEN) { launchSingleTop = true }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = stringResource(R.string.drawer_routines_explorer_all)) },
                    label = { Text(stringResource(R.string.drawer_routines_explorer_all)) },
                    selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN && currentPlaceFilterArgument == null,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) {
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = stringResource(R.string.drawer_activity_history)) },
                    label = { Text(stringResource(R.string.drawer_activity_history)) },
                    selected = currentMainRoute == Routes.ACTIVITY_HISTORY_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.ACTIVITY_HISTORY_SCREEN) {
                            mainNavController.navigate(Routes.ACTIVITY_HISTORY_SCREEN) { launchSingleTop = true }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                Spacer(Modifier.height(8.dp))
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(R.string.drawer_explore_by_place),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LugarEntrenamiento.values().forEach { lugar ->
                    val labelResId = when (lugar) {
                        LugarEntrenamiento.CASA -> R.string.place_filter_home
                        LugarEntrenamiento.GIMNASIO -> R.string.place_filter_gym
                        LugarEntrenamiento.EXTERIOR -> R.string.place_filter_outdoor
                        LugarEntrenamiento.CALISTENIA -> R.string.place_filter_calisthenics
                    }
                    val icon = when (lugar) {
                        LugarEntrenamiento.CASA -> Icons.Filled.Home
                        LugarEntrenamiento.GIMNASIO -> Icons.Filled.FitnessCenter
                        LugarEntrenamiento.EXTERIOR -> Icons.Filled.Terrain
                        LugarEntrenamiento.CALISTENIA -> Icons.Filled.SelfImprovement
                    }
                    val contentDescriptionResId = when (lugar) {
                        LugarEntrenamiento.CASA -> R.string.desc_place_filter_home
                        LugarEntrenamiento.GIMNASIO -> R.string.desc_place_filter_gym
                        LugarEntrenamiento.EXTERIOR -> R.string.desc_place_filter_outdoor
                        LugarEntrenamiento.CALISTENIA -> R.string.desc_place_filter_calisthenics
                    }

                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = stringResource(contentDescriptionResId)) },
                        label = { Text(stringResource(labelResId)) },
                        selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN && currentPlaceFilterArgument == lugar.name,
                        onClick = {
                            scope.launch { drawerState.close() }
                            mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?place=${lugar.name}") {
                                launchSingleTop = true
                                // Opcional: limpiar backstack
                                // popUpTo(mainNavController.graph.findStartDestination().id) {
                                // saveState = true
                                // }
                                // restoreState = true
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                Spacer(Modifier.weight(1f)) // Empuja los siguientes items hacia abajo
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                // --- Ítems Inferiores ---
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.desc_edit_profile)) },
                    label = { Text(stringResource(R.string.desc_edit_profile)) },
                    selected = currentMainRoute == Routes.EDIT_PROFILE_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.EDIT_PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.EDIT_PROFILE_SCREEN) { launchSingleTop = true }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.menu_settings)) },
                    label = { Text(stringResource(R.string.menu_settings)) },
                    selected = currentMainRoute == Routes.SETTINGS_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.SETTINGS_SCREEN) {
                            mainNavController.navigate(Routes.SETTINGS_SCREEN) { launchSingleTop = true }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.menu_logout)) },
                    label = { Text(stringResource(R.string.menu_logout)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut() // Considera inyectar FirebaseAuth o mover esta lógica a un ViewModel
                        mainNavController.navigate(Routes.LOGIN) {
                            popUpTo(mainNavController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    // No se necesitan colores de selección para logout si no es un estado persistente
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        // El último lambda de ModalNavigationDrawer es el contenido principal de la pantalla.
        // NO recibe PaddingValues directamente. El Scaffold interno es quien maneja eso.
    ) {
        // 'it' aquí NO son PaddingValues del ModalNavigationDrawer.
        // El contenido del ModalNavigationDrawer es el Scaffold.
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
                                contentDescription = stringResource(R.string.desc_open_drawer)
                            )
                        }
                    },
                    actions = { // <--- SLOT DE ACCIONES PARA EL TOPAPPBAR
                        // Mostrar el icono del carrito solo si estamos en la sección de la tienda
                        if (currentBottomRoute == BottomNavItem.Store.route) {
                            IconButton(onClick = {
                                // Navegar a la pantalla del carrito usando mainNavController
                                // Asegúrate de tener una ruta como Routes.CART_SCREEN
                                mainNavController.navigate(Routes.CART_SCREEN)
                            }) {
                                BadgedBox(
                                    badge = {
                                        // Usa itemCount de tu CartUiState
                                        if (cartUiState.itemCount > 0) {
                                            Badge { Text("${cartUiState.itemCount}") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingCart,
                                        contentDescription = stringResource(R.string.desc_cart) // Crea este recurso de string
                                    )
                                }
                            }
                        }
                        // Aquí puedes añadir otros IconButton si los necesitas para otras secciones
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Color para el icono del carrito
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
                            label = { Text(stringResource(item.labelResource)) }
                        )
                    }
                }
            }
        ){ contentPadding ->
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(contentPadding), // Aplicar el padding del Scaffold aquí
                // Animaciones de FADE para la navegación del BottomNav
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        mainNavController = mainNavController,
                        //bottomNavController = bottomNavController
                    )
                }
                composable(BottomNavItem.Calisthenics.route) {
                    CalisthenicsProgressionScreen(
                        mainNavController = mainNavController
                    )
                }
                composable(BottomNavItem.Stoicism.route) {
                    StoicismContentScreen(mainNavController = mainNavController)
                }
                composable(BottomNavItem.Running.route) {
                    RunningTabScreen(navController = mainNavController) // mainNavController aquí es correcto si RunningTabScreen lo usa para navegación principal
                }
                // --- NUEVA PANTALLA DE TIENDA ---
                composable(BottomNavItem.Store.route) {
                    StoreScreen(
                        onProductClick = { productId -> // <--- CAMBIO: ahora es productId
                            // Navegar al detalle del producto usando el mainNavController y productId
                            mainNavController.navigate(Routes.productDetail(productId)) // <--- CAMBIO: usa productId
                        }
                        // modifier = Modifier.fillMaxSize() // O el modifier apropiado si es necesario
                    )
                }
            }
        }
    }
}