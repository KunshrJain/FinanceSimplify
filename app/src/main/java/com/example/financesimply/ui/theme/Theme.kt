package com.example.financesimply.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = PureWhite,
    onPrimaryContainer = PureBlack,
    secondary = SurfaceGray,
    onSecondary = PureBlack,
    secondaryContainer = SurfaceGray,
    onSecondaryContainer = PureBlack,
    tertiary = TextGray,
    onTertiary = PureBlack,
    error = PureWhite,
    onError = PureBlack,
    errorContainer = PureWhite,
    onErrorContainer = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = TextGray,
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF1A1A1A),
    inverseSurface = PureWhite,
    inverseOnSurface = PureBlack,
    inversePrimary = PureBlack,
    surfaceTint = PureBlack
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = PureBlack,
    onPrimaryContainer = PureWhite,
    secondary = SurfaceGray,
    onSecondary = PureBlack,
    secondaryContainer = SurfaceGray,
    onSecondaryContainer = PureBlack,
    tertiary = TextGray,
    onTertiary = PureWhite,
    error = PureBlack,
    onError = PureWhite,
    errorContainer = PureBlack,
    onErrorContainer = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextGray,
    outline = Color(0xFFE5E5E5),
    outlineVariant = SurfaceGray,
    inverseSurface = PureBlack,
    inverseOnSurface = PureWhite,
    inversePrimary = PureWhite,
    surfaceTint = PureWhite
)

@Composable
fun SpendSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpendSenseTypography,
        content = content
    )
}
