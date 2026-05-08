package ru.dvfu.appliances.application

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CompletableDeferred
import ru.dvfu.appliances.platform.GoogleSignInTokens

object ActivityHolder {
    var activity: ComponentActivity? = null
    var googleSignInLauncher: ActivityResultLauncher<Intent>? = null
    var pendingSignIn: CompletableDeferred<Result<GoogleSignInTokens>>? = null
}
