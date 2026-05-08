package ru.dvfu.appliances.compose.calendars

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.header.MonthState
import io.github.boguszpawlowski.composecalendar.selection.SelectionState
import ru.dvfu.appliances.compose.viewmodels.EventsState
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.User
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.time.format.TextStyle
import java.util.*

private fun java.time.LocalDate.toKx(): LocalDate = LocalDate(year, monthValue, dayOfMonth)

@Composable
fun <T : SelectionState> ScheduleCalendarDate(
    currentUser: User,
    state: DayState<T>,
    currentDayEvents: List<CalendarEvent>,
    onClick: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
    todayDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    selectionColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val date = state.date
    val dateKx = date.toKx()
    val selectionState = state.selectionState
    val isSelected = selectionState.isDateSelected(date)

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (state.isFromCurrentMonth) 4.dp else 0.dp
        ),
        border =
        if (currentDayEvents.any { it.status == BookingStatus.APPROVED }) BorderStroke(
            2.dp,
            if (currentDayEvents.any { it.user.userId == currentUser.userId && it.status == BookingStatus.APPROVED })
                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        ) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectionColor else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    onClick(dateKx)
                    selectionState.onDateSelected(date)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (dateKx == todayDate) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (dateKx == todayDate) 16.sp else 14.sp,
            )
            if (currentDayEvents.any { it.status == BookingStatus.NONE }) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        .size(6.dp),
                )
            }
        }
    }
}

@Composable
fun SchedulerMonthHeader(
    monthState: MonthState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.testTag("Decrement"),
            onClick = { monthState.currentMonth = monthState.currentMonth.minusMonths(1) },
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                modifier = Modifier.testTag("MonthLabel"),
                text = monthState.currentMonth.month
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                    .lowercase()
                    .replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = monthState.currentMonth.year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            modifier = Modifier.testTag("Increment"),
            onClick = { monthState.currentMonth = monthState.currentMonth.plusMonths(1) },
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
