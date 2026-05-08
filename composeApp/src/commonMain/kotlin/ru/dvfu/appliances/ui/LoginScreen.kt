package ru.dvfu.appliances.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ru.dvfu.appliances.compose.viewmodels.LoginViewModel
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.app_name
import ru.dvfu.appliances.generated.resources.continue_as_a_guest
import ru.dvfu.appliances.generated.resources.ic_google
import ru.dvfu.appliances.generated.resources.login_to_continue
import ru.dvfu.appliances.generated.resources.sign_in_with_google
import ru.dvfu.appliances.platform.GoogleAuthLauncher

data class LoginUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    val viewModel = koinViewModel<LoginViewModel>()
    val launcher = koinInject<GoogleAuthLauncher>()
    val scope = rememberCoroutineScope()
    val state by viewModel.subscribe().collectAsState()

    val uiState = when (val s = state) {
        is BaseViewState.Loading -> LoginUiState(loading = true)
        is BaseViewState.Error -> LoginUiState(errorMessage = s.error.message ?: "Произошла ошибка")
        is BaseViewState.Success<*> -> LoginUiState()
    }

    LoginScreenContent(
        state = uiState,
        onGoogleClick = {
            scope.launch {
                val result = launcher.signIn()
                result.fold(
                    onSuccess = { idToken -> viewModel.signInWithGoogleIdToken(idToken) },
                    onFailure = { err -> viewModel.setError(err) },
                )
            }
        },
        onGuestClick = { viewModel.signInAnonymously() },
        onErrorShown = { viewModel.clearError() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onErrorShown()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.size(24.dp))
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.login_to_continue),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(48.dp))
                AuthButtonsSection(
                    loading = state.loading,
                    onGoogleClick = onGoogleClick,
                    onGuestClick = onGuestClick,
                )
            }

        }

        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun AuthButtonsSection(
    loading: Boolean,
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 360.dp)
        .heightIn(min = 56.dp)

    Button(
        onClick = onGoogleClick,
        enabled = !loading,
        modifier = buttonModifier,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified,
        )
        Spacer(Modifier.size(12.dp))
        Text(stringResource(Res.string.sign_in_with_google), style = MaterialTheme.typography.labelLarge)
    }

    Spacer(Modifier.size(12.dp))

    OutlinedButton(
        onClick = onGuestClick,
        enabled = !loading,
        modifier = buttonModifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Text(stringResource(Res.string.continue_as_a_guest), style = MaterialTheme.typography.labelLarge)
    }
}
