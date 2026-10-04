package com.example.minimallauncher.ui.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.minimallauncher.data.search.createContactDialResult
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
    var contactNameHints by remember { mutableStateOf(emptyList<String>()) }
    var candidates by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var contactCandidates by remember { mutableStateOf(emptyList<ContactPhone>()) }
    var contactHomeRequest by remember { mutableLongStateOf(homeRequest) }
    val currentHomeRequest by rememberUpdatedState(homeRequest)
    var contactQuery by remember { mutableStateOf<String?>(null) }
    var contactsPermissionNeeded by remember { mutableStateOf(false) }
    var contactsDenied by remember { mutableStateOf(false) }
    var contactsRevision by remember { mutableIntStateOf(0) }
    fun dial(contact: ContactPhone) {
        val destination = createContactDialResult(contact)
        if (deviceSearch?.open(destination) == true) visible = false
        else feedback = "The phone app isn’t available"
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsDenied = !granted
        contactsPermissionNeeded = !granted
        if (granted) contactsRevision++
        else feedback = "Allow contacts access to find someone to call"
    }
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
                    command is VoiceCommand.CallContact -> {
                        candidates = emptyList()
                        contactCandidates = emptyList()
                        feedback = "Finding contact…"
                        contactHomeRequest = currentHomeRequest
                        contactQuery = command.name
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                            contactsPermissionNeeded = true
                            feedback = "Allow contacts access to find someone to call"
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
                        else feedback = "Try “Open Camera”, “Call John”, or “Wi-Fi”"
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
        contactCandidates = emptyList()
        contactQuery = null
        contactsPermissionNeeded = false
        recognizer.start(currentApps.map { it.label } + contactNameHints)
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
    LaunchedEffect(deviceSearch, isActive, visible, contactsRevision) {
        if (isActive && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try { contactNameHints = deviceSearch?.voiceContactNames().orEmpty() }
            catch (_: SecurityException) { contactNameHints = emptyList() }
            catch (_: android.database.SQLException) { contactNameHints = emptyList() }
        } else contactNameHints = emptyList()
    }
    LaunchedEffect(contactQuery, contactsRevision, visible, isActive, homeRequest) {
        val query = contactQuery
        if (!visible || !isActive || query == null || contactsPermissionNeeded || contactHomeRequest != homeRequest) return@LaunchedEffect
        try {
            val matches = currentSearch?.contactPhones(query).orEmpty()
            if (!visible || !active || contactHomeRequest != currentHomeRequest || contactQuery != query) return@LaunchedEffect
            contactCandidates = matches
            when {
                matches.isEmpty() -> feedback = "No phone number found for “$query”"
                matches.size == 1 && voiceNameScore(query, matches.single().name) >= 90 && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> dial(matches.single())
                matches.size == 1 -> feedback = "Did you mean this contact?"
                else -> feedback = if (matches.map { it.contactId }.distinct().size > 1) "Who would you like to call?" else "Which number?"
            }
        } catch (_: SecurityException) {
            contactsPermissionNeeded = true
            feedback = "Allow contacts access to find someone to call"
        } catch (_: android.database.SQLException) {
            feedback = "Contacts couldn’t be read. Try again"
        }
    }
    LaunchedEffect(isActive, homeRequest) {
        visible = false
        recognizer.cancel()
    }
    LaunchedEffect(visible) {
        if (!visible) {
            recognizer.cancel()
            contactQuery = null
            contactCandidates = emptyList()
        }
    }
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
                Column(Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.9f, stiffness = 400f)).padding(horizontal = 24.dp, vertical = 32.dp),
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
                    if (contactCandidates.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                            contactCandidates.forEach { contact ->
                                TextButton(onClick = { dial(contact) }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(contact.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                                        Text("${contact.label} · ${contact.number}", color = Color.White.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    if (contactsPermissionNeeded) {
                        val openPermissionSettings = contactsDenied &&
                            (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) == false
                        TextButton(onClick = {
                            if (openPermissionSettings) {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                            } else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                        }) { Text(if (openPermissionSettings) "Contact permissions" else "Allow contacts", color = Color.White) }
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
