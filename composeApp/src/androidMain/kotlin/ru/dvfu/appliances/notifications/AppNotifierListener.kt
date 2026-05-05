package ru.dvfu.appliances.notifications

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.navigation.MainDestinations

class AppNotifierListener(
    private val usersRepository: UsersRepository,
    private val router: NotificationNavRouter,
) : NotifierManager.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewToken(token: String) {
        if (token.isBlank()) return
        scope.launch { usersRepository.setNewMessagingToken(token) }
    }

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
