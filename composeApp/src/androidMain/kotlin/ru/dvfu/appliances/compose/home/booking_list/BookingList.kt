package ru.dvfu.appliances.compose.home.booking_list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.compose.appliance.LoadingItem
import ru.dvfu.appliances.compose.appliance.UserImage
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.*
import ru.dvfu.appliances.compose.home.ApplianceImage
import ru.dvfu.appliances.compose.home.ApplianceName
import ru.dvfu.appliances.compose.home.DateAndTime
import ru.dvfu.appliances.compose.viewmodels.BookingListViewModel
import ru.dvfu.appliances.compose.viewmodels.EventDateAndTime
import ru.dvfu.appliances.model.repository.entity.*
import ru.dvfu.appliances.model.utils.*
import ru.dvfu.appliances.ui.ViewState
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
)
@Composable
fun BookingList(navController: NavController) {
    val viewModel: BookingListViewModel = koinViewModel()
    val currentUser = viewModel.currentUser.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    if (uiState is UiState.InProgress) ModalLoadingDialog()

    val bookingTabs = remember { mutableStateListOf<BookingTabItem>() }
    val pagerState = rememberPagerState { bookingTabs.size }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScheduleAppBar(
                title = stringResource(Res.string.bookings),
                backClick = { navController.popBackStack() })
        }) { padding ->
        Crossfade(targetState = viewState, modifier = Modifier.padding(padding)) { state ->
            when (state) {
                is ViewState.Error -> {
                    Text(text = "Error")
                }
                is ViewState.Loading -> {
                    LoadingItem(Modifier.fillMaxSize())
                }
                is ViewState.Success -> {

                    if (state.data.isEmpty()) {
                        NoBookingsView(Modifier.fillMaxSize())
                    }

                    LaunchedEffect(key1 = state.data) {
                        bookingTabs.clear()
                        bookingTabs.addAll(
                            initTabs(
                                bookings = state.data,
                                currentUser = currentUser.value,
                                viewModel = viewModel,
                                navController = navController
                            )
                        )
                    }


                    if (bookingTabs.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        BookingListTabsView(
                            tabsList = bookingTabs,
                            pagerState = pagerState
                        )

                        BookingTabsContent(
                            tabsList = bookingTabs,
                            pagerState = pagerState
                        )
                    }
                    }
                }
            }
        }
    }
}

private fun initTabs(
    bookings: List<CalendarEvent>,
    currentUser: User,
    viewModel: BookingListViewModel,
    navController: NavController,
): List<BookingTabItem> {

    val result = mutableListOf<BookingTabItem>()

    if (currentUser.isAdmin || bookings.find { it.appliance.isUserSuperuserOrAdmin(currentUser) } != null) {
        result.add(
            BookingTabItem.PendingBookingsTabItem(
                bookings = if (currentUser.isAdmin) {
                    bookings.filter { it.timeEnd.isAfter(LocalDateTime.now()) }
                } else {
                    bookings.filter {
                        it.appliance.isUserSuperuserOrAdmin(currentUser)
                                && it.timeEnd.isAfter(LocalDateTime.now())
                    }
                },
                viewModel = viewModel,
                navController = navController
            )
        )
    }

    val myBookings = bookings.filter { it.user.userId == currentUser.userId }

    result.add(
        BookingTabItem.MyBookingsTabItem(
            bookings = myBookings.filter { it.timeEnd.isAfter(LocalDateTime.now()) },
            viewModel = viewModel,
            navController = navController
        )
    )

    result.add(
        BookingTabItem.PastBookingsTabItem(
            bookings = myBookings.filter { it.timeEnd.isBefore(LocalDateTime.now()) },
            viewModel = viewModel,
            navController = navController
        )
    )
    return result
}

