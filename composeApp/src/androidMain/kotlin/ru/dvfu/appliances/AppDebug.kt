package ru.dvfu.appliances

import android.content.Context
import android.content.pm.ApplicationInfo

fun AppDebug.init(context: Context) {
    isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
