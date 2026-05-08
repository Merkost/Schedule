package ru.dvfu.appliances.notifications

import androidx.navigation.NavController

class NavControllerNotificationRouter(
    private val navController: NavController,
) : NotificationNavRouter {

    override fun navigateTo(route: String?) {
        if (route.isNullOrBlank()) return
        navController.navigate(route)
    }
}
