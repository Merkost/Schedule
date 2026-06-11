package ru.dvfu.appliances.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.platform.AppleAuthLauncher
import ru.dvfu.appliances.platform.GoogleAuthLauncher

actual fun platformModule(): Module = module {
    single(named("isDebug")) { AppDebug.isDebug }
    single { GoogleAuthLauncher() }
    single { AppleAuthLauncher() }
}
