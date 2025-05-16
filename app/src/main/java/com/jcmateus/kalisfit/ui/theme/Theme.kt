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
    primary = MustardDark,
    onPrimary = OnPrimaryLight,
    primaryContainer = MustardPale,
    onPrimaryContainer = TextPrimaryLight, // O un MustardDark más oscuro
    secondary = AccentBlue, // O puedes seguir usando MustardLight si prefieres monocromático
    onSecondary = OnAccent,
    secondaryContainer = Color(0xFFE3F2FD), // Un azul muy pálido
    onSecondaryContainer = TextPrimaryLight,
    tertiary = AccentGreen, // Opcional, puedes usar otro color o un derivado de mostaza
    onTertiary = OnAccent,
    tertiaryContainer = Color(0xFFE8F5E9), // Un verde muy pálido
    onTertiaryContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFFAF0DC), // Una variante de superficie un poco más oscura que el fondo
    onSurfaceVariant = TextSecondaryLight,
    outline = TextSecondaryLight, // O un gris más claro
    error = ErrorRed,
    onError = Color.White,
    // Puedes añadir más aquí: errorContainer, onErrorContainer, etc.
)

private val DarkColorScheme = darkColorScheme(
    primary = MustardLight,
    onPrimary = TextPrimaryLight, // Un negro sobre mostaza claro funciona bien
    primaryContainer = MustardDeep,
    onPrimaryContainer = MustardPale, // O un blanco/amarillo muy claro
    secondary = AccentBlue, // Mantén consistencia con el tema claro si es el mismo rol
    onSecondary = OnAccent,
    secondaryContainer = Color(0xFF0A2A47), // Un azul oscuro para container
    onSecondaryContainer = Color(0xFFD1E6FF),
    tertiary = AccentGreen,
    onTertiary = OnAccent,
    tertiaryContainer = Color(0xFF103112),
    onTertiaryContainer = Color(0xFFC8E6C9),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2C2A25), // Una variante de superficie un poco más clara que el fondo oscuro
    onSurfaceVariant = TextSecondaryDark,
    outline = TextSecondaryDark,
    error = ErrorRed,
    onError = TextPrimaryLight, // Negro sobre rojo en tema oscuro puede ser mejor
)

@Composable
fun KalisFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Mantén esto si quieres Material You en A12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Para forzar un tema específico durante el desarrollo de la Card, puedes hacer:
    // val forcedLightTheme = LightColorScheme
    // val forcedDarkTheme = DarkColorScheme // (pero con colores onPrimary, etc. bien definidos)

    MaterialTheme(
        colorScheme = colorScheme, // o colorScheme = forcedLightTheme para probar
        typography = Typography, // Asumo que tienes Typography definida
        content = content
    )
}