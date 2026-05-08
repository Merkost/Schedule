package ru.dvfu.appliances.model.datasource

import org.kimplify.cedar.Cedar
import kotlinx.coroutines.flow.flow
import ru.dvfu.appliances.model.repository.OfflineRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.FirestoreCollections

class OfflineRepositoryImpl(
    private val collections: FirestoreCollections,
) : OfflineRepository {

    private val log = Cedar.tag("OfflineRepo")

    override suspend fun getUser(userId: String) = flow {
        runCatching {
            collections.users().document(userId).get().data<User>()
        }.fold(
            onSuccess = {
                emit(Result.success(it))
            },
            onFailure = {
                log.e("getUser failed uid=$userId", it)
                emit(Result.failure(it))
            },
        )
    }

    override fun getAppliances() = flow {
        runCatching {
            collections.appliances().get().documents.map { it.data<Appliance>() }
        }.fold(
            onSuccess = { list ->
                if (list.isEmpty()) emit(Result.failure(Throwable())) else emit(Result.success(list))
            },
            onFailure = {
                log.e("getAppliances offline failed", it)
                emit(Result.failure(it))
            },
        )
    }

    override fun getApplianceById(applianceId: String) = flow {
        runCatching {
            collections.appliances().document(applianceId).get().data<Appliance>()
        }.fold(
            onSuccess = {
                emit(Result.success(it))
            },
            onFailure = {
                log.e("getApplianceById failed id=$applianceId", it)
                emit(Result.failure(it))
            },
        )
    }
}
