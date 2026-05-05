package ru.dvfu.appliances.compose

import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.home.Appliances
import ru.dvfu.appliances.compose.home.HomeScreen
import ru.dvfu.appliances.compose.home.profile.Profile

@ExperimentalFoundationApi
@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
fun NavGraphBuilder.addHomeGraph(
    backPress: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    composable(HomeSections.CALENDAR.route) {
        HomeScreen(navController, backPress)
    }
    composable(HomeSections.APPLIANCES.route) {
        Appliances(navController, backPress)
    }
    composable(HomeSections.PROFILE.route) {
        Profile(navController, modifier, backPress)
    }
}

enum class HomeSections(
    val title: org.jetbrains.compose.resources.StringResource,
    val icon: ImageVector,
    val route: String,
) {
    CALENDAR(Res.string.calendar, Icons.Outlined.Home, "home/calendar"),
    APPLIANCES(Res.string.appliances, Icons.Outlined.Apartment, "home/appliances"),
    PROFILE(Res.string.profile, Icons.Outlined.VerifiedUser, "home/profile"),
}

@Composable
fun ScheduleBottomBar(
    tabs: Array<HomeSections>,
    currentRoute: String,
    navigateToRoute: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        tabs.forEach { section ->
            val selected = section.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { navigateToRoute(section.route) },
                icon = { Icon(section.icon, contentDescription = section.name) },
                label = {
                    Text(
                        text = stringResource(section.title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
