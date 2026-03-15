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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val cartViewModel: CartViewModel =
        viewModel(factory = CartViewModelFactory(cartRepository, authRepository))
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
                    currentMainRoute = currentBottomRoute,
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
                                    BadgedBox(badge = {
                                        if (cartUiState.itemCount > 0) Badge {
                                            Text(
                                                "${cartUiState.itemCount}"
                                            )
                                        }
                                    }) {
                                        Icon(Icons.Filled.ShoppingCart, null)
                                    }
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                            BottomNavItem.Home,
                            BottomNavItem.Calisthenics,
                            BottomNavItem.Stoicism,
                            BottomNavItem.Running,
                            BottomNavItem.Store
                        ).forEach { item ->
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
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize()
                ) {
                    composable(BottomNavItem.Home.route) { HomeScreen(mainNavController = mainNavController) }
                    composable(BottomNavItem.Calisthenics.route) {
                        CalisthenicsProgressionScreen(
                            mainNavController = mainNavController
                        )
                    }
                    composable(BottomNavItem.Stoicism.route) {
                        StoicismContentScreen(
                            mainNavController = mainNavController
                        )
                    }
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
    currentMainRoute: String?,
    userProfileViewModel: UserProfileViewModel
) {
    val userProfile by userProfileViewModel.user.collectAsState()
    // Obtenemos la rache real calculada
    val rachaActual by userProfileViewModel.rachaActual.collectAsState()
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)

    ) {
        // --- HEADER PROFESIONAL CON RACHAS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo2),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = userProfile?.nombre ?: "Usuario",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userProfile?.email ?: "Miembro Pro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Foto clickable que lleva al perfil
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    drawerState.close()
                                    mainNavController.navigate(Routes.PROFILE_SCREEN)
                                }
                            }
                    ) {
                        AsyncImage(
                            model = userProfile?.fotoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_default_avatar)
                        )
                        Surface(
                            modifier = Modifier.offset(y = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = userProfile?.nivel?.take(3)?.uppercase() ?: "BEG",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SECCIÓN DE RACHAS Y LOGROS ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Aquí manejamos la racha (Daily Streak)
                    HeaderStat(
                        icon = Icons.Default.Whatshot, // Icono de fuego para racha
                        value = "$rachaActual",
                        label = "Días Racha",
                        color = Color(0xFFFF5722) // Naranja intenso para el fuego
                    )
                    // Divisor vertical sutil
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    HeaderStat(
                        icon = Icons.Default.FitnessCenter,
                        value = "${userProfile?.rutinasCompletadas ?: 0}",
                        label = "Completadas"
                    )
                }
            }
        }
        // --- LISTA DE ITEMS ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                DrawerSectionTitle("ENTRENAMIENTO")
                DrawerItem(
                    Icons.Default.Home,
                    "Panel de Inicio",
                    selected = currentMainRoute == Routes.MAIN_CONTENT
                ) {
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.MAIN_CONTENT) }
                }
                DrawerItem(
                    Icons.Default.MenuBook,
                    "Mis Rutinas",
                    selected = currentMainRoute == Routes.MY_CUSTOM_ROUTINES_SCREEN
                ) {
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.MY_CUSTOM_ROUTINES_SCREEN) }
                }
                DrawerItem(
                    icon = Icons.Default.Search,
                    label = "Explorar Catálogo",
                    selected = currentMainRoute == Routes.ROUTINES_EXPLORER_SCREEN
                ) {
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) }
                }
            }

            item{
                DrawerSectionTitle("RECURSOS")
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    label = "Biblioteca de Ejercicios"
                ){
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.allExercises(false)) }
                }
                DrawerItem(
                    icon = Icons.Default.History,
                    label = "Historial de Actividad",
                    selected = currentMainRoute == Routes.ACTIVITY_HISTORY_SCREEN
                ){
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.ACTIVITY_HISTORY_SCREEN) }
                }
            }

            item{
                DrawerSectionTitle("SOPORTE Y CUENTA")
                DrawerItem(
                    icon = Icons.Default.Settings,
                    label = "Configuración"
                ){
                    scope.launch { drawerState.close(); mainNavController.navigate(Routes.SETTINGS_SCREEN) }
                }
                Spacer(Modifier.height(8.dp))
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Cerrar Sesión",
                    color = MaterialTheme.colorScheme.error
                ){
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
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.7f)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else color
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            unselectedContainerColor = Color.Transparent
        )
    )
}
@Composable
fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun DrawerCollapsibleItem(
    icon: ImageVector,
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
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

@Composable
fun HeaderStat(icon: ImageVector, value: String, label: String, color: Color = MaterialTheme.colorScheme.primary) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}