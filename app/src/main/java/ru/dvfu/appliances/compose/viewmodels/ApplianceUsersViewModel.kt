package ru.dvfu.appliances.compose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.dvfu.appliances.model.repository.Repository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User

class ApplianceUsersViewModel(private val repository: Repository) : ViewModel() {


    val currentContent = MutableStateFlow<List<User>>(listOf())

    fun loadAllUsers(appliance: Appliance) {
        viewModelScope.launch {
            repository.getApplianceUsers(appliance).collect { users ->
                currentContent.value = users as List<User>
            }
        }
    }

}
