package ru.dvfu.appliances.model.datasource

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.dvfu.appliances.model.repository.entity.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserDocumentInitializerTest {

    private val newUser = User(userId = "uid-1", userName = "New", email = "a@b.c", role = 0)

    @Test
    fun createsDocumentWhenMissing() = runTest {
        val created = mutableListOf<User>()
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val initializer = UserDocumentInitializer(
            appScope = appScope,
            docExists = { false },
            createDoc = { created.add(it) },
        )

        initializer.ensure(newUser)
        advanceUntilIdle()

        assertEquals(listOf(newUser), created)
        appScope.cancel()
    }

    @Test
    fun doesNotRecreateExistingDocument() = runTest {
        val created = mutableListOf<User>()
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val initializer = UserDocumentInitializer(
            appScope = appScope,
            docExists = { true },
            createDoc = { created.add(it) },
        )

        initializer.ensure(newUser)
        advanceUntilIdle()

        assertTrue(created.isEmpty())
        appScope.cancel()
    }

    @Test
    fun skipsAnonymousUsers() = runTest {
        val created = mutableListOf<User>()
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val initializer = UserDocumentInitializer(
            appScope = appScope,
            docExists = { false },
            createDoc = { created.add(it) },
        )

        initializer.ensure(newUser.copy(anonymous = true))
        advanceUntilIdle()

        assertTrue(created.isEmpty())
        appScope.cancel()
    }

    @Test
    fun completesWriteEvenWhenCallerScopeIsCancelled() = runTest {
        val created = CompletableDeferred<User>()
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val initializer = UserDocumentInitializer(
            appScope = appScope,
            docExists = { false },
            createDoc = { user ->
                delay(1_000)
                created.complete(user)
            },
        )

        val callerScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        callerScope.launch { initializer.ensure(newUser) }
        callerScope.cancel()
        advanceUntilIdle()

        assertEquals(newUser, created.await())
        appScope.cancel()
    }
}
