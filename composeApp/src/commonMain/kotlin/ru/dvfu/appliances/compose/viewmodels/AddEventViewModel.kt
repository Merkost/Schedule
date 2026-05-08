package ru.dvfu.appliances.compose.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.use_cases.GetAppliancesUseCase
import ru.dvfu.appliances.compose.use_cases.GetEventTimeAvailabilityUseCase
import ru.dvfu.appliances.compose.utils.AvailabilityState
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.entity.*
import ru.dvfu.appliances.model.utils.TimeConstants.DEFAULT_EVENT_DURATION
import ru.dvfu.appliances.model.utils.TimeConstants.MIN_EVENT_DURATION
import ru.dvfu.appliances.model.utils.toMillis
import ru.dvfu.appliances.ui.ViewState
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.datetime.*
import ru.dvfu.appliances.model.utils.TimeConstants
import java.util.*

private val ZONE = TimeConstants.ZONE

private fun nowDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun nowTime(): LocalTime = nowDateTime().time

private fun today(): LocalDate =
    Clock.System.todayIn(TimeZone.currentSystemDefault())

private fun LocalDateTime.plusDuration(d: Duration): LocalDateTime =
    toInstant(ZONE).plus(d).toLocalDateTime(ZONE)

private fun durationBetween(a: LocalDateTime, b: LocalDateTime): Duration =
    b.toInstant(ZONE) - a.toInstant(ZONE)


