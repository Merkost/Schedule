package ru.dvfu.appliances.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import ru.dvfu.appliances.AppBuildConfig
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.di.application
import ru.dvfu.appliances.di.mainActivity
import ru.dvfu.appliances.di.mockRepositoryModule
import ru.dvfu.appliances.di.networkModule
import ru.dvfu.appliances.di.platformModule
import ru.dvfu.appliances.di.repositoryModule
import ru.dvfu.appliances.model.utils.Constants

class Schedule : Application() {

    override fun onCreate() {
        super.onCreate()
        AppDebug.init(this)

        startKoin {
            androidLogger(if (AppDebug.isDebug) Level.ERROR else Level.NONE)
            androidContext(this@Schedule)
            val repoModule = if (AppBuildConfig.USE_MOCK_REPOS) mockRepositoryModule else repositoryModule
            modules(
                listOf(
                    platformModule(),
                    networkModule,
                    application,
                    mainActivity,
                    repoModule
                )
            )
        }

        if (AppDebug.isDebug) { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false) }
        createNotificationChannels()
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