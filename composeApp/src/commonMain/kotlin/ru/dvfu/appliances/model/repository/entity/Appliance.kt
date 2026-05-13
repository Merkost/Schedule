package ru.dvfu.appliances.model.repository.entity

import kotlinx.serialization.Serializable
import ru.dvfu.appliances.model.utils.Constants
import ru.dvfu.appliances.model.utils.randomUUID

@Serializable
data class Appliance(
    val id: String = randomUUID(),
    val name: String = "",
    val description: String = "",
    val color: Int = Constants.DEFAULT_EVENT_COLOR.hashCode(),
    val createdById: String = "",
    val superuserIds: List<String> = listOf(),
    val userIds: List<String> = listOf(),
    val active: Boolean = true,
)

fun Appliance.isUserSuperuserOrAdmin(user: User): Boolean =
    user.isAdmin || superuserIds.contains(user.userId)
