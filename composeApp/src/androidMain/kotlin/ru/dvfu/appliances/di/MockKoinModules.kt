package ru.dvfu.appliances.di

import org.koin.dsl.module
import ru.dvfu.appliances.model.datasource.mock.MockAppliancesRepository
import ru.dvfu.appliances.model.datasource.mock.MockCloudFirestoreDatabase
import ru.dvfu.appliances.model.datasource.mock.MockEventsRepository
import ru.dvfu.appliances.model.datasource.mock.MockOfflineRepository
import ru.dvfu.appliances.model.datasource.mock.MockUsersRepository
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.OfflineRepository
import ru.dvfu.appliances.model.repository.Repository
import ru.dvfu.appliances.model.repository.UsersRepository

val mockRepositoryModule = module {
    single<OfflineRepository> { MockOfflineRepository() }
    single<Repository> { MockCloudFirestoreDatabase() }
    single<EventsRepository> { MockEventsRepository() }
    single<AppliancesRepository> { MockAppliancesRepository() }
    single<UsersRepository> { MockUsersRepository() }
}
