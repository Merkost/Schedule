package ru.dvfu.appliances.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.dvfu.appliances.AppBuildConfig
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.appliance.LoadingItem
import ru.dvfu.appliances.compose.components.ItemsSelection
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.home.ItemAppliance
import ru.dvfu.appliances.compose.viewmodels.UserDetailsViewModel
import ru.dvfu.appliances.model.repository.entity.*

@OptIn(InternalCoroutinesApi::class)
@ExperimentalComposeUiApi
@Composable
fun UserDetails(navController: NavController, upPress: () -> Unit, user: User) {
    val viewModel: UserDetailsViewModel = koinViewModel(parameters = { parametersOf(user) })

    DisposableEffect(viewModel) {
        viewModel.setDetailsUser(user)
        onDispose {}
    }

    var isChangeRoleDialogOpen by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUser.collectAsState()
    val detailsUser by viewModel.detailsUser.collectAsState()
    val userRoleState by viewModel.userRoleState.collectAsState()

    Scaffold(
        topBar = { ScheduleAppBar(stringResource(R.string.user), upPress) },
    ) { innerPadding ->
        if (isChangeRoleDialogOpen) {
            RolesWithSelectionDialog(
                currentUser = detailsUser,
                onDismiss = { isChangeRoleDialogOpen = false },
            ) { newRole ->
                viewModel.updateUserRole(detailsUser, newRole)
                isChangeRoleDialogOpen = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserHeroCard(detailsUser)

            UserDetailRow(
                icon = Icons.Outlined.AlternateEmail,
                label = stringResource(R.string.email),
                value = detailsUser.email.ifBlank { "—" },
            )

            UserRoleCard(
                user = detailsUser,
                currentUser = currentUser,
                userRoleState = userRoleState,
                onRoleChangeClick = { isChangeRoleDialogOpen = true },
            )

            when (detailsUser.role) {
                Roles.GUEST.ordinal -> Unit
                Roles.USER.ordinal, Roles.ADMIN.ordinal -> {
                    SuperUserAppliancesList(viewModel, navController)
                }
            }
        }
    }
}

@Composable
private fun UserHeroCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
            }
            Text(
                text = user.userName.ifBlank { stringResource(R.string.anonymous_user) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            RoleBadge(role = getRole(user.role))
        }
    }
}

@Composable
private fun RoleBadge(role: Roles) {
    val (container, content) = when (role) {
        Roles.ADMIN -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Roles.USER -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(role.stringRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun UserDetailRow(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun UserRoleCard(
    user: User,
    currentUser: User,
    userRoleState: UiState,
    onRoleChangeClick: () -> Unit,
) {
    val canEdit = currentUser.isAdmin && (user.userId != currentUser.userId || AppDebug.isDebug)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.role),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(getRole(user.role).stringRes)) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
            if (canEdit) {
                if (userRoleState is UiState.InProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRoleChangeClick) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun RolesWithSelectionDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSelectedValue: (Roles) -> Unit,
) {
    val roles = remember { Roles.values().toList() }
    var selectedRole by remember { mutableStateOf(getRole(currentUser.role)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.role),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { role ->
                    RoleOption(
                        role = role,
                        selected = role == selectedRole,
                        onSelect = { selectedRole = role },
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    onSelectedValue(selectedRole)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun RoleOption(role: Roles, selected: Boolean, onSelect: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val titleColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Text(
            text = stringResource(role.stringRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SuperUserAppliancesList(viewModel: UserDetailsViewModel, navController: NavController) {
    val superUserAppliances by viewModel.currentSuperUserAppliances.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.superuser_appliances),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
        AppliancesLazyRow(superUserAppliances, navController)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun AppliancesLazyRow(appliances: List<Appliance>?, navController: NavController) {
    appliances?.let {
        if (it.isEmpty()) {
            NoAppliances()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(it) { appliance ->
                    Box(modifier = Modifier.width(160.dp)) {
                        ItemAppliance(appliance) { clicked ->
                            navController.navigate(
                                MainDestinations.APPLIANCE_ROUTE,
                                Arguments.APPLIANCE to clicked,
                            )
                        }
                    }
                }
            }
        }
    } ?: LoadingItem(Modifier.fillMaxWidth().padding(vertical = 16.dp))
}

@Composable
fun NoAppliances() {
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DevicesOther,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.no_appliances),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
