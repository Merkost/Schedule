package ru.dvfu.appliances.compose

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.InternalCoroutinesApi
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalComposeUiApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getFirebaseMessagingToken()
        setContent {
            ScheduleTheme {
                ScheduleApp()
            }
        }
    }

    private fun getFirebaseMessagingToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(this.localClassName, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(this.localClassName, msg)
        }
    }
}

@Composable
@ReadOnlyComposable
fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}
