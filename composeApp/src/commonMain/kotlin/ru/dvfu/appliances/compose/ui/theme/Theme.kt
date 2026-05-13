package ru.dvfu.appliances.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import ru.dvfu.appliances.model.datastore.ThemeMode
import ru.dvfu.appliances.model.datastore.UserDatastore

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
    content: @Composable () -> Unit,
) {
    val datastore: UserDatastore = koinInject()
    val systemDark = isSystemInDarkTheme()
    val themeMode by datastore.getThemeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    ApplyEdgeToEdge(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScheduleTypography,
        shapes = ScheduleShapes,
        content = content,
    )
}
