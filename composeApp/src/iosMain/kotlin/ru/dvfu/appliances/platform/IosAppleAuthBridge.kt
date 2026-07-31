package ru.dvfu.appliances.platform

object IosAppleAuthBridge {
    var signInImpl: ((onResult: (error: String?) -> Unit) -> Unit)? = null
    var linkImpl: ((onResult: (error: String?) -> Unit) -> Unit)? = null
}

fun installAppleSignInBridge(
    signIn: (onResult: (error: String?) -> Unit) -> Unit,
) {
    IosAppleAuthBridge.signInImpl = signIn
}

fun installAppleLinkBridge(
    link: (onResult: (error: String?) -> Unit) -> Unit,
) {
    IosAppleAuthBridge.linkImpl = link
}
