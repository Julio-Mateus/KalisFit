package com.jcmateus.kalisfit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Store

import androidx.compose.ui.graphics.vector.ImageVector
import com.jcmateus.kalisfit.R

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResource: Int // Cambiado a Int para usar R.string.xxx
) {
    object Home : BottomNavItem(
        Routes.HOME_TAB,
        Icons.Filled.Home,
        R.string.bottom_nav_home // Ejemplo: strings.xml -> <string name="bottom_nav_home">Inicio</string>
    )

    object Calisthenics : BottomNavItem(
        Routes.CALISTHENICS_TAB,
        Icons.Filled.FitnessCenter, // O considera Icons.Filled.SportsGymnastics si lo tienes, o un ícono personalizado
        R.string.bottom_nav_calisthenics // Ejemplo: strings.xml -> <string name="bottom_nav_calisthenics">Calistenia</string>
    )

    object Stoicism : BottomNavItem(
        Routes.STOICISM_TAB,
        Icons.Filled.SelfImprovement, // O Icons.Filled.MenuBook, Icons.Filled.Psychology
        R.string.bottom_nav_stoicism // Ejemplo: strings.xml -> <string name="bottom_nav_stoicism">Estoicismo</string>
    )

    object Running : BottomNavItem(
        Routes.RUNNING_TAB,
        Icons.Filled.DirectionsRun,
        R.string.bottom_nav_running // Ejemplo: strings.xml -> <string name="bottom_nav_running">Running</string>
    )

    // --- NUEVO ITEM PARA LA TIENDA ---
    object Store : BottomNavItem(
        route = Routes.STORE_TAB, // Usa la constante de Routes
        labelResource = R.string.title_store, // Necesitarás añadir este string
        icon = Icons.Filled.Store // Icono para la tienda
    )
}

