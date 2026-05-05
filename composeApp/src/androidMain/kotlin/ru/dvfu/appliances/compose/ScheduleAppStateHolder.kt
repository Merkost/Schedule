package ru.dvfu.appliances.compose

import android.content.res.Resources
import android.os.Parcelable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.navigation.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.navigation.MainDestinations

/**
 * Remembers and creates an instance of [AppStateHolder]
 */
@Composable
fun rememberAppStateHolder(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navController: NavHostController = rememberNavController(),
    snackbarManager: SnackbarManager = SnackbarManager,
    resources: Resources = resources(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(snackbarHostState, navController, resources, coroutineScope) {
    AppStateHolder(snackbarHostState, navController, snackbarManager, resources, coroutineScope)
}

@Stable
class AppStateHolder(
    val snackbarHostState: SnackbarHostState,
    val navController: NavHostController,
    private val snackbarManager: SnackbarManager,
    private val resources: Resources,
    coroutineScope: CoroutineScope,
) {
    var current: Any? = null

    init {
        coroutineScope.launch {
            snackbarManager.messages.collect { currentMessages ->
                if (currentMessages.isNotEmpty()) {
                    val message = currentMessages[0]
                    val text = getString(message.messageId)
                    snackbarHostState.showSnackbar(text)
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
    val startDestinationId = findStartDestination(graph).id
    navigate(route) {
        if (HomeSections.values().map { it.route }.contains(route)) {
            launchSingleTop = true
            restoreState = true
            popUpTo(startDestinationId) {
                saveState = true
            }
        }
    }

    if (args.isNotEmpty()) {
        val entry = try {
            getBackStackEntry(route)
        } catch (_: IllegalArgumentException) {
            currentBackStackEntry
        }
        entry?.savedStateHandle?.apply {
            args.forEach { (key, arg) -> set(key, arg) }
        }
        entry?.arguments?.apply {
            args.forEach { (key, arg) -> putParcelable(key, arg) }
        }
    }
}

inline fun <reified T : Parcelable> NavBackStackEntry.requiredArg(key: String): T {
    savedStateHandle.get<T>(key)?.let { return it }
    return requireNotNull(arguments?.getParcelable(key)) { "argument for $key is null" }
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