package ru.dvfu.appliances.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    repoModule: Module = repositoryModule,
    extraModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(
        listOf(
            platformModule(),
            networkModule,
            application,
            mainActivity,
            repoModule,
        ) + extraModules
    )
}
