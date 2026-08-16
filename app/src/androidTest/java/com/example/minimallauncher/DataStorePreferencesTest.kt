package com.example.minimallauncher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minimallauncher.data.preferences.DataStoreLauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStorePreferencesTest {
    @Test
    fun settingsPersistWhenRepositoryIsRecreated() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstRepository = DataStoreLauncherPreferencesRepository(context)
        firstRepository.setUse24HourClock(true)
        firstRepository.setShowDate(false)
        firstRepository.setTheme(ThemePreference.DARK)

        val restored = DataStoreLauncherPreferencesRepository(context).preferences.first()

        assertTrue(restored.use24HourClock)
        assertEquals(false, restored.showDate)
        assertEquals(ThemePreference.DARK, restored.theme)
    }
}
