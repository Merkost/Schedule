package ru.dvfu.appliances.compose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.BaseViewState

class ProfileViewModel(
    private val userDatastore: UserDatastore,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BaseViewState>(BaseViewState.Success(null))
    val uiState: StateFlow<BaseViewState>
        get() = _uiState

    private val _currentUser = MutableStateFlow<User>(User())
    val currentUser = _currentUser.asStateFlow()

    private val _accountDeletionState = MutableStateFlow<UiState>(UiState.Success)
    val accountDeletionState = _accountDeletionState.asStateFlow()

    init {
        getCurrentUser()
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            userDatastore.getCurrentUser.collect {
                if (_currentUser.value.userId == "0" || it.userId != "0") {
                    _currentUser.value = it
                }
            }
        }
        viewModelScope.launch {
            usersRepository.currentUser.collect { user ->
                if (user != null && user.userId != "0") {
                    _currentUser.value = user
                    userDatastore.saveUser(user)
                }
            }
        }
    }

    suspend fun logoutCurrentUser() = usersRepository.logoutCurrentUser()

    fun deleteCurrentAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (_accountDeletionState.value is UiState.InProgress) return@launch

            val current = currentUser.value
            if (current.userId == "0" || !current.anonymous) return@launch

            _accountDeletionState.value = UiState.InProgress
            usersRepository.deleteCurrentAccount().fold(
                onSuccess = {
                    _accountDeletionState.value = UiState.Success
                    onDeleted()
                },
                onFailure = {
                    _accountDeletionState.value = UiState.Error
                    SnackbarManager.showMessage(Res.string.delete_account_failed)
                },
            )
        }
    }

}
