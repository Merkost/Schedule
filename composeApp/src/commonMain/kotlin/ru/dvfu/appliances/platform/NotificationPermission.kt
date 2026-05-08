package ru.dvfu.appliances.platform

import androidx.compose.runtime.Composable

@Composable
expect fun NotificationPermissionRequest()

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
