package com.jcmateus.kalisfit.ui.screens

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.material3.DrawerState
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.MainActivity
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.repositories.AuthRepositoryImpl
import com.jcmateus.kalisfit.data.repositories.CartRepositoryImpl
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.WaveSide
import com.jcmateus.kalisfit.model.WavyBottomHeaderShape
import com.jcmateus.kalisfit.navigation.BottomNavItem
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme
import com.jcmateus.kalisfit.viewmodel.CartViewModel
import com.jcmateus.kalisfit.viewmodel.CartViewModelFactory
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.jcmateus.kalisfit.model.WavyDrawerShape
import com.jcmateus.kalisfit.model.WavyNavigationBarShape
import com.jcmateus.kalisfit.model.WavyTopAppBarShape

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(mainNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity // Cambio a Activity genérica para más flexibilidad
    // ViewModels y Repositories (sin cambios)
    val cartRepository = remember { CartRepositoryImpl(FirebaseFirestore.getInstance()) }
    val authRepository = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }
    val cartViewModelFactory = remember(cartRepository, authRepository) {
        CartViewModelFactory(cartRepository, authRepository)
    }
    val cartViewModel: CartViewModel = viewModel(factory = cartViewModelFactory)
    val cartUiState by cartViewModel.uiState.collectAsState()
    val userProfileViewModel: UserProfileViewModel = viewModel()
    // LaunchedEffect para permisos (sin cambios, pero asegúrate de que askNotificationPermissionInternal exista en tu Activity)
    LaunchedEffect(key1 = activity) {
        if (activity is MainActivity) { // Chequeo más seguro
            activity.askNotificationPermissionInternal()
        }
    }
    val mainNavBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentMainRoute = mainNavBackStackEntry?.destination?.route
    val currentPlaceFilterArgument =
        mainNavBackStackEntry?.arguments?.getString(Routes.Args.PLACE_ARG)
    val currentLevelFilterArgument =
        mainNavBackStackEntry?.arguments?.getString(Routes.Args.LEVEL_ARG)
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
    // Asegúrate de que tu tema KalisFitTheme maneje los colores oscuros/claros apropiadamente.
    KalisFitTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen, // Gestos solo habilitados cuando está abierto o para abrir
            drawerContent = {
                KalisDrawerContent( // Contenido del Drawer refactorizado
                    mainNavController = mainNavController,
                    drawerState = drawerState,
                    scope = scope,
                    currentMainRoute = currentMainRoute,
                    currentPlaceFilterArgument = currentPlaceFilterArgument,
                    currentLevelFilterArgument = currentLevelFilterArgument,
                    userProfileViewModel = userProfileViewModel
                )
            }
        ) {
            Scaffold(
                // Configuración para Edge-to-Edge:
                // Permite que el contenido (incluyendo la BottomBar) se dibuje detrás de las barras del sistema.
                // Scaffold maneja los insets para TopAppBar y BottomAppBar si están presentes.
                // Para que la BottomBar extienda su color, su propio windowInsets debe ser (0,0,0,0)
                // y los items dentro de ella deben aplicar el navigationBarsPadding.
                contentWindowInsets = WindowInsets(
                    0,
                    0,
                    0,
                    0
                ), // Permite que el contenido principal se extienda si es necesario
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
                        ),
                        modifier = Modifier
                            .fillMaxWidth() // Es bueno tenerlo por si acaso
                            .graphicsLayer {
                                shape = WavyTopAppBarShape(
                                    period = 1.2f,
                                    amplitudeFactor = 0.06f
                                ) // Ajusta estos valores
                                clip = true
                            }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth(),
                        /*
                            .graphicsLayer { // <<< SÍ ESTÁ AQUÍ
                                shape = WavyNavigationBarShape(
                                    period = 1.2f,
                                    amplitudeFactor = 0.08f
                                ) // Ajusta estos valores
                                clip = true
                            },
                         */
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), // Un poco de elevación
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets(
                            0,
                            0,
                            0,
                            0
                        ) // Importante para que el color de fondo se extienda
                    ) {
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
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = stringResource(item.labelResource)
                                    )
                                },
                                label = { Text(stringResource(item.labelResource)) },
                                // Este modifier asegura que los items no queden debajo de la barra de gestos/navegación del sistema
                                modifier = Modifier.navigationBarsPadding(),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { contentPadding -> // contentPadding del Scaffold
                NavHost(
                    navController = bottomNavController,
                    startDestination = BottomNavItem.Home.route,
                    modifier = Modifier
                        .padding(contentPadding) // Aplicar el padding del Scaffold (principalmente para TopAppBar)
                        // No es necesario navigationBarsPadding aquí si la BottomBar y sus items ya lo manejan
                        // y el contenido principal no debe dibujarse debajo de una BottomBar transparente.
                        .fillMaxSize(),
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    composable(BottomNavItem.Home.route) {
                        HomeScreen(mainNavController = mainNavController)
                    }
                    composable(BottomNavItem.Calisthenics.route) {
                        CalisthenicsProgressionScreen(mainNavController = mainNavController)
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
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalisDrawerContent(
    mainNavController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    currentMainRoute: String?,
    currentPlaceFilterArgument: String?,
    currentLevelFilterArgument: String?,
    userProfileViewModel: UserProfileViewModel
) {
    // Observa el estado del perfil del usuario desde el ViewModel
    val userProfileState by userProfileViewModel.user.collectAsState() // Esto te da UserProfile?
    // Extrae los datos que necesitas, proveyendo valores por defecto o manejando el caso nulo
    val userImageUrl = userProfileState?.fotoUrl ?: ""
    val userName = userProfileState?.nombre?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.drawer_default_username) // "Nombre de Usuario" o similar
    val userLevelOrInfo = userProfileState?.nivel?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.drawer_default_level) // "Nivel" o información por defecto

    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(max = 300.dp) // Limitar el ancho máximo, se ajustará si es menor
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface, // Un color base limpio
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Estados para controlar la expansión de los submenús
        var expandExplorarRutinas by remember { mutableStateOf(false) }
        var expandPorLugar by remember { mutableStateOf(false) }
        var expandPorNivel by remember { mutableStateOf(false) }
        // --- Encabezado del Drawer ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // Usaremos una nueva shape para el borde inferior
                    shape = WavyBottomHeaderShape(
                        amplitude = 10f,     // Amplitud de la onda (altura)
                        periodFactor = 0.3f  // Factor para la longitud de onda (más pequeño = más ondas a lo ancho)
                    )
                    clip = true
                }
                .background(MaterialTheme.colorScheme.primaryContainer) // Color primario para destacar
                .padding(
                    top = (WindowInsets.statusBars.asPaddingValues()
                        .calculateTopPadding() + 16.dp), // Padding para la barra de estado + espacio
                    bottom = 24.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
            verticalAlignment = Alignment.Top, // Alinear al tope para que los textos queden bien bajo sus iconos
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Columna Izquierda: Logo y Nombre de la App ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // Centrar el logo y el texto debajo
                modifier = Modifier.weight(1f) // Darle peso para que ocupe espacio
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_logo2),
                    contentDescription = null, // El texto de abajo sirve de descripción
                    modifier = Modifier
                        .size(64.dp) // Un poco más grande si quieres
                    // .clip(CircleShape) // Quita el clip si tu logo no es circular o no se ve bien
                    ,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium, // Ajusta el estilo como prefieras
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.width(16.dp)) // Espacio entre las dos columnas principales
            // --- Columna Derecha: Foto de Perfil, Nombre y Nivel del Usuario ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // Centrar la foto y los textos debajo
                modifier = Modifier
                    .weight(1f) // Darle peso
                    .padding(end = 10.dp) // Espacio a la derecha
            ) {
                if (userImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userImageUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_default_placeholder) // Asegúrate que existe
                            .error(R.drawable.ic_error_placeholder)       // Asegúrate que existe
                            .build(),
                        contentDescription = stringResource(R.string.desc_profile_picture),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp) // Un poco más grande
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.desc_profile_picture),
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    userName,
                    style = MaterialTheme.typography.titleSmall, // Ajusta el estilo
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (userLevelOrInfo.isNotBlank() && userLevelOrInfo != stringResource(R.string.drawer_default_level)) {
                    // Solo mostrar el nivel si es significativo y no es el string por defecto
                    Text(
                        userLevelOrInfo,
                        style = MaterialTheme.typography.bodySmall, // Ajusta el estilo
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Para que la LazyColumn tome el espacio restante antes de los items fijos de abajo
            contentPadding = PaddingValues(vertical = 8.dp) // Padding vertical para los items
        ) {
            // --- Ítems Principales ---
            item {
                DrawerMainItem(
                    icon = Icons.Filled.Person,
                    text = stringResource(R.string.drawer_profile),
                    selected = currentMainRoute == Routes.PROFILE_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.PROFILE_SCREEN) {
                            mainNavController.navigate(Routes.PROFILE_SCREEN) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            item {
                DrawerMainItem(
                    icon = Icons.Filled.MenuBook,
                    text = stringResource(R.string.drawer_my_routines),
                    selected = currentMainRoute == Routes.MY_CUSTOM_ROUTINES_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.MY_CUSTOM_ROUTINES_SCREEN) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            item {
                DrawerMainItem(
                    icon = Icons.Filled.FormatListBulleted,
                    text = stringResource(R.string.drawer_all_exercises),
                    selected = currentMainRoute?.startsWith(Routes.ALL_EXERCISES_SCREEN_BASE) == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(Routes.allExercises(isSelectingForRoutine = false)) {
                            launchSingleTop = true
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
            item {
                DrawerMainItem(
                    icon = Icons.Filled.History,
                    text = stringResource(R.string.drawer_activity_history),
                    selected = currentMainRoute == Routes.ACTIVITY_HISTORY_SCREEN,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentMainRoute != Routes.ACTIVITY_HISTORY_SCREEN) {
                            mainNavController.navigate(Routes.ACTIVITY_HISTORY_SCREEN) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            item { ListDivider1() } // Divisor personalizado
            // --- Sección Explorar Rutinas (Desplegable) ---
            item {
                DrawerCollapsibleSectionHeader(
                    icon = Icons.Filled.Search,
                    text = stringResource(R.string.drawer_explore_routines),
                    isExpanded = expandExplorarRutinas,
                    onClick = { expandExplorarRutinas = !expandExplorarRutinas }
                )
            }
            item { // Agrupar AnimatedVisibility en un solo item para LazyColumn
                AnimatedVisibility(
                    visible = expandExplorarRutinas,
                    enter = fadeIn() + expandVertically(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut() + shrinkVertically(animationSpec = tween(durationMillis = 300))
                ) {
                    Column(modifier = Modifier.padding(start = 0.dp)) { // Padding ya manejado en DrawerSubItem y Header
                        DrawerSubItem(
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
                        DrawerCollapsibleSectionHeader(
                            icon = Icons.Filled.LocationOn, // Icono para la subsección
                            text = stringResource(R.string.drawer_explore_by_place),
                            isExpanded = expandPorLugar,
                            onClick = { expandPorLugar = !expandPorLugar },
                            isSubHeader = true,
                            indentLevel = 1 // Nivel de indentación para el icono/texto
                        )
                        AnimatedVisibility(
                            visible = expandPorLugar,
                            enter = fadeIn() + expandVertically(animationSpec = tween(durationMillis = 200)),
                            exit = fadeOut() + shrinkVertically(animationSpec = tween(durationMillis = 200))
                        ) {
                            Column(modifier = Modifier.padding(start = 0.dp)) {
                                LugarEntrenamiento.entries.forEach { lugar ->
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
                                            mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?${Routes.Args.PLACE_ARG}=${lugar.name}") {
                                                launchSingleTop = true
                                            }
                                        },
                                        indentLevel = 1 // Nivel de indentación
                                    )
                                }
                            }
                        }
                        DrawerCollapsibleSectionHeader(
                            icon = Icons.Filled.School,
                            text = stringResource(R.string.drawer_explore_by_level),
                            isExpanded = expandPorNivel,
                            onClick = { expandPorNivel = !expandPorLugar },
                            isSubHeader = true,
                            indentLevel = 1
                        )
                        AnimatedVisibility(
                            visible = expandPorNivel,
                            enter = fadeIn() + expandVertically(animationSpec = tween(durationMillis = 200)),
                            exit = fadeOut() + shrinkVertically(animationSpec = tween(durationMillis = 200))
                        ) {
                            Column(modifier = Modifier.padding(start = 0.dp)) {
                                NivelExperiencia.entries.forEach { nivel ->
                                    val label = when (nivel) {
                                        NivelExperiencia.PRINCIPIANTE -> stringResource(R.string.level_beginner)
                                        NivelExperiencia.INTERMEDIO -> stringResource(R.string.level_intermediate)
                                        NivelExperiencia.AVANZADO -> stringResource(R.string.level_advanced)
                                    }
                                    DrawerSubItem(
                                        icon = Icons.Filled.BarChart,
                                        text = label,
                                        selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN && currentLevelFilterArgument == nivel.name,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?${Routes.Args.LEVEL_ARG}=${nivel.name}") {
                                                launchSingleTop = true
                                            }
                                        },
                                        indentLevel = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } // Fin de LazyColumn
        // --- Ítems Inferiores (fijos al final del DrawerSheet) ---
        ListDivider()
        /*
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
         */
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
        // Logout con un estilo ligeramente diferente o un divisor antes
        // ListDivider() // Opcional si quieres separar más el logout
        NavigationDrawerItem(
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.menu_logout)
                )
            },
            label = { Text(stringResource(R.string.menu_logout)) },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                FirebaseAuth.getInstance().signOut()
                mainNavController.navigate(Routes.LOGIN) {
                    popUpTo(mainNavController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
            shape = RoundedCornerShape(
                topStart = 0.dp,
                bottomStart = 0.dp,
                topEnd = 24.dp,
                bottomEnd = 24.dp
            ), // Forma asimétrica
            colors = NavigationDrawerItemDefaults.colors(
                // Opcional: colores que indiquen "salida"
                unselectedTextColor = MaterialTheme.colorScheme.error,
                unselectedIconColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 8.dp
                ) // Padding para la barra de navegación del sistema + espacio
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMainItem(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String? = text,
    shape: Shape = RoundedCornerShape(
        topEnd = 24.dp,
        bottomEnd = 24.dp
    ) // Forma de píldora hacia la derecha
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = contentDescription) },
        label = {
            Text(
                text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedContainerColor = Color.Transparent, // Fondo transparente cuando no está seleccionado
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp) // Padding entre items y bordes del drawer
    )
}
@Composable
fun DrawerCollapsibleSectionHeader(
    text: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    isSubHeader: Boolean = false,
    indentLevel: Int = 0 // 0 para principal, 1 para sub-sección, etc.
) {
    val startPadding = 16.dp + (indentLevel * 20.dp) // Base + indentación adicional
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = startPadding,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
            .padding(start = if (icon == null && isSubHeader) 24.dp else 0.dp), // Padding extra si no hay icono en subheader
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
            }
            Text(
                text,
                style = if (isSubHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = if (isSubHeader) FontWeight.Normal else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (isExpanded) stringResource(R.string.desc_collapse_section) else stringResource(
                R.string.desc_expand_section
            ),
            tint = MaterialTheme.colorScheme.primary // Icono de expansión más destacado
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerSubItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    contentDescription: String? = text,
    indentLevel: Int = 0 // Nivel de indentación para el icono/texto
) {
    // Calcula el padding inicial basado en el nivel de indentación.
    // NavigationDrawerItemDefaults.ItemPadding ya tiene un padding horizontal (start/end).
    // Queremos añadir a ese padding inicial.
    val baseStartPadding =
        NavigationDrawerItemDefaults.ItemPadding.calculateStartPadding(LayoutDirection.Ltr)
    val totalStartPadding =
        baseStartPadding + ((indentLevel + 1) * 20.dp) // +1 porque ya es subitem

    NavigationDrawerItem(
        icon = {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp)
                ) // Iconos de sub-items un poco más pequeños
            } else {
                // Spacer para alinear texto si otros sub-items tienen icono y este no
                Spacer(Modifier.width(20.dp))
            }
        },
        label = {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), // Más sutil
            selectedIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp), // Bordes redondeados sutiles
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                // Solo necesitamos ajustar el start padding para la indentación,
                // el resto de paddings (top, bottom, end) pueden venir de ItemPadding
                // o ser ajustados específicamente.
                start = totalStartPadding - baseStartPadding, // Ajuste para que el padding total sea el deseado
                top = 2.dp,
                bottom = 2.dp,
                end = 12.dp // Padding derecho para el subitem
            )
            .padding(horizontal = 12.dp) // Padding general del contenedor del sub-item
    )
}
@Composable
fun ListDivider1() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        thickness = 0.5.dp, // Divisor más sutil
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}
