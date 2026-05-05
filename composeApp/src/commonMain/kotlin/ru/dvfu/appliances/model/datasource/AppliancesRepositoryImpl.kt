package ru.dvfu.appliances.model.datasource

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.FirestoreCollections

class AppliancesRepositoryImpl(
    private val collections: FirestoreCollections,
) : AppliancesRepository {

    override suspend fun deleteUserFromAppliance(userIdToDelete: String, from: Appliance): Result<Unit> =
        runCatching {
            collections.appliances().document(from.id).update(
                "userIds" to from.userIds.filter { it != userIdToDelete },
            )
        }

    override suspend fun deleteSuperUserFromAppliance(userIdToDelete: String, from: Appliance): Result<Unit> =
        runCatching {
            collections.appliances().document(from.id).update(
                "superuserIds" to from.superuserIds.filter { it != userIdToDelete },
            )
        }

    override suspend fun getUserAppliances(userId: String): Flow<List<Appliance>> =
        collections.appliances()
            .where { "userIds".contains(userId) }
            .snapshots
            .map { qs -> qs.documents.map { it.data<Appliance>() } }
            .catch { emit(emptyList()) }

    override suspend fun changeApplianceStatus(applianceId: String, isActive: Boolean): Result<Unit> =
        runCatching {
            collections.appliances().document(applianceId).update("active" to isActive)
        }

    override suspend fun getSuperUserAppliances(userId: String): Flow<List<Appliance>> =
        collections.appliances()
            .where { "superuserIds".contains(userId) }
            .snapshots
            .map { qs -> qs.documents.map { it.data<Appliance>() } }
            .catch { emit(emptyList()) }

    override suspend fun addUsersToAppliance(appliance: Appliance, userIds: List<String>): Result<Unit> =
        runCatching {
            collections.appliances().document(appliance.id).update("userIds" to userIds)
        }

    override suspend fun addSuperUsersToAppliance(appliance: Appliance, superuserIds: List<String>): Result<Unit> =
        runCatching {
            collections.appliances().document(appliance.id).update("superuserIds" to superuserIds)
        }

    override suspend fun getApplianceUsers(userIds: List<String>): Flow<List<User>> =
        if (userIds.isEmpty()) {
            kotlinx.coroutines.flow.flow { emit(emptyList()) }
        } else {
            kotlinx.coroutines.flow.flow {
                val users = collections.users()
                    .where { "userId" inArray userIds }
                    .get()
                    .documents.map { it.data<User>() }
                emit(users)
            }.catch { emit(emptyList()) }
        }

    override suspend fun getAppliance(applianceId: String): Flow<Result<Appliance>> =
        collections.appliances().document(applianceId).snapshots
            .map { snap ->
                runCatching { snap.data<Appliance>() }
            }
            .catch { emit(Result.failure(it)) }

    override suspend fun getAppliances(): Flow<List<Appliance>> =
        collections.appliances().snapshots
            .map { qs -> qs.documents.map { it.data<Appliance>() } }
            .catch { emit(emptyList()) }

    override suspend fun getAppliancesOneTime(): Result<List<Appliance>> =
        runCatching {
            collections.appliances().get().documents.map { it.data<Appliance>() }
        }

    override suspend fun addAppliance(appliance: Appliance): Result<Unit> =
        runCatching {
            collections.appliances().document(appliance.id).set(appliance)
        }

    override suspend fun deleteAppliance(applianceId: String): Result<Unit> =
        runCatching {
            collections.appliances().document(applianceId).delete()
        }
}
