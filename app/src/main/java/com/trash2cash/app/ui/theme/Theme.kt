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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Trash2Cash color palette - green theme for environmental focus
private val Green10 = Color(0xFF003314)
private val Green20 = Color(0xFF006627)
private val Green30 = Color(0xFF00993A)
private val Green40 = Color(0xFF00CC4D)
private val Green50 = Color(0xFF00FF60)
private val Green60 = Color(0xFF33FF80)
private val Green70 = Color(0xFF66FFA0)
private val Green80 = Color(0xFF99FFC0)
private val Green90 = Color(0xFFCCFFE0)
private val Green95 = Color(0xFFE6FFF0)
private val Green99 = Color(0xFFF9FFFC)

private val Orange10 = Color(0xFF331100)
private val Orange20 = Color(0xFF662200)
private val Orange30 = Color(0xFF993300)
private val Orange40 = Color(0xFFCC4400)
private val Orange50 = Color(0xFFFF5500)
private val Orange60 = Color(0xFFFF7733)
private val Orange70 = Color(0xFFFF9966)
private val Orange80 = Color(0xFFFFBB99)
private val Orange90 = Color(0xFFFFDDCC)

private val Blue10 = Color(0xFF001122)
private val Blue20 = Color(0xFF002244)
private val Blue30 = Color(0xFF003366)
private val Blue40 = Color(0xFF004488)
private val Blue50 = Color(0xFF0055AA)
private val Blue60 = Color(0xFF3377BB)
private val Blue70 = Color(0xFF6699CC)
private val Blue80 = Color(0xFF99BBDD)
private val Blue90 = Color(0xFFCCDDEE)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Orange40,
    onSecondary = Color.White,
    secondaryContainer = Orange90,
    onSecondaryContainer = Orange10,
    tertiary = Blue40,
    onTertiary = Color.White,
    tertiaryContainer = Blue90,
    onTertiaryContainer = Blue10,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Green99,
    onBackground = Green10,
    surface = Green99,
    onSurface = Green10,
    surfaceVariant = Color(0xFFF0F4F0),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    inverseOnSurface = Color(0xFFEEF2EE),
    inverseSurface = Color(0xFF2E312E),
    inversePrimary = Green60,
    surfaceTint = Green40,
    outlineVariant = Color(0xFFC0C9C1),
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
    tertiary = Blue60,
    onTertiary = Blue20,
    tertiaryContainer = Blue30,
    onTertiaryContainer = Blue90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Green10,
    onBackground = Green90,
    surface = Green10,
    onSurface = Green90,
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938C),
    inverseOnSurface = Green10,
    inverseSurface = Green90,
    inversePrimary = Green40,
    surfaceTint = Green60,
    outlineVariant = Color(0xFF404943),
    scrim = Color(0xFF000000),
)

@Composable
fun Trash2CashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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