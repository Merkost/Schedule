package ru.dvfu.appliances.compose.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = BrandOnPrimaryContainer,
    inversePrimary = BrandPrimaryLight,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryLight,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    tertiaryContainer = BrandTertiaryLight,
    onTertiaryContainer = BrandOnTertiaryContainer,
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorLight,
    onErrorContainer = BrandOnErrorContainer,
    background = BrandBackground,
    onBackground = BrandOnBackground,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    scrim = BrandScrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandPrimaryDark,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = BrandPrimaryLight,
    inversePrimary = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = BrandSecondaryDark,
    secondaryContainer = BrandSecondaryDark,
    onSecondaryContainer = BrandSecondaryLight,
    tertiary = BrandTertiaryLight,
    onTertiary = BrandTertiaryDark,
    tertiaryContainer = BrandTertiaryDark,
    onTertiaryContainer = BrandTertiaryLight,
    error = BrandErrorLight,
    onError = BrandErrorDark,
    errorContainer = BrandErrorDark,
    onErrorContainer = BrandErrorLight,
    background = BrandBackgroundDark,
    onBackground = BrandOnBackgroundDark,
    surface = BrandSurfaceDark,
    onSurface = BrandOnSurfaceDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark,
    scrim = BrandScrim,
)

@Composable
fun ScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScheduleTypography,
        shapes = ScheduleShapes,
        content = content,
    )
}
