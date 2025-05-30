package com.jcmateus.kalisfit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = MustardDark,                  // Título y Polilínea en VisualCard
    onPrimary = OnPrimaryLight,
    primaryContainer = MustardPale,
    onPrimaryContainer = TextPrimaryLight,

    secondary = AccentBlue,
    onSecondary = OnAccent,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = TextPrimaryLight,

    tertiary = AccentGreen,
    onTertiary = OnAccent,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = TextPrimaryLight,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,                 // Fondo de la tarjeta del mapa dentro de VisualCard
    onSurface = TextPrimaryLight,           // Texto sobre la tarjeta del mapa

    // --- Clave para la UserActivityVisualCard ---
    surfaceVariant = MustardPale,           // Fondo principal de UserActivityVisualCard
    onSurfaceVariant = TextPrimaryLight,    // Texto de detalles (InfoRowVisual) en UserActivityVisualCard

    outline = TextSecondaryLight,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = MustardLight,
    onPrimary = TextPrimaryDark, // Ajustado para mejor contraste si TextPrimaryLight es muy oscuro
    primaryContainer = MustardDeep,
    onPrimaryContainer = MustardPale,

    secondary = AccentBlue,
    onSecondary = OnAccent, // Asumiendo que OnAccent (Blanco) funciona bien sobre AccentBlue
    secondaryContainer = Color(0xFF0A2A47),
    onSecondaryContainer = Color(0xFFD1E6FF),

    tertiary = AccentGreen,
    onTertiary = OnAccent,
    tertiaryContainer = Color(0xFF103112),
    onTertiaryContainer = Color(0xFFC8E6C9),

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,

    // Para Dark Theme en UserActivityVisualCard (si lo implementas)
    surfaceVariant = Color(0xFF2C2A25), // Manteniendo tu original para tema oscuro
    onSurfaceVariant = TextSecondaryDark, // Manteniendo tu original para tema oscuro

    outline = TextSecondaryDark,
    error = ErrorRed,
    onError = TextPrimaryDark // Ajustado para mejor contraste, ej. negro sobre rojo
)

@Composable
fun KalisFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Considera ponerlo en 'false' temporalmente para depurar tus colores estáticos
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // FORZAR TEMA CLARO PARA DEPURAR LA TARJETA VISUAL:
        // else -> LightColorScheme // Descomenta esta línea y comenta las dos de arriba para forzar LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asumo que tienes Typography definida
        content = content
    )
}