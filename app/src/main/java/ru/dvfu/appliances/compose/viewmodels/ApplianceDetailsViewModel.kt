package ru.dvfu.appliances.compose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.Repository
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User

class ApplianceDetailsViewModel(
    private val usersRepository: UsersRepository,
    private val repository: AppliancesRepository,
) : ViewModel() {

    companion object {
        val defAppliance = Appliance()
    }

    var appliance: MutableStateFlow<Appliance> = MutableStateFlow(defAppliance)

    val currentUsers = MutableStateFlow<List<User>?>(null)
    val currentSuperUsers = MutableStateFlow<List<User>?>(null)
    val currentUser = usersRepository.currentUserFromDB

    fun setAppliance(applianceFromArg: Appliance) {
        if (appliance.value == defAppliance) {
            loadAllUsers(applianceFromArg.userIds)
            loadAllSuperUsers(applianceFromArg.superuserIds)
            appliance.value = applianceFromArg
            updateAppliance()
        }
    }

    private fun updateAppliance() {
        viewModelScope.launch {
            repository.getAppliance(appliance.value).collect { updatedAppliance ->
                if (updatedAppliance.userIds != currentUsers.value?.map { it.userId })
                    loadAllUsers(updatedAppliance.userIds)
                if (updatedAppliance.superuserIds != currentSuperUsers.value?.map { it.userId })
                    loadAllSuperUsers(updatedAppliance.superuserIds)
                appliance.value = updatedAppliance
            }
        }
    }

    fun deleteAppliance() {
        viewModelScope.launch {
            appliance.value?.let {
                repository.deleteAppliance(it)
            }
        }
    }

    fun loadAllUsers(ids: List<String>) {
        viewModelScope.launch {
            repository.getApplianceUsers(ids).collect { users ->
                currentUsers.value = users
            }
        }
    }

    fun loadAllSuperUsers(ids: List<String>) {
        viewModelScope.launch {
            repository.getApplianceUsers(ids).collect { users ->
                currentSuperUsers.value = users
            }
        }
    }

    fun deleteUser(userToDelete: User, from: Appliance) {
        viewModelScope.launch {
            repository.deleteUserFromAppliance(userToDelete, from)
        }
    }

    fun deleteSuperUser(superUserToDelete: User, from: Appliance) {
        viewModelScope.launch {
            repository.deleteSuperUserFromAppliance(superUserToDelete, from)
        }
    }

}