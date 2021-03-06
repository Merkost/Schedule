package ru.dvfu.appliances.model.datasource

import android.util.Log
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.Event
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.RepositoryCollections
import ru.dvfu.appliances.model.utils.suspendCoroutineWithTimeout
import ru.dvfu.appliances.model.utils.toMillis
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EventsRepositoryImpl(
    private val dbCollections: RepositoryCollections,
    private val notificationManager: NotificationManager,
) : EventsRepository {

    private var TAG = "EventsFirestoreDatabase"

    override suspend fun addNewEvent(event: Event) =
        suspendCoroutineWithTimeout { continuation ->
            dbCollections.getEventsCollection().document(event.id).set(event)
                .addOnCompleteListener {
                    if (it.isSuccessful) continuation.resume(Result.success(Unit))
                    else continuation.resume(Result.failure(it.exception ?: Throwable()))
                }
        }

    override suspend fun deleteEvent(eventToDelete: CalendarEvent) =
        suspendCoroutineWithTimeout { continuation ->
            dbCollections.getEventsCollection().document(eventToDelete.id).delete()
                .addOnCompleteListener(simpleOnCompleteListener(continuation) {
                    runBlocking { notificationManager.eventDeleted(eventToDelete) }
                })
        }

    override suspend fun setNewTimeEnd(eventId: String, timeEnd: Long) =
        suspendCoroutineWithTimeout { continuation ->
            dbCollections.getEventsCollection().document(eventId).update("timeEnd", timeEnd)
                .addOnCompleteListener(simpleOnCompleteListener(continuation))
        }

    override suspend fun setNewEventStatus(
        eventId: String,
        newStatus: BookingStatus,
        managerCommentary: String,
        managerId: String
    ) = suspendCoroutineWithTimeout { continuation ->
        dbCollections.getEventsCollection().document(eventId).update(
            mapOf(
                "status" to newStatus,
                "managerCommentary" to managerCommentary,
                "managedById" to managerId,
                "managedTime" to LocalDateTime.now().toMillis
            )
        ).addOnCompleteListener(simpleOnCompleteListener(continuation))
    }

    override suspend fun updateEvent(eventId: String, data: Map<String, Any?>) =
        suspendCoroutineWithTimeout { continuation ->
            dbCollections.getEventsCollection().document(eventId).update(data)
                .addOnCompleteListener(simpleOnCompleteListener(continuation))
        }

    override suspend fun getAllEvents(): Flow<List<Event>> = callbackFlow {
        val subscription = dbCollections.getEventsCollection().addSnapshotListener { value, error ->
            value?.let { trySend(value.toObjects<Event>()) }
        }

        awaitClose { subscription.remove() }
    }

    override suspend fun getAllEventsForDay(date: LocalDate): Flow<List<Event>> = callbackFlow {
        val subscription = dbCollections.getEventsCollection()
            .whereEqualTo("date", date.toMillis)
            .addSnapshotListener { value, _ ->
                value?.let { trySend(value.toObjects<Event>()) }
            }

        awaitClose { subscription.remove() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getEventsSuccessListener(scope: ProducerScope<List<Event>>): EventListener<QuerySnapshot> =
        EventListener<QuerySnapshot> { snapshots, error ->
            if (error != null) {
                Log.d("Schedule", "Get all events listener error", error)
                return@EventListener
            }

            if (snapshots != null) {
                val events = snapshots.toObjects<Event>()
                scope.trySend(events)
            }
        }

    override suspend fun getApplianceEventsAfterTime(applianceId: String, time: Long) =
        suspendCoroutine<Result<List<Event>>> { continuation ->
            dbCollections.getEventsCollection()
                .whereEqualTo("applianceId", applianceId)
                .whereGreaterThan("timeEnd", time)
                .get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val events = it.result.toObjects<Event>()
                        continuation.resume(Result.success(events))
                    } else continuation.resume(Result.failure(it.exception ?: Throwable()))
                }
        }

    override suspend fun getApplianceDateEvents(
        applianceId: String,
        date: LocalDate
    ): Result<List<Event>> = suspendCoroutine<Result<List<Event>>> { continuation ->
        dbCollections.getEventsCollection()
            .whereEqualTo("applianceId", applianceId)
            .whereEqualTo("date", date.toMillis)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val events = it.result.toObjects<Event>()
                    continuation.resume(Result.success(events))
                } else continuation.resume(Result.failure(it.exception ?: Throwable()))
            }
    }

    override suspend fun getAllEventsWithPeriod(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Result<List<Event>> =
        suspendCoroutine<Result<List<Event>>> { continuation ->
            dbCollections.getEventsCollection()
                .whereGreaterThanOrEqualTo("date", dateStart.toMillis)
                .whereLessThanOrEqualTo("date", dateEnd.toMillis)
                .get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val events = it.result.toObjects<Event>()
                        continuation.resume(Result.success(events))
                    } else continuation.resume(Result.failure(it.exception ?: Throwable()))
                }
        }

    override suspend fun deleteAllApplianceEvents(applianceId: String): Result<Unit> =
        suspendCoroutineWithTimeout(Duration.ofMinutes(1).toMillis()) { continuation ->
            dbCollections.getEventsCollection()
                .whereEqualTo("applianceId", applianceId).get().continueWith {
                    it.result.toObjects<Event>().forEach { event ->
                        dbCollections.getEventsCollection().document(event.id).delete()
                    }
                }.addOnCompleteListener {
                    if (it.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else continuation.resume(Result.failure(it.exception ?: Throwable()))
                }
        }

    override suspend fun hasAtLeastOneEvent(applianceId: String) =
        suspendCoroutine<Boolean> { continuation ->
            dbCollections.getEventsCollection().limit(1).whereEqualTo("applianceId", applianceId)
                .get().addOnCompleteListener {
                    val result = it.result.toObjects<Event>()
                    if (result.isNotEmpty()) continuation.resume(true)
                    else continuation.resume(false)
                }
        }

}
