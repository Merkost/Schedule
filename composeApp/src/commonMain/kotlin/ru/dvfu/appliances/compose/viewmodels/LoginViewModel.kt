package ru.dvfu.appliances.compose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.BaseViewState
import ru.dvfu.appliances.ui.Progress

class LoginViewModel(
    private val usersRepository: UsersRepository,
    private val repository: UsersRepository
) : ViewModel() {

    private val mutableStateFlow: MutableStateFlow<BaseViewState> =
        MutableStateFlow(BaseViewState.Success(null))

    fun subscribe(): StateFlow<BaseViewState> = mutableStateFlow

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            usersRepository.currentUser
                .catch { error -> handleError(error) }
                .collectLatest { user ->
                    user?.let { onSuccess(it) }
                }
        }
    }

    fun signInWithGoogleTokens(idToken: String, accessToken: String?) {
        viewModelScope.launch {
            mutableStateFlow.value = BaseViewState.Loading(null)
            runCatching {
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.credential(idToken, accessToken)
                )
            }.onFailure { handleError(it) }
        }
    }

    fun beginExternalSignIn() {
        mutableStateFlow.value = BaseViewState.Loading(null)
    }

    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            mutableStateFlow.value = BaseViewState.Loading(null)
            runCatching {
                Firebase.auth.signInWithEmailAndPassword(email.trim(), password)
            }.onFailure { handleError(it) }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            mutableStateFlow.value = BaseViewState.Loading(null)
            runCatching { Firebase.auth.signInAnonymously() }
                .onFailure { handleError(it) }
        }
    }

    fun setError(error: Throwable) {
        handleError(error)
    }

    fun clearError() {
        if (mutableStateFlow.value is BaseViewState.Error) {
            mutableStateFlow.value = BaseViewState.Success(null)
        }
    }

    private fun onSuccess(user: User) {
        viewModelScope.launch {
            repository.addNewUser(user).collect { progress ->
                when (progress) {
                    is Progress.Complete -> {
                        mutableStateFlow.value = BaseViewState.Success(user)
                    }
                    is Progress.Loading -> {
                        mutableStateFlow.value = BaseViewState.Loading(null)
                    }
                    is Progress.Error -> {
                        mutableStateFlow.value =
                            BaseViewState.Error(progress.error)
                    }
                }
            }
        }
    }

    private fun handleError(error: Throwable) {
        mutableStateFlow.value = BaseViewState.Error(error)
    }

}
