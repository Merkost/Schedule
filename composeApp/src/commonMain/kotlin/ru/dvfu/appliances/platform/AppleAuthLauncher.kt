package ru.dvfu.appliances.platform

expect class AppleAuthLauncher {
    val isAvailable: Boolean
    suspend fun signIn(): Result<Unit>
    suspend fun link(): Result<Unit>
}
