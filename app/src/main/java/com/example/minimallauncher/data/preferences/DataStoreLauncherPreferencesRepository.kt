package com.example.minimallauncher.data.preferences

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minimallauncher.domain.toggledFavorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PREFERENCES_FILE_NAME = "launcher_preferences"
private val Context.launcherDataStore by preferencesDataStore(name = PREFERENCES_FILE_NAME)

class DataStoreLauncherPreferencesRepository(context: Context) : LauncherPreferencesRepository {
    private val applicationContext = context.applicationContext

    override val preferences: Flow<LauncherPreferences> = applicationContext.launcherDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            values.toLauncherPreferences(defaultUse24HourClock = DateFormat.is24HourFormat(applicationContext))
        }

    override suspend fun setUse24HourClock(enabled: Boolean) {
        applicationContext.launcherDataStore.edit { values ->
            values[USE_24_HOUR_CLOCK] = enabled
        }
    }

    override suspend fun setShowDate(enabled: Boolean) {
        applicationContext.launcherDataStore.edit { values ->
            values[SHOW_DATE] = enabled
        }
    }

    override suspend fun setAutoOpenKeyboard(enabled: Boolean) {
        applicationContext.launcherDataStore.edit { values ->
            values[AUTO_OPEN_KEYBOARD] = enabled
        }
    }

    override suspend fun setDoubleTapToLock(enabled: Boolean) {
        applicationContext.launcherDataStore.edit { values ->
            values[DOUBLE_TAP_TO_LOCK] = enabled
        }
    }

    override suspend fun setTheme(theme: ThemePreference) {
        applicationContext.launcherDataStore.edit { values ->
            values[THEME] = theme.name
        }
    }

    override suspend fun toggleFavorite(appKey: String) {
        applicationContext.launcherDataStore.edit { values ->
            values[FAVORITE_APP_KEYS] = toggledFavorite(values[FAVORITE_APP_KEYS].orEmpty(), appKey)
        }
    }

    private fun Preferences.toLauncherPreferences(defaultUse24HourClock: Boolean) = LauncherPreferences(
        use24HourClock = this[USE_24_HOUR_CLOCK] ?: defaultUse24HourClock,
        showDate = this[SHOW_DATE] ?: true,
        autoOpenKeyboard = this[AUTO_OPEN_KEYBOARD] ?: true,
        doubleTapToLock = this[DOUBLE_TAP_TO_LOCK] ?: true,
        theme = ThemePreference.fromStorage(this[THEME]),
        favoriteAppKeys = this[FAVORITE_APP_KEYS].orEmpty(),
    )

    private companion object {
        val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        val DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("double_tap_to_lock")
        val THEME = stringPreferencesKey("theme")
        val FAVORITE_APP_KEYS = stringSetPreferencesKey("favorite_app_keys")
    }
}
