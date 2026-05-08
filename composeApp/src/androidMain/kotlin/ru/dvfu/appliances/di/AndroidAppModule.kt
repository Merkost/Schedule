package ru.dvfu.appliances.di

import org.koin.dsl.module
import ru.dvfu.appliances.Logger

val androidAppModule = module {
    single { Logger() }
}
