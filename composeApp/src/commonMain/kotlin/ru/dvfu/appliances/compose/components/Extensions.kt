package ru.dvfu.appliances.compose.components

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DAY_NAMES_RU = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val MONTH_NAMES_RU_SHORT = listOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

fun Long.toDate(): String {
    val ldt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.dayOfMonth.toString().padStart(2, '0')}." +
        "${ldt.monthNumber.toString().padStart(2, '0')}." +
        "${ldt.year}"
}

fun Long.toDateWithWeek(): String {
    val ldt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val dow = DAY_NAMES_RU[ldt.dayOfWeek.ordinal]
    val month = MONTH_NAMES_RU_SHORT[ldt.monthNumber - 1]
    return "$dow, ${ldt.dayOfMonth} $month"
}

fun Long.toTime(): String {
    val ldt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
}
