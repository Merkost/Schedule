package ru.dvfu.appliances.model.datasource.mock

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockUsersRepositoryTest {

    @Test
    fun deleteCurrentAccountFailsWhenNoCurrentUser() = runTest {
        val repository = MockUsersRepository()

        repository.logoutCurrentUser().first()
        val result = repository.deleteCurrentAccount()

        assertTrue(result.isFailure)
        assertEquals("No signed-in user", result.exceptionOrNull()?.message)
    }
}
