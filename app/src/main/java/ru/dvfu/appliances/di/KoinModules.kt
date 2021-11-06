package ru.dvfu.appliances.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.dvfu.appliances.Logger
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.viewmodels.NewApplianceViewModel
import ru.dvfu.appliances.compose.viewmodels.AppliancesViewModel
import ru.dvfu.appliances.compose.viewmodels.ProfileViewModel
import ru.dvfu.appliances.compose.viewmodels.UsersViewModel
import ru.dvfu.appliances.model.repository.CloudFirestoreDatabaseImpl
import ru.dvfu.appliances.model.repository.Repository
import ru.dvfu.appliances.model.repository.FirebaseUserRepositoryImpl
import ru.dvfu.appliances.model.repository.UserRepository
import ru.dvfu.appliances.model.viewmodels.ApplianceViewModel
import ru.dvfu.appliances.model.viewmodels.LoginViewModel
import ru.dvfu.appliances.model.viewmodels.MainViewModel
import ru.dvfu.appliances.model.viewmodels.UserDetailsViewModel

val application = module {
    viewModel { MainViewModel() }

    single<Repository> { CloudFirestoreDatabaseImpl() }
    single<UserRepository> { FirebaseUserRepositoryImpl(androidContext()) }

    single { Logger() }
    single { SnackbarManager }
}

val mainActivity = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { UserDetailsViewModel(get(), get()) }
    viewModel { UsersViewModel(get()) }
    viewModel { ApplianceViewModel(get(),get()) }
    viewModel { NewApplianceViewModel(get()) }
    viewModel { AppliancesViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
}