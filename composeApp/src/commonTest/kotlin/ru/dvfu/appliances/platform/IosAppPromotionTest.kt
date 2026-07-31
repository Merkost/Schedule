package ru.dvfu.appliances.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class IosAppPromotionTest {

    @Test
    fun shareMessageContainsCanonicalAppStoreUrl() {
        assertEquals(
            "Schedule is now available on iOS: $IOS_APP_STORE_URL",
            buildIosAppShareMessage("Schedule is now available on iOS: %s"),
        )
    }
}
