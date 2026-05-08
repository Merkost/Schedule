package ru.dvfu.appliances.compose.calendars.event_calendar

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

private class EventDataModifier(
    val positionedEvent: PositionedEvent,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = positionedEvent
}

fun Modifier.eventData(positionedEvent: PositionedEvent) = this.then(EventDataModifier(positionedEvent))

fun formatEventTime(time: LocalTime): String =
    "${time.hour}:${time.minute.toString().padStart(2, '0')}"

fun formatHour(time: LocalTime): String = time.hour.toString()

private val DAY_OF_WEEK_SHORT_RU = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val MONTH_SHORT_RU = listOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

fun formatDay(day: LocalDate): String {
    val dow = DAY_OF_WEEK_SHORT_RU[day.dayOfWeek.isoDayNumber - 1]
    val month = MONTH_SHORT_RU[day.month.ordinal]
    return "$dow, $month ${day.day}"
}
