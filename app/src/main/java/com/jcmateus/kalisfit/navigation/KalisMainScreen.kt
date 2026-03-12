package com.jcmateus.kalisfit.navigation

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.MainActivity
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.repositories.AuthRepositoryImpl
import com.jcmateus.kalisfit.data.repositories.CartRepositoryImpl
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.ui.screens.*
import com.jcmateus.kalisfit.ui.screens.calistenia.CalisthenicsProgressionScreen
import com.jcmateus.kalisfit.ui.screens.running.RunningTabScreen
import com.jcmateus.kalisfit.ui.screens.stoicism.StoicismContentScreen
import com.jcmateus.kalisfit.ui.screens.store.StoreScreen
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme
import com.jcmateus.kalisfit.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun KalisMainScreen(mainNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    
    val cartRepository = remember { CartRepositoryImpl(FirebaseFirestore.getInstance()) }
    val authRepository = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }
    val cartViewModel: CartViewModel = viewModel(factory = CartViewModelFactory(cartRepository, authRepository))
    val cartUiState by cartViewModel.uiState.collectAsState()
    val userProfileViewModel: UserProfileViewModel = viewModel()

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

    KalisFitTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                KalisDrawerContent(
                    mainNavController = mainNavController,
                    drawerState = drawerState,
                    scope = scope,
                    userProfileViewModel = userProfileViewModel
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(topBarTitle, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, null)
                            }
                        },
                        actions = {
                            if (currentBottomRoute == BottomNavItem.Store.route) {
                                IconButton(onClick = { mainNavController.navigate(Routes.CART_SCREEN) }) {
                                    BadgedBox(badge = { if (cartUiState.itemCount > 0) Badge { Text("${cartUiState.itemCount}") } }) {
                                        Icon(Icons.Filled.ShoppingCart, null)
                                    }
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(BottomNavItem.Home, BottomNavItem.Calisthenics, BottomNavItem.Stoicism, BottomNavItem.Running, BottomNavItem.Store).forEach { item ->
                            val selected = currentBottomRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        bottomNavController.navigate(item.route) {
                                            popUpTo(0) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, null) },
                                label = { Text(stringResource(item.labelResource)) }
                            )
                        }
                    }
                }
            ) { contentPadding ->
                NavHost(
                    navController = bottomNavController,
                    startDestination = BottomNavItem.Home.route,
                    modifier = Modifier.padding(contentPadding).fillMaxSize()
                ) {
                    composable(BottomNavItem.Home.route) { HomeScreen(mainNavController = mainNavController) }
                    composable(BottomNavItem.Calisthenics.route) { CalisthenicsProgressionScreen(mainNavController = mainNavController) }
                    composable(BottomNavItem.Stoicism.route) { StoicismContentScreen(mainNavController = mainNavController) }
                    composable(BottomNavItem.Running.route) { RunningTabScreen(navController = mainNavController) }
                    composable(BottomNavItem.Store.route) { 
                        StoreScreen(onProductClick = { productId -> 
                            mainNavController.navigate(Routes.productDetail(productId)) 
                        }) 
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
    userProfileViewModel: UserProfileViewModel
) {
    val userProfile by userProfileViewModel.user.collectAsState()
    var expandLugar by remember { mutableStateOf(false) }
    var expandNivel by remember { mutableStateOf(false) }
    
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        // Header con Logo Original Restaurado
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_logo2),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    AsyncImage(
                        model = userProfile?.fotoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_default_avatar)
                    )
                    Text(userProfile?.nombre ?: "Usuario", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        
        LazyColumn(modifier = Modifier.padding(12.dp)) {
            item {
                DrawerItem(Icons.Default.Home, "Inicio") { scope.launch { drawerState.close(); mainNavController.navigate(Routes.MAIN_CONTENT) } }
                DrawerItem(Icons.Default.MenuBook, "Mis Rutinas") { scope.launch { drawerState.close(); mainNavController.navigate(Routes.MY_CUSTOM_ROUTINES_SCREEN) } }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Sección de Explorar por Lugar
                DrawerCollapsibleItem(Icons.Default.Place, "Por Lugar", expandLugar) { expandLugar = !expandLugar }
                AnimatedVisibility(visible = expandLugar) {
                    Column(modifier = Modifier.padding(start = 24.dp)) {
                        LugarEntrenamiento.entries.forEach { lugar ->
                            DrawerItem(Icons.Default.ChevronRight, lugar.name.lowercase().replaceFirstChar { it.uppercase() }) {
                                scope.launch { drawerState.close(); mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?${Routes.Args.PLACE_ARG}=${lugar.name}") }
                            }
                        }
                    }
                }

                // Sección de Explorar por Nivel
                DrawerCollapsibleItem(Icons.Default.BarChart, "Por Nivel", expandNivel) { expandNivel = !expandNivel }
                AnimatedVisibility(visible = expandNivel) {
                    Column(modifier = Modifier.padding(start = 24.dp)) {
                        listOf("PRINCIPIANTE", "INTERMEDIO", "AVANZADO").forEach { nivel ->
                            DrawerItem(Icons.Default.ChevronRight, nivel.lowercase().replaceFirstChar { it.uppercase() }) {
                                scope.launch { drawerState.close(); mainNavController.navigate("${Routes.ROUTINES_EXPLORER_SCREEN}?${Routes.Args.LEVEL_ARG}=$nivel") }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(Icons.AutoMirrored.Filled.FormatListBulleted, "Biblioteca de Ejercicios") {
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.allExercises(false)) }
                }
                DrawerItem(Icons.Default.History, "Historial de Actividad") {
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.ACTIVITY_HISTORY_SCREEN) }
                }
                DrawerItem(Icons.Default.Settings, "Configuración") {
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.SETTINGS_SCREEN) }
                }
                
                DrawerItem(Icons.AutoMirrored.Filled.Logout, "Cerrar Sesión", color = MaterialTheme.colorScheme.error) {
                    scope.launch {
                        drawerState.close()
                        FirebaseAuth.getInstance().signOut()
                        mainNavController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null, tint = color) },
        label = { Text(label, color = color) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun DrawerCollapsibleItem(icon: ImageVector, label: String, isExpanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
}
