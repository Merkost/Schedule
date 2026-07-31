package ru.dvfu.appliances.platform

expect fun showToast(message: String)

expect fun showError(message: String?)

expect fun openAppNotificationSettings()

expect fun openExternalUrl(url: String)

expect fun shareText(text: String)

expect fun finishApp()
