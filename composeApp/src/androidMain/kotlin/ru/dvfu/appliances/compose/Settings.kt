package ru.dvfu.appliances.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*

@Composable
fun Settings(navController: NavController, upPress: () -> Unit) {
    val context = LocalContext.current

    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var bookingUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
    var remindersEnabled by rememberSaveable { mutableStateOf(true) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { ScheduleAppBar(stringResource(Res.string.settings), backClick = upPress) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection(title = stringResource(Res.string.notifications)) {
                SettingsToggleRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(Res.string.notifications_enable),
                    subtitle = stringResource(Res.string.notifications_enable_subtitle),
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Event,
                    title = stringResource(Res.string.notifications_booking_updates),
                    subtitle = stringResource(Res.string.notifications_booking_updates_subtitle),
                    checked = bookingUpdatesEnabled,
                    onCheckedChange = { bookingUpdatesEnabled = it },
                    enabled = notificationsEnabled,
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(Res.string.notifications_reminders),
                    subtitle = stringResource(Res.string.notifications_reminders_subtitle),
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it },
                    enabled = notificationsEnabled,
                )
                SettingsDivider()
                SettingsLinkRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = stringResource(Res.string.open_system_settings),
                    subtitle = stringResource(Res.string.open_system_settings_subtitle),
                    onClick = { goToNotificationsSettings(context) },
                )
            }

            SettingsSection(title = stringResource(Res.string.appearance)) {
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(Res.string.dark_theme),
                    subtitle = stringResource(Res.string.dark_theme_subtitle),
                    checked = darkTheme,
                    onCheckedChange = { darkTheme = it },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsIcon(icon, enabled)
        SettingsTextColumn(title, subtitle, enabled, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsIcon(icon, enabled = true)
        SettingsTextColumn(title, subtitle, enabled = true, Modifier.weight(1f))
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, enabled: Boolean) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
}

@Composable
private fun SettingsTextColumn(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
            )
        }
    }
}

private fun goToNotificationsSettings(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.parse("package:" + context.packageName)
    }
    context.startActivity(intent)
}
