package com.example.minimallauncher

import androidx.test.platform.app.InstrumentationRegistry
import com.example.minimallauncher.data.apps.PackageManagerApplicationsRepository
import com.example.minimallauncher.domain.matchVoiceApps
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in checks against names supplied by the device owner; never launches apps. */
class AssistantDeviceMatchingTest {
    @Test fun requestedAppsAreFoundOnDevice() = runBlocking {
        val names = InstrumentationRegistry.getArguments().getString("assistantApps")
        assumeTrue(!names.isNullOrBlank())
        val apps = PackageManagerApplicationsRepository(InstrumentationRegistry.getInstrumentation().targetContext).loadApps()
        names!!.split("|").forEach { name ->
            val matches = matchVoiceApps(apps, name)
            println("Assistant app lookup: $name, matches=${matches.size}")
            assertTrue("Requested app was not found: $name", matches.isNotEmpty())
        }
    }
}
