package ru.dvfu.appliances.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.TabPosition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import kotlin.math.absoluteValue

fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
): Modifier = composed {
    if (tabPositions.isEmpty()) return@composed this
    val currentPage = pagerState.currentPage.coerceIn(0, tabPositions.lastIndex)
    val current = tabPositions[currentPage]
    val offset = pagerState.currentPageOffsetFraction
    val next = tabPositions.getOrNull(currentPage + if (offset >= 0f) 1 else -1)
    val fraction = offset.absoluteValue

    val width = if (next != null) lerp(current.width, next.width, fraction) else current.width
    val x = if (next != null) {
        if (offset >= 0f) lerp(current.left, next.left, fraction)
        else lerp(current.left, next.left, fraction)
    } else current.left

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = x)
        .width(width)
}

private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp = Dp(start.value + (stop.value - start.value) * fraction)
