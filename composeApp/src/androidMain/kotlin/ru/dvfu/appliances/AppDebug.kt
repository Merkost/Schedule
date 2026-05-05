package ru.dvfu.appliances

import android.content.Context
import android.content.pm.ApplicationInfo

object AppDebug {
    @Volatile
    var isDebug: Boolean = false
        private set

    fun init(context: Context) {
        isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
