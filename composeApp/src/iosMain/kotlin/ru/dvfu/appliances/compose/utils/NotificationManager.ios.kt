package ru.dvfu.appliances.compose.utils

import ru.dvfu.appliances.compose.viewmodels.EventDateAndTime
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.Event
import ru.dvfu.appliances.model.repository.entity.Roles
import ru.dvfu.appliances.model.repository.entity.User

class NoopNotificationManager : NotificationManager {
    override suspend fun applianceDeleted(appliance: Appliance) {}
    override suspend fun eventUpdated(event: CalendarEvent, data: Map<String, Any?>) {}
    override suspend fun eventDeleted(event: CalendarEvent) {}
    override suspend fun newEvent(newEvent: Event) {}
    override suspend fun newEventStatus(event: CalendarEvent, newStatus: BookingStatus) {}
    override suspend fun eventTimeChanged(event: CalendarEvent, eventDateAndTime: EventDateAndTime) {}
    override suspend fun newUserRole(user: User, role: Roles) {}
}
