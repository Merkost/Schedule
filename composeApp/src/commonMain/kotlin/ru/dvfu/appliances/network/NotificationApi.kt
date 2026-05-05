package ru.dvfu.appliances.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.dvfu.appliances.model.repository.entity.notifications.NotificationConstants
import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification

class NotificationApi(
    private val client: HttpClient,
    private val fcmServerKey: String,
) {
    suspend fun postNotification(payload: PushNotification) {
        if (fcmServerKey.isBlank()) return
        client.post("${NotificationConstants.BASE_URL}/fcm/send") {
            header(HttpHeaders.Authorization, "key=$fcmServerKey")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }
}
