@file:Suppress("DEPRECATION")

package ru.dvfu.appliances.compose.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.navigation.EditProfileRoute
import ru.dvfu.appliances.navigation.LinkedAccountsRoute
import ru.dvfu.appliances.navigation.SettingsRoute
import ru.dvfu.appliances.navigation.UsersRoute
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.DefaultDialog
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.compose.viewmodels.ProfileViewModel
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.repository.entity.isAdmin
import ru.dvfu.appliances.model.repository.entity.isAnonymousOrGuest
import org.koin.compose.koinInject
import ru.dvfu.appliances.platform.GoogleAuthLauncher
import ru.dvfu.appliances.navigation.UserDetailsRoute

@InternalCoroutinesApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(navController: NavController, modifier: Modifier = Modifier, backPress: () -> Unit) {

    val viewModel = koinViewModel<ProfileViewModel>()
    val currentUser by viewModel.currentUser.collectAsState()
    val accountDeletionState by viewModel.accountDeletionState.collectAsState()
    var isDeleteAccountDialogOpen by rememberSaveable { mutableStateOf(false) }
    val deletingAccount = accountDeletionState is UiState.InProgress
    val isRegisteredUser = currentUser.userId != "0" && !currentUser.anonymous

    if (isDeleteAccountDialogOpen) {
        DefaultDialog(
            primaryText = stringResource(Res.string.delete_account_title),
            secondaryText = stringResource(Res.string.delete_account_message),
            negativeButtonText = stringResource(Res.string.cancel),
            onNegativeClick = { if (!deletingAccount) isDeleteAccountDialogOpen = false },
            positiveButtonText = stringResource(Res.string.delete_account_confirm),
            positiveButtonColor = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            onPositiveClick = {
                viewModel.deleteCurrentAccount {
                    isDeleteAccountDialogOpen = false
                }
            },
            onDismiss = { if (!deletingAccount) isDeleteAccountDialogOpen = false },
        )
    }
    if (deletingAccount) {
        ModalLoadingDialog()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { ProfileTopBar(upPress = backPress) },
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                ProfileUserInfo(currentUser)
            }
            Spacer(Modifier.height(24.dp))
            when {
                currentUser.anonymous -> {
                    ProfileActionGroup(title = stringResource(Res.string.profile_account_section)) {
                        ProfileActionRow(
                            icon = Icons.Default.Link,
                            title = stringResource(Res.string.save_account),
                            subtitle = stringResource(Res.string.save_account_subtitle),
                            onClick = { navController.navigate(LinkedAccountsRoute) },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    ProfileActionGroup(
                        title = stringResource(Res.string.profile_danger_zone),
                        danger = true,
                    ) {
                        ProfileActionRow(
                            icon = Icons.Default.DeleteForever,
                            title = stringResource(Res.string.delete_account),
                            subtitle = stringResource(Res.string.profile_delete_account_subtitle),
                            onClick = { isDeleteAccountDialogOpen = true },
                            destructive = true,
                        )
                    }
                }

                isRegisteredUser -> {
                    UserButtons(navController, currentUser)
                }
            }
        }
    }
}

@InternalCoroutinesApi
@Composable
fun UserButtons(navController: NavController, currentUser: User) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileActionGroup(title = stringResource(Res.string.profile_account_section)) {
            ProfileActionRow(
                icon = Icons.Default.AccountCircle,
                title = stringResource(Res.string.account_details),
                subtitle = stringResource(Res.string.profile_account_details_subtitle),
                onClick = { navController.navigate(UserDetailsRoute(userId = currentUser.userId)) },
            )
            ProfileActionDivider()
            ProfileActionRow(
                icon = Icons.Default.Edit,
                title = stringResource(Res.string.profile_edit),
                subtitle = stringResource(Res.string.profile_edit_subtitle),
                onClick = { navController.navigate(EditProfileRoute) },
            )
            ProfileActionDivider()
            ProfileActionRow(
                icon = Icons.Default.Link,
                title = stringResource(Res.string.linked_accounts),
                subtitle = stringResource(Res.string.profile_linked_accounts_subtitle),
                onClick = { navController.navigate(LinkedAccountsRoute) },
            )
        }
        ProfileActionGroup(title = stringResource(Res.string.profile_preferences_section)) {
            ProfileActionRow(
                icon = Icons.Default.Notifications,
                title = stringResource(Res.string.settings),
                subtitle = stringResource(Res.string.profile_settings_subtitle),
                onClick = { navController.navigate(SettingsRoute) },
            )
        }
        if (currentUser.isAdmin) {
            ProfileActionGroup(title = stringResource(Res.string.profile_management_section)) {
                ProfileActionRow(
                    icon = Icons.Default.PersonSearch,
                    title = stringResource(Res.string.users),
                    subtitle = stringResource(Res.string.profile_users_subtitle),
                    onClick = { navController.navigate(UsersRoute) },
                )
            }
        }
    }
}

@Composable
private fun ProfileActionGroup(
    title: String,
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val titleColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val containerColor = if (danger) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val iconContainerColor = contentColor.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (destructive) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ProfileActionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@InternalCoroutinesApi
@Composable
fun LogoutDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel = koinViewModel<ProfileViewModel>()
    val launcher = koinInject<GoogleAuthLauncher>()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.logout_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.logout_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    scope.launch {
                        runCatching { launcher.signOut() }
                        viewModel.logoutCurrentUser().collect { }
                        onDismiss()
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(Res.string.Yes)) }
        },
        dismissButton = {
            androidx.compose.material3.OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.No))
            }
        },
    )
}

@Composable
fun ProfileUserInfo(user: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (user.userPic.isNullOrEmpty()) {
            Icon(
                Icons.Default.Person,
                contentDescription = stringResource(Res.string.user_photo),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            AsyncImage(
                model = user.userPic,
                contentDescription = stringResource(Res.string.user_photo),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
        Text(
            text = if (user.anonymous) stringResource(Res.string.anonymous_user) else user.userName,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        if (user.email.isNotBlank()) {
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val statusLabel = stringResource(
            when {
                user.isAdmin -> Res.string.admin
                user.isAnonymousOrGuest -> Res.string.guest
                else -> Res.string.user
            },
        )
        val statusContainerColor = when {
            user.isAdmin -> MaterialTheme.colorScheme.tertiaryContainer
            user.isAnonymousOrGuest -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        }
        val statusContentColor = when {
            user.isAdmin -> MaterialTheme.colorScheme.onTertiaryContainer
            user.isAnonymousOrGuest -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }
        Surface(
            shape = CircleShape,
            color = statusContainerColor,
            contentColor = statusContentColor,
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@InternalCoroutinesApi
@Preview
@Composable
fun ProfilePreview() {
    MaterialTheme {
        Profile(rememberNavController(), backPress = {})
    }
}

@OptIn(InternalCoroutinesApi::class)
@Composable
fun ProfileTopBar(upPress: () -> Unit) {
    var dialogOnLogout by rememberSaveable { mutableStateOf(false) }
    if (dialogOnLogout) LogoutDialog() { dialogOnLogout = false }

    ScheduleAppBar(
        stringResource(Res.string.profile),
        upPress,
        actions = {
            IconButton(onClick = { dialogOnLogout = true }) {
                Icon(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.logout))
            }
        }
    )
}
