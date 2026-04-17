package ru.dvfu.appliances.compose.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

data class CustomColors(
    val secondaryTextColor: Color,
    val secondaryIconColor: Color,
)

val MaterialTheme.customColors: CustomColors
    @Composable
    @ReadOnlyComposable
    get() = CustomColors(
        secondaryTextColor = colorScheme.onSurfaceVariant,
        secondaryIconColor = colorScheme.onSurfaceVariant,
    )

fun readableOn(
    background: Color,
    onLight: Color = Color(0xFF111111),
    onDark: Color = Color(0xFFFFFFFF),
): Color = if (background.luminance() > 0.5f) onLight else onDark

fun Color.readableOrFallback(surface: Color, fallback: Color): Color {
    if (alpha < 0.1f) return fallback
    return if (abs(luminance() - surface.luminance()) < 0.25f) fallback else this
}
