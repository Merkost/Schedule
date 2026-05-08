package ru.dvfu.appliances.compose.use_cases.event

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import ru.dvfu.appliances.compose.components.toDate
import ru.dvfu.appliances.compose.components.toTime
import ru.dvfu.appliances.compose.use_cases.GetEventTimeAvailabilityUseCase
import ru.dvfu.appliances.compose.utils.AvailabilityState
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.compose.viewmodels.EventDateAndTime
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.utils.toLocalDate
import ru.dvfu.appliances.model.utils.toLocalDateTime
import ru.dvfu.appliances.model.utils.toLocalTime
import ru.dvfu.appliances.model.utils.toMillis
import kotlin.coroutines.suspendCoroutine

class UpdateTimeUseCase(
    private val eventsRepository: EventsRepository,
    private val getEventTimeAvailabilityUseCase: GetEventTimeAvailabilityUseCase,
    private val notificationManager: NotificationManager,
) {
    suspend operator fun invoke(
        event: CalendarEvent,
        eventDateAndTime: EventDateAndTime,
    ) = flow<EventTimeUpdateResult> {

        val isTimeFree = checkNewEventTime(eventDateAndTime).single()

        val availabilityState = getEventTimeAvailabilityUseCase(
            applianceId = event.appliance.id,
            eventDateAndTime = eventDateAndTime,
            event = event
        ).single()
        when (availabilityState) {
            AvailabilityState.Available -> {
                eventsRepository.updateEvent(
                    event.id, mapOf(
                        "date" to eventDateAndTime.date.toMillis,
                        "timeStart" to kotlinx.datetime.LocalDateTime(eventDateAndTime.date, eventDateAndTime.timeStart).toMillis,
                        "timeEnd" to kotlinx.datetime.LocalDateTime(eventDateAndTime.date, eventDateAndTime.timeEnd).toMillis
                    )
                ).fold(
                    onSuccess = {
                        notificationManager.eventTimeChanged(event, eventDateAndTime)
                        emit(EventTimeUpdateResult.Success)
                    },
                    onFailure = {
                        emit(EventTimeUpdateResult.Error)
                    }
                )
            }
            AvailabilityState.Error -> emit(EventTimeUpdateResult.Error)
            AvailabilityState.NotAvailable -> emit(EventTimeUpdateResult.TimeNotFree)
        }

    }

    private suspend fun checkNewEventTime(eventDateAndTime: EventDateAndTime) = flow<Boolean> {
        eventsRepository.getAllEventsWithPeriod(
            dateStart = eventDateAndTime.date,
            dateEnd = eventDateAndTime.date
        ).fold(
            onSuccess = {
                if (it.isEmpty()) {
                    emit(true)
                } else {
                    val result = it.find { event ->
                        val startTime = event.timeStart.toLocalTime()
                        val endTime = event.timeEnd.toLocalTime()
                        (startTime < eventDateAndTime.timeStart || startTime > eventDateAndTime.timeEnd) &&
                                (endTime < eventDateAndTime.timeStart || endTime > eventDateAndTime.timeEnd)
                    }
                    emit(result == null)
                }
            },
            onFailure = {
                emit(false)
            }
        )
    }

}

sealed class EventTimeUpdateResult {
    object Success : EventTimeUpdateResult()
    object TimeNotFree : EventTimeUpdateResult()
    object Error : EventTimeUpdateResult()
}
