package ru.dvfu.appliances.model.datastore

import android.content.Context
import org.koin.core.context.GlobalContext

actual fun dataStorePath(filename: String): String {
    val context = GlobalContext.get().get<Context>()
    return context.filesDir.resolve("$filename.preferences_pb").absolutePath
}
