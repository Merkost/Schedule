package ru.dvfu.appliances.compose.appliance

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.compose.*
import ru.dvfu.appliances.navigation.UserDetailsRoute
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.viewmodels.ApplianceDetailsViewModel
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.repository.entity.isAdmin
import ru.dvfu.appliances.model.repository.entity.isUserSuperuserOrAdmin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ApplianceDetails(navController: NavController, upPress: () -> Unit, appliance: Appliance) {
    val viewModel: ApplianceDetailsViewModel = koinViewModel()
    viewModel.setAppliance(appliance)

    val createdUser by viewModel.createdUser.collectAsState()
    val noApplianceEvents by viewModel.noApplianceEvents.collectAsState()
    val updatedAppliance by viewModel.appliance.collectAsState()
    val user: User by viewModel.currentUser.collectAsState(User())
    val superUsers by viewModel.currentSuperUsers.collectAsState()
    val uiState = viewModel.uiState.collectAsState()

    var applianceDeleteDialog by remember { mutableStateOf(false) }
    if (applianceDeleteDialog) {
        ApplianceDeleteDialog(
            onDismiss = { applianceDeleteDialog = false },
            onConfirm = { viewModel.deleteAppliance() },
        )
    }

    var toggleActiveDialog by remember { mutableStateOf(false) }
    if (toggleActiveDialog) {
        ApplianceToggleActiveDialog(
            currentlyActive = updatedAppliance.active,
            onDismiss = { toggleActiveDialog = false },
            onConfirm = {
                viewModel.disableEnable(!updatedAppliance.active)
                toggleActiveDialog = false
            },
        )
    }

    if (uiState.value is UiState.InProgress) ModalLoadingDialog()

    LaunchedEffect(uiState.value) {
        if (uiState.value is UiState.Success) upPress()
    }

    val canManage = updatedAppliance.isUserSuperuserOrAdmin(user)

    Scaffold(
        topBar = {
            ApplianceTopBar(
                user = user,
                appliance = updatedAppliance,
                noApplianceEvents = noApplianceEvents,
                upPress = upPress,
                deleteClick = { applianceDeleteDialog = true },
                disableEnableClick = { toggleActiveDialog = true },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ApplianceHeroCard(appliance = updatedAppliance) }

            if (updatedAppliance.description.isNotBlank()) {
                item { ApplianceDescriptionCard(description = updatedAppliance.description) }
            }

            createdUser?.let { owner ->
                item {
                    ApplianceOwnerCard(owner = owner) {
                        navController.navigate(UserDetailsRoute(userId = owner.userId))
                    }
                }
            }

            item {
                SuperUsersSectionHeader(
                    count = superUsers?.size,
                    canManage = canManage,
                    onAddClick = { onAddSuperUserClick(navController, updatedAppliance) },
                )
            }

            superUsersItems(
                superUsers = superUsers,
                canManage = canManage,
                onUserClick = { superUser ->
                    navController.navigate(UserDetailsRoute(userId = superUser.userId))
                },
                onUserDelete = { superUser ->
                    viewModel.deleteSuperUser(superUser, updatedAppliance)
                },
            )

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.superUsersItems(
    superUsers: List<User>?,
    canManage: Boolean,
    onUserClick: (User) -> Unit,
    onUserDelete: (User) -> Unit,
) {
    when {
        superUsers == null -> item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        superUsers.isEmpty() -> item {
            EmptySuperUsersCard()
        }
        else -> items(items = superUsers, key = { it.userId.ifEmpty { it.email } }) { superUser ->
            if (canManage) {
                ItemSwipableUser(
                    user = superUser,
                    userClicked = { onUserClick(superUser) },
                    userDeleted = { onUserDelete(superUser) },
                )
            } else {
                ItemUser(user = superUser, userClicked = { onUserClick(superUser) })
            }
        }
    }
}

@Composable
private fun SuperUsersSectionHeader(count: Int?, canManage: Boolean, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.SupervisorAccount,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.superusers),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        if (canManage) {
            FilledTonalIconButton(onClick = onAddClick) {
                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(Res.string.add_superuser))
            }
        }
    }
}

@Composable
private fun EmptySuperUsersCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SupervisorAccount,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.no_users_in_appliance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ApplianceHeroCard(appliance: Appliance, modifier: Modifier = Modifier) {
    val applianceColor = Color(appliance.color)
    val hasUsableColor = applianceColor.alpha >= 0.1f && applianceColor != Color.White
    val tint = if (hasUsableColor) applianceColor else MaterialTheme.colorScheme.primary
    val textColor = when {
        !hasUsableColor -> MaterialTheme.colorScheme.primary
        tint.luminance() > 0.6f -> MaterialTheme.colorScheme.onSurface
        else -> tint
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (hasUsableColor) 0.18f else 0.12f))
                    .border(1.5.dp, tint.copy(alpha = 0.45f), CircleShape),
            ) {
                Text(
                    text = if (appliance.name.isEmpty()) "?" else appliance.name.first().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = appliance.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ApplianceStatusChip(active = appliance.active)
            }
        }
    }
}

@Composable
private fun ApplianceStatusChip(active: Boolean) {
    val container = if (active) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val content = if (active) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onErrorContainer

    SuggestionChip(
        onClick = {},
        enabled = false,
        label = { Text(if (active) "Активен" else "Неактивен") },
        colors = SuggestionChipDefaults.suggestionChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content,
        ),
    )
}

@Composable
private fun ApplianceDescriptionCard(description: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplianceOwnerCard(owner: User, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Владелец прибора",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = owner.userName.ifBlank { stringResource(Res.string.anonymous_user) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ApplianceToggleActiveDialog(
    currentlyActive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = if (currentlyActive) Res.string.appliance_deactivate_title
    else Res.string.appliance_activate_title
    val description = if (currentlyActive) Res.string.appliance_deactivate_description
    else Res.string.appliance_activate_description
    val confirmLabel = if (currentlyActive) Res.string.deactivate else Res.string.activate
    val icon = if (currentlyActive) Icons.Default.DoNotDisturb else Icons.Default.Autorenew
    val iconTint = if (currentlyActive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.tertiary
    val confirmContainer = if (currentlyActive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.tertiary
    val confirmContent = if (currentlyActive) MaterialTheme.colorScheme.onError
    else MaterialTheme.colorScheme.onTertiary

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmContainer,
                    contentColor = confirmContent,
                ),
            ) { Text(stringResource(confirmLabel)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

@Composable
fun ApplianceDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DoNotDisturb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.appliance_delete_sure),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(Res.string.Yes)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.No)) }
        },
    )
}

@Composable
fun ApplianceTopBar(
    user: User,
    appliance: Appliance,
    noApplianceEvents: Boolean,
    upPress: () -> Unit,
    deleteClick: () -> Unit,
    disableEnableClick: (Boolean) -> Unit,
) {
    ScheduleAppBar(
        title = stringResource(Res.string.appliance),
        backClick = upPress,
        actionDelete = user.isAdmin && noApplianceEvents,
        deleteClick = deleteClick,
        actions = {
            if (appliance.isUserSuperuserOrAdmin(user)) {
                IconButton(onClick = { disableEnableClick(!appliance.active) }) {
                    Icon(
                        imageVector = if (appliance.active) Icons.Default.DoNotDisturb else Icons.Default.Autorenew,
                        contentDescription = if (appliance.active) "Деактивировать" else "Активировать",
                    )
                }
            }
        },
    )
}
