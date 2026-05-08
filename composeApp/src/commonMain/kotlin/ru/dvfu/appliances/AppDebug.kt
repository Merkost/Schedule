package ru.dvfu.appliances

import kotlin.concurrent.Volatile

object AppDebug {
    @Volatile
    var isDebug: Boolean = false
}
