package ru.dvfu.appliances.model.repository

import kotlinx.coroutines.flow.Flow
import ru.dvfu.appliances.model.repository.entity.*
import java.time.LocalDate

interface EventsRepository {
    suspend fun addNewEvent(event: Event): Result<Unit>

    suspend fun getEventById(eventId: String): Flow<Result<Event>>

    suspend fun getAllEvents(): Flow<List<Event>>
    suspend fun deleteEvent(eventToDelete: CalendarEvent): Result<Unit>
    suspend fun setNewTimeEnd(eventId: String, timeEnd: Long): Result<Unit>
    suspend fun setNewEventStatus(
        eventId: String,
        newStatus: BookingStatus,
        managerCommentary: String,
        managerId: String
    ): Result<Unit>
    suspend fun updateEvent(eventId: String, data: Map<String, Any?>): Result<Unit>

    suspend fun getAllEventsForDay(date: LocalDate): Flow<List<Event>>

    suspend fun getApplianceEventsAfterTime(applianceId: String, time: Long): Result<List<Event>>
    suspend fun getApplianceDateEvents(applianceId: String, date: LocalDate): Result<List<Event>>
    suspend fun getAllEventsWithPeriod(dateStart: LocalDate, dateEnd: LocalDate): Result<List<Event>>
    suspend fun deleteAllApplianceEvents(id: String): Result<Unit>
    suspend fun hasAtLeastOneEvent(applianceId: String): Boolean
}