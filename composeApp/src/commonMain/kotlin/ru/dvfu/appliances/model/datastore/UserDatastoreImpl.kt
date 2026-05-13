package ru.dvfu.appliances.model.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import okio.Path.Companion.toPath
import ru.dvfu.appliances.compose.calendars.CalendarType
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.network.AppJson

class UserDatastoreImpl : UserDatastore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStorePath("userSettings").toPath() },
    )

    private companion object {
        val USER = stringPreferencesKey("USER")
        val CALENDAR_TYPE = stringPreferencesKey("CALENDAR")
        val THEME_MODE = stringPreferencesKey("THEME_MODE")
    }

    override val getCurrentUser: Flow<User> = dataStore.data.map { prefs ->
        prefs[USER]?.let { AppJson.decodeFromString<User>(it) } ?: User()
    }

    override val getCalendarType: Flow<CalendarType> = dataStore.data.map { prefs ->
        CalendarType.valueOf(prefs[CALENDAR_TYPE] ?: CalendarType.MONTH.name)
    }.catch { e ->
        if (e is IllegalArgumentException) emit(CalendarType.MONTH)
    }

    override suspend fun saveCalendarType(calendarType: CalendarType) {
        dataStore.edit { prefs -> prefs[CALENDAR_TYPE] = calendarType.name }
    }

    override suspend fun saveUser(user: User) {
        dataStore.edit { prefs -> prefs[USER] = AppJson.encodeToString(user) }
    }

    override val getThemeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override suspend fun saveThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE] = mode.name }
    }
}
