package com.jcmateus.kalisfit.ui.theme

import androidx.compose.ui.graphics.Color


// Primarios (Mostaza)
val MustardDark = Color(0xFFC2850B) // Tu primario en tema claro
val MustardLight = Color(0xFFFFD580) // Tu primario en tema oscuro / secundario en claro
val MustardPale = Color(0xFFFFF0C9) // Para containers en tema claro
val MustardDeep = Color(0xFF8A5D00) // Para containers en tema oscuro

// Neutros para Texto y Fondos
val BackgroundLight = Color(0xFFFFF8E7) // Un blanco hueso/crema
val BackgroundDark = Color(0xFF121212) // Un negro estándar para fondos oscuros
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1E1E1E) // Un gris oscuro para superficies en tema oscuro

val TextPrimaryLight = Color(0xFF1C1C1E) // Casi negro para texto en tema claro
val TextPrimaryDark = Color(0xFFE0E0E0) // Un gris muy claro para texto en tema oscuro
val TextSecondaryLight = Color(0xFF6D6D6D) // Gris medio para texto secundario en tema claro
val TextSecondaryDark = Color(0xFFB0B0B0) // Gris claro para texto secundario en tema oscuro

// Colores de Acento y Estado (puedes añadir más aquí si es necesario)
val AccentBlue = Color(0xFF1976D2) // Un azul para acentos
val AccentGreen = Color(0xFF388E3C) // Un verde para acentos o éxito

val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFD32F2F)
val WarningOrange = Color(0xFFFFA000) // Podrías usar MustardLight o un naranja diferente

// Colores "On" (para contraste)
val OnPrimaryLight = Color.White
val OnPrimaryDark = TextPrimaryLight // O un negro puro si MustardLight es muy claro

val OnAccent = Color.White
