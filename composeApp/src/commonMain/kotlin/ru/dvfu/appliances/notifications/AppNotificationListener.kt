package ru.dvfu.appliances.notifications

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.notification.PayloadData
import ru.dvfu.appliances.navigation.MainDestinations

class AppNotificationListener(
    private val router: NotificationNavRouter,
) : KMPNotifier.Listener {

    override fun onNotificationClicked(data: PayloadData) {
        val type = data["notificationType"] as? String
        router.navigateTo(resolveRoute(type))
    }

    private fun resolveRoute(type: String?): String? = when (type) {
        "MY_EVENT", "NEW_EVENT" -> MainDestinations.BOOKING_LIST
        "APPLIANCE" -> MainDestinations.APPLIANCES_ROUTE
        else -> MainDestinations.HOME_ROUTE
    }
}
