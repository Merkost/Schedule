package ru.dvfu.appliances.compose.calendars

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.model.repository.entity.CalendarEvent

@Composable
expect fun MonthWeekCalendar(
    viewModel: WeekCalendarViewModel,
    navController: NavController,
    onEventClick: (CalendarEvent) -> Unit,
)
