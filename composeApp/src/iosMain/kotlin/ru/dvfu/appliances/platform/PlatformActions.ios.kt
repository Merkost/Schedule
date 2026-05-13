package ru.dvfu.appliances.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import ru.dvfu.appliances.application.SnackbarManager

actual fun showToast(message: String) {
    SnackbarManager.showText(message)
}

actual fun showError(message: String?) {
    if (!message.isNullOrBlank()) SnackbarManager.showText(message)
}

actual fun openAppNotificationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}

actual fun finishApp() {
}
