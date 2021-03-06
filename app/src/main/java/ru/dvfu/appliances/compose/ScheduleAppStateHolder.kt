package ru.dvfu.appliances.compose

import android.content.res.Resources
import android.os.Parcelable
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.navigation.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.dvfu.appliances.application.SnackbarManager

/**
 * Destinations used in the [FishingNotesApp].
 */
object MainDestinations {

    const val EDIT_PROFILE = "edit_profile"
    const val EVENT_CALENDAR = "event_calendar"
    const val WEEK_CALENDAR = "week_calendar"
    const val TEMP = "temp"

    const val ADD_EVENT = "add_event"
    const val ADD_BOOKING = "add_booking"


    const val EVENT_INFO = "event_info"


    const val LOGIN_ROUTE = "login"
    const val HOME_ROUTE = "home"

    const val SETTINGS_ROUTE = "settings"
    const val APPLIANCE_ROUTE = "appliance"
    const val ADD_USER_TO_APPLIANCE = "add_user_to_appliance"
    const val ADD_SUPERUSER_TO_APPLIANCE = "add_superuser_to_appliance"
    const val APPLIANCES_ROUTE = "appliances"
    const val NEW_APPLIANCE_ROUTE = "new_appliance"

    const val BOOKING_LIST = "booking_list_screen"

    const val USERS_ROUTE = "users"
    const val USER_DETAILS_ROUTE = "user_details"



    const val NOTES_ROUTE = "notes"

    const val SNACK_ID_KEY = "snackId"
}

object Arguments {
    const val DATE = "date_arg"
    const val EVENT = "event_arg"
    const val USER = "user_arg"
    const val APPLIANCE = "appliance_arg"

}

/**
 * Remembers and creates an instance of [AppStateHolder]
 */
@Composable
fun rememberAppStateHolder(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
    snackbarManager: SnackbarManager = SnackbarManager,
    resources: Resources = resources(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) =
    remember(scaffoldState, navController, /*snackbarManager,*/ resources, coroutineScope) {
        AppStateHolder(scaffoldState, navController, snackbarManager, resources, coroutineScope)
    }

/**
 * Responsible for holding state related to [App] and containing UI-related logic.
 */
@Stable
class AppStateHolder(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    private val snackbarManager: SnackbarManager,
    private val resources: Resources,
    coroutineScope: CoroutineScope
) {
    var current: Any? = null

    // Process snackbars coming from SnackbarManager
    init {
        coroutineScope.launch {
            snackbarManager.messages.collect { currentMessages ->
                if (currentMessages.isNotEmpty()) {
                    val message = currentMessages[0]
                    val text = resources.getText(message.messageId)

                    // Display the snackbar on the screen. `showSnackbar` is a function
                    // that suspends until the snackbar disappears from the screen
                    scaffoldState.snackbarHostState.showSnackbar(text.toString())
                    // Once the snackbar is gone or dismissed, notify the SnackbarManager
                    snackbarManager.setMessageShown(message.id)
                }
            }
        }
    }

    // ----------------------------------------------------------
    // BottomBar state source of truth
    // ----------------------------------------------------------

    val bottomBarTabs = HomeSections.values()
    private val innerScreensRoutes = listOf<String>(
        HomeSections.CALENDAR.route + "/" + MainDestinations.WEEK_CALENDAR,
        HomeSections.CALENDAR.route + "/" + MainDestinations.HOME_ROUTE,
    )
    private val bottomBarRoutes = bottomBarTabs.map { it.route } + innerScreensRoutes

    // Reading this attribute will cause recompositions when the bottom bar needs shown, or not.
    // Not all routes need to show the bottom bar.
    val shouldShowBottomBar: Boolean
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination?.route.apply {
                current = this
            } in bottomBarRoutes

    val shouldShowFab: Boolean
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination?.route == HomeSections.CALENDAR.route

    val currentRoute: String?
        get() = navController.currentDestination?.route

    fun upPress() {
        navController.navigateUp()
    }

    fun navigateToBottomBarRoute(route: String) {
        if (route != currentRoute) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                // Pop up backstack to the first destination and save state. This makes going back
                // to the start destination when pressing back in any other bottom tab.
                popUpTo(findStartDestination(navController.graph).id) {
                    saveState = true
                }
            }
        }
    }
}

fun NavController.navigate(route: String, vararg args: Pair<String, Parcelable>) {
    navigate(route) {
        if (HomeSections.values().map { it.route }.contains(route)) {
            launchSingleTop = true
            restoreState = true
            // Pop up backstack to the first destination and save state. This makes going back
            // to the start destination when pressing back in any other bottom tab.
            popUpTo(findStartDestination(this@navigate.graph).id) {
                saveState = true
            }
        }
    }

    requireNotNull(currentBackStackEntry?.arguments).apply {
        args.forEach { (key: String, arg: Parcelable) ->
            putParcelable(key, arg)
        }
    }
}

inline fun <reified T : Parcelable> NavBackStackEntry.requiredArg(key: String): T {
    return requireNotNull(arguments) { "arguments bundle is null" }.run {
        requireNotNull(getParcelable(key)) { "argument for $key is null" }
    }
}

fun NavController.navigateSingleTop(route: String) {
    navigate(route, navOptions { launchSingleTop = true })
}

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav calendarEvent.
 *
 * This is used to de-duplicate navigation events.
 */
private fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED

private val NavGraph.startDestination: NavDestination?
    get() = findNode(startDestinationId)

/**
 * Copied from similar function in NavigationUI.kt
 *
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-ui/src/main/java/androidx/navigation/ui/NavigationUI.kt
 */
private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
}