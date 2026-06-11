package ru.dvfu.appliances.platform

object IosAppleAuthBridge {
    var signInImpl: ((onResult: (error: String?) -> Unit) -> Unit)? = null
}

fun installAppleSignInBridge(
    signIn: (onResult: (error: String?) -> Unit) -> Unit,
) {
    IosAppleAuthBridge.signInImpl = signIn
}
