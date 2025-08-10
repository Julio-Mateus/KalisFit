package com.jcmateus.kalisfit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.forEach
import androidx.core.graphics.values
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

    val cartRepository = remember { CartRepositoryImpl(FirebaseFirestore.getInstance()) }
    val authRepository = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }
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
    val currentLevelFilterArgument = mainNavBackStackEntry?.arguments?.getString("level") // Para filtro de nivel

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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                // Estados para controlar la expansión de los submenús
                var expandExplorarRutinas by remember { mutableStateOf(false) }
                var expandPorLugar by remember { mutableStateOf(false) }
                var expandPorNivel by remember { mutableStateOf(false) }

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
                DrawerMainItem(
                    icon = Icons.Filled.Person,
                    text = stringResource(R.string.drawer_profile),
                    selected = currentMainRoute == Routes.PROFILE_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.PROFILE_SCREEN) { launchSingleTop = true }
                        }
                    }
                )

                // --- Mis Rutinas ---
                DrawerMainItem(
                    icon = Icons.Filled.MenuBook, // Icono sugerido
                    text = stringResource(R.string.drawer_my_routines), // Crea este string
                    selected = currentMainRoute == Routes.MY_CUSTOM_ROUTINES_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.MY_CUSTOM_ROUTINES_SCREEN) {
                            launchSingleTop = true
                        }
                    }
                )

                // --- Todos los Ejercicios ---
                DrawerMainItem(
                    icon = Icons.Filled.FormatListBulleted, // Icono sugerido
                    text = stringResource(R.string.drawer_all_exercises), // Crea este string
                    selected = currentMainRoute == Routes.ALL_EXERCISES_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.ALL_EXERCISES_SCREEN) {
                            launchSingleTop = true
                        }
                    }
                )

                DrawerMainItem(
                    icon = Icons.Filled.History,
                    text = stringResource(R.string.drawer_activity_history),
                    selected = currentMainRoute == Routes.ACTIVITY_HISTORY_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.ACTIVITY_HISTORY_SCREEN) {
                            mainNavController.navigate(Routes.ACTIVITY_HISTORY_SCREEN) { launchSingleTop = true }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                // --- Sección Explorar Rutinas (Desplegable) ---
                DrawerCollapsibleSectionHeader(
                    icon = Icons.Filled.Search,
                    text = stringResource(R.string.drawer_explore_routines), // Crea este string
                    isExpanded = expandExplorarRutinas,
                    onClick = { expandExplorarRutinas = !expandExplorarRutinas }
                )

                AnimatedVisibility(
                    visible = expandExplorarRutinas,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp)) { // Indentación para subitems
                        // --- Submenú: Ver todas las rutinas (del explorador) ---
                        DrawerSubItem(
                            // Puedes usar un icono específico o ninguno para "Todas"
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            text = stringResource(R.string.drawer_routines_explorer_all),
                            selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN && currentPlaceFilterArgument == null && currentLevelFilterArgument == null,
                            onClick = {
                                scope.launch { drawerState.close() }
                                mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) {
                                    launchSingleTop = true
                                }
                            }
                        )

                        // --- Submenú: Por Lugar (Desplegable) ---
                        DrawerCollapsibleSectionHeader(
                            icon = Icons.Filled.LocationOn,
                            text = stringResource(R.string.drawer_explore_by_place),
                            isExpanded = expandPorLugar,
                            onClick = { expandPorLugar = !expandPorLugar },
                            isSubHeader = true
                        )
                        AnimatedVisibility(
                            visible = expandPorLugar,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp)) { // Mayor indentación
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
                                    DrawerSubItem(
                                        icon = icon,
                                        text = stringResource(labelResId),
                                        selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN && currentPlaceFilterArgument == lugar.name,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?place=${lugar.name}") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // --- Submenú: Por Nivel (Desplegable) ---
                        DrawerCollapsibleSectionHeader(
                            icon = Icons.Filled.School, // Opcional
                            text = stringResource(R.string.drawer_explore_by_level), // Crea este string
                            isExpanded = expandPorNivel,
                            onClick = { expandPorNivel = !expandPorNivel },
                            isSubHeader = true
                        )
                        AnimatedVisibility(
                            visible = expandPorNivel,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp)) { // Mayor indentación
                                NivelExperiencia.values().forEach { nivel -> // Asume que tienes un enum NivelDificultad
                                    // Necesitarás strings para los niveles, ej: R.string.level_beginner
                                    val label = when (nivel) {
                                        NivelExperiencia.PRINCIPIANTE -> stringResource(R.string.level_beginner)
                                        NivelExperiencia.INTERMEDIO -> stringResource(R.string.level_intermediate)
                                        NivelExperiencia.AVANZADO -> stringResource(R.string.level_advanced)
                                    }
                                    // Puedes añadir iconos si quieres
                                    DrawerSubItem(
                                        icon = Icons.Filled.BarChart, // Ejemplo
                                        text = label,
                                        selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN && currentLevelFilterArgument == nivel.name,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            // Asume que tu RoutinesExplorerScreen puede filtrar por nivel
                                            mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?level=${nivel.name}") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }


                Spacer(Modifier.weight(1f)) // Empuja los siguientes items hacia abajo
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                // --- Ítems Inferiores ---
                DrawerMainItem(
                    icon = Icons.Filled.Edit,
                    text = stringResource(R.string.desc_edit_profile),
                    selected = currentMainRoute == Routes.EDIT_PROFILE_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.EDIT_PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.EDIT_PROFILE_SCREEN) { launchSingleTop = true }
                        }
                    }
                )
                DrawerMainItem(
                    icon = Icons.Filled.Settings,
                    text = stringResource(R.string.menu_settings),
                    selected = currentMainRoute == Routes.SETTINGS_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.SETTINGS_SCREEN) {
                            mainNavController.navigate(Routes.SETTINGS_SCREEN) { launchSingleTop = true }
                        }
                    }
                )
                NavigationDrawerItem( // Logout puede seguir siendo un NavigationDrawerItem normal
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
        // ... (resto de tu KalisMainScreen: Scaffold con TopAppBar, BottomBar, NavHost para bottomNavController)
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
                                contentDescription = stringResource(R.string.desc_open_drawer)
                            )
                        }
                    },
                    actions = {
                        if (currentBottomRoute == BottomNavItem.Store.route) {
                            IconButton(onClick = {
                                mainNavController.navigate(Routes.CART_SCREEN)
                            }) {
                                BadgedBox(
                                    badge = {
                                        if (cartUiState.itemCount > 0) {
                                            Badge { Text("${cartUiState.itemCount}") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingCart,
                                        contentDescription = stringResource(R.string.desc_cart)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                modifier = Modifier.padding(contentPadding),
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        mainNavController = mainNavController,
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
                    RunningTabScreen(navController = mainNavController)
                }
                composable(BottomNavItem.Store.route) {
                    StoreScreen(
                        onProductClick = { productId ->
                            mainNavController.navigate(Routes.productDetail(productId))
                        }
                    )
                }
            }
        }
    }
}

// --- Componentes Auxiliares para el Drawer ---

/**
 * Un Composable para los ítems principales del NavigationDrawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMainItem(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String? = text // Por defecto usa el texto como descripción
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = contentDescription) },
        label = { Text(text) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

/**
 * Un Composable para el encabezado de una sección desplegable en el Drawer.
 */
@Composable
fun DrawerCollapsibleSectionHeader(
    text: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null, // Icono opcional para el encabezado de sección principal
    isSubHeader: Boolean = false // Para aplicar un padding diferente si es un sub-encabezado
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (isSubHeader) 32.dp else 16.dp, // Mayor padding horizontal para sub-encabezados
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(if (isSubHeader) 12.dp else 16.dp)) // Ajustar espaciado del icono
            }
            Text(
                text,
                style = if (isSubHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (isExpanded) "Contraer" else "Expandir",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Un Composable para los ítems dentro de una sección desplegable (sub-ítems).
 * Usamos el padding de NavigationDrawerItemDefaults, pero podrías ajustarlo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerSubItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null, // Icono opcional para sub-ítems
    contentDescription: String? = text
) {
    NavigationDrawerItem(
        icon = {
            if (icon != null) Icon(icon, contentDescription = contentDescription)
            // else Spacer(Modifier.width(24.dp)) // Para alinear texto si otros tienen icono
        },
        label = { Text(text) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer, // Color diferente para sub-selección
            selectedIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
            // Colores no seleccionados (opcional, por defecto son adecuados)
            // unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            // unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        // Aplicar padding para que se vea "anidado"
        modifier = Modifier.padding(
            start = (NavigationDrawerItemDefaults.ItemPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp), // Aumentar padding izquierdo
            top = NavigationDrawerItemDefaults.ItemPadding.calculateTopPadding(),
            end = NavigationDrawerItemDefaults.ItemPadding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = NavigationDrawerItemDefaults.ItemPadding.calculateBottomPadding()
        )
        // shape = MaterialTheme.shapes.small // Opcional: forma diferente para sub-items
    )
}
