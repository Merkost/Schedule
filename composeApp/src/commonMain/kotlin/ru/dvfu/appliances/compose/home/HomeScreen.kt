package ru.dvfu.appliances.compose.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.navigation.EventInfoRoute
import ru.dvfu.appliances.compose.calendars.CalendarType
import ru.dvfu.appliances.compose.calendars.EventCalendar
import ru.dvfu.appliances.compose.calendars.MonthWeekCalendar
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.calendars.event_calendar.formatEventTime
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.compose.components.views.DefaultDialog
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.canManageEvent
import ru.dvfu.appliances.model.utils.Constants.TIME_TO_EXIT
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.platform.NotificationPermissionRequest
import ru.dvfu.appliances.platform.PlatformBackHandler
import ru.dvfu.appliances.platform.finishApp
import ru.dvfu.appliances.platform.showToast
import kotlin.time.Clock

@Composable
fun HomeScreen(
    navController: NavController,
    backPress: () -> Unit,
) {
    val viewModel: WeekCalendarViewModel = koinViewModel()
    val currentUser by viewModel.currentUser.collectAsState()
    val calendarType by viewModel.calendarType.collectAsState()

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    NotificationPermissionRequest()

    var eventOptionDialogOpened by remember { mutableStateOf(false) }
    if (eventOptionDialogOpened) EventOptionDialog(
        calendarEvent = viewModel.selectedEvent.value,
        onDismiss = { eventOptionDialogOpened = false },
        onDelete = viewModel::deleteEvent
    )

    BackPressHandler(upPress = { finishApp() })

    Crossfade(targetState = calendarType) { type ->
        when (type) {
            CalendarType.MONTH -> {
                MonthWeekCalendar(
                    viewModel = viewModel,
                    navController = navController,
                    onEventClick = {
                        navController.navigate(EventInfoRoute(eventId = it.id))
                    }
                )
            }
            CalendarType.WEEK -> {
                EventCalendar(
                    viewModel = viewModel,
                    navController = navController,
                    onEventLongClick = {
                        if (currentUser.canManageEvent(it)) {
                            viewModel.selectedEvent.value = it
                            eventOptionDialogOpened = true
                        }
                    },
                    verticalScrollState = verticalScrollState,
                    horizontalScrollState = horizontalScrollState
                )
            }
        }
    }
}

@Composable
fun BackPressHandler(
    upPress: () -> Unit
) {
    val exitMessage = stringResource(Res.string.app_exit_message)
    var lastPressed by remember { mutableStateOf(0L) }

    PlatformBackHandler(enabled = true) {
        val currentMillis = Clock.System.now().toEpochMilliseconds()
        if (currentMillis - lastPressed < TIME_TO_EXIT) {
            upPress()
        } else {
            showToast(exitMessage)
        }
        lastPressed = currentMillis
    }
}

@Composable
fun HomeTopBar(
    uiState: UiState,
    onBookingListOpen: () -> Unit,
    onCalendarSelected: () -> Unit,
    onRetry: () -> Unit
) {
    ScheduleAppBar(
        title = stringResource(Res.string.schedule),
        navigationIcon = {
            Crossfade(targetState = uiState) {
                when (it) {
                    UiState.Error -> {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.ErrorOutline, "")
                        }
                    }
                    UiState.InProgress -> {
                        IconButton(onClick = {}) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                    UiState.Success -> {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.CloudDone, "")
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = { onBookingListOpen() }) {
                Icon(Icons.Default.Book, Icons.Default.Book.name)
            }
            IconButton(onClick = { onCalendarSelected() }) {
                Icon(Icons.Default.ChangeCircle, Icons.Default.ChangeCircle.name)
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EventOptionDialog(
    calendarEvent: CalendarEvent?,
    onDelete: (CalendarEvent) -> Unit,
    onDismiss: () -> Unit
) {
    calendarEvent?.let {
        DefaultDialog(
            primaryText = calendarEvent.appliance.name,
            secondaryText = "${formatEventTime(calendarEvent.timeStart.time)} - ${
                formatEventTime(calendarEvent.timeEnd.time)
            }\n${calendarEvent.commentary}",
            onDismiss = onDismiss,
            neutralButtonText = stringResource(Res.string.delete),
            onNeutralClick = { onDelete(calendarEvent); onDismiss() }
        )
    }
}

