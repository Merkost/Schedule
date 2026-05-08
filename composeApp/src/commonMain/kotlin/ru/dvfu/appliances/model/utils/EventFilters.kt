package ru.dvfu.appliances.model.utils

import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.Roles
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.repository.entity.canManageEvent

fun List<CalendarEvent>.filterForUser(currentUser: User): List<CalendarEvent> =
    when (currentUser.role) {
        Roles.USER.ordinal -> filter {
            currentUser.canManageEvent(it) ||
                it.user.userId == currentUser.userId ||
                it.status == BookingStatus.APPROVED
        }
        Roles.ADMIN.ordinal -> this
        else -> filter { it.status == BookingStatus.APPROVED }
    }

fun List<CalendarEvent>.filterWeekEventsForUser(currentUser: User): List<CalendarEvent> =
    when (currentUser.role) {
        Roles.USER.ordinal -> filter {
            it.status == BookingStatus.APPROVED &&
                (currentUser.canManageEvent(it) || it.user.userId == currentUser.userId)
        }
        Roles.ADMIN.ordinal -> filter { it.status == BookingStatus.APPROVED }
        else -> filter { it.status == BookingStatus.APPROVED }
    }
