package ru.dvfu.appliances.model.datastore

import kotlinx.coroutines.flow.Flow
import ru.dvfu.appliances.compose.calendars.CalendarType
import ru.dvfu.appliances.model.repository.entity.User

interface UserDatastore {

    val getCalendarType: Flow<CalendarType>
    suspend fun saveCalendarType(calendarType: CalendarType)

    val getCurrentUser: Flow<User>
    suspend fun saveUser(user: User)

    val getThemeMode: Flow<ThemeMode>
    suspend fun saveThemeMode(mode: ThemeMode)

    val getIosAppPromotionDismissed: Flow<Boolean>
    suspend fun saveIosAppPromotionDismissed(dismissed: Boolean)
}
