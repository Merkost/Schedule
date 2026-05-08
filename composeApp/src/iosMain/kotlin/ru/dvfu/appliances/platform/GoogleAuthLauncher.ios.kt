package ru.dvfu.appliances.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class GoogleAuthLauncher {

    actual suspend fun signIn(): Result<String> {
        val impl = IosGoogleAuthBridge.signInImpl
            ?: return Result.failure(IllegalStateException("Google sign-in bridge not installed"))
        return suspendCancellableCoroutine { cont ->
            impl { idToken, error ->
                if (cont.isActive) {
                    when {
                        idToken != null -> cont.resume(Result.success(idToken))
                        else -> cont.resume(
                            Result.failure(IllegalStateException(error ?: "Unknown sign-in error"))
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
