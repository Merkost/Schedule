package ru.dvfu.appliances.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController
import ru.dvfu.appliances.compose.ScheduleApp
import ru.dvfu.appliances.compose.ui.theme.ScheduleTheme
import ru.dvfu.appliances.di.initKoin

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    InternalCoroutinesApi::class,
    ExperimentalCoroutinesApi::class,
)
fun MainViewController(): UIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoinIos()
    }
    return ComposeUIViewController {
        ScheduleTheme {
            ScheduleApp()
        }
    }
}

fun initKoinIos() {
    initKoin()
}
