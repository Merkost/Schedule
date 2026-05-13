package ru.dvfu.appliances.compose.home.booking_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.navigation.ApplianceRoute
import ru.dvfu.appliances.navigation.UserDetailsRoute
import ru.dvfu.appliances.compose.components.views.*
import ru.dvfu.appliances.compose.ui.theme.customColors
import ru.dvfu.appliances.compose.viewmodels.BookingListViewModel
import ru.dvfu.appliances.compose.viewmodels.EventDateAndTime
import ru.dvfu.appliances.model.repository.entity.*


sealed class BookingTabItem(
    var titleRes: org.jetbrains.compose.resources.StringResource,
    val count: Int,
    var screen: @Composable () -> Unit,
) {

    class PendingBookingsTabItem(
        bookings: List<CalendarEvent>,
        viewModel: BookingListViewModel,
        navController: NavController
    ) :
        BookingTabItem(
            titleRes = Res.string.booking_requests,
            count = bookings.size,
            screen = { PendingBookingsList(bookings, viewModel, navController) }
        )

    class MyBookingsTabItem(
        bookings: List<CalendarEvent>,
        viewModel: BookingListViewModel,
        navController: NavController
    ) :
        BookingTabItem(
            titleRes = Res.string.my_bookings,
            count = bookings.size,
            screen = { MyBookingsList(bookings, viewModel, navController) }
        )

    class PastBookingsTabItem(
        bookings: List<CalendarEvent>,
        viewModel: BookingListViewModel,
        navController: NavController
    ) :
        BookingTabItem(
            titleRes = Res.string.past_bookings,
            count = bookings.size,
            screen = { PastBookingsList(bookings, viewModel, navController) }
        )
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
        ) { Text(stringResource(Res.string.refuse)) }
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
        ) { Text(stringResource(Res.string.decline)) }
        Button(
            onClick = { approveDialogState = true },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text(stringResource(Res.string.approve)) }
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
        primaryText = stringResource(Res.string.leave_a_commentary),
        secondaryText = stringResource(Res.string.not_necessary),
        positiveButtonText = when (newStatus) {
            BookingStatus.DECLINED -> stringResource(Res.string.decline)
            BookingStatus.APPROVED -> stringResource(Res.string.approve)
            else -> stringResource(Res.string.apply)
        },
        positiveButtonColor = when (newStatus) {
            BookingStatus.DECLINED -> ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White,
            )
            BookingStatus.APPROVED -> ButtonDefaults.buttonColors(
                containerColor = Color.Green,
                contentColor = Color.Black,
            )
            else -> ButtonDefaults.buttonColors()
        },
        onPositiveClick = {
            if (!isError.value) {
                onApplyCommentary(commentary)
            } else {
                SnackbarManager.showMessage(Res.string.too_many_symbols)
            }
        },
        neutralButtonText = stringResource(Res.string.cancel),
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
                    Text(text = stringResource(Res.string.commentary))
                },
                isError = isError.value,
                singleLine = false,
                maxLines = 5
            )
            SecondaryText(
                modifier = Modifier,
                text = "${symbolsCount.value}/$maxSymbols",
                textColor = if (isError.value) {
                    Color.Red
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
            navController.navigate(ApplianceRoute(applianceId = event.appliance.id))
        }
        BookingUser(event.user) {
            navController.navigate(UserDetailsRoute(userId = event.user.userId))
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
                navController.navigate(UserDetailsRoute(userId = it.userId))
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
