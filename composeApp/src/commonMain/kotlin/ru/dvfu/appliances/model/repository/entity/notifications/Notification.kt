package ru.dvfu.appliances.model.repository.entity.notifications

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val title: String,
    val body: String,
)
