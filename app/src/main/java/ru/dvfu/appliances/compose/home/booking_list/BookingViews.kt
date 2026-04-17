package ru.dvfu.appliances.compose.home.booking_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.launch
import ru.dvfu.appliances.compose.components.pagerTabIndicatorOffset
import ru.dvfu.appliances.R
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.Arguments
import ru.dvfu.appliances.compose.MainDestinations
import ru.dvfu.appliances.compose.components.views.*
import ru.dvfu.appliances.compose.navigate
import ru.dvfu.appliances.compose.ui.theme.customColors
import ru.dvfu.appliances.compose.viewmodels.BookingListViewModel
import ru.dvfu.appliances.compose.viewmodels.EventDateAndTime
import ru.dvfu.appliances.model.repository.entity.*


sealed class BookingTabItem(var titleRes: Int, var screen: @Composable () -> Unit) {

    class PendingBookingsTabItem(
        bookings: List<CalendarEvent>,
        viewModel: BookingListViewModel,
        navController: NavController
    ) :
        BookingTabItem(
            titleRes = R.string.booking_requests,
            screen = { PendingBookingsList(bookings, viewModel, navController) }
        )

    class MyBookingsTabItem(
        bookings: List<CalendarEvent>,
        viewModel: BookingListViewModel,
        navController: NavController
    ) :
        BookingTabItem(
            titleRes = R.string.my_bookings,
            screen = { MyBookingsList(bookings, viewModel, navController) }
        )

