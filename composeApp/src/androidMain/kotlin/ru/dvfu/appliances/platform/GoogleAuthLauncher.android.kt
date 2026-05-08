package ru.dvfu.appliances.platform

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import ru.dvfu.appliances.application.ActivityHolder

actual class GoogleAuthLauncher(private val context: Context) {

    private val client: GoogleSignInClient by lazy {
        val webClientId = context.getString(
            context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        )
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build(),
        )
    }

    actual suspend fun signIn(): Result<GoogleSignInTokens> {
        val launcher = ActivityHolder.googleSignInLauncher
            ?: return Result.failure(IllegalStateException("Activity launcher not registered"))
        val deferred = CompletableDeferred<Result<GoogleSignInTokens>>()
        ActivityHolder.pendingSignIn = deferred
        runCatching { launcher.launch(client.signInIntent) }
            .onFailure {
                ActivityHolder.pendingSignIn = null
                return Result.failure(it)
            }
        return deferred.await()
    }

    actual suspend fun signOut() {
        runCatching { client.signOut().await() }
    }

    companion object {
        fun completePendingSignIn(intentData: android.content.Intent?) {
            val deferred = ActivityHolder.pendingSignIn ?: return
            ActivityHolder.pendingSignIn = null
            val task = GoogleSignIn.getSignedInAccountFromIntent(intentData)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken.isNullOrEmpty()) {
                    deferred.complete(Result.failure(IllegalStateException("Google sign-in returned no id token")))
                } else {
                    deferred.complete(
                        Result.success(GoogleSignInTokens(idToken = idToken, accessToken = null)),
                    )
                }
            } catch (e: Throwable) {
                deferred.complete(Result.failure(e))
            }
        }
    }
}
