@file:Suppress("DEPRECATION")

package ru.dvfu.appliances.compose.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import ru.dvfu.appliances.navigation.SettingsRoute
import ru.dvfu.appliances.navigation.UsersRoute
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.components.views.DefaultDialog
import ru.dvfu.appliances.compose.viewmodels.ProfileViewModel
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.repository.entity.isAdmin
import org.koin.compose.koinInject
import ru.dvfu.appliances.platform.GoogleAuthLauncher

@InternalCoroutinesApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(navController: NavController, modifier: Modifier = Modifier, backPress: () -> Unit) {

    val viewModel = koinViewModel<ProfileViewModel>()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = { ProfileTopBar(upPress = backPress) },
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
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
            if (!currentUser.anonymous) UserButtons(navController, currentUser)
        }
    }
}

@InternalCoroutinesApi
@Composable
fun UserButtons(navController: NavController, currentUser: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ColumnButton(Icons.Default.Edit, "Редактировать профиль") {
            navController.navigate(EditProfileRoute)
        }
        ColumnButton(Icons.Default.Notifications, "Настройка уведомлений") {
            navController.navigate(SettingsRoute)
        }
        if (currentUser.isAdmin) {
            ColumnButton(Icons.Default.PersonSearch, "Список пользователей") {
                navController.navigate(UsersRoute)
            }
        }
    }
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
                text = "Выход из аккаунта",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = "Вы уверены, что хотите выйти из своего аккаунта?",
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
fun ColumnButton(image: ImageVector, name: String, click: () -> Unit) {
    OutlinedButton(
        onClick = click,
        modifier = Modifier.fillMaxWidth(),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(image, name, modifier = Modifier.size(25.dp))
                Text(name, modifier = Modifier.padding(start = 10.dp))
            }
        })
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
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            AsyncImage(
                model = user.userPic,
                contentDescription = stringResource(Res.string.user_photo),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
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
                Icon(Icons.AutoMirrored.Filled.Logout, "")
            }
        }
    )
}

