package ru.dvfu.appliances.network

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import org.kimplify.cedar.Cedar
import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification

private val log = Cedar.tag("FCM")

class NotificationApi {
    private val functions by lazy { Firebase.functions("asia-northeast1") }

    suspend fun postNotification(payload: PushNotification) {
        runCatching {
            functions.httpsCallable("sendNotification").invoke(payload)
        }.onFailure { e ->
            log.e("sendNotification failed", e)
        }
    }
}
