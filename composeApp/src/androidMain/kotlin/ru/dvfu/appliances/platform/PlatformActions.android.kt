package ru.dvfu.appliances.platform

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import ru.dvfu.appliances.application.AppContextHolder
import ru.dvfu.appliances.ui.LoginActivity

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

actual fun startLoginScreen() {
    val ctx = AppContextHolder.context
    val googleClient = GoogleSignIn.getClient(
        ctx,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build(),
    )
    googleClient.signOut()
    val intent = Intent(ctx, LoginActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    ctx.startActivity(intent)
}

actual fun finishApp() {
    AppContextHolder.finishCallback?.invoke()
}
