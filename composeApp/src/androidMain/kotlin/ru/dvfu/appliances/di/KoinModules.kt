package ru.dvfu.appliances.di

import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import ru.dvfu.appliances.model.datastore.UserDatastore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.dvfu.appliances.Logger
import ru.dvfu.appliances.compose.utils.EventMapper
import ru.dvfu.appliances.notifications.AppNotifierListener
import ru.dvfu.appliances.notifications.NotificationNavRouterDelegate
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.viewmodels.BookingListViewModel
import ru.dvfu.appliances.compose.home.MainScreenViewModel
import ru.dvfu.appliances.compose.use_cases.*
import ru.dvfu.appliances.compose.use_cases.event.UpdateEventUserCommentUseCase
import ru.dvfu.appliances.compose.use_cases.event.UpdateManagerCommentUseCase
import ru.dvfu.appliances.compose.use_cases.event.UpdateTimeUseCase
import ru.dvfu.appliances.compose.utils.NotificationManager
import ru.dvfu.appliances.compose.utils.NotificationManagerImpl
import ru.dvfu.appliances.compose.viewmodels.*
import ru.dvfu.appliances.compose.viewmodels.ApplianceDetailsViewModel
import ru.dvfu.appliances.compose.viewmodels.LoginViewModel
import ru.dvfu.appliances.compose.viewmodels.MainViewModel
import ru.dvfu.appliances.compose.viewmodels.UserDetailsViewModel
import ru.dvfu.appliances.model.datasource.*
import ru.dvfu.appliances.model.datasource.deprecated.CloudFirestoreDatabaseImpl
import ru.dvfu.appliances.model.datastore.UserDatastoreImpl
import ru.dvfu.appliances.model.repository.*
import ru.dvfu.appliances.model.repository.OfflineRepository
import ru.dvfu.appliances.model.utils.FirestoreCollections
import ru.dvfu.appliances.model.utils.RepositoryCollections

val repositoryModule = module {

    single<FirestoreCollections> { FirestoreCollections() }
    single<OfflineRepository> { OfflineRepositoryImpl(collections = get()) }
    single<RepositoryCollections> { RepositoryCollections(Firebase.firestore) }

    single<Repository> { CloudFirestoreDatabaseImpl(dbCollections = get()) }
    single<EventsRepository> {
        EventsRepositoryImpl(
            dbCollections = get(),
            notificationManager = get()
        )
    }
    single<AppliancesRepository> { AppliancesRepositoryImpl(collections = get()) }
    single<BookingRepository> { BookingRepositoryImpl(collections = get()) }
    single<UsersRepository> {
        FirebaseUsersRepositoryImpl(androidContext(), dbCollections = get(), userDatastore = get())
    }
}

val application = module {
    single<UserDatastore> { UserDatastoreImpl() }
    viewModel { MainViewModel() }

    single { AppNotifierListener(usersRepository = get(), router = NotificationNavRouterDelegate) }

    single { Logger() }
    single { SnackbarManager }

    single<NotificationManager> {
        NotificationManagerImpl(
            userDatastore = get(),
            usersRepository = get(),
            getUserUseCase = get(),
            getApplianceUseCase = get(),
            notificationApi = get(),
        )
    }

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

    viewModel {
        UserDetailsViewModel(
            detUser = it[0],
            usersRepository = get(),
            repository = get(),
            userDatastore = get(),
            notificationManager = get()
        )
    }

    viewModel { ProfileViewModel(get(), get()) }

    //Appliances
    viewModel { ApplianceDetailsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { NewApplianceViewModel(get(), get()) }
    viewModel { AppliancesViewModel(get(), get(), get()) }
    viewModel { AddUserViewModel(it.get(), it.get(), get(), get()) }
    viewModel {
        AddEventViewModel(
            selectedDate = it[0],
            eventsRepository = get(),
            getAppliancesUseCase = get(),
            getEventTimeAvailabilityUseCase = get(),
            userDatastore = get(),
            notificationManager = get()
        )
    }
    viewModel {
        EventInfoViewModel(
            eventArg = it.get(),
            userDatastore = get(),
            eventsRepository = get(),
            updateEventUseCase = get()
        )
    }
    viewModel {
        EditProfileViewModel(userDatastore = get(), userRepository = get())
    }
}

