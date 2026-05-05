package ru.dvfu.appliances.compose.calendars

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import ru.dvfu.appliances.navigation.Arguments
import ru.dvfu.appliances.navigation.MainDestinations
import ru.dvfu.appliances.compose.calendars.event_calendar.Schedule
import ru.dvfu.appliances.compose.home.HomeTopBar
import ru.dvfu.appliances.compose.home.SelectedDate
import ru.dvfu.appliances.compose.navigate
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.isAnonymousOrGuest
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

@Composable
fun EventCalendar(
    viewModel: WeekCalendarViewModel,
    navController: NavController,
    onEventLongClick: (CalendarEvent) -> Unit,
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
) {
    val currentDate = remember { LocalDate.now() }
    val minDate = remember { currentDate.with(WeekFields.of(Locale("ru-RU")).dayOfWeek(), 1L) }
    val maxDate = remember { currentDate.with(WeekFields.of(Locale("ru-RU")).dayOfWeek(), 7L) }

    LaunchedEffect(minDate, maxDate) {
        viewModel.getWeekEvents(minDate, maxDate)
    }

    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.weekEvents.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            HomeTopBar(
                uiState = uiState,
                onBookingListOpen = {
                    navController.navigate(
                        MainDestinations.BOOKING_LIST,
                        Arguments.DATE to SelectedDate()
                    )
                },
                onCalendarSelected = viewModel::setCalendarType,
                onRetry = { viewModel.getWeekEvents(minDate, maxDate) }
            )
        },
        floatingActionButton = {
            if (!currentUser.isAnonymousOrGuest) {
                FloatingActionButton(
                    onClick = { navController.navigate(MainDestinations.ADD_EVENT) })
                { Icon(Icons.Default.Add, "") }
            }
        },
    ) {
        Schedule(
            modifier = Modifier.padding(it),
            calendarEvents = events,
            minDate = minDate,
            maxDate = maxDate,
            onEventClick = {
                navController.navigate(
                    MainDestinations.EVENT_INFO,
                    Arguments.EVENT to it
                )
            },
            onEventLongClick = onEventLongClick,
            verticalScrollState = verticalScrollState,
            horizontalScrollState = horizontalScrollState
        )
    }

}
