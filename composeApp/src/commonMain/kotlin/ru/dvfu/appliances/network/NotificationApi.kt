package ru.dvfu.appliances.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.kimplify.cedar.Cedar
import ru.dvfu.appliances.model.repository.entity.notifications.NotificationConstants
import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification

private val log = Cedar.tag("FCM")

class NotificationApi(
    private val client: HttpClient,
    private val fcmServerKey: String,
) {
    suspend fun postNotification(payload: PushNotification) {
        if (fcmServerKey.isBlank()) return
        val res = client.post("${NotificationConstants.BASE_URL}/fcm/send") {
            header(HttpHeaders.Authorization, "key=$fcmServerKey")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        if (!res.status.isSuccess()) {
            log.e("legacy /fcm/send returned ${res.status.value} — may be deprecated; see docs/2026-05-08-fcm-v1-migration.md")
        }
    }
}
