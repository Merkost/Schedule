package ru.dvfu.appliances.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.dvfu.appliances.AppBuildConfig
import ru.dvfu.appliances.AppDebug

actual fun platformModule(): Module = module {
    single(named("fcmServerKey")) { AppBuildConfig.FCM_SERVER_KEY }
    single(named("isDebug")) { AppDebug.isDebug }
}
