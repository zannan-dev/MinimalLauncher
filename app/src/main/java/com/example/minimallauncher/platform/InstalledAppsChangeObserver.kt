package com.example.minimallauncher.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.Handler
import android.os.Looper

/**
 * Observes package changes using the native LauncherApps API. This ensures instant
 * updates across all profiles (including Work Profiles) without needing background services.
 */
class InstalledAppsChangeObserver(
    context: Context,
    private val onAppsChanged: () -> Unit,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val mainHandler = Handler(Looper.getMainLooper())
    private val refresh = Runnable { if (isRegistered) onAppsChanged() }
    private var isRegistered = false

    private fun scheduleRefresh() {
        mainHandler.removeCallbacks(refresh)
        mainHandler.postDelayed(refresh, 300L)
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = scheduleRefresh()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = scheduleRefresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = scheduleRefresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = scheduleRefresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = scheduleRefresh()
    }

    fun start() {
        if (isRegistered) return
        launcherApps.registerCallback(callback, mainHandler)
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        mainHandler.removeCallbacks(refresh)
        launcherApps.unregisterCallback(callback)
        isRegistered = false
    }
}
