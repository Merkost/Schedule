package ru.dvfu.appliances.model.datasource.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.Event
import ru.dvfu.appliances.model.utils.toLocalDate
import java.time.LocalDate
import java.time.ZoneId

class MockEventsRepository : EventsRepository {

    private val eventsStore = MutableStateFlow(FakeData.generateEvents())

    override suspend fun addNewEvent(event: Event): Result<Unit> {
        eventsStore.update { it + event }
        return Result.success(Unit)
    }

    override suspend fun getAllEvents(): Flow<List<Event>> = eventsStore

    override suspend fun deleteEvent(eventToDelete: CalendarEvent): Result<Unit> {
        eventsStore.update { list -> list.filter { it.id != eventToDelete.id } }
        return Result.success(Unit)
    }

    override suspend fun setNewTimeEnd(eventId: String, timeEnd: Long): Result<Unit> {
        eventsStore.update { list ->
            list.map { if (it.id == eventId) it.copy(timeEnd = timeEnd) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun setNewEventStatus(
        eventId: String,
        newStatus: BookingStatus,
        managerCommentary: String,
        managerId: String
    ): Result<Unit> {
        eventsStore.update { list ->
            list.map {
                if (it.id == eventId) it.copy(
                    status = newStatus,
                    managerCommentary = managerCommentary,
                    managedById = managerId,
                    managedTime = System.currentTimeMillis()
                ) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun updateEvent(eventId: String, data: Map<String, Any?>): Result<Unit> {
        eventsStore.update { list ->
            list.map { event ->
                if (event.id == eventId) applyEventUpdates(event, data) else event
            }
        }
        return Result.success(Unit)
    }

    override suspend fun getAllEventsForDay(date: LocalDate): Flow<List<Event>> =
        eventsStore.map { list -> list.filter { it.date.toLocalDate() == date } }

    override suspend fun getApplianceEventsAfterTime(applianceId: String, time: Long): Result<List<Event>> =
        Result.success(eventsStore.value.filter { it.applianceId == applianceId && it.timeStart > time })

    override suspend fun getApplianceDateEvents(applianceId: String, date: LocalDate): Result<List<Event>> =
        Result.success(eventsStore.value.filter { it.applianceId == applianceId && it.date.toLocalDate() == date })

    override suspend fun getAllEventsWithPeriod(dateStart: LocalDate, dateEnd: LocalDate): Result<List<Event>> {
        val zone = ZoneId.systemDefault()
        val startMillis = dateStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = dateEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return Result.success(eventsStore.value.filter { it.date in startMillis until endMillis })
    }

    override suspend fun deleteAllApplianceEvents(id: String): Result<Unit> {
        eventsStore.update { list -> list.filter { it.applianceId != id } }
        return Result.success(Unit)
    }

    override suspend fun hasAtLeastOneEvent(applianceId: String): Boolean =
        eventsStore.value.any { it.applianceId == applianceId }

    private fun applyEventUpdates(event: Event, data: Map<String, Any?>): Event {
        var updated = event
        data.forEach { (key, value) ->
            updated = when (key) {
                "commentary" -> updated.copy(commentary = value as? String ?: updated.commentary)
                "timeStart" -> updated.copy(timeStart = value as? Long ?: updated.timeStart)
                "timeEnd" -> updated.copy(timeEnd = value as? Long ?: updated.timeEnd)
                "status" -> updated.copy(status = value as? BookingStatus ?: updated.status)
                "managerCommentary" -> updated.copy(managerCommentary = value as? String ?: updated.managerCommentary)
                "managedById" -> updated.copy(managedById = value as? String)
                "managedTime" -> updated.copy(managedTime = value as? Long)
                else -> updated
            }
        }
        return updated
    }
}
