package ru.dvfu.appliances.compose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.dvfu.appliances.R
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.BaseViewState
import ru.dvfu.appliances.ui.Progress
import ru.dvfu.appliances.ui.ViewState

class AddUserViewModel(
    private val areSuperUsers: Boolean,
    private val appliance: Appliance,
    private val repository: AppliancesRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState?>(null)
    val uiState: StateFlow<UiState?>
        get() = _uiState

    private val _usersState = MutableStateFlow<ViewState<List<User>>>(ViewState.Success(listOf()))
    val usersState: StateFlow<ViewState<List<User>>>
        get() = _usersState

    init {
        loadUsers()
    }

    private fun loadUsers() {
        _usersState.value = ViewState.Loading
        viewModelScope.launch {
            usersRepository.getUsers().collect { users ->
                _usersState.value = ViewState.Success(users)
            }
        }
    }

    fun addToAppliance(appliance: Appliance, selectedUsers: MutableList<User>) {
        if (selectedUsers.isEmpty()) SnackbarManager.showMessage(R.string.no_users_chosen)
        else {
            when (areSuperUsers) {
                true -> {
                    val usersToAdd = selectedUsers
                        .map { it.userId }.toMutableList()
                        .apply { addAll(appliance.superuserIds) }
                        .distinct()
                    addSuperUsersToAppliance(appliance, usersToAdd)
                }
                false -> {
                    val usersToAdd = selectedUsers
                        .map { it.userId }.toMutableList()
                        .apply { addAll(appliance.userIds) }
                        .distinct()
                    addUsersToAppliance(appliance, usersToAdd)
                }
            }
        }

    }

    private fun addUsersToAppliance(appliance: Appliance, selectedUsers: List<String>) {
        _uiState.value = UiState.InProgress
        viewModelScope.launch {
            repository.addUsersToAppliance(appliance, selectedUsers).fold(
                onSuccess = {
                    _uiState.value = UiState.Success
                },
                onFailure = {
                    SnackbarManager.showMessage(R.string.add_users_failed)
                    _uiState.value = UiState.Error

                }
            )
        }

    }

    private fun addSuperUsersToAppliance(appliance: Appliance, selectedSuperUsers: List<String>) {
        _uiState.value = UiState.InProgress
        viewModelScope.launch {
            repository.addSuperUsersToAppliance(appliance, selectedSuperUsers).fold(
                onSuccess = {
                    _uiState.value = UiState.Success
                },
                onFailure = {
                    SnackbarManager.showMessage(R.string.add_users_failed)
                    _uiState.value = UiState.Error
                })

        }
    }
}

