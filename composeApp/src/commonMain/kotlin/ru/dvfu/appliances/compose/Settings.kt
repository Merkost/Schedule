package ru.dvfu.appliances.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import com.mmk.kmpnotifier.notification.NotifierManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.components.IosAppPromotionBanner
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.model.datastore.ThemeMode
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.platform.IOS_APP_STORE_URL
import ru.dvfu.appliances.platform.NotificationPermissionResult
import ru.dvfu.appliances.platform.buildIosAppShareMessage
import ru.dvfu.appliances.platform.openAppNotificationSettings
import ru.dvfu.appliances.platform.openExternalUrl
import ru.dvfu.appliances.platform.rememberNotificationPermissionController
import ru.dvfu.appliances.platform.shareText

@Composable
fun Settings(navController: NavController, upPress: () -> Unit) {
    val datastore: UserDatastore = koinInject()
    val notificationManager: NotificationManager = koinInject()
    val usersRepository: UsersRepository = koinInject()
    val permissionController = rememberNotificationPermissionController()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var deniedAlwaysDialog by rememberSaveable { mutableStateOf(false) }

    var permissionState by remember { mutableStateOf<NotificationPermissionResult?>(null) }
    var permissionRefresh by remember { mutableIntStateOf(0) }

    var fcmToken by remember { mutableStateOf<String?>(null) }
    var serverToken by remember { mutableStateOf<String?>(null) }
    var fcmRefresh by remember { mutableIntStateOf(0) }
    var fcmSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(permissionRefresh) {
        permissionState = permissionController.currentState()
    }
    LifecycleResumeEffect(Unit) {
        permissionRefresh++
        fcmRefresh++
        onPauseOrDispose { }
    }

    LaunchedEffect(fcmRefresh) {
        fcmToken = runCatching { NotifierManager.getPushNotifier().getToken() }.getOrNull()
        val uid = usersRepository.currentUser.first()?.userId
        serverToken = uid?.let { usersRepository.getUser(it).getOrNull()?.msgToken }?.takeIf { it.isNotBlank() }
    }

    val themeMode by datastore.getThemeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val iosPromotionDismissed by datastore.getIosAppPromotionDismissed.collectAsState(initial = false)
    val iosShareMessage = buildIosAppShareMessage(stringResource(Res.string.ios_app_promotion_share_message))

    if (deniedAlwaysDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deniedAlwaysDialog = false },
            title = { Text(stringResource(Res.string.notifications)) },
            text = { Text(stringResource(Res.string.notification_permission_denied)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    deniedAlwaysDialog = false
                    permissionController.openSystemSettings()
                }) { Text(stringResource(Res.string.open_system_settings)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deniedAlwaysDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    fun requestPermission() = scope.launch {
        val result = permissionController.request()
        permissionState = result
        if (result == NotificationPermissionResult.DeniedAlways) deniedAlwaysDialog = true
    }

    fun sendTest() = scope.launch {
        val result = permissionController.request()
        permissionState = result
        when (result) {
            NotificationPermissionResult.Granted -> {
                notificationManager.sendTestNotificationToCurrentDevice().fold(
                    onSuccess = { SnackbarManager.showMessage(Res.string.test_notification_sent) },
                    onFailure = { e ->
                        val template = getString(Res.string.test_notification_failed)
                        ru.dvfu.appliances.platform.showError(
                            template.replace("%s", e.message ?: "unknown"),
                        )
                    },
                )
            }
            NotificationPermissionResult.DeniedAlways -> deniedAlwaysDialog = true
            NotificationPermissionResult.Denied -> Unit
        }
    }

    fun syncFcmToken() = scope.launch {
        fcmSyncing = true
        runCatching {
            val token = NotifierManager.getPushNotifier().getToken()
            if (!token.isNullOrBlank()) usersRepository.setNewMessagingToken(token)
            fcmToken = token
            val uid = usersRepository.currentUser.first()?.userId
            serverToken = uid?.let { usersRepository.getUser(it).getOrNull()?.msgToken }?.takeIf { it.isNotBlank() }
        }
        fcmSyncing = false
    }

    fun copyToken() {
        fcmToken?.let {
            clipboard.setText(AnnotatedString(it))
            SnackbarManager.showMessage(Res.string.fcm_diagnostics_copied)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
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
            if (!iosPromotionDismissed) {
                IosAppPromotionBanner(
                    onOpen = { openExternalUrl(IOS_APP_STORE_URL) },
                    onShare = { shareText(iosShareMessage) },
                    onDismiss = {
                        scope.launch { datastore.saveIosAppPromotionDismissed(true) }
                    },
                )
            }

            if (permissionState != null && permissionState != NotificationPermissionResult.Granted) {
                NotificationPermissionBanner(
                    state = permissionState!!,
                    onAllow = { requestPermission() },
                    onOpenSettings = { permissionController.openSystemSettings() },
                )
            }

            SettingsSection(title = stringResource(Res.string.notifications)) {
                SettingsToggleRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(Res.string.notifications_enable),
                    subtitle = stringResource(Res.string.notifications_enable_subtitle),
                    checked = permissionState == NotificationPermissionResult.Granted,
                    onCheckedChange = { enabled ->
                        if (enabled) requestPermission() else openAppNotificationSettings()
                    },
                )
                SettingsDivider()
                SettingsLinkRow(
                    icon = Icons.Outlined.Send,
                    title = stringResource(Res.string.send_test_notification),
                    subtitle = stringResource(Res.string.send_test_notification_subtitle),
                    onClick = { sendTest() },
                )
                SettingsDivider()
                SettingsLinkRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = stringResource(Res.string.open_system_settings),
                    subtitle = stringResource(Res.string.open_system_settings_subtitle),
                    onClick = { openAppNotificationSettings() },
                )
            }

            SettingsSection(title = stringResource(Res.string.diagnostics)) {
                FcmDiagnosticsRow(
                    deviceToken = fcmToken,
                    serverToken = serverToken,
                    syncing = fcmSyncing,
                    onSync = { syncFcmToken() },
                    onCopy = { copyToken() },
                )
            }

            SettingsSection(title = stringResource(Res.string.appearance)) {
                ThemeModeRow(
                    selected = themeMode,
                    onSelected = { mode -> scope.launch { datastore.saveThemeMode(mode) } },
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
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsIcon(icon, enabled)
        SettingsTextColumn(title, subtitle, enabled, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(selected: ThemeMode, onSelected: (ThemeMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsIcon(Icons.Outlined.DarkMode, enabled = true)
            SettingsTextColumn(
                title = stringResource(Res.string.dark_theme),
                subtitle = stringResource(Res.string.dark_theme_subtitle),
                enabled = true,
                modifier = Modifier.weight(1f),
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeModeOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option.mode == selected,
                    onClick = { onSelected(option.mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeModeOption.entries.size,
                    ),
                    label = { Text(stringResource(option.labelRes), maxLines = 1) },
                )
            }
        }
    }
}

private enum class ThemeModeOption(val mode: ThemeMode, val labelRes: StringResource) {
    System(ThemeMode.SYSTEM, Res.string.theme_mode_system),
    Light(ThemeMode.LIGHT, Res.string.theme_mode_light),
    Dark(ThemeMode.DARK, Res.string.theme_mode_dark),
}

@Composable
private fun NotificationPermissionBanner(
    state: NotificationPermissionResult,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val blocked = state == NotificationPermissionResult.DeniedAlways
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.notification_permission_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            if (blocked) Res.string.notification_permission_banner_subtitle_blocked
                            else Res.string.notification_permission_banner_subtitle_denied,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(
                onClick = if (blocked) onOpenSettings else onAllow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (blocked) Res.string.notification_permission_banner_action_open_settings
                        else Res.string.notification_permission_banner_action_allow,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FcmDiagnosticsRow(
    deviceToken: String?,
    serverToken: String?,
    syncing: Boolean,
    onSync: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsIcon(Icons.Outlined.Key, enabled = true)
            SettingsTextColumn(
                title = stringResource(Res.string.fcm_diagnostics_title),
                subtitle = fcmDiagnosticsSubtitle(deviceToken, serverToken),
                enabled = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (!deviceToken.isNullOrBlank()) {
            Text(
                text = "${deviceToken.take(20)}…${deviceToken.takeLast(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onSync,
                enabled = !syncing,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.fcm_diagnostics_refresh), maxLines = 1)
            }
            TextButton(
                onClick = onCopy,
                enabled = !deviceToken.isNullOrBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.fcm_diagnostics_copy), maxLines = 1)
            }
        }
    }
}

@Composable
private fun fcmDiagnosticsSubtitle(deviceToken: String?, serverToken: String?): String = when {
    deviceToken.isNullOrBlank() -> stringResource(Res.string.fcm_diagnostics_unavailable)
    serverToken.isNullOrBlank() -> stringResource(Res.string.fcm_diagnostics_no_server)
    serverToken == deviceToken -> stringResource(Res.string.fcm_diagnostics_synced)
    else -> stringResource(Res.string.fcm_diagnostics_mismatch)
}
