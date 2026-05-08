package ru.dvfu.appliances.platform

data class GoogleSignInTokens(
    val idToken: String,
    val accessToken: String?,
)

expect class GoogleAuthLauncher {
    suspend fun signIn(): Result<GoogleSignInTokens>
    suspend fun signOut()
}
