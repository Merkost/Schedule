package ru.dvfu.appliances.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    extraModules: List<org.koin.core.module.Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(
        listOf(
            platformModule(),
            networkModule,
            application,
            mainActivity,
            repositoryModule,
        ) + extraModules
    )
}
