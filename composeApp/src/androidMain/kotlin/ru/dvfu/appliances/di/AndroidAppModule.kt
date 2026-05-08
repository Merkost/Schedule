package ru.dvfu.appliances.di

import org.koin.dsl.module
import ru.dvfu.appliances.Logger
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.compose.utils.NotificationManagerImpl

val androidAppModule = module {
    single { Logger() }

    single<NotificationManager> {
        NotificationManagerImpl(
            userDatastore = get(),
            usersRepository = get(),
            getUserUseCase = get(),
            getApplianceUseCase = get(),
            notificationApi = get(),
        )
    }
}
