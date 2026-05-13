package ru.dvfu.appliances.compose.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.home.booking_list.EventInfo
import ru.dvfu.appliances.compose.viewmodels.EventInfoViewModel
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.canManageEvent

@Composable
fun EventInfoScreen(navController: NavController, eventArg: CalendarEvent, backPress: () -> Unit) {
    val viewModel: EventInfoViewModel = koinViewModel(parameters = { parametersOf(eventArg) })
    val eventDeleteState by viewModel.eventDeleteState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.event.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var eventDeleteDialog by remember { mutableStateOf(false) }
    if (eventDeleteDialog) {
        EventDeleteDialog(
            onDismiss = { eventDeleteDialog = false },
            onConfirm = { viewModel.deleteEvent() },
        )
    }

    if (uiState is UiState.InProgress) ModalLoadingDialog()

    LaunchedEffect(eventDeleteState) {
        when (eventDeleteState) {
            UiState.Error -> SnackbarManager.showMessage(Res.string.event_delete_failed)
            UiState.Success -> {
                SnackbarManager.showMessage(Res.string.event_delete_successfully)
                backPress()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            ScheduleAppBar(
                title = stringResource(Res.string.booking),
                backClick = backPress,
                actionDelete = currentUser.canManageEvent(event),
                deleteClick = { eventDeleteDialog = true },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EventInfo(
                currentUser = currentUser,
                navController = navController,
                showDateTimeTitle = false,
                event = event,
                onApproveClick = viewModel::onApproveClick,
                onDeclineClick = viewModel::onDeclineClick,
                onSetDateAndTime = viewModel::onSetDateAndTime,
                onCommentarySave = viewModel::onCommentarySave,
                onManagerCommentarySave = viewModel::onManagerCommentarySave,
                onUserRefuseClick = viewModel::onUserRefuse,
            )
        }
    }
}

@Composable
fun EventDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.event_delete_sure),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(Res.string.Yes)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.No))
            }
        },
    )
}

@Composable
fun EventInfoTopBar(couldDeleteEvent: Boolean, upPress: () -> Unit, onDelete: () -> Unit) {
    ScheduleAppBar(
        title = stringResource(Res.string.booking),
        backClick = upPress,
        actionDelete = couldDeleteEvent,
        deleteClick = onDelete,
    )
}
