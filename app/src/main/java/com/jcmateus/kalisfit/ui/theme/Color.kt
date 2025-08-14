package com.jcmateus.kalisfit.ui.theme

import androidx.compose.ui.graphics.Color


// --- Paleta de Colores Base ---
// Primarios (Mostaza)
val MustardDark = Color(0xFFC2850B)      // Primario en tema claro (para título, polilínea)
val MustardLight = Color(0xFFFFD580)     // Primario en tema oscuro / acento en claro
val MustardPale = Color(0xFFFFF0C9)      // Para containers en tema claro Y AHORA PARA surfaceVariant
val MustardDeep = Color(0xFF8A5D00)      // Para containers en tema oscuro
// Neutros para Texto y Fondos
val BackgroundLight = Color(0xFFFFF8E7)  // Fondo general de la app en tema claro
val BackgroundDark = Color(0xFF121212)   // Fondo general de la app en tema oscuro
val SurfaceLight = Color(0xFFFFFFFF)     // Superficie general en tema claro
val SurfaceDark = Color(0xFF1E1E1E)      // Superficie general en tema oscuro
// Colores de Texto
val TextPrimaryLight = Color(0xFF1C1C1E)  // Casi negro para texto principal en tema claro
// TAMBIÉN USADO PARA onSurfaceVariant (texto de detalles en la tarjeta visual)
val TextPrimaryDark = Color(0xFFE0E0E0)   // Gris muy claro para texto principal en tema oscuro
val TextSecondaryLight = Color(0xFF6D6D6D) // Gris medio para texto secundario general en tema claro
// (No usado directamente en la tarjeta visual ahora, pero mantenido por consistencia)
val TextSecondaryDark = Color(0xFFB0B0B0)  // Gris claro para texto secundario general en tema oscuro
// Colores de Acento y Estado
val AccentBlue = Color(0xFF1976D2)       // Para rol 'secondary'
val AccentGreen = Color(0xFF388E3C)      // Para rol 'tertiary'
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFD32F2F)
val WarningOrange = Color(0xFFFFA000)
// Colores "On" (para texto/iconos sobre colores base)
val OnPrimaryLight = Color.White         // Texto/iconos sobre MustardDark
val OnPrimaryDark = TextPrimaryLight     // Texto/iconos sobre MustardLight (casi negro sobre mostaza claro)
val OnAccent = Color.White               // Texto/iconos sobre AccentBlue y AccentGreen
