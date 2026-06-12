package ru.dvfu.appliances.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class AddEventRoute(val dateEpochDay: Long)

@Serializable
data class EventInfoRoute(val eventId: String)

@Serializable
object EditProfileRoute

@Serializable
object LinkedAccountsRoute

@Serializable
data class ApplianceRoute(val applianceId: String)

@Serializable
data class AddUserToApplianceRoute(val applianceId: String)

@Serializable
data class AddSuperuserToApplianceRoute(val applianceId: String)

@Serializable
object AppliancesRoute

@Serializable
object NewApplianceRoute

@Serializable
data class UserDetailsRoute(val userId: String)

@Serializable
object UsersRoute

@Serializable
object BookingListRoute

@Serializable
object SettingsRoute

@Serializable
object LoginRoute
