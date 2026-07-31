package ru.dvfu.appliances.platform

const val IOS_APP_STORE_URL = "https://apps.apple.com/app/id6767415496"

fun buildIosAppShareMessage(template: String): String =
    template.replace("%s", IOS_APP_STORE_URL)
