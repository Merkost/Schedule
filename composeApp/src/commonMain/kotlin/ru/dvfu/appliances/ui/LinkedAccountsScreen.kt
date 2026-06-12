package ru.dvfu.appliances.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ru.dvfu.appliances.compose.ScheduleAppBar
import ru.dvfu.appliances.compose.viewmodels.LinkedAccountsState
import ru.dvfu.appliances.compose.viewmodels.LinkedAccountsViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.ic_google
import ru.dvfu.appliances.generated.resources.link_provider
import ru.dvfu.appliances.generated.resources.linked_accounts
import ru.dvfu.appliances.generated.resources.provider_linked
import ru.dvfu.appliances.generated.resources.save_account
import ru.dvfu.appliances.generated.resources.save_account_subtitle
import ru.dvfu.appliances.model.datasource.LinkedProviders

@Composable
fun LinkedAccountsScreenRoute(
    upPress: () -> Unit,
    viewModel: LinkedAccountsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LinkedAccountsScreen(
        state = state,
        upPress = upPress,
        onLinkGoogle = viewModel::linkGoogle,
        onLinkApple = viewModel::linkApple,
    )
}

@Composable
fun LinkedAccountsScreen(
    state: LinkedAccountsState,
    upPress: () -> Unit,
    onLinkGoogle: () -> Unit,
    onLinkApple: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ScheduleAppBar(
                title = stringResource(
                    if (state.isGuest) Res.string.save_account else Res.string.linked_accounts,
                ),
                backClick = upPress,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isGuest) {
                SaveAccountSubtitle()
            }
            ProviderRow(
                name = "Google",
                linked = state.providers.google,
                loading = state.googleLoading,
                onLink = onLinkGoogle,
                leading = {
                    Image(
                        painter = painterResource(Res.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                },
            )
            if (state.appleAvailable) {
                ProviderRow(
                    name = "Apple",
                    linked = state.providers.apple,
                    loading = state.appleLoading,
                    onLink = onLinkApple,
                    leading = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SaveAccountSubtitle(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(
            text = stringResource(Res.string.save_account_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ProviderRow(
    name: String,
    linked: Boolean,
    loading: Boolean,
    onLink: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                leading()
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (linked) {
                        stringResource(Res.string.provider_linked)
                    } else {
                        stringResource(Res.string.link_provider)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (linked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(Res.string.provider_linked),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Button(
                    onClick = onLink,
                    enabled = !loading,
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(Res.string.link_provider))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LinkedAccountsScreenPreview() {
    MaterialTheme {
        LinkedAccountsScreen(
            state = LinkedAccountsState(
                providers = LinkedProviders(google = true, apple = false),
                isGuest = true,
                appleAvailable = true,
            ),
            upPress = {},
            onLinkGoogle = {},
            onLinkApple = {},
        )
    }
}

@Preview
@Composable
private fun ProviderRowPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ProviderRow(
                name = "Google",
                linked = false,
                loading = false,
                onLink = {},
                leading = {
                    Image(
                        painter = painterResource(Res.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            ProviderRow(
                name = "Apple",
                linked = true,
                loading = false,
                onLink = {},
                leading = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                },
            )
        }
    }
}
