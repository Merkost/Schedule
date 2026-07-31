package ru.dvfu.appliances.notifications

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.push.PushListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.dvfu.appliances.navigation.MainDestinations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppNotifierListenersTest {

    @Test
    fun notificationListenerRoutesKnownAndUnknownPayloads() {
        val router = RecordingNotificationRouter()
        val listener: KMPNotifier.Listener = AppNotificationListener(router)

        mapOf(
            "MY_EVENT" to MainDestinations.BOOKING_LIST,
            "NEW_EVENT" to MainDestinations.BOOKING_LIST,
            "APPLIANCE" to MainDestinations.APPLIANCES_ROUTE,
            "UNKNOWN" to MainDestinations.HOME_ROUTE,
        ).forEach { (type, expectedRoute) ->
            listener.onNotificationClicked(mapOf("notificationType" to type))
            assertEquals(expectedRoute, router.routes.last())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pushListenerForwardsNonBlankTokens() = runTest {
        val receivedTokens = mutableListOf<String>()
        val listener: PushListener = AppPushListener(
            updateToken = { receivedTokens += it },
            scope = this,
        )

        listener.onNewToken("fcm-token")
        runCurrent()

        assertEquals(listOf("fcm-token"), receivedTokens)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pushListenerIgnoresBlankTokens() = runTest {
        var updateCalled = false
        val listener: PushListener = AppPushListener(
            updateToken = { updateCalled = true },
            scope = this,
        )

        listener.onNewToken("   ")
        runCurrent()

        assertTrue(!updateCalled)
    }

    private class RecordingNotificationRouter : NotificationNavRouter {
        val routes = mutableListOf<String?>()

        override fun navigateTo(route: String?) {
            routes += route
        }
    }
}
