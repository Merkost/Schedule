package ru.dvfu.appliances.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.dvfu.appliances.network.NotificationApi
import ru.dvfu.appliances.network.createNotificationHttpClient
import ru.dvfu.appliances.network.defaultHttpClientEngine

val networkModule = module {
    single {
        createNotificationHttpClient(
            engine = defaultHttpClientEngine(),
            enableLogging = get(named("isDebug")),
        )
    }
    single {
        NotificationApi(
            client = get(),
            fcmServerKey = get(named("fcmServerKey")),
        )
    }
}
