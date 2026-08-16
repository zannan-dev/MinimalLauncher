package com.example.minimallauncher.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Observes package changes only while the activity is alive. This avoids a background service while
 * ensuring the app drawer reflects installs, removals, and updates without a manual restart.
 */
class InstalledAppsChangeObserver(
    private val context: Context,
    private val onAppsChanged: () -> Unit,
) {
    private var isRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onAppsChanged()
        }
    }

    fun start() {
        if (isRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        context.unregisterReceiver(receiver)
        isRegistered = false
    }
}
