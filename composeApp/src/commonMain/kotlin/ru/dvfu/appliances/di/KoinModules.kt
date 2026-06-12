package ru.dvfu.appliances.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.home.MainScreenViewModel
import ru.dvfu.appliances.compose.use_cases.ChangeApplianceStatusUseCase
import ru.dvfu.appliances.compose.use_cases.DeleteApplianceUseCase
import ru.dvfu.appliances.compose.use_cases.GetApplianceUseCase
import ru.dvfu.appliances.compose.use_cases.GetAppliancesUseCase
import ru.dvfu.appliances.compose.use_cases.GetDateEventsUseCase
import ru.dvfu.appliances.compose.use_cases.GetEventByIdUseCase
import ru.dvfu.appliances.compose.use_cases.GetEventTimeAvailabilityUseCase
import ru.dvfu.appliances.compose.use_cases.GetPeriodEventsUseCase
import ru.dvfu.appliances.compose.use_cases.GetUserUseCase
import ru.dvfu.appliances.compose.use_cases.UpdateEventStatusUseCase
import ru.dvfu.appliances.compose.use_cases.UpdateEventUseCase
import ru.dvfu.appliances.compose.use_cases.event.UpdateEventUserCommentUseCase
import ru.dvfu.appliances.compose.use_cases.event.UpdateManagerCommentUseCase
import ru.dvfu.appliances.compose.use_cases.event.UpdateTimeUseCase
import ru.dvfu.appliances.compose.utils.EventMapper
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.compose.utils.NotificationManagerImpl
import ru.dvfu.appliances.compose.viewmodels.AddEventViewModel
import ru.dvfu.appliances.compose.viewmodels.AddUserViewModel
import ru.dvfu.appliances.compose.viewmodels.ApplianceDetailsViewModel
import ru.dvfu.appliances.compose.viewmodels.AppliancesViewModel
import ru.dvfu.appliances.compose.viewmodels.BookingListViewModel
import ru.dvfu.appliances.compose.viewmodels.EditProfileViewModel
import ru.dvfu.appliances.compose.viewmodels.EventInfoViewModel
import ru.dvfu.appliances.compose.viewmodels.LinkedAccountsViewModel
import ru.dvfu.appliances.compose.viewmodels.LoginViewModel
import ru.dvfu.appliances.compose.viewmodels.MainViewModel
import ru.dvfu.appliances.compose.viewmodels.NewApplianceViewModel
import ru.dvfu.appliances.compose.viewmodels.ProfileViewModel
import ru.dvfu.appliances.compose.viewmodels.UserDetailsViewModel
import ru.dvfu.appliances.compose.viewmodels.UsersViewModel
import ru.dvfu.appliances.compose.viewmodels.WeekCalendarViewModel
import ru.dvfu.appliances.model.datasource.AppliancesRepositoryImpl
import ru.dvfu.appliances.model.datasource.EventsRepositoryImpl
import ru.dvfu.appliances.model.datasource.FirebaseUsersRepositoryImpl
import ru.dvfu.appliances.model.datasource.OfflineRepositoryImpl
import ru.dvfu.appliances.model.datasource.deprecated.CloudFirestoreDatabaseImpl
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.datastore.UserDatastoreImpl
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.EventsRepository
import ru.dvfu.appliances.model.repository.OfflineRepository
import ru.dvfu.appliances.model.repository.Repository
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.utils.FirestoreCollections
import ru.dvfu.appliances.notifications.AppNotifierListener
import ru.dvfu.appliances.notifications.NotificationNavRouterDelegate

val repositoryModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<FirestoreCollections> { FirestoreCollections() }
    single<OfflineRepository> { OfflineRepositoryImpl(collections = get()) }

    single<Repository> { CloudFirestoreDatabaseImpl(collections = get()) }
    single<EventsRepository> {
        EventsRepositoryImpl(
            collections = get(),
            notificationManager = get()
        )
    }
    single<AppliancesRepository> { AppliancesRepositoryImpl(collections = get()) }
    single<UsersRepository> {
        FirebaseUsersRepositoryImpl(collections = get(), userDatastore = get(), appScope = get())
    }
}

