package ru.dvfu.appliances.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.compose.utils.NoopNotificationManager
import ru.dvfu.appliances.compose.utils.NotificationManager

actual fun platformModule(): Module = module {
    single(named("fcmServerKey")) { "" }
    single(named("isDebug")) { AppDebug.isDebug }
    single<NotificationManager> { NoopNotificationManager() }
}
