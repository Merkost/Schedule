package ru.dvfu.appliances.model.datasource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.dvfu.appliances.model.repository.entity.User

class UserDocumentInitializer(
    private val appScope: CoroutineScope,
    private val docExists: suspend (userId: String) -> Boolean,
    private val createDoc: suspend (user: User) -> Unit,
    private val onError: (Throwable) -> Unit = {},
) {
    fun ensure(user: User) {
        if (user.anonymous || user.userId.isBlank() || user.userId == "0") return
        appScope.launch {
            try {
                if (!docExists(user.userId)) createDoc(user)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }
}
