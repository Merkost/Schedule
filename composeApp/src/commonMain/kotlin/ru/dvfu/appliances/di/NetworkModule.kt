package ru.dvfu.appliances.di

import org.koin.dsl.module
import ru.dvfu.appliances.network.NotificationApi

val networkModule = module {
    single { NotificationApi() }
}
