package ru.dvfu.appliances.model.datasource.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.dvfu.appliances.model.repository.OfflineRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User

class MockOfflineRepository : OfflineRepository {

    private val usersStore = MutableStateFlow(FakeData.allUsers)
    private val appliancesStore = MutableStateFlow(FakeData.allAppliances)

    override suspend fun getUser(userId: String): Flow<Result<User>> =
        usersStore.map { users ->
            val user = users.find { it.userId == userId }
            if (user != null) Result.success(user)
            else Result.failure(NoSuchElementException("User $userId not found"))
        }

    override fun getAppliances(): Flow<Result<List<Appliance>>> =
        appliancesStore.map { Result.success(it) }

    override fun getApplianceById(applianceId: String): Flow<Result<Appliance>> =
        appliancesStore.map { list ->
            val appliance = list.find { it.id == applianceId }
            if (appliance != null) Result.success(appliance)
            else Result.failure(NoSuchElementException("Appliance $applianceId not found"))
        }
}
