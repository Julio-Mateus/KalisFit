package com.jcmateus.kalisfit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home

import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Inicio")
    object Routines : BottomNavItem("routines", Icons.Default.FitnessCenter, "Rutinas")
    object Running : BottomNavItem("running_tab", Icons.Filled.DirectionsRun, "Running") // NUEVO
    object History : BottomNavItem("historial", Icons.Default.History, "Historial")
    // Profile ya no es un ítem de la barra inferior
    // MoreOptions ya no es un ítem de la barra inferior
}

