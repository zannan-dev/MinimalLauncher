package com.example.minimallauncher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minimallauncher.data.preferences.DataStoreLauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStorePreferencesTest {
    @Test
    fun settingsPersistWhenRepositoryIsRecreated() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstRepository = DataStoreLauncherPreferencesRepository(context)
        val original = firstRepository.preferences.first()
        try {
            firstRepository.setShowDate(false)
            firstRepository.setTheme(ThemePreference.DARK)

            val restored = DataStoreLauncherPreferencesRepository(context).preferences.first()

            assertEquals(false, restored.showDate)
            assertEquals(ThemePreference.DARK, restored.theme)
        } finally {
            firstRepository.setShowDate(original.showDate)
            firstRepository.setTheme(original.theme)
        }
    }

    @Test
    fun favoriteOrderPersistsAcrossRepositoryInstances() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DataStoreLauncherPreferencesRepository(context)
        val first = "test_favorite_first"
        val second = "test_favorite_second"
        repository.removeFavorite(first)
        repository.removeFavorite(second)
        try {
            repository.toggleFavorite(first)
            repository.toggleFavorite(second)
            repository.moveFavorite(second, first)

            val restored = DataStoreLauncherPreferencesRepository(context).preferences.first()
            assertEquals(listOf(second, first), restored.favoriteAppOrder.filter { it == first || it == second })
        } finally {
            repository.removeFavorite(first)
            repository.removeFavorite(second)
        }
    }
}
