package com.example.minimallauncher.ui.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import com.example.minimallauncher.ui.motion.LauncherMotion
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.minimallauncher.data.search.DeviceSearchRepository
import com.example.minimallauncher.LauncherAccessibilityService
import com.example.minimallauncher.data.voice.OfflineVoiceRecognizer
import com.example.minimallauncher.domain.*

@Composable
fun HomeAssistant(
    apps: List<LaunchableApp>,
    deviceSearch: DeviceSearchRepository?,
    onLaunchApp: (LaunchableApp) -> Unit,
    isActive: Boolean,
    homeRequest: Long,
    expansionState: MutableState<Boolean>? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val expansion = expansionState ?: remember { mutableStateOf(false) }
    var visible by expansion
    var transcript by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var permissionDenied by remember { mutableStateOf(false) }
    var needsLockAccessibility by remember { mutableStateOf(false) }
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
                val appMatches = if (command is VoiceCommand.OpenApp || command is VoiceCommand.OpenSetting) matchVoiceApps(currentApps, target) else emptyList()
                val certainApp = appMatches.singleOrNull()?.takeIf { voiceNameScore(target, it.label) >= if (command is VoiceCommand.OpenApp) 90 else 100 }
                when {
                    command == null -> feedback = "Try “Open Camera” or “Wi-Fi”"
                    command is VoiceCommand.LockScreen -> {
                        candidates = emptyList()
                        if (LauncherAccessibilityService.lockScreen()) {
                            visible = false
                        } else {
                            needsLockAccessibility = LauncherAccessibilityService.instance == null
                            feedback = if (needsLockAccessibility) "Enable Minimal Launcher in Accessibility to lock the screen"
                                else "Couldn’t lock the screen. Try again"
                        }
                    }
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
                    certainApp != null -> { visible = false; currentLaunch(certainApp) }
                    appMatches.isNotEmpty() && command is VoiceCommand.OpenApp -> { candidates = appMatches.take(5); feedback = if (appMatches.size == 1) "Did you mean this app?" else "Which app?" }
                    else -> {
                        val setting = currentSearch?.settings(target)?.firstOrNull()
                        if (setting != null && currentSearch?.open(setting) == true) visible = false
                        else feedback = "Try “Open Camera” or “Wi-Fi”"
                    }
                }
            }
        }
    }
    val state by recognizer.state.collectAsState()
    fun listen() {
        transcript = ""
        feedback = ""
        needsLockAccessibility = false
        candidates = emptyList()
        recognizer.start(currentApps.map { it.label })
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
    LaunchedEffect(visible) {
        if (!visible) recognizer.cancel()
    }
    DisposableEffect(recognizer, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { recognizer.cancel(); visible = false }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); recognizer.close() }
    }
    BackHandler(enabled = visible) { visible = false }
    val orbSize by animateDpAsState(if (visible) 144.dp else 96.dp,
        spring(dampingRatio = 0.82f, stiffness = 400f), label = "Assistant unfolds")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val detailsHeight = (maxHeight - orbSize).coerceAtLeast(0.dp)
        Column(Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(visible = visible,
                enter = fadeIn(LauncherMotion.fade()) + expandVertically(LauncherMotion.settle(), expandFrom = Alignment.Bottom),
                exit = fadeOut(LauncherMotion.fade()) + shrinkVertically(LauncherMotion.settle(), shrinkTowards = Alignment.Bottom)) {
                Column(Modifier.fillMaxWidth().heightIn(max = detailsHeight).animateContentSize(LauncherMotion.settle()).verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Crossfade(targetState = feedback.ifBlank { state.message }, modifier = Modifier.fillMaxWidth(),
                        animationSpec = tween(180), label = "Assistant status") { message ->
                        Text(message, modifier = Modifier.fillMaxWidth().testTag("Assistant status")
                            .pointerInput(Unit) { detectTapGestures(onTap = {}) }, style = MaterialTheme.typography.bodyLarge,
                            color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    if (transcript.isNotBlank() && feedback.isNotBlank()) {
                        Text(transcript, modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = {}) }, color = Color.White.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    candidates.forEach { app ->
                        TextButton(enabled = visible && isActive, onClick = { visible = false; currentLaunch(app) }) { Text(app.label, color = Color.White) }
                    }
                    if (state.needsModel && Build.VERSION.SDK_INT >= 33) {
                        TextButton(enabled = visible && isActive, onClick = recognizer::downloadModel) { Text("Download offline speech", color = Color.White) }
                    }
                    if (permissionDenied) {
                        TextButton(enabled = visible && isActive, onClick = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        }) { Text("Microphone permission", color = Color.White) }
                    }
                    if (needsLockAccessibility) {
                        TextButton(enabled = visible && isActive, onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }) { Text("Accessibility settings", color = Color.White) }
                    }
                }
            }
            AssistantOrb(
                onClick = {
                    if (!visible) { visible = true; start() }
                    else if (state.busy) recognizer.cancel() else start()
                },
                description = if (!visible) "Voice assistant" else if (state.busy) "Stop listening" else "Speak again",
                diameter = orbSize, listening = visible && state.listening, level = state.level, enabled = isActive,
            )
        }
    }
}
