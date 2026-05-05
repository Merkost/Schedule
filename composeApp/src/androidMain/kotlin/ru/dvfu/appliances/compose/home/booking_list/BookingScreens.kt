package ru.dvfu.appliances.compose.home.booking_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.viewmodels.BookingListViewModel
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import java.time.LocalDateTime

@Composable
fun PendingBookingsList(
    bookings: List<CalendarEvent>,
    viewModel: BookingListViewModel,
    navController: NavController,
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val pending = bookings.filter { it.status == BookingStatus.NONE }

    if (pending.isEmpty()) {
        BookingEmptyState(stringResource(R.string.no_books))
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = pending.size) { index ->
                PendingBookingItemView(
                    currentUser = currentUser,
                    booking = pending[index],
                    navController = navController,
                    onApproveClick = { event, comment ->
                        viewModel.manageBookStatus(event = event, managerCommentary = comment, status = BookingStatus.APPROVED)
                    },
                    onDeclineClick = { event, comment ->
                        viewModel.manageBookStatus(event = event, managerCommentary = comment, status = BookingStatus.DECLINED)
                    },
                    onRefuseClick = viewModel::onUserRefuse,
                    onSetDateAndTime = { event, newTime ->
                        viewModel.updateEventDateAndTime(event, newTime)
                    },
                    onApplyCommentary = { event, userComment ->
                        viewModel.updateEventComment(event, userComment)
                    },
                    onApplyManagerCommentary = { event, managerComment ->
                        viewModel.updateEventManagerComment(event, managerComment)
                    },
                )
            }
        }
    }
}

@Composable
fun MyBookingsList(
    bookings: List<CalendarEvent>,
    viewModel: BookingListViewModel,
    navController: NavController,
) {
    if (bookings.isEmpty()) {
        BookingEmptyState(stringResource(R.string.no_books))
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = bookings.size) { index ->
                MyBookingRequestItemView(
                    booking = bookings[index],
                    navController = navController,
                    currentUser = viewModel.currentUser.collectAsState().value,
                    onDeclineClick = { event, commentary ->
                        viewModel.onUserRefuse(event, commentary)
                    },
                    onSetDateAndTime = { event, newTime ->
                        viewModel.updateEventDateAndTime(event, newTime)
                    },
                    onApplyCommentary = { event, newCommentary ->
                        viewModel.updateEventComment(event, newCommentary)
                    },
                )
            }
        }
    }
}

@Composable
fun PastBookingsList(
    bookings: List<CalendarEvent>,
    viewModel: BookingListViewModel,
    navController: NavController,
) {
    val sorted = bookings.sortedByDescending { it.date }

    if (sorted.isEmpty()) {
        BookingEmptyState(stringResource(R.string.no_books))
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = sorted.size) { index ->
                BookingDeclinedOrPastItemView(
                    booking = sorted[index],
                    currentUser = viewModel.currentUser.collectAsState().value,
                    navController = navController,
                )
            }
        }
    }
}

@Composable
private fun BookingEmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
