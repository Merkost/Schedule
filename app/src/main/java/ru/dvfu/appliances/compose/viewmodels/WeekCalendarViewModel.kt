package ru.dvfu.appliances.compose.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dvfu.appliances.R
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth


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

    private val _currentDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val currentDate = _currentDate.asStateFlow()

    private val _weekEvents = MutableStateFlow<List<CalendarEvent>>(listOf())
    val weekEvents = _weekEvents.asStateFlow()

    private val _reposEvents = MutableStateFlow<Set<Event>>(setOf())
    val selectedEvent = mutableStateOf<CalendarEvent?>(null)

    private val _currentUser = MutableStateFlow<User>(User())
    val currentUser = _currentUser.asStateFlow()

    private var _dayEvents =
        mutableStateMapOf<LocalDate, EventsState>(Pair(LocalDate.now(), EventsState.Loading))
    val dayEvents = _dayEvents

    /*private var _monthEvents =
        mutableStateMapOf<LocalDate, List<Event>>(Pair(LocalDate.now(), listOf()))
    val monthEvents = _monthEvents*/

    private var _monthEvents =
        MutableStateFlow(listOf<Event>())
    val monthEvents = _monthEvents
/*
    private val _dayEvents = MutableStateFlow<WeekEvents>(WeekEvents())
    val dayEvents: StateFlow<WeekEvents> = _dayEvents.asStateFlow()*/

    private val appliances = MutableStateFlow<List<Appliance>>(listOf())

    init {
        getDayEvents(LocalDate.now())
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

    private fun getDayEvents(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _dayEvents = _dayEvents.apply {
                get(date)?.let { put(date, EventsState.Loading) }
            }
            getDateEventsUseCase(date).collectLatest {
                _reposEvents.value = (_reposEvents.value.plus(it))
                val dayEvents = eventMapper.mapEvents(it).filterForUser(currentUser.value)
                _dayEvents = _dayEvents.apply {
                    replace(date, EventsState.Loaded(dayEvents))?.let {
                        put(date, EventsState.Loaded(dayEvents))
                    }
                }
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
                    SnackbarManager.showMessage(R.string.event_delete_successfully)
                },
                onFailure = {
                    SnackbarManager.showMessage(R.string.event_delete_failed)
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

    fun onMonthChanged(currentMonth: YearMonth) {
        getEventsForMonth(currentMonth)
    }

    private fun getEventsForMonth(currentMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = UiState.InProgress
            val dates = currentMonth.getDates()
            _dayEvents = _dayEvents.apply {
                dates.forEach { date ->
                    if (get(date) !is EventsState.Loaded) {
                        put(date, EventsState.Loading)
                    }
                }
            }
            getPeriodEventsUseCase(dates.first(), dates.last()).collectLatest { result ->
                _reposEvents.value = (_reposEvents.value.plus(result))
                val dayEvents = eventMapper.mapEvents(result).filterForUser(currentUser.value).groupBy { it.date }
                _dayEvents = _dayEvents.apply {
                    dayEvents.forEach { (date, events) ->
                        replace(date, EventsState.Loaded(events))?.let {
                            put(date, EventsState.Loaded(events))
                        }
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
                    SnackbarManager.showMessage(R.string.status_changed)
                    _managingUiState.value = UiState.Success
                },
                onFailure = {
                    SnackbarManager.showMessage(R.string.change_status_failed)
                    _managingUiState.value = UiState.Error
                }
            )
        }
    }

}

private fun YearMonth.getDates(): List<LocalDate> = (1..lengthOfMonth()).map { atDay(it) }

sealed class EventsState() {
    object Loading : EventsState()
    class Loaded(val events: List<CalendarEvent>) : EventsState()
}

