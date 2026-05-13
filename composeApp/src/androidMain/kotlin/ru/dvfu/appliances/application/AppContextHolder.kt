package ru.dvfu.appliances.application

import android.content.Context

object AppContextHolder {
    lateinit var context: Context
        internal set

    var finishCallback: (() -> Unit)? = null
}
