package com.trash2cash.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Trash2Cash Enhanced color palette - vibrant green theme for environmental focus
private val Green10 = Color(0xFF002910)
private val Green20 = Color(0xFF005227)
private val Green30 = Color(0xFF007B3D)
private val Green40 = Color(0xFF00A854)
private val Green50 = Color(0xFF00D66B)
private val Green60 = Color(0xFF33E088)
private val Green70 = Color(0xFF66EBA5)
private val Green80 = Color(0xFF99F5C2)
private val Green90 = Color(0xFFCCFAE0)
private val Green95 = Color(0xFFE6FDF0)
private val Green99 = Color(0xFFF5FFF9)

private val Emerald = Color(0xFF10B981)
private val Teal = Color(0xFF14B8A6)
private val Lime = Color(0xFF84CC16)

private val Orange10 = Color(0xFF3D1700)
private val Orange20 = Color(0xFF7A2E00)
private val Orange30 = Color(0xFFB74500)
private val Orange40 = Color(0xFFFF5C00)
private val Orange50 = Color(0xFFFF8533)
private val Orange60 = Color(0xFFFF9F66)
private val Orange70 = Color(0xFFFFB999)
private val Orange80 = Color(0xFFFFD3CC)
private val Orange90 = Color(0xFFFFE9E0)

private val Blue10 = Color(0xFF001E2F)
private val Blue20 = Color(0xFF003C5E)
private val Blue30 = Color(0xFF005A8D)
private val Blue40 = Color(0xFF0078BC)
private val Blue50 = Color(0xFF0096EB)
private val Blue60 = Color(0xFF33ABEF)
private val Blue70 = Color(0xFF66C0F3)
private val Blue80 = Color(0xFF99D5F7)
private val Blue90 = Color(0xFFCCEAFB)

// Gradient colors for modern UI
val GreenGradient = listOf(
    Color(0xFF00D66B),
    Color(0xFF00A854)
)

val OrangeGradient = listOf(
    Color(0xFFFF8533),
    Color(0xFFFF5C00)
)

val TealGradient = listOf(
    Color(0xFF14B8A6),
    Color(0xFF0D9488)
)

val SkyGradient = listOf(
    Color(0xFF0EA5E9),
    Color(0xFF0284C7)
)

// Custom gradient brushes
val PrimaryGradientBrush = Brush.horizontalGradient(GreenGradient)
val SecondaryGradientBrush = Brush.horizontalGradient(OrangeGradient)
val TertiaryGradientBrush = Brush.horizontalGradient(TealGradient)

private val LightColorScheme = lightColorScheme(
    primary = Green50,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Orange40,
    onSecondary = Color.White,
    secondaryContainer = Orange90,
    onSecondaryContainer = Orange10,
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color.White,
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFFD4D4D4),
    inverseOnSurface = Color(0xFFFAFAFA),
    inverseSurface = Color(0xFF262626),
    inversePrimary = Green60,
    surfaceTint = Green50,
    outlineVariant = Color(0xFFE5E5E5),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Green60,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Orange60,
    onSecondary = Orange20,
    secondaryContainer = Orange30,
    onSecondaryContainer = Orange90,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF0F766E),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFFCCFBF1),
    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF7F1D1D),
    onError = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF525252),
    inverseOnSurface = Color(0xFF171717),
    inverseSurface = Color(0xFFFAFAFA),
    inversePrimary = Green40,
    surfaceTint = Green60,
    outlineVariant = Color(0xFF404040),
    scrim = Color(0xFF000000),
)

@Composable
fun Trash2CashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}