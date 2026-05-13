package ru.dvfu.appliances.model.repository.entity.notifications

import kotlinx.serialization.Serializable

@Serializable
data class PushNotification(
    val to: String,
    val notification: Notification,
    val data: NotificationData,
)