val application = module {
    single<UserDatastore> { UserDatastoreImpl() }
    viewModel { MainViewModel() }

    single<NotificationManager> {
        NotificationManagerImpl(
            userDatastore = get(),
            usersRepository = get(),
            getUserUseCase = get(),
            getApplianceUseCase = get(),
            notificationApi = get(),
        )
    }

    single { AppNotifierListener(usersRepository = get(), router = NotificationNavRouterDelegate) }

    single { SnackbarManager }

    factory { ChangeApplianceStatusUseCase(appliancesRepository = get(), eventsRepository = get()) }
    factory { DeleteApplianceUseCase(appliancesRepository = get(), eventsRepository = get()) }
    factory { GetApplianceUseCase(offlineRepository = get(), appliancesRepository = get()) }
    factory { GetAppliancesUseCase(offlineRepository = get(), appliancesRepository = get()) }
    factory { GetUserUseCase(offlineRepository = get(), usersRepository = get()) }
    factory { GetEventByIdUseCase(eventsRepository = get(), eventMapper = get()) }
    factory { GetEventTimeAvailabilityUseCase(get()) }
    factory { GetDateEventsUseCase(get()) }
    factory { GetPeriodEventsUseCase(get()) }
    factory {
        UpdateEventStatusUseCase(
            eventsRepository = get(),
            userDatastore = get(),
            notificationManager = get()
        )
    }
    factory {
        UpdateEventUseCase(
            updateUserCommentUseCase = UpdateEventUserCommentUseCase(eventsRepository = get()),
            updateManagerCommentUseCase = UpdateManagerCommentUseCase(get()),
            updateEventStatusUseCase = get(),
            updateTimeUseCase = UpdateTimeUseCase(
                eventsRepository = get(),
                getEventTimeAvailabilityUseCase = get(),
                notificationManager = get()
            )
        )
    }

    single { EventMapper(getUserUseCase = get(), getApplianceUseCase = get()) }
}

val mainActivity = module {
    viewModel { UsersViewModel(get()) }
    viewModel {
        BookingListViewModel(
            getUserUseCase = get(),
            getApplianceUseCase = get(),
            userDatastore = get(),
            updateEvent = get(),
            eventsRepository = get()
        )
    }

    viewModel { LoginViewModel(get(), get()) }

    viewModel {
        MainScreenViewModel(
            usersRepository = get(),
            userDatastore = get(),
            appliancesRepository = get()
        )
    }
    viewModel {
        WeekCalendarViewModel(
            eventsRepository = get(),
            userDatastore = get(),
            getDateEventsUseCase = get(),
            getPeriodEventsUseCase = get(),
            eventMapper = get(),
            updateEventUseCase = get()
        )
    }

    viewModel { (detUser: ru.dvfu.appliances.model.repository.entity.User) ->
        UserDetailsViewModel(
            detUser = detUser,
            usersRepository = get(),
            repository = get(),
            userDatastore = get(),
            notificationManager = get()
        )
    }

    viewModel { ProfileViewModel(get(), get()) }
    viewModel { LinkedAccountsViewModel(get(), get(), get()) }

    viewModel { ApplianceDetailsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { NewApplianceViewModel(get(), get()) }
    viewModel { AppliancesViewModel(get(), get(), get()) }
    viewModel { (areSuperUsers: Boolean, appliance: ru.dvfu.appliances.model.repository.entity.Appliance) ->
        AddUserViewModel(areSuperUsers, appliance, get(), get())
    }
    viewModel { (selectedDate: kotlinx.datetime.LocalDate) ->
        AddEventViewModel(
            selectedDate = selectedDate,
            eventsRepository = get(),
            getAppliancesUseCase = get(),
            getEventTimeAvailabilityUseCase = get(),
            userDatastore = get(),
            notificationManager = get()
        )
    }
    viewModel { (eventArg: ru.dvfu.appliances.model.repository.entity.CalendarEvent) ->
        EventInfoViewModel(
            eventArg = eventArg,
            userDatastore = get(),
            eventsRepository = get(),
            updateEventUseCase = get()
        )
    }
    viewModel {
        EditProfileViewModel(userDatastore = get(), userRepository = get())
    }
}
