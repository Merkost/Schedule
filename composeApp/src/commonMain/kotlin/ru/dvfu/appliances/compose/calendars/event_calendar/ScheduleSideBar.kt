package ru.dvfu.appliances.compose.calendars.event_calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime

private fun LocalTime.toMinutes(): Int = hour * 60 + minute
private fun localTimeOfMinutes(total: Int): LocalTime {
    val m = ((total % (24 * 60)) + 24 * 60) % (24 * 60)
    return LocalTime(m / 60, m % 60)
}

internal fun LocalTime.plusHours(h: Long): LocalTime = localTimeOfMinutes(toMinutes() + h.toInt() * 60)
internal fun LocalTime.truncatedToHours(): LocalTime = LocalTime(hour, 0)
internal fun minutesBetween(a: LocalTime, b: LocalTime): Long = (b.toMinutes() - a.toMinutes()).toLong()

@Composable
fun ScheduleSidebar(
    hourHeight: Dp,
    modifier: Modifier = Modifier,
    minTime: LocalTime = LocalTime(0, 0),
    maxTime: LocalTime = LocalTime(23, 59, 59),
    label: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
) {
    val numMinutes = minutesBetween(minTime, maxTime).toInt() + 1
    val numHours = numMinutes / 60
    val firstHour = minTime.truncatedToHours()
    val firstHourOffsetMinutes = if (firstHour == minTime) 0L else minutesBetween(minTime, firstHour.plusHours(1))
    val firstHourOffset = hourHeight * (firstHourOffsetMinutes / 60f)
    val startTime = if (firstHour == minTime) firstHour else firstHour.plusHours(1)
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(firstHourOffset))
        repeat(numHours) { i ->
            Box(modifier = Modifier.height(hourHeight)) {
                label(startTime.plusHours(i.toLong()))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleSidebarPreview() {
    ScheduleSidebar(hourHeight = 64.dp)
}
