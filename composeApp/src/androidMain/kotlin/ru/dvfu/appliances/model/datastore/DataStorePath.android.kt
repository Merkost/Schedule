package ru.dvfu.appliances.model.datastore

import android.content.Context
import org.kimplify.cedar.Cedar
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
                Cedar.tag("DataStorePath").e("Failed to migrate legacy datastore file $legacyFile", it)
            }
        }
    }
    return newFile.absolutePath
}
