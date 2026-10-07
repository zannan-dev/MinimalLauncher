package com.example.minimallauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {
    @Test fun screenLockCommandsAreRecognized() {
        listOf("Lock the screen", "lock screen", "Please lock the screen.",
            "Can you lock the screen please", "Could you please lock the phone?", "Lock device")
            .forEach { assertEquals(VoiceCommand.LockScreen, interpretVoiceCommand(it)) }
    }
    @Test fun lockSettingsAndAppNamesRemainSearchable() {
        assertEquals(VoiceCommand.OpenSetting("lock screen"), interpretVoiceCommand("Lock screen settings"))
        assertEquals(VoiceCommand.OpenApp("lock screen"), interpretVoiceCommand("Open Lock Screen"))
        assertEquals(VoiceCommand.OpenSetting("do not lock the screen"), interpretVoiceCommand("Do not lock the screen"))
    }
    @Test fun appCommandsKeepMultiwordNames() {
        assertEquals(VoiceCommand.OpenApp("google maps"), interpretVoiceCommand("Please open Google Maps."))
        assertEquals(VoiceCommand.OpenApp("camera"), interpretVoiceCommand("Launch Camera please"))
        assertEquals(VoiceCommand.OpenApp("phone"), interpretVoiceCommand("Open Phone"))
    }
    @Test fun restrictedTogglesBecomeSystemControls() {
        assertEquals(VoiceCommand.OpenControls(VoiceCommand.Control.WIFI), interpretVoiceCommand("Turn on Wi-Fi"))
        assertEquals(VoiceCommand.OpenControls(VoiceCommand.Control.WIFI), interpretVoiceCommand("Switch off wi fi"))
        assertEquals(VoiceCommand.OpenControls(VoiceCommand.Control.MOBILE_DATA), interpretVoiceCommand("Enable mobile data"))
        assertEquals(VoiceCommand.OpenControls(VoiceCommand.Control.HOTSPOT), interpretVoiceCommand("Turn off hot spot"))
        assertEquals(VoiceCommand.OpenControls(VoiceCommand.Control.BLUETOOTH), interpretVoiceCommand("Bluetooth settings"))
    }
    @Test fun settingNamesRemainSearchable() {
        assertEquals(VoiceCommand.OpenSetting("screen timeout"), interpretVoiceCommand("Show screen timeout"))
        assertEquals(VoiceCommand.OpenSetting("Phone settings"), interpretVoiceCommand("Open settings"))
        assertEquals(VoiceCommand.OpenSetting("build number"), interpretVoiceCommand("Build number"))
    }
    @Test fun callingCommandsHaveNoAction() {
        assertNull(interpretVoiceCommand("Please call John Smith."))
        assertNull(interpretVoiceCommand("Could you call Mum please"))
        assertNull(interpretVoiceCommand("Call Camera"))
        assertNull(interpretVoiceCommand("Dial Office Settings"))
        assertNull(interpretVoiceCommand("call"))
        assertNull(interpretVoiceCommand("Can you dial please"))
    }
    @Test fun emptySpeechHasNoAction() {
        assertNull(interpretVoiceCommand(" ... "))
        assertNull(interpretVoiceCommand(""))
    }
}
