package ru.dvfu.appliances.model.repository.entity.notifications

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import ru.dvfu.appliances.model.repository.entity.notifications.NotificationConstants.CONTENT_TYPE

interface NotificationAPI {

    @Headers("Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Header("Authorization") authorization: String,
        @Body notification: PushNotification,
    ): Response<ResponseBody>
}