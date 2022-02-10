package ru.dvfu.appliances.compose

import Drawer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.google.accompanist.pager.ExperimentalPagerApi
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.appliance.AddUser
import ru.dvfu.appliances.compose.appliance.Appliance
import ru.dvfu.appliances.compose.appliance.NewAppliance
import ru.dvfu.appliances.compose.components.FabMenuItem
import ru.dvfu.appliances.compose.components.FabWithMenu
import ru.dvfu.appliances.compose.components.MultiFabState

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@InternalCoroutinesApi
@Composable
fun ScheduleApp() {
    //ProvideWindowInsets {
    MaterialTheme {
        val appStateHolder = rememberAppStateHolder()
        //var visible by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val result = remember { mutableStateOf("") }
        val selectedItem = remember { mutableStateOf("upload") }

        Scaffold(
            /*floatingActionButton = {
                if (appStateHolder.shouldShowFab) {
                    FloatingActionButton(
                        onClick = { result.value = "FAB clicked" },
                        backgroundColor = Color(0xFFFF8C00),

                    ) {
                        Icon(Icons.Filled.Add, "")
                    }
                }
            },*/
            isFloatingActionButtonDocked = true,
            floatingActionButtonPosition = FabPosition.Center,
            drawerContent = {
                Drawer(
                    scope = scope,
                    scaffoldState = appStateHolder.scaffoldState,
                    navController = appStateHolder.navController
                )
            },
            bottomBar = {
                if (appStateHolder.shouldShowBottomBar) {
                    ScheduleBottomBar(
                        tabs = appStateHolder.bottomBarTabs,
                        currentRoute = appStateHolder.currentRoute!!,
                        navigateToRoute = appStateHolder::navigateToBottomBarRoute
                    )
                }
                //ScheduleBottomBar(result, selectedItem, fabShape)
            },
            scaffoldState = appStateHolder.scaffoldState
        ) { innerPaddingModifier ->
            //Spacer(modifier = Modifier.statusBarsHeight())
            NavHost(
                navController = appStateHolder.navController,
                startDestination = MainDestinations.HOME_ROUTE,
                modifier = Modifier.padding(innerPaddingModifier)
            ) {
                NavGraph(
                    navController = appStateHolder.navController,
                    upPress = appStateHolder::upPress,
                    openDrawer = { scope.launch { appStateHolder.scaffoldState.drawerState.open() } }
                )
            }
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
    upPress: () -> Unit,
    openDrawer: () -> Unit,
    navController: NavController,
    backPress: () -> Unit = { navController.popBackStack() }
) {
    navigation(
        route = MainDestinations.HOME_ROUTE,
        startDestination = HomeSections.CALENDAR.route
    ) {
        addHomeGraph(navController, openDrawer = openDrawer, upPress)
    }

    composable(MainDestinations.ADD_EVENT) {
        AddEvent(navController = navController)
    }

    /*composable(MainDestinations.LOGIN_ROUTE) {
        LoginScreen(navController = navController)
    }*/

    composable(
        route = MainDestinations.APPLIANCE_ROUTE,
    ) { Appliance(navController, upPress, it.requiredArg(Arguments.APPLIANCE)) }

    composable(
        route = MainDestinations.ADD_USER_TO_APPLIANCE,
    ) { AddUser(navController, it.requiredArg(Arguments.APPLIANCE)) }

    composable(
        route = MainDestinations.ADD_SUPERUSER_TO_APPLIANCE,
    ) { AddUser(navController, it.requiredArg(Arguments.APPLIANCE), areSuperUsers = true) }

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
        route = MainDestinations.SETTINGS_ROUTE,
    ) { Settings(navController, upPress) }
}

@Composable
fun AddEvent(navController: NavController) {

    Scaffold(topBar = {
        ScheduleAppBar(title = "Добавление события", backClick = navController::popBackStack)
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            SelectableCalendar()
        }
    }

}


@Composable
fun MainScreen(navController: NavController, openDrawer: () -> Unit) {

    val fabState = remember { mutableStateOf(MultiFabState.COLLAPSED) }

    Scaffold(topBar = {
        TopAppBar(
            title = { /*Text(text = R.string.androidx_startup)*/ },
            navigationIcon = {
                IconButton(onClick = { openDrawer() }
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "")
                }
            },
            backgroundColor = Color(0xFFFF5470)
        )
    }, floatingActionButton = {
        FabWithMenu(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .zIndex(5f),
            fabState = fabState,
            items = listOf(
                FabMenuItem(
                    icon = Icons.Default.MoreTime,
                    text = "Создать бронирование",
                    onClick = { navController.navigate(MainDestinations.ADD_EVENT) }
                ),
                FabMenuItem(
                    icon = Icons.Default.AddTask,
                    text = "Создать событие",
                    onClick = { navController.navigate(MainDestinations.ADD_EVENT) }
                )
            )
        )
    }
    ) {
        AnimatedVisibility(
            fabState.value == MultiFabState.EXPANDED,
            modifier = Modifier
                .zIndex(4f)
                .fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(0.6f))
                    .clickable(role = Role.Image) {
                        fabState.value = MultiFabState.COLLAPSED
                    })
        }

        Box(
            Modifier
                .background(Color(0XFFE3DAC9))
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Text(
                text = stringResource(id = R.string.calendar),
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}