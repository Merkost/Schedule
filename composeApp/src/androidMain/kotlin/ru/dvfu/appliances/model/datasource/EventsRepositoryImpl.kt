package ru.dvfu.appliances.model.datasource

import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.Event
import ru.dvfu.appliances.model.utils.FirestoreCollections
import ru.dvfu.appliances.model.utils.toMillis
import java.time.LocalDate
import java.time.LocalDateTime

class EventsRepositoryImpl(
    private val collections: FirestoreCollections,
    private val notificationManager: NotificationManager,
) : EventsRepository {

    override suspend fun addNewEvent(event: Event): Result<Unit> = runCatching {
        collections.events().document(event.id).set(event)
    }

    override suspend fun deleteEvent(eventToDelete: CalendarEvent): Result<Unit> = runCatching {
        collections.events().document(eventToDelete.id).delete()
        notificationManager.eventDeleted(eventToDelete)
    }

    override suspend fun setNewTimeEnd(eventId: String, timeEnd: Long): Result<Unit> = runCatching {
        collections.events().document(eventId).update("timeEnd" to timeEnd)
    }

    override suspend fun setNewEventStatus(
        eventId: String,
        newStatus: BookingStatus,
        managerCommentary: String,
        managerId: String,
    ): Result<Unit> = runCatching {
        collections.events().document(eventId).update(
            "status" to newStatus.name,
            "managerCommentary" to managerCommentary,
            "managedById" to managerId,
            "managedTime" to LocalDateTime.now().toMillis,
        )
    }

    override suspend fun updateEvent(eventId: String, data: Map<String, Any?>): Result<Unit> = runCatching {
        collections.events().document(eventId).update(*data.toList().toTypedArray())
    }

    override suspend fun getEventById(eventId: String): Flow<Result<Event>> =
        collections.events().document(eventId).snapshots
            .map { snap ->
                if (snap.exists) Result.success(snap.data<Event>())
                else Result.failure(NoSuchElementException("Event $eventId not found"))
            }
            .catch { emit(Result.failure(it)) }

    override suspend fun getAllEvents(): Flow<List<Event>> =
        collections.events().snapshots
            .map { qs -> qs.documents.map { it.data<Event>() } }
            .catch { emit(emptyList()) }

    override suspend fun getAllEventsForDay(date: LocalDate): Flow<List<Event>> =
        collections.events()
            .where { "date" equalTo date.toMillis }
            .snapshots
            .map { qs -> qs.documents.map { it.data<Event>() } }
            .catch { emit(emptyList()) }

    override suspend fun getApplianceEventsAfterTime(applianceId: String, time: Long): Result<List<Event>> =
        runCatching {
            collections.events()
                .where {
                    ("applianceId" equalTo applianceId) and ("timeEnd" greaterThan time)
                }
                .get()
                .documents.map { it.data<Event>() }
        }

    override suspend fun getApplianceDateEvents(applianceId: String, date: LocalDate): Result<List<Event>> =
        runCatching {
            collections.events()
                .where {
                    ("applianceId" equalTo applianceId) and ("date" equalTo date.toMillis)
                }
                .get()
                .documents.map { it.data<Event>() }
        }

    override suspend fun getAllEventsWithPeriod(dateStart: LocalDate, dateEnd: LocalDate): Result<List<Event>> =
        runCatching {
            collections.events()
                .where {
                    ("date" greaterThanOrEqualTo dateStart.toMillis) and ("date" lessThanOrEqualTo dateEnd.toMillis)
                }
                .get()
                .documents.map { it.data<Event>() }
        }

    override suspend fun deleteAllApplianceEvents(id: String): Result<Unit> = runCatching {
        val events = collections.events()
            .where { "applianceId" equalTo id }
            .get()
            .documents.map { it.data<Event>() }
        events.forEach { event ->
            collections.events().document(event.id).delete()
        }
    }

    override suspend fun hasAtLeastOneEvent(applianceId: String): Boolean = runCatching {
        collections.events()
            .where { "applianceId" equalTo applianceId }
            .limit(1)
            .get()
            .documents
            .isNotEmpty()
    }.getOrDefault(false)
}
