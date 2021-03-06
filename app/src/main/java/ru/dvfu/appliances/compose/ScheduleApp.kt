package ru.dvfu.appliances.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.compose.getViewModel
import ru.dvfu.appliances.compose.appliance.AddUsersToAppliance
import ru.dvfu.appliances.compose.appliance.ApplianceDetails
import ru.dvfu.appliances.compose.appliance.NewAppliance
import ru.dvfu.appliances.compose.home.*
import ru.dvfu.appliances.compose.home.booking_list.BookingList
import ru.dvfu.appliances.compose.home.profile.EditProfile
import java.time.LocalDate

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@InternalCoroutinesApi
@Composable
fun ScheduleApp() {
    val appStateHolder = rememberAppStateHolder()
    val viewModel: MainScreenViewModel = getViewModel()
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            if (appStateHolder.shouldShowBottomBar) {
                ScheduleBottomBar(
                    tabs = appStateHolder.bottomBarTabs,
                    currentRoute = appStateHolder.currentRoute!!,
                    navigateToRoute = appStateHolder::navigateToBottomBarRoute
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = it,
                modifier = Modifier.navigationBarsWithImePadding(),
                snackbar = { snackbarData -> AppSnackbar(snackbarData) }
            )
        },
        scaffoldState = appStateHolder.scaffoldState,
        modifier = Modifier.navigationBarsWithImePadding()
    ) { innerPaddingModifier ->
        NavHost(
            navController = appStateHolder.navController,
            startDestination = MainDestinations.HOME_ROUTE,
            modifier = Modifier.padding(innerPaddingModifier)
        ) {
            NavGraph(
                navController = appStateHolder.navController,
                upPress = appStateHolder::upPress,
            )
        }
    }
}


@ExperimentalComposeUiApi
@OptIn(ExperimentalPagerApi::class)
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalMaterialApi
private fun NavGraphBuilder.NavGraph(
    navController: NavController,
    backPress: () -> Unit = { navController.popBackStack() },
    upPress: () -> Unit
) {
    navigation(
        route = MainDestinations.HOME_ROUTE,
        startDestination = HomeSections.CALENDAR.route
    ) {
        addHomeGraph(
            navController = navController,
            backPress = upPress
        )
    }

    composable(MainDestinations.ADD_EVENT) {
        val selectedDate =
            it.arguments?.getParcelable<SelectedDate>(Arguments.DATE)?.value ?: LocalDate.now()
        AddEvent(selectedDate = selectedDate, upPress)
    }

    composable(MainDestinations.EVENT_INFO) {
        EventInfoScreen(navController, eventArg = it.requiredArg(Arguments.EVENT), backPress)
    }

    composable(MainDestinations.EDIT_PROFILE) {
        EditProfile() {
            navController.popBackStack()
        }
    }


    /*composable(MainDestinations.LOGIN_ROUTE) {
        LoginScreen(navController = navController)
    }*/

    composable(
        route = MainDestinations.APPLIANCE_ROUTE,
    ) { ApplianceDetails(navController, upPress, it.requiredArg(Arguments.APPLIANCE)) }

    composable(
        route = MainDestinations.ADD_USER_TO_APPLIANCE,
    ) { AddUsersToAppliance(navController, it.requiredArg(Arguments.APPLIANCE)) }

    composable(
        route = MainDestinations.ADD_SUPERUSER_TO_APPLIANCE,
    ) {
        AddUsersToAppliance(
            navController,
            it.requiredArg(Arguments.APPLIANCE),
            areSuperUsers = true
        )
    }

    composable(
        route = MainDestinations.APPLIANCES_ROUTE,
    ) { Appliances(navController, upPress) }

    composable(
        route = MainDestinations.NEW_APPLIANCE_ROUTE,
    ) { NewAppliance(upPress) }

    composable(
        route = MainDestinations.USER_DETAILS_ROUTE,
    ) { UserDetails(navController, upPress, it.requiredArg(Arguments.USER)) }

    composable(
        route = MainDestinations.USERS_ROUTE,
    ) { Users(navController, upPress) }

    composable(
        route = MainDestinations.BOOKING_LIST
    ) {
        BookingList(navController = navController)
    }

    composable(
        route = MainDestinations.SETTINGS_ROUTE,
    ) { Settings(navController, upPress) }

}

@Composable
fun AppSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = CircleShape,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    //actionColor: Color = JetsnackTheme.colors.brand,
    elevation: Dp = 6.dp
) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        //actionColor = actionColor,
        elevation = elevation
    )
}
