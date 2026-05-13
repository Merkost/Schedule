package ru.dvfu.appliances.model.datasource.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User

class MockAppliancesRepository : AppliancesRepository {

    private val appliancesStore = MutableStateFlow(FakeData.allAppliances)

    override suspend fun addUsersToAppliance(appliance: Appliance, userIds: List<String>): Result<Unit> {
        appliancesStore.update { list ->
            list.map {
                if (it.id == appliance.id) it.copy(userIds = it.userIds + userIds) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun addSuperUsersToAppliance(appliance: Appliance, superuserIds: List<String>): Result<Unit> {
        appliancesStore.update { list ->
            list.map {
                if (it.id == appliance.id) it.copy(superuserIds = it.superuserIds + superuserIds) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun addAppliance(appliance: Appliance): Result<Unit> {
        appliancesStore.update { it + appliance }
        return Result.success(Unit)
    }

    override suspend fun getAppliances(): Flow<List<Appliance>> = appliancesStore

    override suspend fun getAppliancesOneTime(): Result<List<Appliance>> =
        Result.success(appliancesStore.value)

    override suspend fun getApplianceUsers(userIds: List<String>): Flow<List<User>> {
        val allUsers = FakeData.allUsers
        return MutableStateFlow(allUsers.filter { it.userId in userIds })
    }

    override suspend fun getAppliance(applianceId: String): Flow<Result<Appliance>> {
        return appliancesStore.map { list ->
            val appliance = list.find { it.id == applianceId }
            if (appliance != null) Result.success(appliance)
            else Result.failure(NoSuchElementException("Appliance $applianceId not found"))
        }
    }

    override suspend fun deleteAppliance(applianceId: String): Result<Unit> {
        appliancesStore.update { list -> list.filter { it.id != applianceId } }
        return Result.success(Unit)
    }

    override suspend fun deleteUserFromAppliance(userIdToDelete: String, from: Appliance): Result<Unit> {
        appliancesStore.update { list ->
            list.map {
                if (it.id == from.id) it.copy(userIds = it.userIds - userIdToDelete) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteSuperUserFromAppliance(userIdToDelete: String, from: Appliance): Result<Unit> {
        appliancesStore.update { list ->
            list.map {
                if (it.id == from.id) it.copy(superuserIds = it.superuserIds - userIdToDelete) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun getSuperUserAppliances(userId: String): Flow<List<Appliance>> =
        appliancesStore.map { list -> list.filter { userId in it.superuserIds } }

    override suspend fun getUserAppliances(userId: String): Flow<List<Appliance>> =
        appliancesStore.map { list -> list.filter { userId in it.userIds } }

    override suspend fun changeApplianceStatus(applianceId: String, isActive: Boolean): Result<Unit> {
        appliancesStore.update { list ->
            list.map { if (it.id == applianceId) it.copy(active = isActive) else it }
        }
        return Result.success(Unit)
    }
}
