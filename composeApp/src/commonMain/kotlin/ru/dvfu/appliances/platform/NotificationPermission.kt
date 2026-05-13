package ru.dvfu.appliances.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

@Composable
fun rememberNotificationPermissionController(): NotificationPermissionController {
    val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)
    return remember(controller) { NotificationPermissionController(controller) }
}

class NotificationPermissionController(
    private val controller: dev.icerock.moko.permissions.PermissionsController,
) {
    suspend fun isGranted(): Boolean = currentState() == NotificationPermissionResult.Granted

    suspend fun currentState(): NotificationPermissionResult =
        when (controller.getPermissionState(dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION)) {
            PermissionState.Granted -> NotificationPermissionResult.Granted
            PermissionState.DeniedAlways -> NotificationPermissionResult.DeniedAlways
            else -> NotificationPermissionResult.Denied
        }

    suspend fun request(): NotificationPermissionResult = try {
        controller.providePermission(dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION)
        NotificationPermissionResult.Granted
    } catch (_: DeniedAlwaysException) {
        NotificationPermissionResult.DeniedAlways
    } catch (_: DeniedException) {
        NotificationPermissionResult.Denied
    }

    fun openSystemSettings() = controller.openAppSettings()
}

enum class NotificationPermissionResult { Granted, Denied, DeniedAlways }

@Composable
fun NotificationPermissionRequest() {
    val controller = rememberNotificationPermissionController()
    LaunchedEffect(controller) {
        if (!controller.isGranted()) controller.request()
    }
}
