package ru.dvfu.appliances

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import ru.dvfu.appliances.model.FirebaseMessagingViewModel
import ru.dvfu.appliances.model.utils.Constants
import ru.dvfu.appliances.model.utils.Constants.NOTIFICATION_CHANNEL_ID
import java.util.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val viewModel: FirebaseMessagingViewModel by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.body?.let { Log.d("MSG", it) }
        remoteMessage.notification?.let {
            val channelId = getNotificationTypeChannel(remoteMessage.data)
            showNotification(this, notification = it, channelId = channelId)
        }
    }

    private fun getNotificationTypeChannel(data: Map<String, String>): String =
        data["notificationType"]?.let { Constants.NotificationType.valueOf(it).channelId } ?: NOTIFICATION_CHANNEL_ID

    private fun showNotification(
        context: Context,
        notification: RemoteMessage.Notification,
        channelId: String,
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val bitmap = notification.imageUrl?.let { url ->
            runCatching {
                val target = Glide.with(context).asBitmap().load(url).submit()
                val result = target.get()
                Glide.with(context).clear(target)
                result
            }.getOrNull()
        }

        val builder = Notification.Builder(this, channelId)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setStyle(Notification.BigTextStyle())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title ?: "")
            .setContentText(notification.body ?: "")
        if (bitmap != null) builder.setLargeIcon(bitmap)
        notificationManager.notify(Random().nextInt(), builder.build())
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        viewModel.onNewToken(s)
        Log.d("NEW_TOKEN", s)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }
}