package ru.dvfu.appliances.platform

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import ru.dvfu.appliances.application.AppContextHolder

actual fun showToast(message: String) {
    val ctx = AppContextHolder.context
    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
}

actual fun showError(message: String?) {
    showToast(message ?: "Error")
}

actual fun openAppNotificationSettings() {
    val ctx = AppContextHolder.context
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.parse("package:" + ctx.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

actual fun openExternalUrl(url: String) {
    val ctx = AppContextHolder.context
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

actual fun shareText(text: String) {
    val ctx = AppContextHolder.context
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

actual fun finishApp() {
    AppContextHolder.finishCallback?.invoke()
}
