package ru.dvfu.appliances.model.repository.entity.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    @SerialName("notificationType")
    val notificationType: String = "DEFAULT",
)
