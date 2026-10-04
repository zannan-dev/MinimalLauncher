package com.example.minimallauncher

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import com.example.minimallauncher.data.search.AndroidDeviceSearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minimallauncher.data.search.createContactDialResult
import com.example.minimallauncher.data.search.createDeviceSearchIntent
import com.example.minimallauncher.domain.ContactPhone
import org.junit.Assert.*
import org.junit.Test

class ContactCallingTest {
    @Test fun missingContactDoesNotProduceADialTarget() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
        val matches = AndroidDeviceSearchRepository(context).contactPhones("zzqxv938174")
        assertTrue(matches.isEmpty())
    }

    @Test fun contactNumberOpensResolvableDialerWithoutPlacingCall() {
        val number = "+1 (555) 0100;123#"
        val intent = createDeviceSearchIntent(createContactDialResult(ContactPhone(1, "Test contact", number, "Work")))
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel", intent.data?.scheme)
        assertEquals(number, intent.data?.schemeSpecificPart)
        assertNull(intent.data?.fragment)
        assertNotNull(intent.resolveActivity(InstrumentationRegistry.getInstrumentation().targetContext.packageManager))
        assertFalse(intent.hasExtra(":settings:show_fragment_args"))
        // Deliberately do not start the intent or place a call during automated tests.
    }
}
