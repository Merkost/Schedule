package ru.dvfu.appliances.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(
    date: LocalDate,
    minDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    onDateSet: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val minDateMillis = remember(minDate) {
        minDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
    val initialMillis = remember(date) {
        date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= minDateMillis
            override fun isSelectableYear(year: Int): Boolean = year >= minDate.year
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSet(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    onDismiss()
                },
            ) { Text(stringResource(Res.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        colors = DatePickerDefaults.colors(),
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    time: LocalTime,
    onTimeSet: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = true,
    )

    TimePickerMaterialDialog(
        state = timePickerState,
        onConfirm = {
            onTimeSet(LocalTime(timePickerState.hour, timePickerState.minute))
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerMaterialDialog(
    state: TimePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(24.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.time),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start),
                )
                TimePicker(state = state, colors = TimePickerDefaults.colors())
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    TextButton(onClick = onConfirm) { Text(stringResource(Res.string.apply)) }
                }
            }
        }
    }
}
