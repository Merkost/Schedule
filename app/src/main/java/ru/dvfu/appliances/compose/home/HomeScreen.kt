package ru.dvfu.appliances.compose.home

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.parcelize.Parcelize
import org.koin.androidx.compose.getViewModel
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.compose.calendars.CalendarType
import ru.dvfu.appliances.compose.calendars.EventCalendar
import ru.dvfu.appliances.compose.calendars.MonthWeekCalendar
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.calendars.event_calendar.EventTimeFormatter
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.compose.components.views.DefaultDialog
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.canManageEvent
import ru.dvfu.appliances.model.utils.Constants.TIME_TO_EXIT
import ru.dvfu.appliances.model.utils.showToast
import java.time.LocalDate

@Composable
fun HomeScreen(
    navController: NavController,
    backPress: () -> Unit,
) {
    val viewModel: WeekCalendarViewModel = getViewModel()
    val currentUser by viewModel.currentUser.collectAsState()
    val calendarType by viewModel.calendarType.collectAsState()
    val context = LocalContext.current

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var eventOptionDialogOpened by remember { mutableStateOf(false) }
    if (eventOptionDialogOpened) EventOptionDialog(
        calendarEvent = viewModel.selectedEvent.value,
        onDismiss = { eventOptionDialogOpened = false },
        onDelete = viewModel::deleteEvent
    )

    BackPressHandler(upPress = { (context as MainActivity).finishAffinity() })

    Crossfade(targetState = calendarType) { type ->
        when (type) {
            CalendarType.MONTH -> {
                MonthWeekCalendar(
                    viewModel = viewModel,
                    navController = navController,
                    onEventClick = {
                        navController.navigate(
                            MainDestinations.EVENT_INFO,
                            Arguments.EVENT to it
                        )
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

@Parcelize
data class SelectedDate(val value: LocalDate = LocalDate.now()) : Parcelable

@Composable
fun BackPressHandler(
    upPress: () -> Unit
) {
    val context = LocalContext.current
    var lastPressed by remember { mutableStateOf(0L) }

    BackHandler(true) {

        val currentMillis = System.currentTimeMillis()
        if (currentMillis - lastPressed < TIME_TO_EXIT) {
            upPress()
        } else {
            showToast(
                context.applicationContext,
                context.getString(R.string.app_exit_message)
            )
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
        title = stringResource(id = R.string.schedule),
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
                                color = MaterialTheme.colors.surface,
                                modifier = Modifier.size(24.dp)
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
            secondaryText = "${calendarEvent.timeStart.format(EventTimeFormatter)} - ${
                calendarEvent.timeEnd.format(EventTimeFormatter)
            }\n${calendarEvent.commentary}",
            onDismiss = onDismiss,
            neutralButtonText = stringResource(id = R.string.delete),
            onNeutralClick = { onDelete(calendarEvent); onDismiss() }
        )
    }
}
