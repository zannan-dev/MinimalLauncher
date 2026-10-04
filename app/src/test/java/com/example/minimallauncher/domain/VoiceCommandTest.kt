package com.example.minimallauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {
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
    @Test fun callsKeepNamesAndTakePrecedenceOverAppsAndSettings() {
        assertEquals(VoiceCommand.CallContact("john smith"), interpretVoiceCommand("Please call John Smith."))
        assertEquals(VoiceCommand.CallContact("mum"), interpretVoiceCommand("Could you call Mum please"))
        assertEquals(VoiceCommand.CallContact("camera"), interpretVoiceCommand("Call Camera"))
        assertEquals(VoiceCommand.CallContact("office settings"), interpretVoiceCommand("Dial Office Settings"))
        assertNull(interpretVoiceCommand("call"))
        assertNull(interpretVoiceCommand("Can you dial please"))
    }
    @Test fun emptySpeechHasNoAction() {
        assertNull(interpretVoiceCommand(" ... "))
        assertNull(interpretVoiceCommand(""))
    }
}
