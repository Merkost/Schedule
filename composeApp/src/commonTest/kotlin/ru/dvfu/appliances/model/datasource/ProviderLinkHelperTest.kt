package ru.dvfu.appliances.model.datasource

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderLinkHelperTest {

    @Test
    fun googleProviderIsDetected() {
        assertEquals(
            LinkedProviders(google = true, apple = false),
            linkedProviders(listOf("google.com")),
        )
    }

    @Test
    fun emptyProviderListReturnsNoLinkedProviders() {
        assertEquals(
            LinkedProviders(google = false, apple = false),
            linkedProviders(emptyList()),
        )
    }

    @Test
    fun googleAndAppleProvidersAreDetected() {
        assertEquals(
            LinkedProviders(google = true, apple = true),
            linkedProviders(listOf("google.com", "apple.com")),
        )
    }

    @Test
    fun credentialAlreadyInUseMapsToAlreadyInUse() {
        assertEquals(
            LinkError.ALREADY_IN_USE,
            classifyLinkError(IllegalStateException("ERROR_CREDENTIAL_ALREADY_IN_USE")),
        )
    }

    @Test
    fun cancellationExceptionMapsToCancelled() {
        assertEquals(
            LinkError.CANCELLED,
            classifyLinkError(CancellationException("cancelled")),
        )
    }

    @Test
    fun unknownThrowableMapsToGeneric() {
        assertEquals(
            LinkError.GENERIC,
            classifyLinkError(IllegalStateException("network failed")),
        )
    }
}
