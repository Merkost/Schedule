package ru.dvfu.appliances.compose.calendars

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.Arguments
import ru.dvfu.appliances.compose.MainDestinations
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.home.HomeTopBar
import ru.dvfu.appliances.compose.home.SelectedDate
import ru.dvfu.appliances.compose.home.booking_list.BookingCommentaryDialog
import ru.dvfu.appliances.compose.navigate
import ru.dvfu.appliances.compose.viewmodels.EventsState
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.model.repository.entity.*
import ru.dvfu.appliances.model.utils.formattedTime
import ru.dvfu.appliances.model.utils.loadingModifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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

    var pinnedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }

    val calendarState = rememberSelectableCalendarState(
        initialSelection = emptyList(),
        onSelectionChanged = { selection ->
            viewModel.onDateSelectionChanged(selection)
            val next = selection.firstOrNull()
            pinnedDate = if (next != null && next == pinnedDate) null else next
        },
    )

    BackHandler(pinnedDate != null || calendarState.monthState.currentMonth != YearMonth.now()) {
        if (pinnedDate != null) {
            pinnedDate = null
            calendarState.selectionState.selection = emptyList()
        } else {
            calendarState.monthState.currentMonth = YearMonth.now()
        }
    }

    LaunchedEffect(calendarState.monthState.currentMonth) {
        viewModel.onMonthChanged(calendarState.monthState.currentMonth)
    }

    if (managingUiState is UiState.InProgress) {
        ModalLoadingDialog()
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                uiState = uiState,
                onBookingListOpen = {
                    navController.navigate(MainDestinations.BOOKING_LIST)
                },
                onCalendarSelected = viewModel::setCalendarType,
                onRetry = { viewModel.onMonthChanged(calendarState.monthState.currentMonth) }
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (!currentUser.isAnonymousOrGuest) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(
                            MainDestinations.ADD_EVENT,
                            Arguments.DATE to SelectedDate(currentDate)
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
        ) {
            SelectableCalendar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                calendarState = calendarState,
                dayContent = { dayState ->
                    ScheduleCalendarDate(
                        currentUser = currentUser,
                        state = dayState,
                        currentDayEvents = (dayEvents[dayState.date] as? EventsState.Loaded)?.events.orEmpty()
                            .filter { it.status != BookingStatus.DECLINED && it.appliance.active }
                    )
                },
                monthHeader = { SchedulerMonthHeader(it) }
            )

            EventsPanel(
                pinnedDate = pinnedDate,
                currentMonth = calendarState.monthState.currentMonth,
                dayEvents = dayEvents,
                uiState = uiState,
                currentUser = currentUser,
                scrollState = scrollState,
                onClearSelection = {
                    pinnedDate = null
                    calendarState.selectionState.selection = emptyList()
                },
                onEventClick = onEventClick,
                onApproveClick = viewModel::onApproveClick,
                onDeclineClick = viewModel::onDeclineClick,
            )
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
    currentUser: User
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
            newStatus = BookingStatus.APPROVED
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
            newStatus = BookingStatus.DECLINED
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(12.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    color = Color(event.appliance.color).copy(alpha = contentAlpha),
                    CircleShape
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
            onClick = { onEventClick(event) }
        ) {
            Column(Modifier.padding(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.weight(1f, false)
                    ) {
                        Text(
                            text = formattedTime(event.timeStart, event.timeEnd),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                            modifier = childModifier
                        )
                        Text(
                            text = event.appliance.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = childModifier
                        )

                    }
                    if (event.status == BookingStatus.NONE && currentUser.canManageEvent(event)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { declineDialogState = true },
                                shape = CircleShape,
                                enabled = uiState !is UiState.InProgress
                            ) {
                                Icon(Icons.Default.Close, "")
                            }
                            OutlinedButton(
                                onClick = { approveDialogState = true },
                                shape = CircleShape,
                                enabled = uiState !is UiState.InProgress
                            ) {
                                Icon(Icons.Default.Done, "")
                            }
                        }
                    } else {
                        IconButtonWithoutOnClick(modifier = Modifier.padding(start = 4.dp)) {
                            Icon(event.status.icon, "status", tint = event.status.color)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, "")
                    Text(
                        text = event.user.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = childModifier
                    )
                }

                if (event.commentary.isNotBlank()) {
                    Text(
                        text = event.commentary,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        modifier = childModifier
                    )
                }
            }

        }
    }
}

@Composable
fun IconButtonWithoutOnClick(modifier: Modifier = Modifier, function: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize),
        contentAlignment = Alignment.Center
    ) {
        function()
    }
}

@Composable
private fun ColumnScope.EventsPanel(
    pinnedDate: LocalDate?,
    currentMonth: YearMonth,
    dayEvents: Map<LocalDate, EventsState>,
    uiState: UiState,
    currentUser: User,
    scrollState: androidx.compose.foundation.ScrollState,
    onClearSelection: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onApproveClick: (CalendarEvent, String) -> Unit,
    onDeclineClick: (CalendarEvent, String) -> Unit,
) = EventsPanelImpl(
    pinnedDate, currentMonth, dayEvents, uiState, currentUser, scrollState,
    onClearSelection, onEventClick, onApproveClick, onDeclineClick,
    Modifier.weight(1f),
)

@Composable
private fun EventsPanelImpl(
    pinnedDate: LocalDate?,
    currentMonth: YearMonth,
    dayEvents: Map<LocalDate, EventsState>,
    uiState: UiState,
    currentUser: User,
    scrollState: androidx.compose.foundation.ScrollState,
    onClearSelection: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onApproveClick: (CalendarEvent, String) -> Unit,
    onDeclineClick: (CalendarEvent, String) -> Unit,
    weightModifier: Modifier,
) {
    val monthEvents: List<Pair<LocalDate, List<CalendarEvent>>> = dayEvents
        .asSequence()
        .filter { (date, _) -> YearMonth.from(date) == currentMonth }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(weightModifier)
            .animateContentSize(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (pinnedDate != null) {
                    pinnedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.getDefault()))
                } else {
                    currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("LLLL", java.util.Locale.getDefault()))
                        .replaceFirstChar { it.titlecase() } + " — все события"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (pinnedDate != null) {
                androidx.compose.material3.TextButton(onClick = onClearSelection) {
                    Text("Весь месяц")
                }
            }
        }

        Crossfade(targetState = Triple(pinnedDate, isLoading, monthEvents.size), label = "eventsCrossfade") { _ ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 100.dp),
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
private fun EventDateHeader(date: LocalDate) {
    val locale = java.util.Locale.getDefault()
    val isToday = date == LocalDate.now()
    val dayOfWeek = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.titlecase() }
    val dayOfMonth = date.dayOfMonth
    val monthName = date.month.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, locale).lowercase()

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
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                .then(if (!isToday) Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface) else Modifier),
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
