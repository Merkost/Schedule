package ru.dvfu.appliances.compose.appliance

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.model.repository.entity.Appliance

typealias ComposableFun = @Composable (navController: NavController, appliance: Appliance) -> Unit

sealed class TabItem(var icon: ImageVector, var titleRes: org.jetbrains.compose.resources.StringResource, var screen: ComposableFun) {

    @ExperimentalAnimationApi
    object Users : TabItem(Icons.Default.People, Res.string.users, { navController, appliance ->
        ApplianceUsers(navController = navController, appliance)
    })

    @ExperimentalAnimationApi
    object SuperUsers :
        TabItem(Icons.Default.SupervisorAccount, Res.string.superusers, { navController, appliance ->
            ApplianceSuperUsers(navController = navController, appliance)
        })
}
