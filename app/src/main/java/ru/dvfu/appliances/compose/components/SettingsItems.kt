package ru.dvfu.appliances.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsMenuLink(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    SettingsRow(
        modifier = modifier.clickable(onClick = onClick),
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = action,
    )
}

@Composable
fun SettingsCheckbox(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    initialChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(initialChecked) }
    val toggle = {
        if (enabled) {
            checked = !checked
            onCheckedChange(checked)
        }
    }
    SettingsRow(
        modifier = modifier.clickable(enabled = enabled, onClick = toggle),
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = {
            Checkbox(checked = checked, enabled = enabled, onCheckedChange = { toggle() })
        },
    )
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    initialChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(initialChecked) }
    val toggle = {
        if (enabled) {
            checked = !checked
            onCheckedChange(checked)
        }
    }
    SettingsRow(
        modifier = modifier.clickable(enabled = enabled, onClick = toggle),
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, enabled = enabled, onCheckedChange = { toggle() })
        },
    )
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                ) { icon() }
            }
            Spacer(Modifier.width(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
            if (subtitle != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { subtitle() }
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}
