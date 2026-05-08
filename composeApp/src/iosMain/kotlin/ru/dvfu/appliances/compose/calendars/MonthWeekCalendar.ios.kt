package ru.dvfu.appliances.compose.calendars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.model.repository.entity.CalendarEvent

@Composable
actual fun MonthWeekCalendar(
    viewModel: WeekCalendarViewModel,
    navController: NavController,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Month calendar — iOS placeholder",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