    class PastBookingsTabItem(
        bookings: List<CalendarEvent>,
        viewModel: BookingListViewModel,
        navController: NavController
    ) :
        BookingTabItem(
            titleRes = R.string.past_bookings,
            screen = { PastBookingsList(bookings, viewModel, navController) }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListTabsView(
    modifier: Modifier = Modifier,
    tabsList: List<BookingTabItem>,
    pagerState: PagerState
) {
    val scope = rememberCoroutineScope()

    SecondaryScrollableTabRow(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        selectedTabIndex = pagerState.currentPage,
        indicator = {
            SecondaryIndicator(
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    ) {
        tabsList.forEachIndexed { index, tab ->
            Tab(selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = { PrimaryText(text = stringResource(id = tab.titleRes)) })
        }
    }
}

@Composable
fun BookingTabsContent(
    modifier: Modifier = Modifier,
    tabsList: List<BookingTabItem>,
    pagerState: PagerState
) {
    HorizontalPager(
        modifier = modifier.fillMaxSize(),
        state = pagerState,
        verticalAlignment = Alignment.Top,
    ) { page ->
        tabsList[page].screen()
    }
}

@Composable
fun PendingBookingItemView(
    currentUser: User,
    booking: CalendarEvent,
    navController: NavController,
    onApproveClick: (CalendarEvent, String) -> Unit,
    onDeclineClick: (CalendarEvent, String) -> Unit,
    onRefuseClick: (CalendarEvent, String) -> Unit,
    onSetDateAndTime: (CalendarEvent, EventDateAndTime) -> Unit,
    onApplyCommentary: (CalendarEvent, String) -> Unit,
    onApplyManagerCommentary: (CalendarEvent, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BookingItem {
        EventInfo(
            currentUser = currentUser,
            event = booking,
            showDateTimeTitle = false,
            onApproveClick = onApproveClick,
            onDeclineClick = onDeclineClick,
            onUserRefuseClick = onRefuseClick,
            onSetDateAndTime = onSetDateAndTime,
            onCommentarySave = onApplyCommentary,
            onManagerCommentarySave = onApplyManagerCommentary,
            navController = navController
        )
    }
}

@Composable
fun MyBookingRequestItemView(
    modifier: Modifier = Modifier,
    navController: NavController,
    booking: CalendarEvent,
    currentUser: User,
    onDeclineClick: (CalendarEvent, String) -> Unit,
    onSetDateAndTime: (CalendarEvent, EventDateAndTime) -> Unit,
    onApplyCommentary: (CalendarEvent, String) -> Unit,
) {
    BookingItem {
        EventInfo(
            currentUser = currentUser,
            event = booking,
            showDateTimeTitle = false,
            onApproveClick = { _, _ ->

            },
            onDeclineClick = { _, _ ->

            },
            onManagerCommentarySave = { _, _ ->

            },
            onUserRefuseClick = onDeclineClick,
            onSetDateAndTime = onSetDateAndTime,
            onCommentarySave = onApplyCommentary,
            navController = navController
        )
    }
}


@Composable
fun BookingDeclinedOrPastItemView(
    modifier: Modifier = Modifier,
    currentUser: User,
    navController: NavController,
    booking: CalendarEvent,
) {
    BookingItem {

        EventInfo(
            currentUser = currentUser,
            event = booking,
            showDateTimeTitle = false,
            onApproveClick = { _, _ ->

            },
            onDeclineClick = { _, _ ->

            },
            onUserRefuseClick = { _, _ ->

            },
            onSetDateAndTime = { _, _ ->

            },
            onCommentarySave = { _, _ ->

            },
            onManagerCommentarySave = { _, _ ->

            },
            navController = navController
        )
    }
}


@Composable
fun DeclineBookingButton(
    modifier: Modifier = Modifier,
    onDeclineClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(
            onClick = onDeclineClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) { Text(stringResource(R.string.refuse)) }
    }
}

@Composable
fun BookingButtons(
    modifier: Modifier = Modifier,
    onApproveClick: (String) -> Unit,
    onDeclineClick: (String) -> Unit,
) {
    var approveDialogState by remember { mutableStateOf(false) }
    var declineDialogState by remember { mutableStateOf(false) }

    if (approveDialogState) {
        BookingCommentaryDialog(
            commentArg = "",
            onCancel = { approveDialogState = false },
            onApplyCommentary = {
                approveDialogState = false
                onApproveClick(it)
            },
            newStatus = BookingStatus.APPROVED,
        )
    }

    if (declineDialogState) {
        BookingCommentaryDialog(
            commentArg = "",
            onCancel = { declineDialogState = false },
            onApplyCommentary = {
                declineDialogState = false
                onDeclineClick(it)
            },
            newStatus = BookingStatus.DECLINED,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { declineDialogState = true },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) { Text(stringResource(id = R.string.decline)) }
        Button(
            onClick = { approveDialogState = true },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text(stringResource(id = R.string.approve)) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookingCommentaryDialog(
    commentArg: String,
    newStatus: BookingStatus? = null,
    onCancel: () -> Unit,
    onApplyCommentary: (String) -> Unit
) {
    val maxSymbols = remember { 256 }
    var commentary by remember { mutableStateOf(commentArg) }
    val symbolsCount = remember(commentary) { mutableStateOf(commentary.length) }
    val isError = remember(symbolsCount.value) { mutableStateOf(symbolsCount.value >= maxSymbols) }

    DefaultDialog(
        primaryText = stringResource(R.string.leave_a_commentary),
        secondaryText = stringResource(R.string.not_necessary),
        positiveButtonText = when (newStatus) {
            BookingStatus.DECLINED -> stringResource(id = R.string.decline)
            BookingStatus.APPROVED -> stringResource(id = R.string.approve)
            else -> stringResource(id = R.string.apply)
        },
        positiveButtonColor = when (newStatus) {
            BookingStatus.DECLINED -> ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
            BookingStatus.APPROVED -> ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )
            else -> ButtonDefaults.buttonColors()
        },
        onPositiveClick = {
            if (!isError.value) {
                onApplyCommentary(commentary)
            } else {
                SnackbarManager.showMessage(R.string.too_many_symbols)
            }
        },
        neutralButtonText = stringResource(id = R.string.cancel),
        onNeutralClick = onCancel,
        onDismiss = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                value = commentary,
                onValueChange = {
                    commentary = it
                },
                label = {
                    Text(text = stringResource(id = R.string.commentary))
                },
                isError = isError.value,
                singleLine = false,
                maxLines = 5
            )
            SecondaryText(
                modifier = Modifier,
                text = "${symbolsCount.value}/$maxSymbols",
                textColor = if (isError.value) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.customColors.secondaryTextColor
                }
            )
        }
    }

}

@Composable
fun EventInfo(
    currentUser: User,
    modifier: Modifier = Modifier,
    event: CalendarEvent,
    showDateTimeTitle: Boolean = true,
    onApproveClick: (CalendarEvent, String) -> Unit,
    onDeclineClick: (CalendarEvent, String) -> Unit,
    onUserRefuseClick: (CalendarEvent, String) -> Unit,
    onSetDateAndTime: (CalendarEvent, EventDateAndTime) -> Unit,
    onCommentarySave: (CalendarEvent, String) -> Unit,
    onManagerCommentarySave: (CalendarEvent, String) -> Unit,
    navController: NavController,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BookingTime(
            editable = currentUser.canManageEvent(event) || event.canBeRefused(currentUser),
            timeStart = event.timeStart,
            timeEnd = event.timeEnd,
            onSetNewDateAndTime = { onSetDateAndTime(event, it) },
        )
        BookingAppliance(event.appliance) {
            navController.navigate(
                MainDestinations.APPLIANCE_ROUTE,
                Arguments.APPLIANCE to event.appliance,
            )
        }
        BookingUser(event.user) {
            navController.navigate(
                MainDestinations.USER_DETAILS_ROUTE,
                Arguments.USER to event.user,
            )
        }
        BookingCommentary(
            commentary = event.commentary,
            editable = event.user.userId == currentUser.userId && event.status == BookingStatus.NONE,
            onCommentarySave = { comment -> onCommentarySave(event, comment) },
        )
        BookingStatus(
            book = event,
            currentUser = currentUser,
            onUserClick = {
                navController.navigate(
                    MainDestinations.USER_DETAILS_ROUTE,
                    Arguments.USER to it,
                )
            },
            onApprove = onApproveClick,
            onDecline = onDeclineClick,
            onUserRefuse = onUserRefuseClick,
            onManagerCommentarySave = onManagerCommentarySave,
        )
    }
}

@Composable
fun BookingItem(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}
