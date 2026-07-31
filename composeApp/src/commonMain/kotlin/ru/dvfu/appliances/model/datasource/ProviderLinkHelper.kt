package ru.dvfu.appliances.model.datasource

import kotlinx.coroutines.CancellationException

data class LinkedProviders(
    val google: Boolean = false,
    val apple: Boolean = false,
)

fun linkedProviders(providerData: List<String>): LinkedProviders = LinkedProviders(
    google = "google.com" in providerData,
    apple = "apple.com" in providerData,
)

enum class LinkError {
    ALREADY_IN_USE,
    CANCELLED,
    GENERIC,
}

fun classifyLinkError(e: Throwable): LinkError {
    if (e is CancellationException) return LinkError.CANCELLED

    val message = e.message.orEmpty().lowercase()
    return when {
        "error_credential_already_in_use" in message -> LinkError.ALREADY_IN_USE
        "credential already" in message && "use" in message -> LinkError.ALREADY_IN_USE
        "already associated" in message -> LinkError.ALREADY_IN_USE
        "already linked" in message -> LinkError.ALREADY_IN_USE
        "error_cancelled" in message -> LinkError.CANCELLED
        "cancelled" in message || "canceled" in message -> LinkError.CANCELLED
        else -> LinkError.GENERIC
    }
}
