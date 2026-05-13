package ru.dvfu.appliances.model.utils

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.loadingModifier(
    enabled: Boolean = true,
) = composed {
    if (!enabled) return@composed Modifier
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200)),
        label = "shimmerProgress",
    )
    val base = Color.LightGray
    val highlight = Color.White.copy(alpha = 0.6f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 400f - 200f, 0f),
        end = Offset(progress * 400f + 200f, 0f),
    )
    Modifier
        .clip(CircleShape)
        .background(brush)
}
