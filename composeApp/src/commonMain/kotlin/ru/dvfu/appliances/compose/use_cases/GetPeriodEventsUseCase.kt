package ru.dvfu.appliances.compose.use_cases

import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.Event

class GetPeriodEventsUseCase(
    private val eventsRepository: EventsRepository,
) {
    suspend operator fun invoke(
        dateStart: LocalDate,
        dateEnd: LocalDate,
    ) = flow<List<Event>> {
        eventsRepository.getAllEventsWithPeriod(dateStart, dateEnd).fold(
            onSuccess = { emit(it) },
            onFailure = { emit(listOf()) },
        )
    }
}
