package ru.dvfu.appliances.compose.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.utils.EventMapper
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.calendars.CalendarType
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.use_cases.*
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.*
import ru.dvfu.appliances.model.utils.filterForUser
import ru.dvfu.appliances.model.utils.filterWeekEventsForUser
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.datetime.daysUntil


class WeekCalendarViewModel(
    private val eventsRepository: EventsRepository,
    private val userDatastore: UserDatastore,
    private val getPeriodEventsUseCase: GetPeriodEventsUseCase,
    private val getDateEventsUseCase: GetDateEventsUseCase,
    private val eventMapper: EventMapper,
    private val updateEventUseCase: UpdateEventUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.InProgress)
    val uiState = _uiState.asStateFlow()

    private val _managingUiState = MutableStateFlow<UiState>(UiState.Success)
    val managingUiState = _managingUiState.asStateFlow()

    private val _calendarType = MutableStateFlow<CalendarType>(CalendarType.MONTH)
    val calendarType = _calendarType.asStateFlow()

    private val _currentDate = MutableStateFlow<LocalDate>(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val currentDate = _currentDate.asStateFlow()

    private val _weekEvents = MutableStateFlow<List<CalendarEvent>>(listOf())
    val weekEvents = _weekEvents.asStateFlow()

    private val _reposEvents = MutableStateFlow<Set<Event>>(setOf())
    val selectedEvent = mutableStateOf<CalendarEvent?>(null)

    private val _currentUser = MutableStateFlow<User>(User())
    val currentUser = _currentUser.asStateFlow()

    private var _dayEvents =
        mutableStateMapOf<LocalDate, EventsState>(Pair(Clock.System.todayIn(TimeZone.currentSystemDefault()), EventsState.Loading))
    val dayEvents = _dayEvents

    /*private var _monthEvents =
        mutableStateMapOf<LocalDate, List<Event>>(Pair(Clock.System.todayIn(TimeZone.currentSystemDefault()), listOf()))
    val monthEvents = _monthEvents*/

    private var _monthEvents =
        MutableStateFlow(listOf<Event>())
    val monthEvents = _monthEvents
/*
    private val _dayEvents = MutableStateFlow<WeekEvents>(WeekEvents())
    val dayEvents: StateFlow<WeekEvents> = _dayEvents.asStateFlow()*/

    private val appliances = MutableStateFlow<List<Appliance>>(listOf())

    init {
        getDayEvents(Clock.System.todayIn(TimeZone.currentSystemDefault()))
        getCurrentUser()
        getCalendarTypeListener()
    }

    private fun getCalendarTypeListener() {
        viewModelScope.launch {
            userDatastore.getCalendarType.collect {
                _calendarType.value = it
            }
        }
    }

    private fun getDayEvents(date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())) {
        viewModelScope.launch {
            if (_dayEvents[date] !is EventsState.Loaded) {
                _dayEvents[date] = EventsState.Loading
            }
            getDateEventsUseCase(date).collectLatest {
                _reposEvents.value = (_reposEvents.value.plus(it))
                val mapped = eventMapper.mapEvents(it).filterForUser(currentUser.value)
                _dayEvents[date] = EventsState.Loaded(mapped)
            }
        }
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            userDatastore.getCurrentUser.collect {
                _currentUser.value = it
            }
        }
    }

    fun deleteEvent(eventToDelete: CalendarEvent) {
        viewModelScope.launch {
            eventsRepository.deleteEvent(eventToDelete).fold(
                onSuccess = {
                    SnackbarManager.showMessage(Res.string.event_delete_successfully)
                },
                onFailure = {
                    SnackbarManager.showMessage(Res.string.event_delete_failed)
                }
            )
        }
    }

    fun getWeekEvents(minDate: LocalDate, maxDate: LocalDate) {
        _uiState.value = UiState.InProgress
        viewModelScope.launch {
            _weekEvents.value = eventMapper.mapEvents(
                getPeriodEventsUseCase.invoke(
                    dateStart = minDate,
                    dateEnd = maxDate
                ).first()
            ).filterWeekEventsForUser(currentUser.value)
            _uiState.value = UiState.Success
        }
    }

    fun setCalendarType() {
        viewModelScope.launch {
            val newCalendarType = when (calendarType.value) {
                CalendarType.MONTH -> CalendarType.WEEK
                else -> CalendarType.MONTH
            }
            userDatastore.saveCalendarType(newCalendarType)
        }
    }

    fun onDateSelectionChanged(selectedDateList: List<LocalDate>) {
        selectedDateList.firstOrNull()?.let { selectedDate ->
            if (currentDate.value != selectedDate) {
                _currentDate.value = selectedDate
                getDayEvents(selectedDate)
            }
        }
    }

    fun onMonthChanged(year: Int, monthNumber: Int) {
        getEventsForMonth(year, monthNumber)
    }

    private fun getEventsForMonth(year: Int, monthNumber: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.InProgress
            val dates = monthDates(year, monthNumber)
            _dayEvents = _dayEvents.apply {
                dates.forEach { date ->
                    if (get(date) !is EventsState.Loaded) {
                        put(date, EventsState.Loading)
                    }
                }
            }
            getPeriodEventsUseCase(dates.first(), dates.last()).collectLatest { result ->
                _reposEvents.value = (_reposEvents.value.plus(result))
                val grouped = eventMapper.mapEvents(result).filterForUser(currentUser.value).groupBy { it.date }
                _dayEvents = _dayEvents.apply {
                    dates.forEach { date ->
                        put(date, EventsState.Loaded(grouped[date].orEmpty()))
                    }
                }
                _uiState.value = UiState.Success
            }
        }
    }


    fun onApproveClick(event: CalendarEvent, commentary: String) {
        updateEventStatus(BookingStatus.APPROVED, event, commentary)
    }

    fun onDeclineClick(event: CalendarEvent, commentary: String) {
        updateEventStatus(BookingStatus.DECLINED, event, commentary)
    }

    private fun updateEventStatus(
        bookingStatus: BookingStatus,
        event: CalendarEvent,
        managerCommentary: String,
    ) {
        _managingUiState.value = UiState.InProgress
        viewModelScope.launch {
            updateEventUseCase.updateEventStatusUseCase.invoke(
                event,
                bookingStatus,
                managerCommentary,
            ).first().fold(
                onSuccess = {
                    SnackbarManager.showMessage(Res.string.status_changed)
                    _managingUiState.value = UiState.Success
                },
                onFailure = {
                    SnackbarManager.showMessage(Res.string.change_status_failed)
                    _managingUiState.value = UiState.Error
                }
            )
        }
    }

}

private fun monthDates(year: Int, monthNumber: Int): List<LocalDate> {
    val firstOfMonth = LocalDate(year, monthNumber, 1)
    val firstOfNext = if (monthNumber == 12) LocalDate(year + 1, 1, 1)
        else LocalDate(year, monthNumber + 1, 1)
    val len = firstOfMonth.daysUntil(firstOfNext)
    return (1..len).map { LocalDate(year, monthNumber, it) }
}

sealed class EventsState() {
    object Loading : EventsState()
    class Loaded(val events: List<CalendarEvent>) : EventsState()
}

