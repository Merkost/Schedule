package ru.dvfu.appliances.model.datasource

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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

    private val log = Logger.withTag("EventsRepo")

    private fun decodeEvents(docs: List<dev.gitlive.firebase.firestore.DocumentSnapshot>, where: String): List<Event> {
        val out = mutableListOf<Event>()
        docs.forEach { d ->
            runCatching { out += d.data<Event>() }
                .onFailure { log.e(it) { "decode Event failed in $where docId=${d.id}" } }
        }
        if (out.size != docs.size) {
            log.e { "decode partial in $where: ${out.size}/${docs.size} docs decoded" }
        }
        return out
    }

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
                if (snap.exists) runCatching { snap.data<Event>() }
                    .onFailure { log.e(it) { "getEventById deser failed eventId=$eventId" } }
                else Result.failure(NoSuchElementException("Event $eventId not found"))
            }
            .catch { e ->
                log.e(e) { "getEventById flow failed eventId=$eventId" }
                emit(Result.failure(e))
            }

    override suspend fun getAllEvents(): Flow<List<Event>> =
        collections.events().snapshots
            .onStart { log.d { "getAllEvents subscribed" } }
            .map { qs ->
                val list = decodeEvents(qs.documents, "getAllEvents")
                log.d { "getAllEvents emit count=${list.size}" }
                list
            }
            .catch { e ->
                log.e(e) { "getAllEvents flow failed" }
                emit(emptyList())
            }

    override suspend fun getAllEventsForDay(date: LocalDate): Flow<List<Event>> =
        collections.events()
            .where { "date" equalTo date.toMillis }
            .snapshots
            .onStart { log.d { "getAllEventsForDay subscribed date=$date" } }
            .map { qs ->
                val list = decodeEvents(qs.documents, "getAllEventsForDay")
                log.d { "getAllEventsForDay emit count=${list.size} date=$date" }
                list
            }
            .catch { e ->
                log.e(e) { "getAllEventsForDay flow failed date=$date" }
                emit(emptyList())
            }

    override suspend fun getApplianceEventsAfterTime(applianceId: String, time: Long): Result<List<Event>> =
        runCatching {
            val docs = collections.events()
                .where {
                    ("applianceId" equalTo applianceId) and ("timeEnd" greaterThan time)
                }
                .get()
                .documents
            decodeEvents(docs, "getApplianceEventsAfterTime")
        }.onFailure { log.e(it) { "getApplianceEventsAfterTime failed applianceId=$applianceId" } }

    override suspend fun getApplianceDateEvents(applianceId: String, date: LocalDate): Result<List<Event>> =
        runCatching {
            val docs = collections.events()
                .where {
                    ("applianceId" equalTo applianceId) and ("date" equalTo date.toMillis)
                }
                .get()
                .documents
            decodeEvents(docs, "getApplianceDateEvents")
        }.onFailure { log.e(it) { "getApplianceDateEvents failed applianceId=$applianceId date=$date" } }

    override suspend fun getAllEventsWithPeriod(dateStart: LocalDate, dateEnd: LocalDate): Result<List<Event>> =
        runCatching {
            val docs = collections.events()
                .where {
                    ("date" greaterThanOrEqualTo dateStart.toMillis) and ("date" lessThanOrEqualTo dateEnd.toMillis)
                }
                .get()
                .documents
            val list = decodeEvents(docs, "getAllEventsWithPeriod")
            log.d { "getAllEventsWithPeriod result count=${list.size} period=$dateStart..$dateEnd" }
            list
        }.onFailure { log.e(it) { "getAllEventsWithPeriod failed period=$dateStart..$dateEnd" } }

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
