package ru.dvfu.appliances.compose.appliance

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.dvfu.appliances.compose.components.SwipeToDeleteItem
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.navigation.AddUserToApplianceRoute
import ru.dvfu.appliances.navigation.UserDetailsRoute
import ru.dvfu.appliances.compose.viewmodels.ApplianceDetailsViewModel
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.repository.entity.isUserSuperuserOrAdmin


@ExperimentalAnimationApi
@Composable
fun ApplianceUsers(
    navController: NavController,
    appliance: Appliance,
) {
    val detailsViewModel: ApplianceDetailsViewModel = koinViewModel()
    val currentUser: User by detailsViewModel.currentUser.collectAsState(User())

    val users by detailsViewModel.currentUsers.collectAsState()

    Scaffold(containerColor = Color.Transparent) { padding ->

        Crossfade(users, modifier = Modifier.padding(padding)) { animatedUiState ->
            SwipableUsers(
                users = animatedUiState,
                userClicked = { user ->
                    onUserClick(user, navController)
                },
                addClicked = { onAddClick(navController, appliance) },
                deleteClicked = { userToDelete ->
                    detailsViewModel.deleteUser(userToDelete, appliance)
                },
                isSuperuserOrAdmin = appliance.isUserSuperuserOrAdmin(currentUser)
            )
        }
    }
}

fun onAddClick(navController: NavController, appliance: Appliance) {
    navController.navigate(AddUserToApplianceRoute(applianceId = appliance.id))
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@Composable
fun SwipableUsers(
    users: List<User>?,
    userClicked: (User) -> Unit,
    addClicked: () -> Unit,
    deleteClicked: (User) -> Unit,
    isSuperuserOrAdmin: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp)
    ) {
        if (isSuperuserOrAdmin) item { ItemAdd(addClicked) }
        users?.let {
            if (users.isNotEmpty()) {
                items(users) { user ->
                    if (isSuperuserOrAdmin) {
                        ItemSwipableUser(user,
                            userClicked = { userClicked(user) },
                            userDeleted = { deleteClicked(user) })
                    } else {
                        ItemUser(
                            user,
                            userClicked = { userClicked(user) },
                        )
                    }
                }
            } else
                item {
                    NoElementsView(
                        mainText = stringResource(Res.string.no_users_in_appliance),
                        onClickAction = { }
                    )
                }
        } ?: item {
            LoadingItem(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun LoadingItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun NoElementsView(
    modifier: Modifier = Modifier,
    mainText: String,
    secondaryText: String = "",
    onClickAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(text = mainText)
        Spacer(modifier = Modifier.height(8.dp))
        if (secondaryText.isNotEmpty()) {
            Text(
                modifier = Modifier.clickable {
                    onClickAction()
                },
                text = secondaryText
            )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ItemSwipableUser(user: User, userClicked: () -> Unit, userDeleted: () -> Unit) {
    SwipeToDeleteItem(onDelete = userDeleted) {
        ItemUser(user, userClicked)
    }
}

private fun onUserClick(user: User, navController: NavController) {
    navController.navigate(UserDetailsRoute(userId = user.userId))
}
