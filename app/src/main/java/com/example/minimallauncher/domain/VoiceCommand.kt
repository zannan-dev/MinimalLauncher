package com.example.minimallauncher.domain

import java.util.Locale

sealed interface VoiceCommand {
    data class OpenApp(val name: String) : VoiceCommand
    data class OpenSetting(val name: String) : VoiceCommand
    data class OpenControls(val control: Control) : VoiceCommand
    enum class Control { WIFI, MOBILE_DATA, HOTSPOT, BLUETOOTH }
}

/** Deliberately bounded local commands; no remote model or guessed system changes. */
fun interpretVoiceCommand(transcript: String): VoiceCommand? {
    val words = transcript.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .replace(Regex("\\s+"), " ").trim().removePrefix("please ").removeSuffix(" please")
    if (words.isBlank()) return null
    val target = words.replace(Regex("^(?:can you |could you )?(?:turn on |turn off |switch on |switch off |enable |disable |open |launch |start |show |go to )"), "")
        .removeSuffix(" settings").trim()
    return when (target) {
        "wifi", "wi fi", "wireless", "internet" -> VoiceCommand.OpenControls(VoiceCommand.Control.WIFI)
        "mobile data", "cellular data", "data" -> VoiceCommand.OpenControls(VoiceCommand.Control.MOBILE_DATA)
        "hotspot", "hot spot", "tethering", "personal hotspot" -> VoiceCommand.OpenControls(VoiceCommand.Control.HOTSPOT)
        "bluetooth", "blue tooth" -> VoiceCommand.OpenControls(VoiceCommand.Control.BLUETOOTH)
        "settings", "phone settings" -> VoiceCommand.OpenSetting("Phone settings")
        else -> if (words.startsWith("open ") || words.startsWith("launch ") || words.startsWith("start ")) {
            VoiceCommand.OpenApp(target)
        } else VoiceCommand.OpenSetting(target)
    }
}
