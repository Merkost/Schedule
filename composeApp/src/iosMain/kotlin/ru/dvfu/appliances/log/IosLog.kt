package ru.dvfu.appliances.log

import org.kimplify.cedar.Cedar

fun iosLog(tag: String, message: String) {
    Cedar.tag(tag).d(message)
}

fun iosLogError(tag: String, message: String) {
    Cedar.tag(tag).e(message)
}
