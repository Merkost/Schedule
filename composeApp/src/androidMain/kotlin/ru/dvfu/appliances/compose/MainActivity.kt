package ru.dvfu.appliances.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import ru.dvfu.appliances.application.AppContextHolder
import ru.dvfu.appliances.compose.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalCoroutinesApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContextHolder.finishCallback = { finishAffinity() }
        getFirebaseMessagingToken()
        setContent {
            ScheduleTheme {
                ScheduleApp()
            }
        }
    }

    override fun onDestroy() {
        AppContextHolder.finishCallback = null
        super.onDestroy()
    }

    private fun getFirebaseMessagingToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(this.localClassName, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(this.localClassName, "FCM token: $token")
        }
    }
}
