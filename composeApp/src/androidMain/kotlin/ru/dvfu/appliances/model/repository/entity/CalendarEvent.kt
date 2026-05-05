package ru.dvfu.appliances.model.repository.entity

import kotlinx.coroutines.flow.first
import ru.dvfu.appliances.compose.use_cases.GetApplianceUseCase
import ru.dvfu.appliances.compose.use_cases.GetUserUseCase
import ru.dvfu.appliances.model.utils.TimeConstants.MINUTES_BEFORE_END
import ru.dvfu.appliances.model.utils.toLocalDate
import ru.dvfu.appliances.model.utils.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
    val timeMins = LocalDateTime.now().until(timeEnd, ChronoUnit.MINUTES)
    return user.userId == currentUser.userId && timeMins > MINUTES_BEFORE_END
}

fun User.canManageEvent(event: CalendarEvent): Boolean {
    return (isAdmin || event.appliance.superuserIds.contains(userId)) &&
        event.timeEnd.isAfter(LocalDateTime.now())
}

suspend fun Event.toCalendarEvent(
    getUserUseCase: GetUserUseCase,
    getApplianceUseCase: GetApplianceUseCase,
): CalendarEvent = run {
    CalendarEvent(
        id = id,
        date = date.toLocalDate(),
        timeCreated = timeCreated.toLocalDateTime(),
        timeStart = timeStart.toLocalDateTime(),
        timeEnd = timeEnd.toLocalDateTime(),
        commentary = commentary,
        user = getUserUseCase(userId).first().getOrDefault(User()),
        appliance = getApplianceUseCase(applianceId).first().getOrDefault(Appliance()),
        managedUser = managedById?.let {
            getUserUseCase(managedById).first().getOrDefault(User())
        },
        managedTime = managedTime?.toLocalDateTime(),
        managerCommentary = managerCommentary,
        status = status,
    )
}
