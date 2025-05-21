package com.jcmateus.kalisfit.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.navigation.Routes
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import kotlin.random.Random


@SuppressLint("RememberReturnType")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mainNavController: NavHostController, bottomNavController: NavHostController) { // bottomNavController se recibe pero no se usa en este ejemplo directamente
    val userViewModel: UserProfileViewModel = viewModel()

    // Recolecta los estados del ViewModel
    val user by userViewModel.user.collectAsState()
    val isLoadingUser by userViewModel.isLoadingUser.collectAsState()
    val userErrorMessage by userViewModel.userErrorMessage.collectAsState()

    val recommendedRoutines by userViewModel.recommendedRoutines.collectAsState()
    val isLoadingRoutines by userViewModel.isLoadingRoutines.collectAsState()
    val routinesError by userViewModel.routinesErrorMessage.collectAsState()

    val tipsGenerales = remember {
        listOf(
            "Mantén una buena postura durante los ejercicios para prevenir lesiones y maximizar la efectividad.",
            "La hidratación es clave. Bebe suficiente agua antes, durante y después de tus entrenamientos.",
            "No olvides calentar antes de cada sesión y estirar al finalizar para mejorar la flexibilidad y reducir el dolor muscular.",
            "Escucha a tu cuerpo. Si sientes dolor agudo, detente y descansa. No te exijas más de la cuenta, especialmente al principio.",
            "La consistencia es más importante que la intensidad al inicio. Es mejor entrenar 3-4 veces por semana de forma moderada que una vez muy intenso y luego abandonar.",
            "Una dieta equilibrada rica en proteínas, carbohidratos complejos y grasas saludables potenciará tus resultados.",
            "El descanso es tan importante como el ejercicio. Asegúrate de dormir entre 7-9 horas para una buena recuperación muscular.",
            "Varía tus rutinas cada 4-6 semanas para evitar el estancamiento y seguir progresando.",
            "Establece metas realistas y medibles. Te ayudará a mantener la motivación a largo plazo.",
            "¡Disfruta el proceso! Encuentra actividades que te gusten para que el ejercicio se convierta en un hábito placentero."
        )
    }

    val tipDelDia = remember(tipsGenerales, user) { // Re-calcular si el usuario cambia (o al inicio)
        if (tipsGenerales.isNotEmpty()) {
            tipsGenerales.random(Random(System.currentTimeMillis()))
        } else {
            "¡Recuerda mantenerte activo hoy!"
        }
    }

    LaunchedEffect(key1 = Unit) {
        userViewModel.loadUserProfile() // Esto iniciará la carga del perfil y, si tiene éxito, la carga de rutinas
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Estado 1: Cargando el perfil de usuario
        if (isLoadingUser) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Cargando perfil...",
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        // Estado 2: Error al cargar el perfil de usuario
        else if (user == null && userErrorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        userErrorMessage ?: "No se pudo cargar el perfil del usuario.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { userViewModel.loadUserProfile() }, // Reintentar cargar perfil
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Reintentar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        // Estado 3: Perfil cargado con éxito, mostrar contenido principal
        else if (user != null) {
            val currentUser = user!! // Sabemos que no es nulo aquí
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp + 56.dp // Espacio extra para el BottomNav si es persistente
                )
            ) {
                // --- SECCIÓN BIENVENIDA ---
                item {
                    Text(
                        text = "Hola, ${currentUser.nombre} 👋",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Nivel: ${currentUser.nivel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (currentUser.objetivos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tus Objetivos:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp)) // Espacio antes del FlowRow
                        FlowRow(
                            // Ya no usamos mainAxisSpacing ni crossAxisSpacing aquí directamente
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacio horizontal ENTRE ítems
                        ) {
                            currentUser.objetivos.forEach { objetivo ->
                                Text(
                                    text = objetivo,
                                    modifier = Modifier
                                        // Añadimos padding vertical a cada ítem para simular crossAxisSpacing
                                        .padding(vertical = 4.dp) // (8.dp / 2) en cada lado del ítem
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp), // Tu padding original para el contenido interno
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // --- TIP DEL DÍA ---
                item {
                    SectionTitle("💡 Tip del Día")
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            tipDelDia,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // --- TIPS RECIENTES (Placeholder) ---
                item {
                    SectionTitle("📚 Tips Recientes")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(3) { index -> // Reemplaza con tus datos reales de tips
                            TipCard(
                                title = "Consejo de Calistenia #${index + 1}",
                                onClick = {
                                    // TODO: Navegar al detalle del tip si tienes una pantalla para ello
                                    // mainNavController.navigate(Routes.tipDetailScreen(tipId = "someTipId${index + 1}"))
                                }
                            )
                        }
                    }
                }

                // --- RUTINAS RECOMENDADAS ---
                item {
                    SectionTitle("🏋️‍♂️ Tus Rutinas Recomendadas")
                    when {
                        isLoadingRoutines -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        routinesError != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    routinesError ?: "Error al cargar rutinas.",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { userViewModel.loadRecommendedRoutines(currentUser) }, // Reintentar cargar rutinas
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(top=8.dp)
                                ) {
                                    Text("Reintentar", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }

                        recommendedRoutines.isEmpty() && !isLoadingRoutines -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    "No hay rutinas recomendadas disponibles en este momento.",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        "Explorar Todas las Rutinas",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(items = recommendedRoutines, key = { it.id }) { rutina ->
                                    RoutineCard(rutina = rutina, navController = mainNavController)
                                }
                            }
                        }
                    }
                }

                // --- BOTONES DE ACCIÓN ---
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    val puedeEmpezarRecomendada =
                        recommendedRoutines.isNotEmpty() && routinesError == null && !isLoadingRoutines

                    Button(
                        onClick = {
                            if (puedeEmpezarRecomendada) {
                                val primeraRutina = recommendedRoutines.firstOrNull()
                                primeraRutina?.id?.let { rutinaId ->
                                    // NAVEGACIÓN CORRECTA AL DETALLE DE LA RUTINA
                                    mainNavController.navigate(Routes.routineDetail(rutinaId))
                                }
                            } else {
                                // NAVEGACIÓN CORRECTA A EXPLORAR RUTINAS
                                mainNavController.navigate(Routes.ROUTINES_EXPLORER_SCREEN)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = user != null // Habilitado si el usuario está cargado
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (puedeEmpezarRecomendada) "Empezar rutina recomendada" else "Explorar rutinas")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            // NAVEGACIÓN CORRECTA AL PERFIL
                            mainNavController.navigate(Routes.PROFILE_SCREEN)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        enabled = user != null
                    ) {
                        Text("Ver mi perfil")
                    }
                    // Spacer(modifier = Modifier.height(16.dp)) // El padding del LazyColumn ya da espacio abajo
                }
            }
        }
        // Estado 4: Caso por defecto o estado inicial antes de que LaunchedEffect se active completamente
        // y el usuario aún no está cargado ni hay error.
        else if (!isLoadingUser && user == null && userErrorMessage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Este estado es breve, mientras se dispara el LaunchedEffect.
                // Un CircularProgressIndicator es apropiado si el usuario aún no está cargado.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Inicializando...",
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp) // Añadido padding vertical por defecto
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // El título describe la sección
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp) // Un tamaño ligeramente mayor puede ser bueno
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall, // Estilo para títulos de sección
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipCard(title: String, imageUrl: String? = null, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp) // Ancho fijo para consistencia en LazyRow
            .height(150.dp), // Altura fija
        shape = RoundedCornerShape(16.dp), // Bordes redondeados consistentes
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Aquí iría la lógica para cargar la imagen si imageUrl no es nulo
            // Por ejemplo, usando Coil:
            // if (imageUrl != null) {
            //     AsyncImage(
            //         model = ImageRequest.Builder(LocalContext.current)
            //             .data(imageUrl)
            //             .crossfade(true)
            //             .build(),
            //         contentDescription = title,
            //         modifier = Modifier
            //             .fillMaxWidth()
            //             .height(80.dp), // Altura para la imagen
            //         contentScale = ContentScale.Crop
            //     )
            // } else {
            // Si no hay imagen, puedes poner un placeholder o un icono
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // Misma altura que la imagen para consistencia
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook, // Icono placeholder
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            // }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(12.dp), // Padding para el texto
                maxLines = 2 // Limitar a 2 líneas para evitar desbordamiento
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCard(rutina: Rutina, navController: NavHostController) {
    Card(
        onClick = {
            // NAVEGACIÓN CORRECTA AL DETALLE DE LA RUTINA
            navController.navigate(Routes.routineDetail(rutina.id))
        },
        modifier = Modifier
            .width(280.dp) // Ancho de la tarjeta
            .height(180.dp), // Altura de la tarjeta
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) // Borde más sutil
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween // Distribuye el espacio
        ) {
            Column { // Contenido superior
                // Aquí podrías añadir una imagen si tu modelo `Rutina` la tiene
                // if (rutina.imagenUrl != null) {
                //     AsyncImage(...)
                // }
                Text(
                    text = rutina.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.primary // Destacar el nombre
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // Detalles inferiores
                if (rutina.nivelRecomendado.isNotEmpty()) {
                    Text(
                        "Nivel: ${rutina.nivelRecomendado.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rutina.lugarEntrenamiento.isNotEmpty()) {
                    Text(
                        "Lugar: ${rutina.lugarEntrenamiento.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rutina.ejercicios.isNotEmpty()) {
                    Text(
                        "Ejercicios: ${rutina.ejercicios.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