class AddEventViewModel(
    private val selectedDate: LocalDate,
    private val eventsRepository: EventsRepository,
    private val getAppliancesUseCase: GetAppliancesUseCase,
    private val getEventTimeAvailabilityUseCase: GetEventTimeAvailabilityUseCase,
    private val userDatastore: UserDatastore,
    private val notificationManager: NotificationManager,
) : ViewModel() {

    private val _selectedAppliance = MutableStateFlow<Appliance?>(null)
    val selectedAppliance = _selectedAppliance.asStateFlow()

    private val _uiState = MutableStateFlow<UiState?>(null)
    val uiState = _uiState.asStateFlow()

    private val _autoApproveToggleEnabled = MutableStateFlow<Boolean>(false)
    val autoApproveToggleEnabled = _autoApproveToggleEnabled.asStateFlow()

    private val _autoApproveToggle = MutableStateFlow<Boolean>(false)
    val autoApproveToggle = _autoApproveToggle.asStateFlow()

    private val _appliancesState = MutableStateFlow<ViewState<List<Appliance>>>(ViewState.Loading)
    val appliancesState = _appliancesState.asStateFlow()

    private val currentUser = MutableStateFlow(User())

    val date = mutableStateOf(selectedDate)
    val timeStart = mutableStateOf<LocalDateTime>(
        if (selectedDate > today()) selectedDate.atTime(8, 0)
        else selectedDate.atTime(nowTime())
    )
    val timeEnd = mutableStateOf<LocalDateTime>(timeStart.value.plusDuration(DEFAULT_EVENT_DURATION))
    val commentary = mutableStateOf("")

    val isDurationError: MutableStateFlow<Boolean>
        get() = MutableStateFlow(
            timeEnd.value < timeStart.value || durationBetween(
                timeStart.value, timeEnd.value,
            ) < MIN_EVENT_DURATION
        )
    val duration: MutableStateFlow<String>
        get() {
            val dur = durationBetween(timeStart.value, timeEnd.value)
            val totalMinutes = dur.inWholeMinutes.coerceAtLeast(0)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val period = String.format(
                Locale.getDefault(), "%02d:%02d", hours, minutes,
            )
            return MutableStateFlow(period)
        }

    init {
        getCurrentUser()
        loadAppliances()
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            userDatastore.getCurrentUser.collect {
                currentUser.value = it
            }
        }
    }

    private fun loadAppliances() {
        viewModelScope.launch {
            getAppliancesUseCase.invoke().collect { result ->
                val appliances = result.getOrDefault(listOf()).filter { it.active }
                _appliancesState.value = ViewState.Success(appliances)
            }
        }
    }

    fun addEvent() {
        _uiState.value = UiState.InProgress
        viewModelScope.launch {
            val selectedAppliance = selectedAppliance.value
            if (isDurationError.value || selectedAppliance == null) {
                showError()
            } else {
                val availabilityResult = getEventTimeAvailabilityUseCase.invoke(
                    selectedAppliance.id,
                    eventDateAndTime = EventDateAndTime(
                        timeStart = timeStart.value.time,
                        timeEnd = timeEnd.value.time,
                        date = date.value
                    )
                ).single()

                when (availabilityResult) {
                    AvailabilityState.Available -> addNewEvent(
                        if (autoApproveToggle.value && autoApproveToggleEnabled.value)
                            prepareApprovedEvent(selectedAppliance)
                        else prepareEvent(selectedAppliance)

                    )
                    AvailabilityState.Error -> {
                        _uiState.value = UiState.Error
                        SnackbarManager.showMessage(Res.string.new_event_failed)
                    }
                    AvailabilityState.NotAvailable -> {
                        _uiState.value = UiState.Error
                        SnackbarManager.showMessage(Res.string.time_not_free)
                    }
                }
            }
        }
    }

    private fun prepareEvent(selectedAppliance: Appliance): Event {
        return Event(
            date = date.value.toMillis,
            timeCreated = nowDateTime().toMillis,
            timeStart = timeStart.value.toMillis,
            timeEnd = timeEnd.value.toMillis,
            commentary = commentary.value,
            applianceId = selectedAppliance.id,
            userId = currentUser.value.userId,
            status = BookingStatus.NONE,
        )
    }

    private fun prepareApprovedEvent(selectedAppliance: Appliance): Event {
        return Event(
            date = date.value.toMillis,
            timeCreated = nowDateTime().toMillis,
            timeStart = timeStart.value.toMillis,
            timeEnd = timeEnd.value.toMillis,
            commentary = commentary.value,
            applianceId = selectedAppliance.id,
            userId = currentUser.value.userId,
            managedTime = nowDateTime().toMillis,
            managerCommentary = "",
            managedById = currentUser.value.userId,
            status = BookingStatus.APPROVED,
        )
    }

    private fun addNewEvent(event: Event) {
        viewModelScope.launch {
            eventsRepository.addNewEvent(event).fold(
                onSuccess = {
                    notificationManager.newEvent(event)
                    SnackbarManager.showMessage(Res.string.add_event_success)
                    _uiState.value = UiState.Success
                },
                onFailure = {
                    SnackbarManager.showMessage(Res.string.add_event_failed)
                    _uiState.value = UiState.Error
                }
            )
        }
    }

    private fun showError() {
        when {
            durationBetween(timeStart.value, timeEnd.value) < MIN_EVENT_DURATION -> {
                SnackbarManager.showMessage(Res.string.duration_error)
            }
            selectedAppliance.value == null -> {
                SnackbarManager.showMessage(Res.string.appliance_not_chosen)
            }
            else -> SnackbarManager.showMessage(Res.string.error_occured)
        }
        _uiState.value = UiState.Error
    }

    fun onApplianceSelected(appliance: Appliance) {
        _selectedAppliance.value = appliance.takeIf { it != _selectedAppliance.value }
        if (selectedAppliance.value == null) {
            _autoApproveToggleEnabled.value = false
            _autoApproveToggle.value = false
        } else {
            appliance.isUserSuperuserOrAdmin(currentUser.value).let {
                _autoApproveToggleEnabled.value = it
                _autoApproveToggle.value = it
            }
        }
    }

    fun onAutoApproveToggleChanged(newBoolean: Boolean) {
        _autoApproveToggle.value = newBoolean
    }

    fun onCommentarySet(commentary: String) {
        this.commentary.value = commentary
    }

    fun onDateSet(date: LocalDate) {
        if (AppDebug.isDebug && date < today()) {
            SnackbarManager.showMessage(Res.string.past_day_error)
            return
        }
        this.date.value = LocalDate(date.year, date.month, date.dayOfMonth)
        if (date > today()) {
            timeStart.value = date.atTime(8, 0)
            timeEnd.value = timeStart.value.plusDuration(DEFAULT_EVENT_DURATION)
        } else {
            timeStart.value = date.atTime(nowTime())
            timeEnd.value = timeStart.value.plusDuration(DEFAULT_EVENT_DURATION)
        }
    }

    fun onTimeStartSet(time: LocalTime) {
        timeStart.value = date.value.atTime(time.hour, time.minute)
    }

    fun onTimeEndSet(time: LocalTime) {
        timeEnd.value = date.value.atTime(time.hour, time.minute)
    }

}