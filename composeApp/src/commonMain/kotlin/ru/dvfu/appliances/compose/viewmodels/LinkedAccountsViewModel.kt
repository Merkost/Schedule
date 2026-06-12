package ru.dvfu.appliances.compose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.link_error_already_in_use
import ru.dvfu.appliances.generated.resources.link_error_generic
import ru.dvfu.appliances.model.datasource.LinkedProviders
import ru.dvfu.appliances.model.datasource.LinkError
import ru.dvfu.appliances.model.datasource.classifyLinkError
import ru.dvfu.appliances.model.datasource.linkedProviders
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.platform.AppleAuthLauncher
import ru.dvfu.appliances.platform.GoogleAuthLauncher

data class LinkedAccountsState(
    val providers: LinkedProviders = LinkedProviders(),
    val isGuest: Boolean = false,
    val googleLoading: Boolean = false,
    val appleLoading: Boolean = false,
    val appleAvailable: Boolean = false,
)

class LinkedAccountsViewModel(
    private val usersRepository: UsersRepository,
    private val googleAuthLauncher: GoogleAuthLauncher,
    private val appleAuthLauncher: AppleAuthLauncher,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LinkedAccountsState(appleAvailable = appleAuthLauncher.isAvailable),
    )
    val state: StateFlow<LinkedAccountsState> = _state.asStateFlow()

    init {
        refreshAuthState()
        viewModelScope.launch {
            Firebase.auth.authStateChanged.collect {
                refreshAuthState()
            }
        }
    }

    fun linkGoogle() {
        val currentState = _state.value
        if (currentState.googleLoading || currentState.providers.google) return

        viewModelScope.launch {
            _state.update { it.copy(googleLoading = true) }
            val result = runCatching {
                val tokens = googleAuthLauncher.signIn().getOrThrow()
                val user = Firebase.auth.currentUser ?: error("No authenticated user")
                user.linkWithCredential(GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken))
                handleLinkSuccess()
            }
            _state.update { it.copy(googleLoading = false) }
            result.onFailure(::handleLinkFailure)
        }
    }

    fun linkApple() {
        val currentState = _state.value
        if (
            currentState.appleLoading ||
            currentState.providers.apple ||
            !currentState.appleAvailable
        ) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(appleLoading = true) }
            val result = runCatching {
                appleAuthLauncher.link().getOrThrow()
                handleLinkSuccess()
            }
            _state.update { it.copy(appleLoading = false) }
            result.onFailure(::handleLinkFailure)
        }
    }

    private suspend fun handleLinkSuccess() {
        runCatching { Firebase.auth.currentUser?.reload() }
        refreshAuthState()
        usersRepository.ensureCurrentUserDocument()
    }

    private fun refreshAuthState() {
        val currentUser = Firebase.auth.currentUser
        val providerIds = currentUser?.providerData?.map { it.providerId }.orEmpty()
        _state.update {
            it.copy(
                providers = linkedProviders(providerIds),
                isGuest = currentUser?.isAnonymous ?: false,
                appleAvailable = appleAuthLauncher.isAvailable,
            )
        }
    }

    private fun handleLinkFailure(error: Throwable) {
        when (classifyLinkError(error)) {
            LinkError.ALREADY_IN_USE -> SnackbarManager.showMessage(Res.string.link_error_already_in_use)
            LinkError.CANCELLED -> Unit
            LinkError.GENERIC -> SnackbarManager.showMessage(Res.string.link_error_generic)
        }
    }
}
