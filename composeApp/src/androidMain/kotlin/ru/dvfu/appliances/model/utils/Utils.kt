package ru.dvfu.appliances.model.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.app
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.context.unloadKoinModules
import ru.dvfu.appliances.R
import ru.dvfu.appliances.di.repositoryModule
import ru.dvfu.appliances.model.repository.entity.*
import java.time.Duration
import java.util.*


fun randomUUID() = UUID.randomUUID().toString()

fun Modifier.loadingModifier(
    enabled: Boolean = true,
) = composed(inspectorInfo = debugInspectorInfo {
    name = "loadingModifier"
    value = enabled
}) {
    if (!enabled) return@composed Modifier
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200)),
        label = "shimmerProgress",
    )
    val base = Color.LightGray
    val highlight = Color.White.copy(alpha = 0.6f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(progress * 400f - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(progress * 400f + 200f, 0f),
    )
    Modifier
        .clip(CircleShape)
        .background(brush)
}

fun showError(applicationContext: Context, text: String?) {
    showToast(
        applicationContext.applicationContext,
        text ?: applicationContext.resources.getString(R.string.error_occured)
    )
}

fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

suspend inline fun suspendCoroutineWithTimeout(
    timeout: Long = Duration.ofSeconds(8L).toMillis(),
    crossinline block: (CancellableContinuation<Result<Unit>>) -> Unit,
): Result<Unit> {
    return withTimeoutOrNull(timeout) {
        if (isNetworkAvailable(Firebase.app.applicationContext)) {
            suspendCancellableCoroutine(block)
        } else Result.failure(Throwable("Отсутствует интернет соединение"))
    } ?: run {
        Result.success(Unit)
        /*Firebase.firestore.terminate()
        Firebase.firestore.clearPersistence().await()
        Firebase.app.applicationContext.apply {
            unloadKoinModules(repositoryModule)
            loadKoinModules(repositoryModule)
        }
        Result.failure<T>(Throwable())*/
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

fun List<CalendarEvent>.filterForUser(currentUser: User): List<CalendarEvent> =
    when (currentUser.role) {
        Roles.USER.ordinal -> {
            filter { currentUser.canManageEvent(it) || it.user.userId == currentUser.userId || it.status == BookingStatus.APPROVED }
        }
        Roles.ADMIN.ordinal -> {
            this
        }
        else -> {
            filter { it.status == BookingStatus.APPROVED }
        }
    }

fun List<CalendarEvent>.filterWeekEventsForUser(currentUser: User): List<CalendarEvent> =
    when (currentUser.role) {
        Roles.USER.ordinal -> {
            filter { it.status == BookingStatus.APPROVED && (currentUser.canManageEvent(it) || it.user.userId == currentUser.userId) }
        }
        Roles.ADMIN.ordinal -> {
            filter { it.status == BookingStatus.APPROVED }
        }
        else -> {
            filter { it.status == BookingStatus.APPROVED }
        }
    }
