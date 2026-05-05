package ru.dvfu.appliances.model.datasource

import kotlinx.coroutines.flow.flow
import ru.dvfu.appliances.model.repository.OfflineRepository
import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.FirestoreCollections

class OfflineRepositoryImpl(
    private val collections: FirestoreCollections,
) : OfflineRepository {

    override suspend fun getUser(userId: String) = flow {
        runCatching {
            collections.users().document(userId).get().data<User>()
        }.fold(
            onSuccess = { emit(Result.success(it)) },
            onFailure = { emit(Result.failure(it)) },
        )
    }

    override fun getAppliances() = flow {
        runCatching {
            collections.appliances().get().documents.map { it.data<Appliance>() }
        }.fold(
            onSuccess = { list ->
                if (list.isEmpty()) emit(Result.failure(Throwable())) else emit(Result.success(list))
            },
            onFailure = { emit(Result.failure(it)) },
        )
    }

    override fun getApplianceById(applianceId: String) = flow {
        runCatching {
            collections.appliances().document(applianceId).get().data<Appliance>()
        }.fold(
            onSuccess = { emit(Result.success(it)) },
            onFailure = { emit(Result.failure(it)) },
        )
    }
}
