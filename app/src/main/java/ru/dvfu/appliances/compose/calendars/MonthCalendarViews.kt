package ru.dvfu.appliances.compose.calendars

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.util.fastAny
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.header.MonthState
import io.github.boguszpawlowski.composecalendar.selection.SelectionState
import ru.dvfu.appliances.compose.ui.theme.Blue500
import ru.dvfu.appliances.compose.viewmodels.EventsState
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.User
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun <T : SelectionState> ScheduleCalendarDate(
    currentUser: User,
    state: DayState<T>,
    currentDayEvents: List<CalendarEvent>,
    onClick: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
    todayDate: LocalDate = LocalDate.now(),
    selectionColor: Color = MaterialTheme.colors.primaryVariant,
) {
    val date = state.date
    val selectionState = state.selectionState
    val isSelected = selectionState.isDateSelected(date)

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        shape = CircleShape,
        elevation = if (state.isFromCurrentMonth) 4.dp else 0.dp,
        border =
        if (currentDayEvents.any { it.status == BookingStatus.APPROVED }) BorderStroke(
            2.dp,
            if (currentDayEvents.any { it.user.userId == currentUser.userId && it.status == BookingStatus.APPROVED })
                MaterialTheme.colors.secondary else MaterialTheme.colors.primary
        ) else null,
        backgroundColor = if (isSelected) selectionColor else MaterialTheme.colors.surface
    ) {
        Box(
            modifier = Modifier.clickable {
                onClick(date)
                selectionState.onDateSelected(date)
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (date == todayDate) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (date == todayDate) 16.sp else 14.sp,
            )
        }

        if (currentDayEvents.any { it.status == BookingStatus.NONE }) {

            val color = Color.LightGray
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)

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
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier.testTag("Decrement"),
            onClick = { monthState.currentMonth = monthState.currentMonth.minusMonths(1) }
        ) {
            Image(
                imageVector = Icons.Default.KeyboardArrowLeft,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface),
                contentDescription = "Previous",
            )
        }
        Text(
            modifier = Modifier.testTag("MonthLabel"),
            text = monthState.currentMonth.month
                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                .lowercase()
                .replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = monthState.currentMonth.year.toString(), style = MaterialTheme.typography.h4)
        IconButton(
            modifier = Modifier.testTag("Increment"),
            onClick = { monthState.currentMonth = monthState.currentMonth.plusMonths(1) }
        ) {
            Image(
                imageVector = Icons.Default.KeyboardArrowRight,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface),
                contentDescription = "Next",
            )
        }
    }
}