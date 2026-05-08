package ru.dvfu.appliances.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.kimplify.cedar.Cedar
import org.kimplify.cedar.ConsoleTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import ru.dvfu.appliances.AppBuildConfig
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.R
import ru.dvfu.appliances.di.androidAppModule
import ru.dvfu.appliances.di.application
import ru.dvfu.appliances.di.initKoin
import ru.dvfu.appliances.di.mainActivity
import ru.dvfu.appliances.di.mockRepositoryModule
import ru.dvfu.appliances.di.networkModule
import ru.dvfu.appliances.di.platformModule
import ru.dvfu.appliances.di.repositoryModule
import ru.dvfu.appliances.init
import ru.dvfu.appliances.model.utils.Constants

class Schedule : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext
        AppDebug.init(this)
        Cedar.plant(ConsoleTree)

        if (AppBuildConfig.USE_MOCK_REPOS) {
            startKoin {
                androidLogger(if (AppDebug.isDebug) Level.ERROR else Level.NONE)
                androidContext(this@Schedule)
                modules(
                    listOf(
                        platformModule(),
                        networkModule,
                        application,
                        mainActivity,
                        androidAppModule,
                        mockRepositoryModule,
                    )
                )
            }
        } else {
            initKoin(extraModules = listOf(androidAppModule)) {
                androidLogger(if (AppDebug.isDebug) Level.ERROR else Level.NONE)
                androidContext(this@Schedule)
            }
        }

        if (AppDebug.isDebug) { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false) }
        createNotificationChannels()

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            NotifierManager.initialize(
                NotificationPlatformConfiguration.Android(
                    notificationIconResId = R.mipmap.ic_launcher,
                    showPushNotification = true,
                )
            )
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Constants.NotificationType.entries.forEach { notificationType ->
            val notificationChannel = NotificationChannel(
                notificationType.channelId, notificationType.title,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = notificationType.description
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}
