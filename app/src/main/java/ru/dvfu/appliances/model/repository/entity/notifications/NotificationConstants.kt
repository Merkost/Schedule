package ru.dvfu.appliances.model.repository.entity.notifications

object NotificationConstants {
    const val BASE_URL = "https://fcm.googleapis.com"
    // FCM server keys must never ship in the client; push sending belongs on a trusted backend.
    const val SERVER_KEY = ""
    const val CONTENT_TYPE = "application/json"
}
