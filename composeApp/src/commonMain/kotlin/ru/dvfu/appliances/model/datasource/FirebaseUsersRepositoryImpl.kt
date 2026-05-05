package ru.dvfu.appliances.model.datasource

import com.mmk.kmpnotifier.notification.NotifierManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    override suspend fun getUsers(): Flow<List<User>> =
        collections.users().snapshots
            .map { qs -> qs.documents.map { it.data<User>() } }
            .catch { emit(emptyList()) }

    override val currentUser: Flow<User?>
        get() = Firebase.auth.authStateChanged.map { it?.let(::mapFirebaseUserToUser) }

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
                val userFromDb = existing.data<User>()
                userDatastore.saveUser(userFromDb)
                uploadMessagingToken(userFromDb.userId)
            } else {
                collections.users().document(user.userId).set(user)
                userDatastore.saveUser(user)
                uploadMessagingToken(user.userId)
            }
        }
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
