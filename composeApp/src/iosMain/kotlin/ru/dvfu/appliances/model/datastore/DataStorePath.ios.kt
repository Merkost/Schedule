package ru.dvfu.appliances.model.datastore

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun dataStorePath(filename: String): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    )
    val documents = paths.firstOrNull() as? String ?: ""
    return "$documents/$filename.preferences_pb"
}
