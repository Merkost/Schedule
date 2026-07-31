package ru.dvfu.appliances.compose.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.MyCard
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.components.*
import ru.dvfu.appliances.compose.viewmodels.AddEventViewModel
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.components.views.PrimaryText
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.utils.formatFull
import ru.dvfu.appliances.model.utils.toHoursAndMinutes
import kotlinx.datetime.format
import ru.dvfu.appliances.ui.ViewState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEvent(selectedDate: LocalDate, upPress: () -> Unit) {
    val viewModel: AddEventViewModel = koinViewModel { parametersOf(selectedDate) }
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) upPress()
    }

    if (uiState is UiState.InProgress) ModalLoadingDialog()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ScheduleAppBar(
                title = stringResource(Res.string.new_event),
                backClick = upPress,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState !is UiState.InProgress && uiState != UiState.Success) viewModel.addEvent()
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        },
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            DateAndTime(
                date = viewModel.date.value,
                timeStart = viewModel.timeStart.value.time,
                timeEnd = viewModel.timeEnd.value.time,
                onDateSet = viewModel::onDateSet,
                onTimeStartSet = viewModel::onTimeStartSet,
                onTimeEndSet = viewModel::onTimeEndSet,
                duration = viewModel.duration.collectAsState().value,
                isDurationError = viewModel.isDurationError.collectAsState().value,
            )
            Commentary(
                commentary = viewModel.commentary.value,
                onCommentarySet = viewModel::onCommentarySet,
            )
            ChooseAppliance(
                appliancesState = viewModel.appliancesState.collectAsState().value,
                selectedAppliance = viewModel.selectedAppliance.collectAsState(),
                onApplianceSelected = viewModel::onApplianceSelected,
            )
            AutoApproveToggle(
                viewModel.autoApproveToggleEnabled.collectAsState().value,
                viewModel.autoApproveToggle.collectAsState().value,
                viewModel::onAutoApproveToggleChanged,
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun AutoApproveToggle(shouldBeShown: Boolean, value: Boolean, onValueChange: (Boolean) -> Unit) {
    AnimatedVisibility(visible = shouldBeShown, enter = fadeIn(), exit = fadeOut()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Автоматически одобрить событие",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Checkbox(checked = value, onCheckedChange = onValueChange)
            }
        }
    }
}

@Composable
fun Commentary(
    modifier: Modifier = Modifier,
    commentary: String,
    onCommentarySet: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = commentary,
        onValueChange = onCommentarySet,
        label = { Text(stringResource(Res.string.commentary)) },
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
fun ChooseAppliance(
    appliancesState: ViewState<List<Appliance>>,
    selectedAppliance: State<Appliance?>,
    onApplianceSelected: (Appliance) -> Unit,
) {
    when (appliancesState) {
        is ViewState.Error -> {}
        is ViewState.Success -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryText(
                    text = stringResource(Res.string.choose_appliance),
                    modifier = Modifier.fillMaxWidth(),
                )
                ApplianceSelection(
                    radioOptions = appliancesState.data,
                    currentOption = selectedAppliance,
                    onSelectedItem = onApplianceSelected,
                )
            }
        }
        is ViewState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ItemApplianceSelectable(
    appliance: Appliance,
    isSelected: Boolean,
    applianceClicked: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "applianceCardColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "applianceBorderColor",
    )

    Card(
        onClick = applianceClicked,
        modifier = Modifier
            .width(130.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(appliance.color).copy(alpha = 0.15f)),
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = if (appliance.name.isEmpty()) "" else appliance.name.first().uppercase(),
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(appliance.color).let { c ->
                            if (c == Color.White || c.alpha < 0.1f) MaterialTheme.colorScheme.primary else c
                        },
                    )
                }
            }
            Text(
                text = appliance.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun DateAndTime(
    date: LocalDate,
    timeStart: LocalTime,
    timeEnd: LocalTime,
    onDateSet: ((LocalDate) -> Unit)? = null,
    onTimeStartSet: ((LocalTime) -> Unit)? = null,
    onTimeEndSet: ((LocalTime) -> Unit)? = null,
    duration: String? = null,
    isDurationError: Boolean = false,
) {
    val dateSetState = remember { mutableStateOf(false) }
    val timeSetStartState = remember { mutableStateOf(false) }
    val timeSetEndState = remember { mutableStateOf(false) }

    onDateSet?.let {
        if (dateSetState.value) DatePicker(date, onDateSet = onDateSet) {
            dateSetState.value = false
        }
    }
    onTimeStartSet?.let {
        if (timeSetStartState.value) TimePicker(timeStart, onTimeSet = onTimeStartSet) {
            timeSetStartState.value = false
        }
    }
    onTimeEndSet?.let {
        if (timeSetEndState.value) TimePicker(timeEnd, onTimeSet = onTimeEndSet) {
            timeSetEndState.value = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryText(
            text = stringResource(Res.string.date_and_time),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = date.formatFull(),
            onValueChange = {},
            label = { Text(stringResource(Res.string.date)) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                onDateSet?.let {
                    IconButton(onClick = { dateSetState.value = true }) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = timeStart.toHoursAndMinutes(),
                onValueChange = {},
                label = { Text(stringResource(Res.string.time_start)) },
                readOnly = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    onTimeStartSet?.let {
                        IconButton(onClick = { timeSetStartState.value = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
            OutlinedTextField(
                value = timeEnd.toHoursAndMinutes(),
                onValueChange = {},
                label = { Text(stringResource(Res.string.time_end)) },
                readOnly = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    onTimeEndSet?.let {
                        IconButton(onClick = { timeSetEndState.value = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
        }
        duration?.let {
            val durationColor by animateColorAsState(
                targetValue = if (isDurationError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "durationColor",
            )
            Text(
                text = "Продолжительность: $duration",
                style = MaterialTheme.typography.bodySmall,
                color = durationColor,
            )
        }
    }
}
