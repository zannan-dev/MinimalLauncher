package com.example.minimallauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilteringTest {
    private val camera = LaunchableApp("camera", "CameraActivity", "Camera")
    private val phone = LaunchableApp("phone", "PhoneActivity", "Phone")
    private val messages = LaunchableApp("messages", "MessagesActivity", "Messages")

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
}
