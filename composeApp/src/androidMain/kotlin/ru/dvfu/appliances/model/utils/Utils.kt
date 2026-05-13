package ru.dvfu.appliances.model.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.Firebase
import com.google.firebase.app
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import ru.dvfu.appliances.platform.showToast as platformShowToast

fun showError(applicationContext: Context, text: String?) {
    platformShowToast(text ?: "Error")
}

fun showToast(context: Context, text: String) {
    platformShowToast(text)
}

suspend inline fun suspendCoroutineWithTimeout(
    timeout: Long = 8.seconds.inWholeMilliseconds,
    crossinline block: (CancellableContinuation<Result<Unit>>) -> Unit,
): Result<Unit> {
    return withTimeoutOrNull(timeout) {
        if (isNetworkAvailable(Firebase.app.applicationContext)) {
            suspendCancellableCoroutine(block)
        } else Result.failure(Throwable("Отсутствует интернет соединение"))
    } ?: run {
        Result.success(Unit)
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
        return getNetworkCapabilities(activeNetwork)?.run {
            when {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } ?: false
    }
}
