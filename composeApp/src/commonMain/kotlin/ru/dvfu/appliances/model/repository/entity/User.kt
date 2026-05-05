package ru.dvfu.appliances.model.repository.entity

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String = "0",
    val msgToken: String = "",
    val userName: String = "",
    val email: String = "",
    val birthday: Long = 0,
    val role: Int = Roles.GUEST.ordinal,
    val anonymous: Boolean = false,
    val userPic: String = "",
)

val User.isAdmin: Boolean
    get() = role == Roles.ADMIN.ordinal

val User.isAnonymousOrGuest: Boolean
    get() = role == Roles.GUEST.ordinal || anonymous
