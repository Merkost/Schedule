package ru.dvfu.appliances.model.datasource.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.Progress

class MockUsersRepository : UsersRepository {

    private val usersStore = MutableStateFlow(FakeData.allUsers)
    private val currentUserStore = MutableStateFlow<User?>(FakeData.adminUser)

    override val currentUser: Flow<User?> = currentUserStore

    override suspend fun getUsers(): Flow<List<User>> = usersStore

    override suspend fun logoutCurrentUser(): Flow<Boolean> {
        currentUserStore.value = null
        return MutableStateFlow(true)
    }

    override suspend fun addNewUser(user: User): StateFlow<Progress> {
        usersStore.update { it + user }
        return MutableStateFlow(Progress.Complete)
    }

    override suspend fun getUser(userId: String): Result<User> {
        val user = usersStore.value.find { it.userId == userId }
        return if (user != null) Result.success(user) else Result.failure(NoSuchElementException("User $userId not found"))
    }

    override suspend fun updateUserField(userId: String, data: Map<String, Any>): Result<Unit> {
        usersStore.update { users ->
            users.map { if (it.userId == userId) applyFieldUpdates(it, data) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun updateCurrentUserField(data: Map<String, Any>) {
        val current = currentUserStore.value ?: return
        currentUserStore.value = applyFieldUpdates(current, data)
        usersStore.update { users ->
            users.map { if (it.userId == current.userId) currentUserStore.value!! else it }
        }
    }

    override suspend fun uploadCurrentMessagingToken() {
    }

    override suspend fun setUserListener(user: User) {
        currentUserStore.value = user
    }

    override suspend fun setNewProfileData(userId: String, data: Map<String, Any>): Result<Unit> =
        updateUserField(userId, data)

    private fun applyFieldUpdates(user: User, data: Map<String, Any>): User {
        var updated = user
        data.forEach { (key, value) ->
            updated = when (key) {
                "userName" -> updated.copy(userName = value as String)
                "email" -> updated.copy(email = value as String)
                "role" -> updated.copy(role = value as Int)
                "msgToken" -> updated.copy(msgToken = value as String)
                "userPic" -> updated.copy(userPic = value as String)
                else -> updated
            }
        }
        return updated
    }
}
