package ru.dvfu.appliances.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.mmk.kmpnotifier.notification.NotifierManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import ru.dvfu.appliances.compose.appliance.AddUsersToAppliance
import ru.dvfu.appliances.compose.appliance.ApplianceDetails
import ru.dvfu.appliances.compose.appliance.NewAppliance
import ru.dvfu.appliances.compose.home.*
import ru.dvfu.appliances.compose.home.booking_list.BookingList
import ru.dvfu.appliances.compose.home.profile.EditProfile
import ru.dvfu.appliances.compose.use_cases.GetApplianceUseCase
import ru.dvfu.appliances.compose.use_cases.GetEventByIdUseCase
import ru.dvfu.appliances.compose.use_cases.GetUserUseCase
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.navigation.AddEventRoute
import ru.dvfu.appliances.navigation.AddSuperuserToApplianceRoute
import ru.dvfu.appliances.navigation.AddUserToApplianceRoute
import ru.dvfu.appliances.navigation.ApplianceRoute
import ru.dvfu.appliances.navigation.AppliancesRoute
import ru.dvfu.appliances.navigation.BookingListRoute
import ru.dvfu.appliances.navigation.EditProfileRoute
import ru.dvfu.appliances.navigation.EventInfoRoute
import ru.dvfu.appliances.navigation.HomeRoute
import ru.dvfu.appliances.navigation.LoginRoute
import ru.dvfu.appliances.navigation.MainDestinations
import ru.dvfu.appliances.navigation.NewApplianceRoute
import ru.dvfu.appliances.navigation.SettingsRoute
import ru.dvfu.appliances.navigation.UserDetailsRoute
import ru.dvfu.appliances.navigation.UsersRoute
import ru.dvfu.appliances.notifications.AppNotifierListener
import ru.dvfu.appliances.notifications.NavControllerNotificationRouter
import ru.dvfu.appliances.notifications.NotificationNavRouterDelegate
import ru.dvfu.appliances.ui.LoginScreen
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@Composable
fun ScheduleApp() {
    val appStateHolder = rememberAppStateHolder()
    val notifierListener: AppNotifierListener = koinInject()

    LaunchedEffect(notifierListener) {
        NotifierManager.addListener(notifierListener)
    }
    DisposableEffect(appStateHolder.navController) {
        NotificationNavRouterDelegate.attach(
            NavControllerNotificationRouter(appStateHolder.navController)
        )
        onDispose { NotificationNavRouterDelegate.detach() }
    }

    val isAuthenticated by produceState<Boolean?>(initialValue = null) {
        Firebase.auth.authStateChanged
            .map { it != null }
            .distinctUntilChanged()
            .collect { value = it }
    }

    LaunchedEffect(isAuthenticated) {
        val authed = isAuthenticated ?: return@LaunchedEffect
        val controller = appStateHolder.navController
        val current = controller.currentDestination?.route ?: return@LaunchedEffect
        val onLogin = current.contains("LoginRoute")
        if (authed && onLogin) {
            controller.navigate(MainDestinations.HOME_ROUTE) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!authed && !onLogin) {
            controller.navigate(LoginRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
        if (isAuthenticated == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        val start: Any = if (isAuthenticated == true) MainDestinations.HOME_ROUTE else LoginRoute
        NavHost(
            navController = appStateHolder.navController,
            startDestination = start,
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
    composable<LoginRoute> {
        LoginScreen()
    }

    navigation(
        route = MainDestinations.HOME_ROUTE,
        startDestination = HomeSections.CALENDAR.route,
    ) {
        addHomeGraph(navController = navController, backPress = upPress)
    }

    composable<AddEventRoute> { entry ->
        val r = entry.toRoute<AddEventRoute>()
        AddEvent(selectedDate = LocalDate.fromEpochDays(r.dateEpochDay.toInt()), upPress)
    }
    composable<EventInfoRoute> { entry ->
        val r = entry.toRoute<EventInfoRoute>()
        LoadCalendarEvent(eventId = r.eventId) { event ->
            EventInfoScreen(navController, eventArg = event, backPress)
        }
    }
    composable<EditProfileRoute> {
        EditProfile { navController.popBackStack() }
    }
    composable<ApplianceRoute> { entry ->
        val r = entry.toRoute<ApplianceRoute>()
        LoadAppliance(applianceId = r.applianceId) { appliance ->
            ApplianceDetails(navController, upPress, appliance)
        }
    }
    composable<AddUserToApplianceRoute> { entry ->
        val r = entry.toRoute<AddUserToApplianceRoute>()
        LoadAppliance(applianceId = r.applianceId) { appliance ->
            AddUsersToAppliance(navController, appliance)
        }
    }
    composable<AddSuperuserToApplianceRoute> { entry ->
        val r = entry.toRoute<AddSuperuserToApplianceRoute>()
        LoadAppliance(applianceId = r.applianceId) { appliance ->
            AddUsersToAppliance(navController, appliance, areSuperUsers = true)
        }
    }
    composable<AppliancesRoute> { Appliances(navController, upPress) }
    composable<NewApplianceRoute> { NewAppliance(upPress) }
    composable<UserDetailsRoute> { entry ->
        val r = entry.toRoute<UserDetailsRoute>()
        LoadUser(userId = r.userId) { user ->
            UserDetails(navController, upPress, user)
        }
    }
    composable<UsersRoute> { Users(navController, upPress) }
    composable<BookingListRoute> { BookingList(navController = navController) }
    composable<SettingsRoute> { Settings(navController, upPress) }
}

@Composable
private fun LoadCalendarEvent(eventId: String, content: @Composable (CalendarEvent) -> Unit) {
    val useCase = koinInject<GetEventByIdUseCase>()
    val result by produceState<Result<CalendarEvent>?>(initialValue = null, key1 = eventId) {
        useCase(eventId).collect { value = it }
    }
    val event = result?.getOrNull()
    if (event == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        content(event)
    }
}

@Composable
private fun LoadAppliance(applianceId: String, content: @Composable (Appliance) -> Unit) {
    val useCase = koinInject<GetApplianceUseCase>()
    val result by produceState<Result<Appliance>?>(initialValue = null, key1 = applianceId) {
        useCase(applianceId).collect { value = it }
    }
    val appliance = result?.getOrNull()
    if (appliance == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        content(appliance)
    }
}

@Composable
private fun LoadUser(userId: String, content: @Composable (User) -> Unit) {
    val useCase = koinInject<GetUserUseCase>()
    val result by produceState<Result<User>?>(initialValue = null, key1 = userId) {
        useCase(userId).collect { value = it }
    }
    val user = result?.getOrNull()
    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        content(user)
    }
}
