package ru.dvfu.appliances.compose.appliance

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.components.ColorPicker
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.ui.theme.pickerColors
import ru.dvfu.appliances.compose.viewmodels.NewApplianceViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewAppliance(backPressed: () -> Unit) {
    val viewModel: NewApplianceViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val titleRequiredMsg = stringResource(Res.string.appliance_title_required)

    val (selectedColor, onColorSelected) = remember { mutableStateOf<Color?>(pickerColors[0]) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) backPressed()
    }

    if (uiState is UiState.InProgress) ModalLoadingDialog()

    val title = viewModel.title.value
    val canSave = title.isNotBlank()

    Scaffold(
        topBar = {
            ScheduleAppBar(
                title = stringResource(Res.string.new_appliance_title),
                backClick = backPressed,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    keyboard?.hide()
                    if (!viewModel.createNewAppliance()) {
                        Toast.makeText(
                            context.applicationContext,
                            titleRequiredMsg,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text(stringResource(Res.string.save)) },
                expanded = canSave,
                modifier = Modifier.animateContentSize(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AppliancePreviewCard(
                name = title,
                color = selectedColor,
            )

            FormSection(stringResource(Res.string.appliance_basics)) {
                OutlinedTextField(
                    value = viewModel.title.value,
                    onValueChange = { viewModel.title.value = it },
                    label = { Text(stringResource(Res.string.main_name)) },
                    singleLine = true,
                    isError = title.isBlank(),
                    supportingText = {
                        if (title.isBlank()) Text(stringResource(Res.string.appliance_title_required))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = viewModel.description.value,
                    onValueChange = { viewModel.description.value = it },
                    label = { Text(stringResource(Res.string.appliance_description_label)) },
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormSection(stringResource(Res.string.appliance_color_label)) {
                ColorPicker(
                    colors = pickerColors,
                    selectedColor = selectedColor,
                    onColorSelected = { color ->
                        onColorSelected(color)
                        viewModel.selectedColor.value = color
                    },
                )
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun FormSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AppliancePreviewCard(name: String, color: Color?) {
    val tint = color ?: MaterialTheme.colorScheme.primary
    val hasColor = color != null && color.alpha >= 0.1f
    val textColor = when {
        !hasColor -> MaterialTheme.colorScheme.primary
        tint.luminance() > 0.6f -> MaterialTheme.colorScheme.onSurface
        else -> tint
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (hasColor) 0.18f else 0.12f))
                    .border(1.5.dp, tint.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.appliance_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = name.ifBlank { stringResource(Res.string.main_name) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun NewApplianceFab(onFabClicked: () -> Unit) {
    androidx.compose.material3.FloatingActionButton(
        modifier = Modifier.animateContentSize(),
        onClick = onFabClicked,
    ) {
        Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.add_new_appliance))
    }
}

@Composable
fun ErrorDialog(errorDialog: MutableState<Boolean>) {
    val viewModel: NewApplianceViewModel = koinInject()
    AlertDialog(
        title = { Text(stringResource(Res.string.error_generic_title)) },
        text = { Text(stringResource(Res.string.error_generic_description)) },
        onDismissRequest = { errorDialog.value = false },
        confirmButton = {
            OutlinedButton(
                onClick = { viewModel.createNewAppliance() },
                content = { Text(stringResource(Res.string.try_again)) },
            )
        },
        dismissButton = {
            OutlinedButton(
                onClick = { errorDialog.value = false },
                content = { Text(stringResource(Res.string.cancel)) },
            )
        },
    )
}
