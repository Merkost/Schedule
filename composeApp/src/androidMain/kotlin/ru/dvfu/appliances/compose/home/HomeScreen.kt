package ru.dvfu.appliances.compose.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.navigation.EventInfoRoute
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

@Composable
fun HomeScreen(
    navController: NavController,
    backPress: () -> Unit,
) {
    val viewModel: WeekCalendarViewModel = koinViewModel()
    val currentUser by viewModel.currentUser.collectAsState()
    val calendarType by viewModel.calendarType.collectAsState()
    val context = LocalContext.current

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    NotificationPermissionRequest()

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
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — KMPNotifier picks up the granted state on next token call */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun BackPressHandler(
    upPress: () -> Unit
) {
    val context = LocalContext.current
    val exitMessage = stringResource(Res.string.app_exit_message)
    var lastPressed by remember { mutableStateOf(0L) }

    BackHandler(true) {

        val currentMillis = System.currentTimeMillis()
        if (currentMillis - lastPressed < TIME_TO_EXIT) {
            upPress()
        } else {
            showToast(
                context.applicationContext,
                exitMessage
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
            secondaryText = "${calendarEvent.timeStart.format(EventTimeFormatter)} - ${
                calendarEvent.timeEnd.format(EventTimeFormatter)
            }\n${calendarEvent.commentary}",
            onDismiss = onDismiss,
            neutralButtonText = stringResource(Res.string.delete),
            onNeutralClick = { onDelete(calendarEvent); onDismiss() }
        )
    }
}
