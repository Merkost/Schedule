package ru.dvfu.appliances.compose.appliance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.viewmodels.AddUserViewModel
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.ViewState

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddUsersToAppliance(
    navController: NavController,
    appliance: Appliance,
    areSuperUsers: Boolean = false,
) {
    val viewModel: AddUserViewModel = koinViewModel(parameters = { parametersOf(areSuperUsers, appliance) })
    val uiState by viewModel.uiState.collectAsState()
    val usersState by viewModel.usersState.collectAsState()

    val applianceUsers by remember {
        mutableStateOf(if (areSuperUsers) appliance.superuserIds else appliance.userIds)
    }

    if (uiState is UiState.InProgress) ModalLoadingDialog()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) navController.popBackStack()
    }

    val selectedUsers = remember { mutableStateListOf<User>() }

    Scaffold(
        topBar = {
            ScheduleAppBar(
                title = if (areSuperUsers) stringResource(Res.string.add_superuser)
                else stringResource(Res.string.add_user),
                backClick = navController::popBackStack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.addToAppliance(appliance, selectedUsers) },
                expanded = selectedUsers.isNotEmpty(),
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = {
                    Text(
                        text = stringResource(Res.string.users_selected_count, selectedUsers.size),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Crossfade(targetState = usersState, label = "addUsersList") { state ->
                when (state) {
                    is ViewState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ViewState.Success -> {
                        UsersWithSelection(
                            users = state.data,
                            applianceUsers = applianceUsers,
                            selectedUsers = selectedUsers,
                            addUser = { selectedUsers.add(it) },
                            removeUser = { selectedUsers.remove(it) },
                        )
                    }
                    is ViewState.Error -> {
                        EmptyState(
                            icon = Icons.Outlined.PeopleAlt,
                            message = stringResource(Res.string.error_occured),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FabWithLoading(showLoading: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Crossfade(targetState = showLoading, label = "fabLoading") {
            if (it) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            else content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun UsersWithSelection(
    users: List<User>,
    applianceUsers: List<String>,
    selectedUsers: List<User>,
    addUser: (User) -> Unit,
    removeUser: (User) -> Unit,
) {
    val usersToShow = remember(users, applianceUsers) {
        users.filter { !applianceUsers.contains(it.userId) }
    }

    if (usersToShow.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.PeopleAlt,
            message = stringResource(Res.string.no_users_to_add),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(usersToShow, key = { it.userId }) { user ->
            val isSelected = selectedUsers.any { it.userId == user.userId }
            ItemUserWithSelection(user, isSelected) {
                if (isSelected) removeUser(user) else addUser(user)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun NoUsersView() {
    EmptyState(icon = Icons.Outlined.PeopleAlt, message = stringResource(Res.string.no_users_to_add))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemUserWithSelection(user: User, isSelected: Boolean, userClicked: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else Color.Transparent

    Card(
        onClick = userClicked,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserImage(modifier = Modifier.size(44.dp), user = user)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = user.userName.ifBlank { stringResource(Res.string.anonymous_user) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SelectionIndicator(isSelected)
        }
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun UserImage(modifier: Modifier, user: User) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (user.userPic.isEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        } else {
            AsyncImage(
                model = user.userPic,
                contentDescription = stringResource(Res.string.user_photo),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
