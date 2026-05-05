package ru.dvfu.appliances.notifications

interface NotificationNavRouter {
    fun navigateTo(route: String?)
}

object NotificationNavRouterDelegate : NotificationNavRouter {
    private var current: NotificationNavRouter? = null

    fun attach(router: NotificationNavRouter) {
        current = router
    }

    fun detach() {
        current = null
    }

    override fun navigateTo(route: String?) {
        current?.navigateTo(route)
    }
}
