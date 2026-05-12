package ru.dvfu.appliances.compose.calendars

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.datetime.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.home.HomeTopBar
import ru.dvfu.appliances.compose.home.booking_list.BookingCommentaryDialog
import ru.dvfu.appliances.compose.viewmodels.EventsState
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.new_event
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.repository.entity.canManageEvent
import ru.dvfu.appliances.model.repository.entity.isAnonymousOrGuest
import ru.dvfu.appliances.model.utils.formattedDate
import ru.dvfu.appliances.model.utils.formattedTime
import ru.dvfu.appliances.model.utils.loadingModifier
import ru.dvfu.appliances.navigation.AddEventRoute
import ru.dvfu.appliances.navigation.BookingListRoute
import ru.dvfu.appliances.platform.PlatformBackHandler
import kotlin.time.Clock

private val MONTH_NAMES_RU_FULL = listOf(
    "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
)

private val DAY_OF_WEEK_NAMES_RU = mapOf(
    DayOfWeek.MONDAY to "Пн",
    DayOfWeek.TUESDAY to "Вт",
    DayOfWeek.WEDNESDAY to "Ср",
    DayOfWeek.THURSDAY to "Чт",
    DayOfWeek.FRIDAY to "Пт",
    DayOfWeek.SATURDAY to "Сб",
    DayOfWeek.SUNDAY to "Вс",
)

private val MONTH_SHORT_RU = listOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

@Composable
fun MonthWeekCalendar(
    viewModel: WeekCalendarViewModel,
    navController: NavController,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val managingUiState by viewModel.managingUiState.collectAsState()
    val dayEvents = viewModel.dayEvents
    val scrollState = rememberScrollState()

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(120) }
    val endMonth = remember { currentMonth.plusMonths(120) }
    val firstDayOfWeek = DayOfWeek.MONDAY
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
    )

    var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var pinnedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .distinctUntilChanged()
            .collect { ym ->
                viewModel.onMonthChanged(ym.year, ym.month.ordinal + 1)
            }
    }

    PlatformBackHandler(
        enabled = pinnedDate != null || calendarState.firstVisibleMonth.yearMonth != currentMonth
    ) {
        if (pinnedDate != null) {
            pinnedDate = null
            selectedDate = null
        } else {
            coroutineScope.launch { calendarState.animateScrollToMonth(currentMonth) }
        }
    }

    if (managingUiState is UiState.InProgress) {
        ModalLoadingDialog()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            HomeTopBar(
                uiState = uiState,
                onBookingListOpen = { navController.navigate(BookingListRoute) },
                onCalendarSelected = viewModel::setCalendarType,
                onRetry = {
                    val ym = calendarState.firstVisibleMonth.yearMonth
                    viewModel.onMonthChanged(ym.year, ym.month.ordinal + 1)
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (!currentUser.isAnonymousOrGuest) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(
                            AddEventRoute(dateEpochDay = currentDate.toEpochDays())
                        )
                    },
                ) {
                    Text(text = stringResource(Res.string.new_event))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
        ) {
            SchedulerMonthHeader(
                yearMonth = calendarState.firstVisibleMonth.yearMonth,
                onPrev = {
                    val target = calendarState.firstVisibleMonth.yearMonth.minusMonths(1)
                    if (target >= startMonth) {
                        coroutineScope.launch { calendarState.animateScrollToMonth(target) }
                    }
                },
                onNext = {
                    val target = calendarState.firstVisibleMonth.yearMonth.plusMonths(1)
                    if (target <= endMonth) {
                        coroutineScope.launch { calendarState.animateScrollToMonth(target) }
                    }
                },
            )

            DaysOfWeekRow(daysOfWeek = daysOfWeek)

            HorizontalCalendar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = calendarState,
                dayContent = { day ->
                    val date = day.date
                    val events = (dayEvents[date] as? EventsState.Loaded)?.events
                        .orEmpty()
                        .filter { it.status != BookingStatus.DECLINED && it.appliance.active }
                    ScheduleCalendarDate(
                        currentUser = currentUser,
                        day = day,
                        currentDayEvents = events,
                        isSelected = selectedDate == date,
                        today = today,
                        onClick = {
                            if (day.position != DayPosition.MonthDate) return@ScheduleCalendarDate
                            selectedDate = if (selectedDate == date) null else date
                            pinnedDate = if (pinnedDate == date) null else date
                            viewModel.onDateSelectionChanged(listOfNotNull(selectedDate))
                        },
                    )
                },
            )

            EventsPanel(
                pinnedDate = pinnedDate,
                currentMonth = calendarState.firstVisibleMonth.yearMonth,
                dayEvents = dayEvents,
                uiState = uiState,
                currentUser = currentUser,
                scrollState = scrollState,
                onClearSelection = {
                    pinnedDate = null
                    selectedDate = null
                },
                onEventClick = onEventClick,
                onApproveClick = viewModel::onApproveClick,
                onDeclineClick = viewModel::onDeclineClick,
            )
        }
    }
}

