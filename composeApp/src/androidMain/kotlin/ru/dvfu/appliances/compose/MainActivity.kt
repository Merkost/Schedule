package ru.dvfu.appliances.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import ru.dvfu.appliances.application.ActivityHolder
import ru.dvfu.appliances.application.AppContextHolder
import ru.dvfu.appliances.compose.ui.theme.ScheduleTheme
import ru.dvfu.appliances.platform.GoogleAuthLauncher

class MainActivity : ComponentActivity() {

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalCoroutinesApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        KMPNotifier.onCreateOrOnNewIntent(intent)
        enableEdgeToEdge()
        AppContextHolder.finishCallback = { finishAffinity() }

        ActivityHolder.activity = this
        ActivityHolder.googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            GoogleAuthLauncher.completePendingSignIn(result.data)
        }

        setContent {
            ScheduleTheme {
                ScheduleApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        KMPNotifier.onCreateOrOnNewIntent(intent)
    }

    override fun onDestroy() {
        AppContextHolder.finishCallback = null
        if (ActivityHolder.activity === this) {
            ActivityHolder.activity = null
            ActivityHolder.googleSignInLauncher = null
        }
        super.onDestroy()
    }
}
