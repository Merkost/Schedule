package ru.dvfu.appliances.compose

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.navigation.MainDestinations

@Composable
fun rememberAppStateHolder(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navController: NavHostController = rememberNavController(),
    snackbarManager: SnackbarManager = SnackbarManager,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(snackbarHostState, navController, coroutineScope) {
    AppStateHolder(snackbarHostState, navController, snackbarManager, coroutineScope)
}

@Stable
class AppStateHolder(
    val snackbarHostState: SnackbarHostState,
    val navController: NavHostController,
    private val snackbarManager: SnackbarManager,
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

    val bottomBarTabs = HomeSections.entries.toTypedArray()
    private val innerScreensRoutes = listOf<String>(
        HomeSections.CALENDAR.route + "/" + MainDestinations.WEEK_CALENDAR,
        HomeSections.CALENDAR.route + "/" + MainDestinations.HOME_ROUTE,
    )
    private val bottomBarRoutes = bottomBarTabs.map { it.route } + innerScreensRoutes

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
                popUpTo(findStartDestination(navController.graph).id) {
                    saveState = true
                }
            }
        }
    }
}

fun NavController.navigateSingleTop(route: String) {
    navigate(route, navOptions { launchSingleTop = true })
}

private val NavGraph.startDestination: NavDestination?
    get() = findNode(startDestinationId)

private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
}
