package ru.dvfu.appliances.platform

object IosGoogleAuthBridge {
    var signInImpl: ((onResult: (idToken: String?, error: String?) -> Unit) -> Unit)? = null
    var signOutImpl: (() -> Unit)? = null
}

fun installSignInBridge(
    signIn: (onResult: (idToken: String?, error: String?) -> Unit) -> Unit,
    signOut: () -> Unit,
) {
    IosGoogleAuthBridge.signInImpl = signIn
    IosGoogleAuthBridge.signOutImpl = signOut
}
