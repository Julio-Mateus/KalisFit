package com.jcmateus.kalisfit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Inicio")
    object Tips : BottomNavItem("tips", Icons.Default.Star, "Tips")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Perfil")
    object History : BottomNavItem("historial", Icons.Default.History, "Historial") // ✅ nuevo ítem
}

