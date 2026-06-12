package ru.dvfu.appliances.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class AppleAuthLauncher {

    actual val isAvailable: Boolean = true

    actual suspend fun signIn(): Result<Unit> {
        val impl = IosAppleAuthBridge.signInImpl
            ?: return Result.failure(IllegalStateException("Apple sign-in bridge not installed"))
        return suspendCancellableCoroutine { cont ->
            impl { error ->
                if (!cont.isActive) return@impl
                when (error) {
                    null -> cont.resume(Result.success(Unit))
                    "canceled" -> cont.resume(Result.failure(CancellationException("Apple sign-in canceled")))
                    else -> cont.resume(Result.failure(IllegalStateException(error)))
                }
            }
        }
    }

    actual suspend fun link(): Result<Unit> {
        val impl = IosAppleAuthBridge.linkImpl
            ?: return Result.failure(IllegalStateException("Apple link bridge not installed"))
        return suspendCancellableCoroutine { cont ->
            impl { error ->
                if (!cont.isActive) return@impl
                when (error) {
                    null -> cont.resume(Result.success(Unit))
                    "canceled" -> cont.resume(Result.failure(CancellationException("Apple link canceled")))
                    else -> cont.resume(Result.failure(IllegalStateException(error)))
                }
            }
        }
    }
}
