package ru.dvfu.appliances.platform

expect class GoogleAuthLauncher {
    suspend fun signIn(): Result<String>
    suspend fun signOut()
}
