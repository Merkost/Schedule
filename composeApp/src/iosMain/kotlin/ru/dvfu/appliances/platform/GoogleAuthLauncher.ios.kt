package ru.dvfu.appliances.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class GoogleAuthLauncher {

    actual suspend fun signIn(): Result<GoogleSignInTokens> {
        val impl = IosGoogleAuthBridge.signInImpl
            ?: return Result.failure(IllegalStateException("Google sign-in bridge not installed"))
        return suspendCancellableCoroutine { cont ->
            impl { idToken, accessToken, error ->
                if (cont.isActive) {
                    when {
                        idToken != null -> cont.resume(
                            Result.success(GoogleSignInTokens(idToken = idToken, accessToken = accessToken)),
                        )
                        else -> cont.resume(
                            Result.failure(IllegalStateException(error ?: "Unknown sign-in error")),
                        )
                    }
                }
            }
        }
    }

    actual suspend fun signOut() {
        IosGoogleAuthBridge.signOutImpl?.invoke()
    }
}
