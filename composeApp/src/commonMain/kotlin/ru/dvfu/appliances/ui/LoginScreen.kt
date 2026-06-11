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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.datetime.Clock
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
import ru.dvfu.appliances.generated.resources.app_logo
import ru.dvfu.appliances.generated.resources.app_name
import ru.dvfu.appliances.generated.resources.continue_as_a_guest
import ru.dvfu.appliances.generated.resources.ic_google
import ru.dvfu.appliances.generated.resources.login_to_continue
import ru.dvfu.appliances.generated.resources.qa_cancel
import ru.dvfu.appliances.generated.resources.qa_email_label
import ru.dvfu.appliances.generated.resources.qa_password_label
import ru.dvfu.appliances.generated.resources.qa_sign_in_action
import ru.dvfu.appliances.generated.resources.qa_sign_in_title
import ru.dvfu.appliances.generated.resources.sign_in_with_apple
import ru.dvfu.appliances.generated.resources.sign_in_with_google
import ru.dvfu.appliances.platform.AppleAuthLauncher
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
    val appleLauncher = koinInject<AppleAuthLauncher>()
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
                    onSuccess = { tokens ->
                        viewModel.signInWithGoogleTokens(tokens.idToken, tokens.accessToken)
                    },
                    onFailure = { err -> viewModel.setError(err) },
                )
            }
        },
        showAppleButton = appleLauncher.isAvailable,
        onAppleClick = {
            scope.launch {
                viewModel.beginExternalSignIn()
                val result = appleLauncher.signIn()
                result.onFailure { err -> viewModel.setError(err) }
            }
        },
        onGuestClick = { viewModel.signInAnonymously() },
        onErrorShown = { viewModel.clearError() },
        onQaSignIn = { email, password ->
            viewModel.signInWithEmailPassword(email, password)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onGoogleClick: () -> Unit,
    showAppleButton: Boolean = false,
    onAppleClick: () -> Unit = {},
    onGuestClick: () -> Unit,
    onErrorShown: () -> Unit,
    onQaSignIn: (email: String, password: String) -> Unit = { _, _ -> },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var qaDialogOpen by remember { mutableStateOf(false) }

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
                Image(
                    painter = painterResource(Res.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            val now = Clock.System.now().toEpochMilliseconds()
                            tapCount = if (now - lastTapAt > 2_000L) 1 else tapCount + 1
                            lastTapAt = now
                            if (tapCount >= 5) {
                                tapCount = 0
                                qaDialogOpen = true
                            }
                        },
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
                    showAppleButton = showAppleButton,
                    onAppleClick = onAppleClick,
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

        if (qaDialogOpen) {
            QaSignInDialog(
                onDismiss = { qaDialogOpen = false },
                onSubmit = { email, password ->
                    qaDialogOpen = false
                    onQaSignIn(email, password)
                },
            )
        }
    }
}

@Composable
private fun QaSignInDialog(
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = email.isNotBlank() && password.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.qa_sign_in_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.qa_email_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(Res.string.qa_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(email, password) },
                enabled = canSubmit,
            ) { Text(stringResource(Res.string.qa_sign_in_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.qa_cancel)) }
        },
    )
}

@Composable
private fun AuthButtonsSection(
    loading: Boolean,
    onGoogleClick: () -> Unit,
    showAppleButton: Boolean,
    onAppleClick: () -> Unit,
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

    if (showAppleButton) {
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = onAppleClick,
            enabled = !loading,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(Res.string.sign_in_with_apple),
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
