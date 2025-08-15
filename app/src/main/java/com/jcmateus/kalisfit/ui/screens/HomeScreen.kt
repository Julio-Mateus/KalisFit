package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
//import android.icu.util.TimeUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.model.UserActivity
import com.jcmateus.kalisfit.model.UserCustomRoutine
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.LastActivityItem
import com.jcmateus.kalisfit.viewmodel.ResumenSemanal
import com.jcmateus.kalisfit.viewmodel.UserProfile
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlin.random.Random
import java.util.concurrent.TimeUnit


@SuppressLint("RememberReturnType")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeScreen(
    mainNavController: NavHostController,
) {
    val userViewModel: UserProfileViewModel = viewModel()

    val user by userViewModel.user.collectAsState()
    val isLoadingUser by userViewModel.isLoadingUser.collectAsState()
    val userErrorMessage by userViewModel.userErrorMessage.collectAsState()

    val homeScreenSummary by userViewModel.homeScreenSummary.collectAsState()
    val lastActivity by userViewModel.lastActivity.collectAsState()
    val isLoadingHomeScreenData by userViewModel.isLoadingHomeScreenData.collectAsState()
    val homeScreenError by userViewModel.homeScreenErrorMessage.collectAsState()

    val recommendedRoutines by userViewModel.recommendedRoutines.collectAsState()
    val isLoadingRoutines by userViewModel.isLoadingRoutines.collectAsState()
    val routinesError by userViewModel.routinesErrorMessage.collectAsState()

    // --- Nuevos estados para Rutinas Personalizadas ---
    val userCustomRoutines by userViewModel.userCustomRoutines.collectAsState()
    val isLoadingUserCustomRoutines by userViewModel.isLoadingUserCustomRoutines.collectAsState()
    val userCustomRoutinesError by userViewModel.userCustomRoutinesError.collectAsState()
    // --- Fin nuevos estados ---

    val tipsGenerales = remember {
        listOf(
            "Mantén una buena postura durante los ejercicios.",
            "La hidratación es clave: bebe agua antes, durante y después.",
            "Calienta antes y estira después de cada sesión.",
            "Escucha a tu cuerpo; si sientes dolor agudo, detente.",
            "Consistencia > Intensidad al inicio. Entrena 3-4 veces/semana.",
            "Una dieta equilibrada potenciará tus resultados.",
            "Duerme 7-9 horas para una buena recuperación muscular.",
            "Varía tus rutinas cada 4-6 semanas para progresar.",
            "Establece metas realistas y medibles para mantener la motivación.",
            "¡Disfruta el proceso! Encuentra actividades que te gusten."
        )
    }

    val tipDelDia = remember(tipsGenerales, user?.uid) {
        if (tipsGenerales.isNotEmpty()) {
            val seed = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)) + (user?.uid?.hashCode()?.toLong() ?: 0L)
            tipsGenerales.random(Random(seed))
        } else {
            "¡Recuerda mantenerte activo hoy!"
        }
    }

    // Pull to refresh state
    // *** ACTUALIZADO isRefreshing ***
    val isRefreshing = isLoadingUser || isLoadingHomeScreenData || isLoadingRoutines || isLoadingUserCustomRoutines
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            userViewModel.loadUserProfile() // Carga perfil, luego recomendaciones
            userViewModel.refreshHomeScreenData() // Carga resumen, última actividad y rutinas personalizadas
            // No es necesario llamar a refreshUserCustomRoutines directamente si refreshHomeScreenData ya lo hace
        }
    }
    // Observa los estados de carga para finalizar el refresh
    // *** ACTUALIZADO LaunchedEffect para endRefresh ***
    LaunchedEffect(isLoadingUser, isLoadingHomeScreenData, isLoadingRoutines, isLoadingUserCustomRoutines) {
        if (!isLoadingUser && !isLoadingHomeScreenData && !isLoadingRoutines && !isLoadingUserCustomRoutines) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoadingUser && user == null -> {
                LoadingIndicator(text = "Cargando perfil...")
            }
            user == null && userErrorMessage != null -> {
                ErrorState(
                    message = userErrorMessage ?: "No se pudo cargar el perfil.",
                    onRetry = { userViewModel.loadUserProfile() }
                )
            }
            user != null -> {
                val currentUser = user!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp + 56.dp + 16.dp // Espacio para BottomNav y algo más
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    stickyHeader(key = "header") {
                        HeaderCard(
                            user = currentUser,
                            tipDelDia = tipDelDia,
                            modifier = Modifier
                                .animateItemPlacement(tween(durationMillis = 500))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(bottom = 8.dp)
                        )
                    }

                    item(key = "summary") {
                        Column(modifier = Modifier.animateItemPlacement(tween(durationMillis = 500))) {
                            SectionTitle1("📈 Tu Semana")
                            when {
                                isLoadingHomeScreenData && homeScreenSummary == null -> LoadingCard(text = "Cargando resumen...")
                                homeScreenError != null && homeScreenSummary == null -> ErrorCard(
                                    message = homeScreenError ?: "Error al cargar resumen.",
                                    onRetry = { userViewModel.refreshHomeScreenData() }
                                )
                                homeScreenSummary != null -> WeeklySummaryCard(summary = homeScreenSummary!!)
                                else -> NoDataCard(message = "Aún no hay datos para tu resumen semanal. ¡Empieza a entrenar!")
                            }
                        }
                    }

                    item(key = "last_activity") {
                        Column(modifier = Modifier.animateItemPlacement(tween(durationMillis = 500))) {
                            SectionTitle1("⏱️ Última Actividad")
                            when (val activity = lastActivity) {
                                is LastActivityItem.Loading -> LoadingCard(text = "Cargando última actividad...")
                                is LastActivityItem.None -> NoDataCard(message = "No has registrado actividades recientemente.")
                                is LastActivityItem.Routine -> LastActivityRoutineCard(activity.progreso, mainNavController)
                                is LastActivityItem.FreeActivity -> LastActivityFreeCard(activity.activity, mainNavController)
                            }
                            if (homeScreenError != null && lastActivity is LastActivityItem.None) {
                                Spacer(Modifier.height(8.dp))
                                ErrorCard(
                                    message = homeScreenError ?: "Error al cargar última actividad.",
                                    onRetry = { userViewModel.refreshHomeScreenData() }
                                )
                            }
                        }
                    }
                    // --- MIS RUTINAS (PERSONALIZADAS) --- // *** NUEVA SECCIÓN ***
                    item(key = "my_custom_routines") {
                        Column(modifier = Modifier.animateItemPlacement(tween(durationMillis = 500))) {
                            SectionTitle1(
                                title = "📖 Mis Rutinas Guardadas", // O "Continuar con mis Rutinas"
                                actionText = if (userCustomRoutines.isNotEmpty()) "Ver todas" else null, // Opcional
                                onActionClick = if (userCustomRoutines.isNotEmpty()) {
                                    {
                                        mainNavController.navigate(Routes.MY_CUSTOM_ROUTINES_SCREEN) // Navegar a una pantalla de "Mis Rutinas" si la tienes
                                    }
                                } else null
                            )
                            when {
                                isLoadingUserCustomRoutines && userCustomRoutines.isEmpty() -> LoadingCard(
                                    text = "Cargando tus rutinas..."
                                )
                                userCustomRoutinesError != null && userCustomRoutines.isEmpty() -> ErrorCard(
                                    message = userCustomRoutinesError ?: "Error al cargar tus rutinas.",
                                    onRetry = { userViewModel.refreshUserCustomRoutines() }
                                )
                                userCustomRoutines.isEmpty() && !isLoadingUserCustomRoutines -> {
                                    NoDataCard(message = "Aún no has guardado ninguna rutina personalizada.") {
                                        // Opcionalmente, un botón para ir a crear/explorar rutinas
                                        Button(
                                            onClick = { mainNavController.navigate(Routes.MY_CUSTOM_ROUTINES_SCREEN) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Explorar Rutinas")
                                        }
                                    }
                                }
                                userCustomRoutines.isNotEmpty() -> {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(
                                            items = userCustomRoutines.take(5), // Mostrar un máximo en el home
                                            key = { it.id }) { rutina -> // Asume que UserCustomRoutine tiene 'id'
                                            UserCustomRoutineCard( // *** NUEVO COMPOSABLE ***
                                                userRoutine = rutina,
                                                navController = mainNavController,
                                                modifier = Modifier.width(280.dp)
                                            )
                                        }
                                    }
                                }
                                // else -> {} // No es necesario si cubrimos todos los casos
                            }
                        }
                    }
                    // --- FIN NUEVA SECCIÓN ---
                    // --- RUTINAS RECOMENDADAS --
                    item(key = "recommendations") {
                        Column(modifier = Modifier.animateItemPlacement(tween(durationMillis = 500))) {
                            SectionTitle1("🏋️‍♂️ Tus Rutinas Recomendadas")
                            when {
                                isLoadingRoutines && recommendedRoutines.isEmpty() -> LoadingCard(
                                    text = "Buscando recomendaciones..."
                                )
                                routinesError != null && recommendedRoutines.isEmpty() -> ErrorCard(
                                    message = routinesError ?: "Error al cargar rutinas.",
                                    onRetry = { userViewModel.refreshRecommendations() }
                                )
                                recommendedRoutines.isEmpty() && !isLoadingRoutines -> {
                                    NoDataCard(message = "No hay rutinas recomendadas ahora. ¡Explora y encuentra la tuya!") {
                                        Button(
                                            onClick = { mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Explorar Todas las Rutinas")
                                        }
                                    }
                                }
                                else -> {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(
                                            items = recommendedRoutines, // Ya se limitan a 5 en el ViewModel
                                            key = { it.id }) { rutina ->
                                            RoutineCard(
                                                rutina = rutina,
                                                navController = mainNavController,
                                                modifier = Modifier.width(280.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "actions") {
                        Column(modifier = Modifier.animateItemPlacement(tween(durationMillis = 500))) {
                            ActionButtonsSection(
                                navController = mainNavController,
                                userIsPresent = user != null
                            )
                        }
                    }
                }
            }
            else -> {
                LoadingIndicator(text = "Inicializando...")
            }
        }

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
// --- NUEVO COMPOSABLE PARA UserCustomRoutine ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCustomRoutineCard(
    userRoutine: UserCustomRoutine, // Tu modelo de datos para rutinas personalizadas
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "userCustomRoutineScaleAnimation"
    )
    Card(
        onClick = {
            // Lógica de navegación:
            // Si la rutina personalizada tiene un 'originalTemplateId' Y NO es una rutina
            // que solo exista como personalizada (es decir, el usuario quiere ver los detalles
            // de la plantilla base en lugar de ejecutar su versión directamente),
            // entonces navega al detalle de la plantilla.
            // SI NO, o si el usuario siempre debe ejecutar SU versión,
            // entonces navega a la pantalla de ejecución con el customRoutineId.

            // CASO 1: El usuario quiere iniciar SU versión personalizada de la rutina.
            // Esta es la acción más probable para "Mis Rutinas Guardadas".
            //navController.navigate(Routes.startCustomRoutine(userRoutine.id))

            // CASO 2: (Alternativa o si quieres ofrecer ambas opciones en otro lugar)
            // Si quieres que al hacer clic se vea el detalle de la PLANTILLA ORIGINAL
            // (si existe) en lugar de iniciar la rutina personalizada:

            if (userRoutine.originalTemplateId != null && userRoutine.originalTemplateId.isNotBlank()) {
                // Navega al detalle de la plantilla original si existe
                navController.navigate(Routes.routineDetail(userRoutine.originalTemplateId))
            } else {
                // Si no hay plantilla original (es 100% custom) O si siempre se ejecuta la custom,
                // navega para iniciar la rutina personalizada
                navController.navigate(
                    Routes.startRoutineExecution(customRoutineId = userRoutine.id)
                )
            }
        },
        interactionSource = interactionSource,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!userRoutine.imagenUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userRoutine.imagenUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Imagen de la rutina: ${userRoutine.nombrePersonalizado}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "Sin imagen",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = userRoutine.nombrePersonalizado.ifBlank { "Rutina Personalizada" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (userRoutine.ejercicios.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Filled.FitnessCenter,
                        text = "${userRoutine.ejercicios.size} ejercicios",
                    )
                }
                if (userRoutine.numeroDeRondas > 0) {
                    DetailRow(
                        icon = Icons.Filled.Timelapse,
                        text = "${userRoutine.numeroDeRondas} rondas",
                    )
                }
            }
        }
    }
}
@Composable
fun HeaderCard(user: UserProfile, tipDelDia: String, modifier: Modifier = Modifier) {
    // La card ya tiene su propio color, así que no se transparentará.
    // El 'modifier' que le pasamos desde el stickyHeader se encargará del fondo.
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer, // Un color ligeramente distinto al fondo
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Saludo principal
            Text(
                text = "Hola, ${user.nombre.takeIf { it.isNotBlank() } ?: "Usuario"} 👋",
                style = MaterialTheme.typography.headlineSmall, // Ajustamos un poco el tamaño
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Separador con el Tip del Día
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tipDelDia,
                    style = MaterialTheme.typography.bodyMedium, // Ajustamos
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun SectionTitle1(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp), // Un poco de espacio vertical extra
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Título principal
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f) // Ocupa todo el espacio posible
        )

        // Botón de acción (si se proporciona)
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCard(
    rutina: Rutina,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    // 2. Observamos si el componente está siendo presionado
    val isPressed by interactionSource.collectIsPressedAsState()

// 3. Definimos el estado de la animación (la escala de la tarjeta)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scaleAnimation"
    )
    Card(
        onClick = {
            navController.navigate(Routes.routineDetail(rutina.slug.ifBlank { rutina.id })) // Usar slug si está disponible
        },
        interactionSource = interactionSource,
        modifier = modifier
            .height(IntrinsicSize.Min) // Para que la card se ajuste a su contenido si es más alto
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // Ligeramente diferente para destacar
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(rutina.imagenUrl)
                    .crossfade(true)
                    // .placeholder(R.drawable.placeholder_routine_card) // Define un placeholder
                    // .error(R.drawable.error_routine_card) // Define una imagen de error
                    .build(),
                contentDescription = "Imagen de la rutina: ${rutina.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp) // Altura fija para la imagen
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = rutina.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Nivel
                if (rutina.nivelRecomendado.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Filled.FitnessCenter,
                        text = "Nivel: ${rutina.nivelRecomendado.joinToString(", ")}"
                    )
                }
                // Lugar
                if (rutina.lugarEntrenamiento.mapNotNull { it.name }.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Filled.CalendarToday, // Placeholder, elige un icono adecuado
                        text = "Lugar: ${rutina.lugarEntrenamiento.joinToString(", ") { it.name }}"
                    )
                }
                // Objetivos (opcional, si quieres mostrarlo en la card)
                if (rutina.objetivos.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Filled.FitnessCenter, // Cambiar icono
                        text = "Objetivos: ${
                            rutina.objetivos.take(2).joinToString(", ")
                        }${if (rutina.objetivos.size > 2) "..." else ""}",
                        maxLines = 1
                    )
                }
            }
        }
    }
}
@Composable
fun DetailRow(icon: ImageVector, text: String, maxLines: Int = 1) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
fun WeeklySummaryCard(summary: ResumenSemanal) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                value = summary.rutinasCompletadas.toString(),
                label = "Rutinas"
            )
            Divider(
                modifier = Modifier
                    .height(50.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            SummaryItem(
                value = formatDuration(summary.tiempoTotalEntrenadoSegundos),
                label = "Tiempo Total"
            )
            // Puedes añadir más items aquí si ResumenSemanal tiene más campos
        }
    }
}
@Composable
fun SummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastActivityRoutineCard(progreso: ProgresoRutina, navController: NavHostController) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scaleAnimation"
    )
    Card(
        onClick = { /* Podrías navegar al detalle del progreso si tienes esa pantalla */
            // navController.navigate(Routes.progressDetail(progreso.id))
        },
        interactionSource = interactionSource,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = progreso.nombreRutina,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                icon = Icons.Filled.CalendarToday,
                text = "Fecha: ${UserProfileViewModel.formatFirebaseTimestampForDisplay(progreso.fecha)}"
            )
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(
                icon = Icons.Filled.Timelapse,
                text = "Duración: ${formatDuration(progreso.tiempoTotalSesionSegundos)}"
            )
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(
                icon = Icons.Filled.FitnessCenter,
                text = "Rondas: ${progreso.rondasRealizadas}"
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastActivityFreeCard(activity: UserActivity, navController: NavHostController) {
    Card(
        onClick = { /* Podrías navegar al detalle de la actividad libre */
            // navController.navigate(Routes.activityDetail(activity.id))
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                // UserActivity no tiene un "nombre" per se, usa la fecha o un título genérico
                text = "Actividad Libre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                icon = Icons.Filled.CalendarToday,
                text = "Fecha: ${UserProfileViewModel.formatDateForDisplay(activity.timestamp)}"
            )
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(
                icon = Icons.Filled.Timelapse,
                text = "Duración: ${formatDuration(activity.elapsedTimeSeconds.toInt())}"
            )
            if (activity.distanceKm > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    text = "Distancia: ${String.format("%.2f", activity.distanceKm)} km"
                )
            }
        }
    }
}
@Composable
fun ActionButtonsSection(navController: NavHostController, userIsPresent: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = {
                // Podrías decidir qué hacer basado en si hay recomendaciones o no
                navController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) // Simplificado a explorar siempre
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            enabled = userIsPresent,
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Explorar Rutinas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer )
        }

        OutlinedButton(
            onClick = {
                navController.navigate(Routes.PROFILE_SCREEN)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            enabled = userIsPresent,
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text("Ver Mi Perfil", fontSize = 16.sp)
        }
    }
}
@Composable
fun LoadingIndicator(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
@Composable
fun LoadingCard(text: String = "Cargando...") {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Reintentar")
            }
        }
    }
}
@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Reintentar")
            }
        }
    }
}
@Composable
fun NoDataCard(message: String, content: (@Composable () -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (content != null) {
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}
fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds < 0) return "0s"
    val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%dh %02dm", hours, minutes)
        minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
        else -> String.format("%ds", seconds)
    }
}

