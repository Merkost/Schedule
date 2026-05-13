package ru.dvfu.appliances.network

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.functions.functions
import org.kimplify.cedar.Cedar
import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification

private val log = Cedar.tag("FCM")

class NotificationApi {
    private val functions by lazy { Firebase.functions("asia-northeast1") }

    suspend fun postNotification(payload: PushNotification) {
        try {
            Firebase.auth.currentUser?.getIdToken(true)
            functions.httpsCallable("sendNotification").invoke(payload)
        } catch (e: Throwable) {
            log.e("sendNotification failed: ${e::class.simpleName}: ${e.message}", e)
            throw e
        }
    }
}
