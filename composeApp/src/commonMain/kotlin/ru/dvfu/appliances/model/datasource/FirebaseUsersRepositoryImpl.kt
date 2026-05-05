package ru.dvfu.appliances.model.datasource

import co.touchlab.kermit.Logger
import com.mmk.kmpnotifier.notification.NotifierManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.Roles
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.FirestoreCollections
import ru.dvfu.appliances.ui.Progress

class FirebaseUsersRepositoryImpl(
    private val collections: FirestoreCollections,
    private val userDatastore: UserDatastore,
) : UsersRepository {

    private val log = Logger.withTag("UsersRepo")

    override suspend fun getUsers(): Flow<List<User>> =
        collections.users().snapshots
            .map { qs ->
                val list = qs.documents.map { it.data<User>() }
                log.d { "getUsers emit count=${list.size}" }
                list
            }
            .catch { e ->
                log.e(e) { "getUsers failed" }
                emit(emptyList())
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<User?>
        get() = Firebase.auth.authStateChanged
            .onEach { fb -> log.d { "authStateChanged uid=${fb?.uid}" } }
            .flatMapLatest { fbUser ->
                if (fbUser == null) {
                    flowOf(null)
                } else {
                    val fallback = mapFirebaseUserToUser(fbUser)
                    collections.users().document(fbUser.uid).snapshots
                        .map { snap ->
                            if (snap.exists) {
                                runCatching { snap.data<User>() }
                                    .onFailure { log.e(it) { "currentUser deser failed for uid=${fbUser.uid}" } }
                                    .getOrDefault(fallback)
                            } else {
                                log.d { "currentUser doc missing for uid=${fbUser.uid}, using auth fallback" }
                                fallback
                            }
                        }
                        .onEach { u -> log.d { "currentUser emit uid=${u?.userId} role=${u?.role} name=${u?.userName}" } }
                        .catch { e ->
                            log.e(e) { "currentUser snapshots failed for uid=${fbUser.uid}, falling back to auth user" }
                            emit(fallback)
                        }
                }
            }

    override suspend fun addNewUser(user: User): StateFlow<Progress> {
        val flow = MutableStateFlow<Progress>(Progress.Loading())

        if (user.anonymous) {
            userDatastore.saveUser(user)
            flow.tryEmit(Progress.Complete)
            return flow
        }

        runCatching {
            val existing = collections.users().document(user.userId).get()
            if (existing.exists) {
                val userFromDb = runCatching { existing.data<User>() }
                    .onFailure { log.e(it) { "addNewUser deser existing user failed uid=${user.userId}" } }
                    .getOrDefault(user)
                log.d { "addNewUser found existing uid=${userFromDb.userId} role=${userFromDb.role}" }
                userDatastore.saveUser(userFromDb)
                uploadMessagingToken(userFromDb.userId)
            } else {
                log.d { "addNewUser creating new user uid=${user.userId}" }
                collections.users().document(user.userId).set(user)
                userDatastore.saveUser(user)
                uploadMessagingToken(user.userId)
            }
        }.onFailure { log.e(it) { "addNewUser failed uid=${user.userId}" } }
        flow.tryEmit(Progress.Complete)
        return flow
    }

    private suspend fun uploadMessagingToken(userId: String) {
        runCatching {
            val token = NotifierManager.getPushNotifier().getToken()
            if (!token.isNullOrBlank()) {
                collections.users().document(userId).update("msgToken" to token)
            }
        }
    }

    override suspend fun setUserListener(user: User) {
        // Snapshot subscription owner moved to ViewModels; this remains a no-op
        // to satisfy the interface contract.
    }

    override suspend fun setNewProfileData(
        userId: String,
        data: Map<String, Any>,
    ): Result<Unit> = runCatching {
        collections.users().document(userId).update(*data.toList().toTypedArray())
    }

    override suspend fun logoutCurrentUser(): Flow<Boolean> = flow {
        Firebase.auth.signOut()
        emit(true)
    }

    override suspend fun getUser(userId: String): Result<User> = runCatching {
        collections.users().document(userId).get().data<User>()
    }

    override suspend fun updateUserField(userId: String, data: Map<String, Any>): Result<Unit> =
        runCatching {
            collections.users().document(userId).update(*data.toList().toTypedArray())
        }

    override suspend fun updateCurrentUserField(data: Map<String, Any>) {
        val userId = userDatastore.getCurrentUser.first().userId
        runCatching {
            collections.users().document(userId).update(*data.toList().toTypedArray())
        }
    }

    private fun mapFirebaseUserToUser(firebaseUser: FirebaseUser): User = User(
        userId = firebaseUser.uid,
        userName = firebaseUser.displayName ?: "Аноним",
        email = firebaseUser.email ?: "",
        role = Roles.GUEST.ordinal,
        anonymous = firebaseUser.isAnonymous,
        userPic = firebaseUser.photoURL ?: "",
    )
}
