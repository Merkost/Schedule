package ru.dvfu.appliances.model.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.utils.TimeConstants.ZONE

object TimeConstants {
    val MIN_EVENT_DURATION: Duration = 30.minutes
    val DEFAULT_EVENT_DURATION: Duration = 1.hours
    const val MINUTES_BEFORE_END: Int = 5

    val ZONE: TimeZone = TimeZone.of("Asia/Vladivostok")
}

private val MONTH_NAMES_RU = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

fun LocalDate.formatFull(): String =
    "$day ${MONTH_NAMES_RU[month.ordinal]} $year"

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(ZONE)

fun Long.toLocalTime(): LocalTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(ZONE).time

fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(ZONE).date

val LocalDateTime.toMillis: Long
    get() = this.toInstant(ZONE).toEpochMilliseconds()

val LocalDate.toMillis: Long
    get() = this.atStartOfDayIn(ZONE).toEpochMilliseconds()

fun LocalTime.toHoursAndMinutes(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

fun formattedTime(timeStart: LocalDateTime, timeEnd: LocalDateTime): String =
    "${timeStart.time.toHoursAndMinutes()} - ${timeEnd.time.toHoursAndMinutes()}"

fun formattedDate(date: LocalDate): String =
    "${date.day} ${MONTH_NAMES_RU[date.month.ordinal]}"

fun formattedDateTime(date: LocalDate, timeStart: LocalDateTime, timeEnd: LocalDateTime): String =
    "${formattedDate(date)}, ${formattedTime(timeStart, timeEnd)}"

fun formattedDateTimeStatus(
    date: LocalDate,
    timeStart: LocalDateTime,
    timeEnd: LocalDateTime,
    status: BookingStatus,
): String =
    "${formattedDate(date)}, ${formattedTime(timeStart, timeEnd)}, ${status.getName().uppercase()}"

fun formattedAppliance(name: String): String = "\"$name\""

fun formattedApplianceDateTime(
    name: String,
    date: LocalDate,
    timeStart: LocalDateTime,
    timeEnd: LocalDateTime,
): String =
    "${formattedAppliance(name)}, ${formattedDate(date)}, ${formattedTime(timeStart, timeEnd)}"

fun formattedApplianceDateTimeStatus(
    name: String,
    date: LocalDate,
    timeStart: LocalDateTime,
    timeEnd: LocalDateTime,
    status: BookingStatus,
): String =
    "${formattedAppliance(name)}, ${formattedDateTimeStatus(date, timeStart, timeEnd, status)}"

val LocalDateTime.toDateAndTime: String
    get() = "${date.day} ${MONTH_NAMES_RU[date.month.ordinal]}, ${time.toHoursAndMinutes()}"
