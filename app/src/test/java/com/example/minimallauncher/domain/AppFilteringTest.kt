package com.example.minimallauncher.domain

import android.os.UserHandle
import org.mockito.Mockito.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilteringTest {
    private val userHandle = mock(UserHandle::class.java)
    private val camera = LaunchableApp("camera", "CameraActivity", "Camera", userHandle, false)
    private val phone = LaunchableApp("phone", "PhoneActivity", "Phone", userHandle, false)
    private val messages = LaunchableApp("messages", "MessagesActivity", "Messages", userHandle, false)

    @Test
    fun `sortApps orders labels without case sensitivity`() {
        val apps = listOf(phone, camera.copy(label = "camera"), messages)

        assertEquals(listOf("camera", "Messages", "Phone"), sortApps(apps).map { it.label })
    }

    @Test
    fun `filterApps ignores case and surrounding spaces`() {
        val apps = listOf(camera, phone, messages)

        assertEquals(listOf(camera), filterApps(apps, "  CAM  "))
        assertEquals(apps, filterApps(apps, ""))
    }

    @Test
    fun `toggledFavorite adds then removes the app key`() {
        val added = toggledFavorite(emptySet(), camera.key)

        assertTrue(camera.key in added)
        assertFalse(camera.key in toggledFavorite(added, camera.key))
    }

    @Test
    fun `favorite order retains existing keys and moves one favorite`() {
        val favorites = setOf(camera.key, phone.key, messages.key)
        val order = orderedFavoriteKeys(favorites, listOf(phone.key, camera.key, "removed"))

        assertEquals(listOf(phone.key, camera.key, messages.key), order)
        assertEquals(
            listOf(messages.key, phone.key, camera.key),
            movedFavorite(order, messages.key, phone.key),
        )
        assertEquals(
            listOf(camera.key, messages.key, phone.key),
            movedFavorite(order, phone.key, messages.key),
        )
    }
}