@Composable
private fun SchedulerMonthHeader(
    yearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabel = MONTH_NAMES_RU_FULL[yearMonth.month.ordinal]
        .replaceFirstChar { it.uppercase() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = yearMonth.year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DaysOfWeekRow(daysOfWeek: List<DayOfWeek>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = DAY_OF_WEEK_NAMES_RU[dayOfWeek] ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ScheduleCalendarDate(
    currentUser: User,
    day: CalendarDay,
    currentDayEvents: List<CalendarEvent>,
    isSelected: Boolean,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val date = day.date
    val isFromCurrentMonth = day.position == DayPosition.MonthDate

    val border = if (currentDayEvents.any { it.status == BookingStatus.APPROVED }) {
        BorderStroke(
            2.dp,
            if (currentDayEvents.any {
                    it.user.userId == currentUser.userId && it.status == BookingStatus.APPROVED
                }) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
    } else null

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFromCurrentMonth) 4.dp else 0.dp
        ),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectionColor else MaterialTheme.colorScheme.surface
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = isFromCurrentMonth) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            val isToday = date == today
            Text(
                text = date.day.toString(),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isToday) 16.sp else 14.sp,
                color = if (isFromCurrentMonth) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
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
private fun ColumnScope.EventsPanel(
    pinnedDate: LocalDate?,
    currentMonth: YearMonth,
    dayEvents: Map<LocalDate, EventsState>,
    uiState: UiState,
    currentUser: User,
    scrollState: ScrollState,
    onClearSelection: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onApproveClick: (CalendarEvent, String) -> Unit,
    onDeclineClick: (CalendarEvent, String) -> Unit,
) {
    val monthEvents: List<Pair<LocalDate, List<CalendarEvent>>> = dayEvents
        .asSequence()
        .filter { (date, _) ->
            date.year == currentMonth.year && date.month == currentMonth.month
        }
        .mapNotNull { (date, state) ->
            val events = (state as? EventsState.Loaded)?.events
                ?.filter { it.status != BookingStatus.DECLINED && it.appliance.active }
                ?.sortedBy { it.timeStart }
            if (events.isNullOrEmpty()) null else date to events
        }
        .sortedBy { it.first }
        .toList()

    val dayState = pinnedDate?.let { dayEvents[it] }
    val isLoading = dayState is EventsState.Loading ||
        (pinnedDate == null && dayEvents.values.any { it is EventsState.Loading } && monthEvents.isEmpty())

    val monthLabel = MONTH_NAMES_RU_FULL[currentMonth.month.ordinal]
        .replaceFirstChar { it.uppercase() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (pinnedDate != null) {
                    formattedDate(pinnedDate)
                } else {
                    "$monthLabel — все события"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (pinnedDate != null) {
                TextButton(onClick = onClearSelection) {
                    Text("Весь месяц")
                }
            }
        }

        Crossfade(
            targetState = Triple(pinnedDate, isLoading, monthEvents.size),
            label = "eventsCrossfade",
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    isLoading -> repeat(3) { ShimmerEventPlaceholder() }
                    pinnedDate != null -> {
                        val events = (dayState as? EventsState.Loaded)?.events.orEmpty()
                        if (events.isEmpty()) {
                            EventsEmptyState("Нет событий на выбранный день")
                        } else {
                            events.forEach { event ->
                                EventView(
                                    onEventClick = onEventClick,
                                    uiState = uiState,
                                    event = event,
                                    currentUser = currentUser,
                                    onApproveClick = onApproveClick,
                                    onDeclineClick = onDeclineClick,
                                )
                            }
                        }
                    }
                    else -> {
                        if (monthEvents.isEmpty()) {
                            EventsEmptyState("В этом месяце нет событий")
                        } else {
                            monthEvents.forEach { (date, events) ->
                                EventDateHeader(date)
                                events.forEach { event ->
                                    EventView(
                                        onEventClick = onEventClick,
                                        uiState = uiState,
                                        event = event,
                                        currentUser = currentUser,
                                        onApproveClick = onApproveClick,
                                        onDeclineClick = onDeclineClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventView(
    modifier: Modifier = Modifier,
    childModifier: Modifier = Modifier,
    uiState: UiState,
    onEventClick: (CalendarEvent) -> Unit,
    onApproveClick: (CalendarEvent, String) -> Unit,
    onDeclineClick: (CalendarEvent, String) -> Unit,
    event: CalendarEvent,
    currentUser: User,
) {
    val contentAlpha = when (event.status) {
        BookingStatus.DECLINED -> 0.38f
        else -> 1f
    }

    var approveDialogState by remember { mutableStateOf(false) }
    var declineDialogState by remember { mutableStateOf(false) }

    if (approveDialogState) {
        BookingCommentaryDialog(
            commentArg = "",
            onCancel = { approveDialogState = false },
            onApplyCommentary = {
                approveDialogState = false
                onApproveClick(event, it)
            },
            newStatus = BookingStatus.APPROVED,
        )
    }

    if (declineDialogState) {
        BookingCommentaryDialog(
            commentArg = "",
            onCancel = { declineDialogState = false },
            onApplyCommentary = {
                approveDialogState = false
                onDeclineClick(event, it)
            },
            newStatus = BookingStatus.DECLINED,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(12.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    color = Color(event.appliance.color).copy(alpha = contentAlpha),
                    shape = CircleShape,
                )
                .then(childModifier)
        )
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp)
                .clipToBounds()
                .then(childModifier),
            onClick = { onEventClick(event) },
        ) {
            Column(Modifier.padding(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.weight(1f, false),
                    ) {
                        Text(
                            text = formattedTime(event.timeStart, event.timeEnd),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                            modifier = childModifier,
                        )
                        Text(
                            text = event.appliance.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = childModifier,
                        )
                    }
                    if (event.status == BookingStatus.NONE && currentUser.canManageEvent(event)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 4.dp),
                        ) {
                            OutlinedButton(
                                onClick = { declineDialogState = true },
                                shape = CircleShape,
                                enabled = uiState !is UiState.InProgress,
                            ) {
                                Icon(Icons.Default.Close, "")
                            }
                            OutlinedButton(
                                onClick = { approveDialogState = true },
                                shape = CircleShape,
                                enabled = uiState !is UiState.InProgress,
                            ) {
                                Icon(Icons.Default.Done, "")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.padding(start = 4.dp).size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(event.status.icon, "status", tint = event.status.color)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(Icons.Default.AccountCircle, "")
                    Text(
                        text = event.user.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = childModifier,
                    )
                }

                if (event.commentary.isNotBlank()) {
                    Text(
                        text = event.commentary,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        modifier = childModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDateHeader(date: LocalDate) {
    val isToday = date == Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dayOfWeek = DAY_OF_WEEK_NAMES_RU[date.dayOfWeek] ?: ""
    val dayOfMonth = date.day
    val monthName = MONTH_SHORT_RU[date.month.ordinal]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Column {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = monthName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun EventsEmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShimmerEventPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(12.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .loadingModifier(),
        )
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .loadingModifier(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .loadingModifier(),
                )
            }
        }
    }
}

