package ru.dvfu.appliances.model.repository.entity.notifications

import ru.dvfu.appliances.BuildConfig

object NotificationConstants {
    const val BASE_URL = "https://fcm.googleapis.com"
    val SERVER_KEY: String = BuildConfig.FCM_SERVER_KEY
    const val CONTENT_TYPE = "application/json"
}
