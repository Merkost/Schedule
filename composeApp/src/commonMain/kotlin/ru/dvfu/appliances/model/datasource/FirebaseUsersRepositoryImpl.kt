package ru.dvfu.appliances.model.datasource

import org.kimplify.cedar.Cedar
import com.mmk.kmpnotifier.notification.NotifierManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.Roles
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.FirestoreCollections
import ru.dvfu.appliances.ui.Progress

class FirebaseUsersRepositoryImpl(
    private val collections: FirestoreCollections,
    private val userDatastore: UserDatastore,
    private val appScope: CoroutineScope,
) : UsersRepository {

    private val log = Cedar.tag("UsersRepo")

    private val userDocumentInitializer = UserDocumentInitializer(
        appScope = appScope,
        docExists = { uid -> collections.users().document(uid).get().exists },
        createDoc = { user ->
            log.d("ensureUserDocument creating uid=${user.userId}")
            collections.users().document(user.userId).set(user)
            userDatastore.saveUser(user)
            uploadMessagingToken(user.userId)
        },
        onError = { e -> log.e("ensureUserDocument failed", e) },
    )

    init {
        appScope.launch {
            Firebase.auth.authStateChanged
                .distinctUntilChanged { old, new -> old?.uid == new?.uid }
                .collect { fbUser ->
                    if (fbUser != null && !fbUser.isAnonymous) {
                        userDocumentInitializer.ensure(mapFirebaseUserToUser(fbUser))
                    }
                }
        }
    }

    override suspend fun getUsers(): Flow<List<User>> =
        collections.users().snapshots
            .map { qs ->
                val list = qs.documents.map { it.data<User>() }
                log.d("getUsers emit count=${list.size}")
                list
            }
            .catch { e ->
                log.e("getUsers failed", e)
                emit(emptyList())
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<User?>
        get() = Firebase.auth.authStateChanged
            .onEach { fb -> log.d("authStateChanged uid=${fb?.uid}") }
            .flatMapLatest { fbUser ->
                if (fbUser == null) {
                    flowOf(null)
                } else {
                    val fallback = mapFirebaseUserToUser(fbUser)
                    collections.users().document(fbUser.uid).snapshots
                        .map { snap ->
                            if (snap.exists) {
                                runCatching { snap.data<User>() }
                                    .onFailure { log.e("currentUser deser failed for uid=${fbUser.uid}", it) }
                                    .getOrDefault(fallback)
                            } else {
                                log.d("currentUser doc missing for uid=${fbUser.uid}, using auth fallback")
                                fallback
                            }
                        }
                        .onEach { u -> log.d("currentUser emit uid=${u.userId} role=${u.role} name=${u.userName}") }
                        .catch { e ->
                            log.e("currentUser snapshots failed for uid=${fbUser.uid}, falling back to auth user", e)
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
                    .onFailure { log.e("addNewUser deser existing user failed uid=${user.userId}", it) }
                    .getOrDefault(user)
                log.d("addNewUser found existing uid=${userFromDb.userId} role=${userFromDb.role}")
                userDatastore.saveUser(userFromDb)
                uploadMessagingToken(userFromDb.userId)
            } else {
                log.d("addNewUser creating new user uid=${user.userId}")
                collections.users().document(user.userId).set(user)
                userDatastore.saveUser(user)
                uploadMessagingToken(user.userId)
            }
        }.onFailure { log.e("addNewUser failed uid=${user.userId}", it) }
        flow.tryEmit(Progress.Complete)
        return flow
    }

    override suspend fun uploadCurrentMessagingToken() {
        val uid = currentUser.first()?.userId?.takeIf { it.isNotBlank() && it != "0" } ?: return
        runCatching {
            val token = NotifierManager.getPushNotifier().getToken()
            if (token.isNullOrBlank()) return@runCatching
            collections.users().document(uid).updateFields {
                "msgToken" to token
            }
        }
    }

    private suspend fun uploadMessagingToken(userId: String) {
        runCatching {
            val token = NotifierManager.getPushNotifier().getToken()
            if (!token.isNullOrBlank()) {
                collections.users().document(userId).updateFields {
                    "msgToken" to token
                }
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
        collections.users().document(userId).updateFields {
            data.forEach { (key, value) -> key to value }
        }
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
            collections.users().document(userId).updateFields {
                data.forEach { (key, value) -> key to value }
            }
        }

    override suspend fun updateCurrentUserField(data: Map<String, Any>) {
        val userId = userDatastore.getCurrentUser.first().userId
        runCatching {
            collections.users().document(userId).updateFields {
                data.forEach { (key, value) -> key to value }
            }
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
