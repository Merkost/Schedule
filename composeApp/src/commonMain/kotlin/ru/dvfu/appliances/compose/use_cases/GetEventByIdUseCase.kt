package ru.dvfu.appliances.compose.use_cases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.dvfu.appliances.compose.utils.EventMapper
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.CalendarEvent

class GetEventByIdUseCase(
    private val eventsRepository: EventsRepository,
    private val eventMapper: EventMapper,
) {
    suspend operator fun invoke(eventId: String): Flow<Result<CalendarEvent>> = flow {
        eventsRepository.getEventById(eventId).collect { result ->
            result.fold(
                onSuccess = { emit(Result.success(eventMapper.mapEvent(it))) },
                onFailure = { emit(Result.failure(it)) }
            )
        }
    }
}