@Composable
fun BookingListHeader(stringResource: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {

        Card(
            border = BorderStroke(2.dp, Color.Black),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                stringResource, modifier = Modifier
                    .padding(4.dp)
                    .padding(horizontal = 10.dp), style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun NoBookingsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryText(text = stringResource(Res.string.no_books))
    }
}

@Composable
fun BookingStatus(
    book: CalendarEvent,
    currentUser: User,
    onUserClick: (User) -> Unit,
    onDecline: ((CalendarEvent, String) -> Unit),
    onApprove: ((CalendarEvent, String) -> Unit),
    onUserRefuse: ((CalendarEvent, String) -> Unit),
    onManagerCommentarySave: (CalendarEvent, String) -> Unit
) {
    HorizontalDivider()
    Spacer(modifier = Modifier.size(12.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (book.status) {
            BookingStatus.APPROVED -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BookStatus(book, currentUser, onCommentarySave = onManagerCommentarySave) {
                        onUserClick(it)
                    }
                    if (book.canBeRefused(currentUser)) {
                        val declineComment = stringResource(Res.string.declined_by_user)
                        DeclineBookingButton(
                            onDeclineClick = { onUserRefuse(book, declineComment) }
                        )
                    }
                }

            }
            BookingStatus.DECLINED -> {
                BookStatus(book, currentUser, onCommentarySave = onManagerCommentarySave) {
                    onUserClick(it)
                }
            }
            BookingStatus.NONE -> {
                if (currentUser.canManageEvent(book)) {
                    BookingButtons(
                        onDeclineClick = { onDecline(book, it) },
                        onApproveClick = { onApprove(book, it) })
                } else {
                    BookStatus(book, currentUser, onCommentarySave = onManagerCommentarySave) {
                        onUserClick(it)
                    }
                }
            }
        }
    }
}

