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
    secondary = PrimaryAccentVariant,
    tertiary = BudgetSafe,
    background = PureBlack,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onTertiary = PureBlack,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = TextGray,
    outline = DividerGray,
    outlineVariant = DividerGray,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = PureWhite,
    error = BudgetDanger,
    errorContainer = BudgetDanger.copy(alpha = 0.1f),
    onErrorContainer = BudgetDanger
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    secondary = Color(0xFF444444),
    tertiary = BudgetSafe,
    background = PureWhite,
    surface = Color(0xFFF5F5F5),
    surfaceVariant = PureWhite,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onTertiary = PureWhite,
    onBackground = PureBlack,
    onSurface = PureBlack,
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFDDDDDD),
    outlineVariant = Color(0xFFDDDDDD),
    primaryContainer = Color(0xFFEEEEEE),
    onPrimaryContainer = PureBlack,
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
