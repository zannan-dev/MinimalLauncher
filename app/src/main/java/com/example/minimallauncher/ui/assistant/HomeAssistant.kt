package com.example.minimallauncher.ui.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.minimallauncher.data.search.DeviceSearchRepository
import com.example.minimallauncher.data.voice.OfflineVoiceRecognizer
import com.example.minimallauncher.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAssistant(
    apps: List<LaunchableApp>,
    deviceSearch: DeviceSearchRepository?,
    onLaunchApp: (LaunchableApp) -> Unit,
    isActive: Boolean,
    homeRequest: Long,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var permissionDenied by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    val currentApps by rememberUpdatedState(apps)
    val currentSearch by rememberUpdatedState(deviceSearch)
    val currentLaunch by rememberUpdatedState(onLaunchApp)
    val active by rememberUpdatedState(isActive)
    val recognizer = remember(context) {
        OfflineVoiceRecognizer(context) { spoken ->
            if (visible && active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                transcript = spoken
                val command = interpretVoiceCommand(spoken)
                val target = when (command) {
                    is VoiceCommand.OpenApp -> command.name
                    is VoiceCommand.OpenSetting -> command.name
                    else -> ""
                }
                val exactApps = currentApps.filter { it.label.equals(target, ignoreCase = true) }
                val appMatches = if (exactApps.isNotEmpty()) exactApps else if (command is VoiceCommand.OpenApp) filterApps(currentApps, target) else emptyList()
                when {
                    command is VoiceCommand.OpenControls -> {
                        val destination = when (command.control) {
                            VoiceCommand.Control.WIFI -> DeviceSearchResult("voice:wifi", "Wi-Fi controls", "", if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS)
                            VoiceCommand.Control.MOBILE_DATA -> DeviceSearchResult("voice:data", "Internet controls", "", if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_DATA_ROAMING_SETTINGS)
                            VoiceCommand.Control.HOTSPOT -> DeviceSearchResult("voice:hotspot", "Hotspot controls", "", "android.settings.TETHER_SETTINGS")
                            VoiceCommand.Control.BLUETOOTH -> DeviceSearchResult("voice:bluetooth", "Bluetooth controls", "", Settings.ACTION_BLUETOOTH_SETTINGS)
                        }
                        val opened = currentSearch?.open(destination) == true || (command.control == VoiceCommand.Control.HOTSPOT && currentSearch?.open(destination.copy(action = Settings.ACTION_WIRELESS_SETTINGS)) == true)
                        if (opened) {
                            visible = false
                        } else feedback = "These controls aren’t available"
                    }
                    appMatches.size == 1 -> { visible = false; currentLaunch(appMatches.single()) }
                    appMatches.size > 1 -> { candidates = appMatches.take(5); feedback = "Which app?" }
                    else -> {
                        val setting = currentSearch?.settings(target)?.firstOrNull()
                        if (setting != null && currentSearch?.open(setting) == true) visible = false
                        else feedback = "Try “Open Camera”, “Wi-Fi”, or “Screen timeout”"
                    }
                }
            }
        }
    }
    val state by recognizer.state.collectAsState()
    fun listen() {
        transcript = ""
        feedback = ""
        candidates = emptyList()
        recognizer.start()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted && visible && active) listen()
        else if (!granted) feedback = "Allow microphone access to speak"
    }
    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            permissionDenied = false
            listen()
        } else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(isActive, homeRequest) {
        visible = false
        recognizer.cancel()
    }
    LaunchedEffect(visible) { if (!visible) recognizer.cancel() }
    DisposableEffect(recognizer, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { recognizer.cancel(); visible = false }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); recognizer.close() }
    }
    AssistantOrb(
        onClick = { visible = true; start() },
        description = "Voice assistant",
        enabled = isActive,
    )
    if (visible) {
        ModalBottomSheet(onDismissRequest = { visible = false }, dragHandle = null,
            containerColor = Color.Black, contentColor = Color.White) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = { visible = false }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Outlined.Close, "Close assistant", tint = Color.White.copy(alpha = 0.6f))
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AssistantOrb(
                        onClick = { if (state.busy) recognizer.cancel() else start() },
                        description = if (state.busy) "Stop listening" else "Speak again",
                        diameter = 144.dp,
                        listening = state.listening,
                        level = state.level,
                    )
                    Text(feedback.ifBlank { state.message }, style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (transcript.isNotBlank() && feedback.isNotBlank()) {
                        Text(transcript, color = Color.White.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    candidates.forEach { app ->
                        TextButton(onClick = { visible = false; currentLaunch(app) }) { Text(app.label, color = Color.White) }
                    }
                    if (state.needsModel && Build.VERSION.SDK_INT >= 33) {
                        TextButton(onClick = recognizer::downloadModel) { Text("Download offline speech", color = Color.White) }
                    }
                    if (permissionDenied) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        }) { Text("Microphone permission", color = Color.White) }
                    }
                }
            }
        }
    }
}
