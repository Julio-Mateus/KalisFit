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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jcmateus.kalisfit.viewmodel.UserProfileViewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.navigation.BottomNavItem
import com.jcmateus.kalisfit.navigation.Routes
import kotlin.collections.isNotEmpty
import kotlin.random.Random

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mainNavController: NavHostController, bottomNavController: NavHostController) {
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

    val tipDelDia = remember(tipsGenerales) {
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
                // Añade contentPadding para el espaciado horizontal y vertical si es necesario
                contentPadding = PaddingValues(
                    start = 16.dp, // Espacio a la izquierda de todos los items
                    end = 16.dp,   // Espacio a la derecha de todos los items
                    top = 16.dp,   // Espacio en la parte superior del primer item
                    bottom = 16.dp // Espacio después del último item (antes de la Nav Bar)
                )
            ) {
                // --- SECCIÓN BIENVENIDA ---
                item {
                    Text(
                        text = "Hola, ${user!!.nombre} 👋", // user no será nulo aquí
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Nivel: ${user!!.nivel}", // user no será nulo aquí
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (user!!.objetivos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tus Objetivos:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (objetivo in user!!.objetivos) { // Esta línea es la 149 según tu error
                                Text(text = objetivo, modifier = Modifier.padding(bottom = 8.dp)) // Temporalmente reemplaza AssistChip
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
                        items(3) { index ->
                            TipCard(
                                title = "Consejo de Calistenia #${index + 1}",
                                onClick = { /* TODO: Navegar al detalle del tip */ }
                            )
                        }
                    }
                }

                // --- RUTINAS RECOMENDADAS ---
                item {
                    SectionTitle(
                        "🏋️‍♂️ Tus Rutinas Recomendadas"
                    )
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
                                // Podrías añadir un botón de reintento para rutinas aquí si lo deseas
                                // Button(onClick = { userViewModel.refreshRecommendations() }) { Text("Reintentar") }
                            }
                        }

                        recommendedRoutines.isEmpty() && !isLoadingRoutines -> { // Asegurarse de no mostrar esto mientras carga
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
                                    onClick = { /* TODO: Navegar a una pantalla de exploración de rutinas, ej: mainNavController.navigate(Routes.EXPLORE_ROUTINES) */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        "Explorar Todas las Rutinas",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        else -> { // recommendedRoutines no está vacía
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
                                    mainNavController.navigate("${Routes.ROUTINE_DETAIL}/$rutinaId")
                                }
                            } else {
                                // mainNavController.navigate(Routes.EXPLORE_ROUTINES) // Ejemplo
                                // TODO: Definir acción si no hay rutinas o navegar a explorar
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = user != null // Se habilita si el usuario está cargado
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (puedeEmpezarRecomendada) "Empezar rutina recomendada" else "Explorar rutinas")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            // NAVEGACIÓN CORRECTA AL PERFIL
                            mainNavController.navigate(Routes.PROFILE) // Asegúrate que Routes.PROFILE esté definida
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        enabled = user != null // O currentUser != null, dependiendo de tu variable
                    ) {
                        Text("Ver mi perfil")
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Espacio al final
                }
            }
        }
        // Estado 4: Caso por defecto o estado inicial antes de que LaunchedEffect se active completamente
        else if (!isLoadingUser && user == null && userErrorMessage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Puedes poner un CircularProgressIndicator aquí también si prefieres
                // o dejarlo en blanco mientras espera la primera llamada de loadUserProfile
                Text("Inicializando...", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
@Composable
fun SectionTitle(title: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // <--- COLOR
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall, // Un estilo más prominente para títulos de sección
            color = MaterialTheme.colorScheme.onBackground // <--- COLOR
        )
    }
}

@Composable
fun TipCard(title: String, imageUrl: String? = null, onClick: () -> Unit) { // imageUrl es opcional
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(150.dp), // Un poco más alto para imagen
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // <--- COLOR
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant // <--- COLOR
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (imageUrl != null) {
                // AsyncImage(model = imageUrl, contentDescription = title, ...) // Usar Coil o Glide
            } else {
                // Placeholder si no hay imagen, o un icono grande
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer), // <--- COLOR
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(12.dp),
                maxLines = 2
            )
        }
    }
}


@Composable
fun RoutineCard(rutina: Rutina, navController: NavHostController) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp), // Ajusta la altura si añades imagen
        shape = RoundedCornerShape(16.dp),
        onClick = {
            // Navega usando el ID de la rutina.
            // Asegúrate que tu pantalla de destino sepa cómo manejar este ID.
            navController.navigate("${Routes.ROUTINE_DETAIL}/${rutina.id}")
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface, // o primaryContainer
            contentColor = MaterialTheme.colorScheme.onSurface  // o onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary) // Borde para destacar
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column { // Columna para el contenido superior (nombre e imagen si la hay)
                // Opcional: Mostrar imagen de la rutina si existe
                /*
                if (rutina.imagenUrl != null) {
                    AsyncImage(
                        model = rutina.imagenUrl,
                        contentDescription = "Imagen de ${rutina.nombre}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp) // Ajusta según necesites
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                */
                Text(
                    rutina.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
            }

            Column { // Columna para los detalles inferiores
                if (rutina.nivelRecomendado.isNotEmpty()) {
                    Text(
                        "Nivel: ${rutina.nivelRecomendado.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (rutina.lugarEntrenamiento.isNotEmpty()) {
                    Text(
                        "Lugar: ${rutina.lugarEntrenamiento.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (rutina.ejercicios.isNotEmpty()) {
                    Text(
                        "Ejercicios: ${rutina.ejercicios.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

