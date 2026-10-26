package com.example.minimallauncher.data.preferences

import com.example.minimallauncher.domain.RecentSearchResult
import com.example.minimallauncher.domain.withRecentResult
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minimallauncher.domain.toggledHomeFavorite
import com.example.minimallauncher.domain.orderedFavoriteKeys
import com.example.minimallauncher.domain.movedFavorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import org.json.JSONArray

private const val PREFERENCES_FILE_NAME = "launcher_preferences"
private val Context.launcherDataStore by preferencesDataStore(name = PREFERENCES_FILE_NAME)

class DataStoreLauncherPreferencesRepository(context: Context) : LauncherPreferencesRepository {
    private val applicationContext = context.applicationContext

    override val preferences: Flow<LauncherPreferences> = applicationContext.launcherDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            values.toLauncherPreferences()
        }

    override suspend fun recordRecentSearchResult(result: RecentSearchResult) {
        applicationContext.launcherDataStore.edit { values ->
            val updated = decodeRecentSearchResults(values[RECENT_SEARCH_RESULTS]).withRecentResult(result)
            values[RECENT_SEARCH_RESULTS] = encodeRecentSearchResults(updated)
        }
    }

    override suspend fun clearRecentSearchResults() {
        applicationContext.launcherDataStore.edit { it.remove(RECENT_SEARCH_RESULTS) }
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

    override suspend fun setTheme(theme: ThemePreference) {
        applicationContext.launcherDataStore.edit { values ->
            values[THEME] = theme.name
        }
    }

    override suspend fun toggleFavorite(appKey: String) {
        applicationContext.launcherDataStore.edit { values ->
            val favorites = values[FAVORITE_APP_KEYS].orEmpty()
            val updated = toggledHomeFavorite(favorites, appKey)
            if (updated == favorites) return@edit
            val order = orderedFavoriteKeys(favorites, decodeFavoriteOrder(values[FAVORITE_APP_ORDER]))
            values[FAVORITE_APP_KEYS] = updated
            values[FAVORITE_APP_ORDER] = encodeFavoriteOrder(
                if (appKey in updated) order + appKey else order - appKey
            )
        }
    }

    override suspend fun removeFavorite(appKey: String) {
        applicationContext.launcherDataStore.edit { values ->
            val favorites = values[FAVORITE_APP_KEYS].orEmpty()
            if (appKey in favorites) {
                values[FAVORITE_APP_KEYS] = favorites - appKey
                val order = orderedFavoriteKeys(favorites, decodeFavoriteOrder(values[FAVORITE_APP_ORDER]))
                values[FAVORITE_APP_ORDER] = encodeFavoriteOrder(order - appKey)
            }
        }
    }

    override suspend fun moveFavorite(fromKey: String, toKey: String) {
        applicationContext.launcherDataStore.edit { values ->
            val order = orderedFavoriteKeys(
                values[FAVORITE_APP_KEYS].orEmpty(),
                decodeFavoriteOrder(values[FAVORITE_APP_ORDER]),
            )
            values[FAVORITE_APP_ORDER] = encodeFavoriteOrder(movedFavorite(order, fromKey, toKey))
        }
    }

    private fun Preferences.toLauncherPreferences() = LauncherPreferences(
        recentSearchResults = decodeRecentSearchResults(this[RECENT_SEARCH_RESULTS]),
        showDate = this[SHOW_DATE] ?: true,
        autoOpenKeyboard = this[AUTO_OPEN_KEYBOARD] ?: true,
        theme = ThemePreference.fromStorage(this[THEME]),
        favoriteAppKeys = this[FAVORITE_APP_KEYS].orEmpty(),
        favoriteAppOrder = orderedFavoriteKeys(
            this[FAVORITE_APP_KEYS].orEmpty(), decodeFavoriteOrder(this[FAVORITE_APP_ORDER]),
        ),
    )

    private companion object {
        val RECENT_SEARCH_RESULTS = stringPreferencesKey("recent_search_results")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        val THEME = stringPreferencesKey("theme")
        val FAVORITE_APP_KEYS = stringSetPreferencesKey("favorite_app_keys")
        val FAVORITE_APP_ORDER = stringPreferencesKey("favorite_app_order")
    }

    private fun decodeFavoriteOrder(saved: String?): List<String> = try {
        if (saved == null) emptyList() else JSONArray(saved).let { array ->
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf(String::isNotEmpty) }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun encodeFavoriteOrder(order: List<String>): String = JSONArray(order).toString()
}
