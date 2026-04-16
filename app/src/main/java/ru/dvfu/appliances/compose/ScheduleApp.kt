package ru.dvfu.appliances.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.compose.koinViewModel
import ru.dvfu.appliances.compose.appliance.AddUsersToAppliance
import ru.dvfu.appliances.compose.appliance.ApplianceDetails
import ru.dvfu.appliances.compose.appliance.NewAppliance
import ru.dvfu.appliances.compose.home.*
import ru.dvfu.appliances.compose.home.booking_list.BookingList
import ru.dvfu.appliances.compose.home.profile.EditProfile
import java.time.LocalDate

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@Composable
fun ScheduleApp() {
    val appStateHolder = rememberAppStateHolder()
    val viewModel: MainScreenViewModel = koinViewModel()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (appStateHolder.shouldShowBottomBar) {
                ScheduleBottomBar(
                    tabs = appStateHolder.bottomBarTabs,
                    currentRoute = appStateHolder.currentRoute!!,
                    navigateToRoute = appStateHolder::navigateToBottomBarRoute,
                )
            }
        },
        snackbarHost = { SnackbarHost(appStateHolder.snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = appStateHolder.navController,
            startDestination = MainDestinations.HOME_ROUTE,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            NavGraph(
                navController = appStateHolder.navController,
                upPress = appStateHolder::upPress,
            )
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
private fun NavGraphBuilder.NavGraph(
    navController: NavController,
    backPress: () -> Unit = { navController.popBackStack() },
    upPress: () -> Unit,
) {
    navigation(
        route = MainDestinations.HOME_ROUTE,
        startDestination = HomeSections.CALENDAR.route,
    ) {
        addHomeGraph(navController = navController, backPress = upPress)
    }

    composable(MainDestinations.ADD_EVENT) {
        val selectedDate = it.arguments?.getParcelable<SelectedDate>(Arguments.DATE)?.value ?: LocalDate.now()
        AddEvent(selectedDate = selectedDate, upPress)
    }
    composable(MainDestinations.EVENT_INFO) {
        EventInfoScreen(navController, eventArg = it.requiredArg(Arguments.EVENT), backPress)
    }
    composable(MainDestinations.EDIT_PROFILE) {
        EditProfile { navController.popBackStack() }
    }
    composable(MainDestinations.APPLIANCE_ROUTE) {
        ApplianceDetails(navController, upPress, it.requiredArg(Arguments.APPLIANCE))
    }
    composable(MainDestinations.ADD_USER_TO_APPLIANCE) {
        AddUsersToAppliance(navController, it.requiredArg(Arguments.APPLIANCE))
    }
    composable(MainDestinations.ADD_SUPERUSER_TO_APPLIANCE) {
        AddUsersToAppliance(navController, it.requiredArg(Arguments.APPLIANCE), areSuperUsers = true)
    }
    composable(MainDestinations.APPLIANCES_ROUTE) { Appliances(navController, upPress) }
    composable(MainDestinations.NEW_APPLIANCE_ROUTE) { NewAppliance(upPress) }
    composable(MainDestinations.USER_DETAILS_ROUTE) {
        UserDetails(navController, upPress, it.requiredArg(Arguments.USER))
    }
    composable(MainDestinations.USERS_ROUTE) { Users(navController, upPress) }
    composable(MainDestinations.BOOKING_LIST) { BookingList(navController = navController) }
    composable(MainDestinations.SETTINGS_ROUTE) { Settings(navController, upPress) }
}
