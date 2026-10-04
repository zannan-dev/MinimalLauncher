package com.example.minimallauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.minimallauncher.ui.motion.LauncherMotion
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.LauncherAccessibilityService
import com.example.minimallauncher.domain.LaunchableApp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    favoriteApps: List<LaunchableApp>,
    showDate: Boolean,
    doubleTapToLock: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onMoveFavorite: (LaunchableApp, LaunchableApp) -> Unit,
    onRemoveFavorite: (LaunchableApp) -> Unit,
    assistantContent: @Composable () -> Unit = {},
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var isDefaultLauncher by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var use24HourClock by remember(context) { mutableStateOf(DateFormat.is24HourFormat(context)) }
    val locale = Locale.getDefault()
    val timeFormatter = remember(use24HourClock, locale) {
        DateTimeFormatter.ofPattern(if (use24HourClock) "HH:mm" else "h:mm a", locale)
    }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }
    val dateText = remember(now.toLocalDate(), locale) {
        if (locale.language == "en") {
            val day = now.dayOfMonth
            val suffix = if (day in 11..13) "th" else when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
            now.format(DateTimeFormatter.ofPattern("EEEE, ", locale)) +
                "$day$suffix " + now.format(DateTimeFormatter.ofPattern("MMMM", locale))
        } else {
            now.format(dateFormatter)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(context, lifecycleOwner) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_TIME_TICK ||
                    intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
                    now = LocalDateTime.now()
                    use24HourClock = DateFormat.is24HourFormat(context)
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
                use24HourClock = DateFormat.is24HourFormat(context)
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                use24HourClock = DateFormat.is24HourFormat(context)
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(onOpenNotifications) {
                var drag = 0f
                var handled = false
                val threshold = 72.dp.toPx()
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount ->
                        drag += amount
                        if (!handled && drag > threshold) {
                            handled = true
                            onOpenNotifications()
                        }
                    },
                    onDragEnd = { drag = 0f; handled = false },
                    onDragCancel = { drag = 0f; handled = false },
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
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = now.format(timeFormatter),
                fontSize = 25.sp,
                lineHeight = 32.sp,
                color = Color.White,
                modifier = Modifier.semantics { contentDescription = "Current time" },
            )
            AnimatedVisibility(
                visible = showDate,
                enter = expandVertically(LauncherMotion.settle()) + fadeIn(LauncherMotion.fade()),
                exit = shrinkVertically(LauncherMotion.settle()) + fadeOut(LauncherMotion.fade()),
            ) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dateText,
                        fontSize = 13.sp,
                        color = Color.White,
                    )
                }
            }
            BatteryStatus()
            if (!isDefaultLauncher) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Set as default launcher",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onSetDefaultLauncher),
                )
            }
        }
        HomeFavorites(
            apps = favoriteApps,
            onLaunchApp = onLaunchApp,
            onMoveFavorite = onMoveFavorite,
            onRemoveFavorite = onRemoveFavorite,
            modifier = Modifier.fillMaxSize().padding(top = maxHeight * 0.44f),
        )
        androidx.compose.foundation.layout.Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            assistantContent()
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
                if (level >= 0 && scale > 0) batteryPct = level * 100 / scale.toFloat()
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
                    if (level >= 0 && scale > 0) batteryPct = level * 100 / scale.toFloat()
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
        Text(
            text = (if (isCharging) "+" else "") + "${batteryPct.toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
