package ru.dvfu.appliances.model.datasource

import org.kimplify.cedar.Cedar
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ru.dvfu.appliances.model.repository.AppliancesRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.FirestoreCollections

class AppliancesRepositoryImpl(
    private val collections: FirestoreCollections,
) : AppliancesRepository {

    private val log = Cedar.tag("AppliancesRepo")

    override suspend fun deleteUserFromAppliance(userIdToDelete: String, from: Appliance): Result<Unit> =
        runCatching {
            collections.appliances().document(from.id).updateFields {
                "userIds" to from.userIds.filter { it != userIdToDelete }
            }
        }

    override suspend fun deleteSuperUserFromAppliance(userIdToDelete: String, from: Appliance): Result<Unit> =
        runCatching {
            collections.appliances().document(from.id).updateFields {
                "superuserIds" to from.superuserIds.filter { it != userIdToDelete }
            }
        }

    override suspend fun getUserAppliances(userId: String): Flow<List<Appliance>> =
        collections.appliances()
            .where { "userIds".contains(userId) }
            .snapshots
            .onStart { log.d("getUserAppliances subscribed userId=$userId") }
            .map { qs ->
                val list = qs.documents.mapNotNull { d ->
                    runCatching { d.data<Appliance>() }
                        .onFailure { log.e("getUserAppliances deser failed docId=${d.id}", it) }
                        .getOrNull()
                }
                log.d("getUserAppliances emit count=${list.size}")
                list
            }
            .catch { e ->
                log.e("getUserAppliances flow failed userId=$userId", e)
                emit(emptyList())
            }

    override suspend fun changeApplianceStatus(applianceId: String, isActive: Boolean): Result<Unit> =
        runCatching {
            collections.appliances().document(applianceId).updateFields {
                "active" to isActive
            }
        }

    override suspend fun getSuperUserAppliances(userId: String): Flow<List<Appliance>> =
        collections.appliances()
            .where { "superuserIds".contains(userId) }
            .snapshots
            .onStart { log.d("getSuperUserAppliances subscribed userId=$userId") }
            .map { qs ->
                val list = qs.documents.mapNotNull { d ->
                    runCatching { d.data<Appliance>() }
                        .onFailure { log.e("getSuperUserAppliances deser failed docId=${d.id}", it) }
                        .getOrNull()
                }
                log.d("getSuperUserAppliances emit count=${list.size}")
                list
            }
            .catch { e ->
                log.e("getSuperUserAppliances flow failed userId=$userId", e)
                emit(emptyList())
            }

    override suspend fun addUsersToAppliance(appliance: Appliance, userIds: List<String>): Result<Unit> =
        runCatching {
            collections.appliances().document(appliance.id).updateFields {
                "userIds" to userIds
            }
        }

    override suspend fun addSuperUsersToAppliance(appliance: Appliance, superuserIds: List<String>): Result<Unit> =
        runCatching {
            collections.appliances().document(appliance.id).updateFields {
                "superuserIds" to superuserIds
            }
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
                    .onFailure { log.e("getAppliance deser failed applianceId=$applianceId", it) }
            }
            .catch { e ->
                log.e("getAppliance flow failed applianceId=$applianceId", e)
                emit(Result.failure(e))
            }

    override suspend fun getAppliances(): Flow<List<Appliance>> =
        collections.appliances().snapshots
            .onStart { log.d("getAppliances subscribed") }
            .map { qs ->
                val list = qs.documents.mapNotNull { d ->
                    runCatching { d.data<Appliance>() }
                        .onFailure { log.e("getAppliances deser failed docId=${d.id}", it) }
                        .getOrNull()
                }
                log.d("getAppliances emit count=${list.size}")
                list
            }
            .catch { e ->
                log.e("getAppliances flow failed", e)
                emit(emptyList())
            }

    override suspend fun getAppliancesOneTime(): Result<List<Appliance>> =
        runCatching {
            val docs = collections.appliances().get().documents
            val list = docs.mapNotNull { d ->
                runCatching { d.data<Appliance>() }
                    .onFailure { log.e("getAppliancesOneTime deser failed docId=${d.id}", it) }
                    .getOrNull()
            }
            log.d("getAppliancesOneTime got count=${list.size}/${docs.size}")
            list
        }.onFailure { log.e("getAppliancesOneTime failed", it) }

    override suspend fun addAppliance(appliance: Appliance): Result<Unit> =
        runCatching {
            collections.appliances().document(appliance.id).set(appliance)
        }

    override suspend fun deleteAppliance(applianceId: String): Result<Unit> =
        runCatching {
            collections.appliances().document(applianceId).delete()
        }
}
