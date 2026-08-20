package com.example.minimallauncher.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.LauncherAccessibilityService
import com.example.minimallauncher.domain.LaunchableApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("WrongConstant")
private fun expandNotifications(context: Context): Boolean {
    return try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
fun HomeScreen(
    use24HourClock: Boolean,
    showDate: Boolean,
    doubleTapToLock: Boolean,
    isFlowZoneEnabled: Boolean,
    flowZoneState: com.example.minimallauncher.ui.FlowZoneState,
    onToggleFlowZoneTimer: () -> Unit,
    onSkipFlowZonePhase: () -> Unit,
    onResetFlowZone: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var isDefaultLauncher by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(context, lifecycleOwner) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_TIME_TICK ||
                    intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
                    now = LocalDateTime.now()
                }
            }
        }
        val intentFilter = android.content.IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        
        var isRegistered = false
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                context.registerReceiver(receiver, intentFilter)
                isRegistered = true
                now = LocalDateTime.now() // Update immediately when coming to foreground
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // Check if we are the default launcher
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                isDefaultLauncher = resolveInfo?.activityInfo?.packageName == context.packageName
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (isRegistered) {
                    context.unregisterReceiver(receiver)
                    isRegistered = false
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isRegistered) {
                context.unregisterReceiver(receiver)
            }
        }
    }
    var verticalDrag by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onOpenDrawer) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        verticalDrag += dragAmount
                        if (verticalDrag < -72f) {
                            onOpenDrawer()
                            verticalDrag = 0f
                        } else if (verticalDrag > 72f) {
                            expandNotifications(context)
                            verticalDrag = 0f
                        }
                    },
                    onDragEnd = { verticalDrag = 0f },
                )
            }
            .pointerInput(onOpenSettings) {
                detectTapGestures(
                    onLongPress = { onOpenSettings() },
                    onDoubleTap = {
                        if (doubleTapToLock) {
                            if (!LauncherAccessibilityService.lockScreen()) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        }
                    }
                )
            }
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = now.format(
                DateTimeFormatter.ofPattern(if (use24HourClock) "HH:mm" else "h:mm", Locale.getDefault()),
            ),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { contentDescription = "Current time" },
        )
        if (showDate) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE\nd MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        
        BatteryStatus()
        
        Spacer(Modifier.height(32.dp))

        Spacer(Modifier.height(32.dp))

        if (isFlowZoneEnabled) {
            FlowZoneTimer(
                state = flowZoneState,
                onToggleTimer = onToggleFlowZoneTimer,
                onSkipPhase = onSkipFlowZonePhase,
                onReset = onResetFlowZone
            )
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            if (!isDefaultLauncher) {
                androidx.compose.material3.TextButton(onClick = onSetDefaultLauncher) {
                    Text(
                        text = "Set as default launcher",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryStatus() {
    val context = LocalContext.current
    var batteryPct by remember { androidx.compose.runtime.mutableFloatStateOf(-1f) }
    var isCharging by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(context, lifecycleOwner) {
        val intentFilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                batteryPct = level * 100 / scale.toFloat()
                val status: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                val plugged: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1)
                val isPlugged = plugged > 0
                isCharging = isPlugged || status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
            }
        }

        var isRegistered = false

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                val batteryStatus: Intent? = context.registerReceiver(receiver, intentFilter)
                isRegistered = true
                batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    batteryPct = level * 100 / scale.toFloat()
                    val status: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    val plugged: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1)
                    val isPlugged = plugged > 0
                    isCharging = isPlugged || status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (isRegistered) {
                    context.unregisterReceiver(receiver)
                    isRegistered = false
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isRegistered) {
                context.unregisterReceiver(receiver)
            }
        }
    }

    if (batteryPct >= 0) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            if (isCharging) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    modifier = Modifier.size(16.dp).padding(end = 2.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "${batteryPct.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
