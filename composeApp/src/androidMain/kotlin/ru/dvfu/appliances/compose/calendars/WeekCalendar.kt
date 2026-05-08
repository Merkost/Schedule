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
import ru.dvfu.appliances.navigation.AddEventRoute
import ru.dvfu.appliances.navigation.BookingListRoute
import ru.dvfu.appliances.navigation.EventInfoRoute
import ru.dvfu.appliances.compose.calendars.event_calendar.Schedule
import ru.dvfu.appliances.compose.home.HomeTopBar
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.isAnonymousOrGuest
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@Composable
fun EventCalendar(
    viewModel: WeekCalendarViewModel,
    navController: NavController,
    onEventLongClick: (CalendarEvent) -> Unit,
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
) {
    val currentDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val minDate = remember {
        val daysFromMonday = currentDate.dayOfWeek.isoDayNumber - 1
        currentDate.minus(daysFromMonday, DateTimeUnit.DAY)
    }
    val maxDate = remember {
        val daysToSunday = 7 - currentDate.dayOfWeek.isoDayNumber
        currentDate.plus(daysToSunday, DateTimeUnit.DAY)
    }

    LaunchedEffect(minDate, maxDate) {
        viewModel.getWeekEvents(minDate, maxDate)
    }

    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.weekEvents.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            HomeTopBar(
                uiState = uiState,
                onBookingListOpen = {
                    navController.navigate(BookingListRoute)
                },
                onCalendarSelected = viewModel::setCalendarType,
                onRetry = { viewModel.getWeekEvents(minDate, maxDate) }
            )
        },
        floatingActionButton = {
            if (!currentUser.isAnonymousOrGuest) {
                FloatingActionButton(
                    onClick = { navController.navigate(AddEventRoute(dateEpochDay = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong())) })
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
                navController.navigate(EventInfoRoute(eventId = it.id))
            },
            onEventLongClick = onEventLongClick,
            verticalScrollState = verticalScrollState,
            horizontalScrollState = horizontalScrollState
        )
    }

}
