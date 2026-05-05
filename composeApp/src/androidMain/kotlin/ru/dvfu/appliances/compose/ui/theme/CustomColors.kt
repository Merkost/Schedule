package ru.dvfu.appliances.compose.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

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