@Composable
fun BookStatus(
    book: CalendarEvent,
    currentUser: User,
    onCommentarySave: (CalendarEvent, String) -> Unit,
    onUserClick: (User) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = book.status.color.copy(alpha = 0.12f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = book.status.icon,
                    contentDescription = null,
                    tint = book.status.color,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.status.getName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = book.status.color,
                    )
                    book.managedTime?.let {
                        Text(
                            text = it.toDateAndTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (book.status != BookingStatus.NONE) {
            book.managedUser?.let {
                BookingUser(
                    user = it,
                    shouldShowHeader = false,
                    header = stringResource(Res.string.manager_commentary),
                ) { onUserClick(it) }
            }
            if (book.managerCommentary.isNotEmpty()) {
                BookingCommentary(
                    commentary = book.managerCommentary,
                    header = stringResource(Res.string.manager_commentary),
                    editable = currentUser.canManageEvent(book),
                    onCommentarySave = { onCommentarySave(book, it) },
                )
            }
        }
    }
}

@Composable
fun BookingCommentary(
    modifier: Modifier = Modifier,
    header: String = stringResource(Res.string.commentary),
    commentary: String,
    editable: Boolean,
    onCommentarySave: (String) -> Unit,
) {
    var commentaryDialog by remember { mutableStateOf(false) }

    if (commentaryDialog) {
        BookingCommentaryDialog(
            commentArg = commentary,
            onApplyCommentary = {
                onCommentarySave(it)
                commentaryDialog = false
            },
            onCancel = { commentaryDialog = false },
        )
    }

    if (commentary.isBlank() && !editable) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (editable) {
                    IconButton(onClick = { commentaryDialog = true }) {
                        Icon(
                            imageVector = if (commentary.isBlank()) Icons.Default.Add else Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Text(
                text = commentary.ifBlank { stringResource(Res.string.not_necessary) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (commentary.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookingTime(
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    timeStart: LocalDateTime,
    timeEnd: LocalDateTime,
    onSetNewDateAndTime: ((EventDateAndTime) -> Unit)? = null,
) {
    var dialogState by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formattedTime(timeStart, timeEnd),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = timeStart.toLocalDate().format(TimeConstants.FULL_DATE_FORMAT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
            if (editable) {
                IconButton(onClick = { dialogState = !dialogState }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit_booking_date_and_time),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    if (dialogState) {
        var dialogDate by remember { mutableStateOf(timeStart.toLocalDate()) }
        var dialogTimeStart by remember { mutableStateOf(timeStart.toLocalTime()) }
        var dialogTimeEnd by remember { mutableStateOf(timeEnd.toLocalTime()) }
        val isError by remember(dialogTimeStart, dialogTimeEnd) {
            mutableStateOf(
                dialogTimeEnd.isBefore(dialogTimeStart) || Duration.between(
                    dialogTimeStart,
                    dialogTimeEnd
                ) < Duration.ofMinutes(30)
            )
        }
        val duration by remember(dialogTimeStart, dialogTimeEnd) {
            val dur = Duration.between(dialogTimeStart, dialogTimeEnd)
            val period = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                dur.toHours(),
                dur.minusHours(dur.toHours()).toMinutes(),
            )
            mutableStateOf(period)
        }

        DefaultDialog(
            positiveButtonText = stringResource(Res.string.apply),
            neutralButtonText = stringResource(Res.string.cancel),
            onDismiss = { dialogState = false },
            onPositiveClick = {
                if (isError) {
                    SnackbarManager.showMessage(Res.string.time_end_is_before_start)
                } else {
                    dialogState = false
                    if (onSetNewDateAndTime != null) {
                        onSetNewDateAndTime(
                            EventDateAndTime(
                                date = dialogDate,
                                timeStart = dialogTimeStart,
                                timeEnd = dialogTimeEnd
                            )
                        )
                    }
                }
            },
            onNeutralClick = { dialogState = false }

        ) {
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                DateAndTime(
                    date = dialogDate,
                    timeStart = dialogTimeStart,
                    timeEnd = dialogTimeEnd,
                    duration = duration,
                    onDateSet = if (dialogTimeStart.atDate(dialogDate)
                            .isBefore(LocalDateTime.now())
                    ) {
                        null
                    } else {
                        { dialogDate = it }
                    },
                    onTimeStartSet = if (dialogTimeStart.atDate(dialogDate)
                            .isBefore(LocalDateTime.now())
                    ) {
                        null
                    } else {
                        { dialogTimeStart = it }
                    },
                    onTimeEndSet = { dialogTimeEnd = it },
                    isDurationError = isError,
                )
            }
        }
    }
}

@Composable
fun BookingAppliance(
    appliance: Appliance,
    shouldShowHeader: Boolean = true,
    onApplianceClick: () -> Unit,
) {
    DetailRow(
        leading = { ApplianceImage(appliance, modifier = Modifier.size(40.dp)) },
        title = appliance.name,
        subtitle = if (appliance.description.isNotBlank()) appliance.description else stringResource(Res.string.appliance),
        onClick = onApplianceClick,
    )
}

@Composable
fun BookingUser(
    user: User,
    shouldShowHeader: Boolean = true,
    header: String = stringResource(Res.string.user),
    onUserClick: () -> Unit,
) {
    DetailRow(
        leading = { UserImage(modifier = Modifier.size(40.dp), user = user) },
        title = if (user.userName.isBlank()) stringResource(Res.string.anonymous_user) else user.userName,
        subtitle = user.email.takeIf { it.isNotBlank() } ?: header,
        onClick = onUserClick,
    )
}

@Composable
private fun DetailRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            leading()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentaryTextField(
    modifier: Modifier = Modifier,
    text: String,
    labelText: String = "",
    readOnly: Boolean = false,
    onTextChanged: (String) -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    OutlinedTextField(
        label = { if (labelText.isNotEmpty()) Text(text = labelText) },
        value = text, onValueChange = onTextChanged,
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
            .then(modifier),
        readOnly = readOnly,
        trailingIcon = {
            AnimatedVisibility(visible = text.isNotEmpty() && readOnly.not()) {
                IconButton(onClick = { onTextChanged("") }) {
                    Icon(Icons.Default.Close, Icons.Default.Close.name)
                }
            }
        }
    )

}
