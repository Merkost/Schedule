package ru.dvfu.appliances.model.repository.entity

import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.model.utils.StringOperation

import org.jetbrains.compose.resources.StringResource

enum class Roles(override val stringRes: StringResource, val pluralStringRes: StringResource): StringOperation {
    GUEST(Res.string.guest, Res.string.guests),
    USER(Res.string.user, Res.string.users),
    ADMIN(Res.string.admin, Res.string.admins);

    fun isAdmin(): Boolean {
        return when(this) {
            ADMIN -> true
            else -> false
        }
    }

}

fun getRole(ordinal: Int): Roles {
    return when (ordinal) {
        Roles.GUEST.ordinal -> Roles.GUEST
        Roles.USER.ordinal -> Roles.USER
        Roles.ADMIN.ordinal -> Roles.ADMIN
        else -> Roles.GUEST
    }
}