package ru.dvfu.appliances.notifications

import com.mmk.kmpnotifier.push.PushListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AppPushListener(
    private val updateToken: suspend (String) -> Unit,
    private val scope: CoroutineScope,
) : PushListener {

    override fun onNewToken(token: String) {
        if (token.isBlank()) return
        scope.launch { updateToken(token) }
    }
}
