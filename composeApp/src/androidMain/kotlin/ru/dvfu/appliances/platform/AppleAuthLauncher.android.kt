package ru.dvfu.appliances.platform

actual class AppleAuthLauncher {
    actual val isAvailable: Boolean = false
    actual suspend fun signIn(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Apple sign-in not supported on Android"))

    actual suspend fun link(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Apple sign-in not supported on Android"))
}
