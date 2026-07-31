package ru.dvfu.appliances.compose.viewmodels

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.dvfu.appliances.application.Message
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.calendars.CalendarType
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.delete_account_failed
import ru.dvfu.appliances.model.datastore.ThemeMode
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.ui.Progress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun deleteCurrentAccountDoesNotCallRepositoryForDefaultCurrentUser() = viewModelTest {
        val usersRepository = FakeUsersRepository()
        val viewModel = createViewModel(
            usersRepository = usersRepository,
            currentUser = User(),
        )
        var deleted = false

        viewModel.deleteCurrentAccount { deleted = true }
        advanceUntilIdle()

        assertEquals(0, usersRepository.deleteCurrentAccountCalls)
        assertFalse(deleted)
        assertTrue(viewModel.accountDeletionState.value is UiState.Success)
    }

    @Test
    fun deleteCurrentAccountDoesNotCallRepositoryForRegisteredCurrentUser() = viewModelTest {
        val usersRepository = FakeUsersRepository()
        val viewModel = createViewModel(
            usersRepository = usersRepository,
            currentUser = User(userId = "user-1", anonymous = false),
        )
        var deleted = false

        viewModel.deleteCurrentAccount { deleted = true }
        advanceUntilIdle()

        assertEquals(0, usersRepository.deleteCurrentAccountCalls)
        assertFalse(deleted)
        assertTrue(viewModel.accountDeletionState.value is UiState.Success)
    }

    @Test
    fun deleteCurrentAccountSetsInProgressThenSuccessAndInvokesCallbackForAnonymousUser() = viewModelTest {
        val deletionResult = CompletableDeferred<Result<Unit>>()
        val usersRepository = FakeUsersRepository { deletionResult.await() }
        val viewModel = createViewModel(usersRepository = usersRepository)
        var deleted = false

        viewModel.deleteCurrentAccount { deleted = true }
        runCurrent()

        assertEquals(1, usersRepository.deleteCurrentAccountCalls)
        assertFalse(deleted)
        assertTrue(viewModel.accountDeletionState.value is UiState.InProgress)

        deletionResult.complete(Result.success(Unit))
        advanceUntilIdle()

        assertTrue(deleted)
        assertTrue(viewModel.accountDeletionState.value is UiState.Success)
    }

    @Test
    fun deleteCurrentAccountIgnoresDuplicateCallsWhileDeletionIsInProgress() = viewModelTest {
        val deletionResult = CompletableDeferred<Result<Unit>>()
        val usersRepository = FakeUsersRepository { deletionResult.await() }
        val viewModel = createViewModel(usersRepository = usersRepository)
        var deletedCount = 0

        viewModel.deleteCurrentAccount { deletedCount++ }
        viewModel.deleteCurrentAccount { deletedCount++ }
        runCurrent()

        assertEquals(1, usersRepository.deleteCurrentAccountCalls)
        assertTrue(viewModel.accountDeletionState.value is UiState.InProgress)

        deletionResult.complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(1, deletedCount)
        assertTrue(viewModel.accountDeletionState.value is UiState.Success)
    }

    @Test
    fun deleteCurrentAccountSetsErrorAndShowsSnackbarWhenRepositoryFails() = viewModelTest {
        val usersRepository = FakeUsersRepository {
            Result.failure(IllegalStateException("delete failed"))
        }
        val viewModel = createViewModel(usersRepository = usersRepository)
        var deleted = false

        viewModel.deleteCurrentAccount { deleted = true }
        advanceUntilIdle()

        assertEquals(1, usersRepository.deleteCurrentAccountCalls)
        assertFalse(deleted)
        assertTrue(viewModel.accountDeletionState.value is UiState.Error)
        val message = SnackbarManager.messages.value.last()
        assertTrue(message is Message.FromResource)
        assertEquals(Res.string.delete_account_failed, message.resource)
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        clearSnackbarMessages()
        try {
            block()
        } finally {
            clearSnackbarMessages()
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.createViewModel(
        usersRepository: FakeUsersRepository,
        currentUser: User = User(userId = "guest-1", anonymous = true),
    ): ProfileViewModel {
        val viewModel = ProfileViewModel(
            userDatastore = FakeUserDatastore(currentUser),
            usersRepository = usersRepository,
        )
        advanceUntilIdle()
        return viewModel
    }

    private fun clearSnackbarMessages() {
        SnackbarManager.messages.value.forEach { message ->
            SnackbarManager.setMessageShown(message.id)
        }
    }

    private class FakeUsersRepository(
        private val deleteCurrentAccountResult: suspend () -> Result<Unit> = { Result.success(Unit) },
    ) : UsersRepository {
        var deleteCurrentAccountCalls = 0

        override val currentUser: Flow<User?> = flowOf(null)

        override fun ensureCurrentUserDocument() = Unit

        override suspend fun getUsers(): Flow<List<User>> = flowOf(emptyList())

        override suspend fun logoutCurrentUser(): Flow<Boolean> = flowOf(true)

        override suspend fun deleteCurrentAccount(): Result<Unit> {
            deleteCurrentAccountCalls++
            return deleteCurrentAccountResult()
        }

        override suspend fun addNewUser(user: User): StateFlow<Progress> = error("Unexpected call")

        override suspend fun getUser(userId: String): Result<User> = error("Unexpected call")

        override suspend fun updateUserField(userId: String, data: Map<String, Any>): Result<Unit> =
            error("Unexpected call")

        override suspend fun updateCurrentUserField(data: Map<String, Any>) = error("Unexpected call")

        override suspend fun setUserListener(user: User) = Unit

        override suspend fun uploadCurrentMessagingToken() = Unit

        override suspend fun setNewProfileData(userId: String, data: Map<String, Any>): Result<Unit> =
            error("Unexpected call")
    }

    private class FakeUserDatastore(currentUser: User) : UserDatastore {
        override val getCalendarType: Flow<CalendarType> = flowOf(CalendarType.WEEK)
        override val getCurrentUser: Flow<User> = flowOf(currentUser)
        override val getThemeMode: Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)
        override val getIosAppPromotionDismissed: Flow<Boolean> = flowOf(false)

        override suspend fun saveCalendarType(calendarType: CalendarType) = Unit

        override suspend fun saveUser(user: User) = Unit

        override suspend fun saveThemeMode(mode: ThemeMode) = Unit

        override suspend fun saveIosAppPromotionDismissed(dismissed: Boolean) = Unit
    }
}
