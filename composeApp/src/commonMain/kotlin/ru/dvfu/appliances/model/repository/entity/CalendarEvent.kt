package ru.dvfu.appliances.model.repository.entity

import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import ru.dvfu.appliances.compose.use_cases.GetApplianceUseCase
import ru.dvfu.appliances.compose.use_cases.GetUserUseCase
import ru.dvfu.appliances.model.utils.TimeConstants.MINUTES_BEFORE_END
import ru.dvfu.appliances.model.utils.TimeConstants.ZONE
import ru.dvfu.appliances.model.utils.toLocalDate
import ru.dvfu.appliances.model.utils.toLocalDateTime

data class CalendarEvent(
    val id: String = "",
    val date: LocalDate,
    val timeCreated: LocalDateTime,
    var timeStart: LocalDateTime,
    var timeEnd: LocalDateTime,
    var commentary: String = "",
    val user: User = User(),
    var appliance: Appliance = Appliance(),
    val managedUser: User? = null,
    val managedTime: LocalDateTime? = null,
    val managerCommentary: String = "",
    val status: BookingStatus = BookingStatus.NONE,
)

fun CalendarEvent.canBeRefused(currentUser: User): Boolean {
    val now = Clock.System.now()
    val end = timeEnd.toInstant(ZONE)
    val minutesUntilEnd = (end - now).inWholeMinutes
    return user.userId == currentUser.userId && minutesUntilEnd > MINUTES_BEFORE_END
}

fun User.canManageEvent(event: CalendarEvent): Boolean {
    val nowLdt = Clock.System.now().toLocalDateTime(ZONE)
    return (isAdmin || event.appliance.superuserIds.contains(userId)) &&
        event.timeEnd > nowLdt
}

suspend fun Event.toCalendarEvent(
    getUserUseCase: GetUserUseCase,
    getApplianceUseCase: GetApplianceUseCase,
): CalendarEvent = CalendarEvent(
    id = id,
    date = date.toLocalDate(),
    timeCreated = timeCreated.toLocalDateTime(),
    timeStart = timeStart.toLocalDateTime(),
    timeEnd = timeEnd.toLocalDateTime(),
    commentary = commentary,
    user = getUserUseCase(userId).first().getOrDefault(User()),
    appliance = getApplianceUseCase(applianceId).first().getOrDefault(Appliance()),
    managedUser = managedById?.let { getUserUseCase(it).first().getOrDefault(User()) },
    managedTime = managedTime?.toLocalDateTime(),
    managerCommentary = managerCommentary,
    status = status,
)
