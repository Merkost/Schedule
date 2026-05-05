package ru.dvfu.appliances.model.datastore

import android.content.Context
import co.touchlab.kermit.Logger
import org.koin.core.context.GlobalContext

actual fun dataStorePath(filename: String): String {
    val context = GlobalContext.get().get<Context>()
    val newFile = context.filesDir.resolve("$filename.preferences_pb")
    if (!newFile.exists()) {
        val legacyFile = context.filesDir.resolve("datastore/$filename.preferences_pb")
        if (legacyFile.exists()) {
            runCatching {
                legacyFile.copyTo(newFile, overwrite = false)
                legacyFile.delete()
            }.onFailure {
                Logger.withTag("DataStorePath").e(it) { "Failed to migrate legacy datastore file $legacyFile" }
            }
        }
    }
    return newFile.absolutePath
}
