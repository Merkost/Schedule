package ru.dvfu.appliances.compose.components.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.dvfu.appliances.R

@Composable
fun DefaultDialog(
    primaryText: String = "",
    secondaryText: String? = null,
    neutralButtonText: String = "",
    onNeutralClick: () -> Unit = { },
    negativeButtonText: String = "",
    onNegativeClick: () -> Unit = { },
    positiveButtonText: String = "",
    positiveButtonColor: ButtonColors = ButtonDefaults.buttonColors(),
    onPositiveClick: () -> Unit = { },
    onDismiss: () -> Unit = { },
    content: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = if (primaryText.isNotEmpty()) {
            { Text(primaryText, style = MaterialTheme.typography.titleLarge) }
        } else null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content?.invoke()
            }
        },
        confirmButton = {
            if (positiveButtonText.isNotEmpty()) {
                Button(
                    onClick = onPositiveClick,
                    colors = positiveButtonColor,
                ) { Text(positiveButtonText) }
            }
        },
        dismissButton = {
            if (negativeButtonText.isNotEmpty()) {
                OutlinedButton(onClick = onNegativeClick) { Text(negativeButtonText) }
            } else if (neutralButtonText.isNotEmpty()) {
                TextButton(onClick = onNeutralClick) { Text(neutralButtonText) }
            }
        },
    )
}

@Composable
fun ModalLoadingDialog(text: String = stringResourceLocal(R.string.loading)) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun stringResourceLocal(id: Int): String = androidx.compose.ui.res.stringResource(id)
