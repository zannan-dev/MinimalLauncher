package com.example.minimallauncher.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle

/**
 * Observes package changes using the native LauncherApps API. This ensures instant
 * updates across all profiles (including Work Profiles) without needing background services.
 */
class InstalledAppsChangeObserver(
    context: Context,
    private val onAppsChanged: () -> Unit,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private var isRegistered = false

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = onAppsChanged()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = onAppsChanged()
        override fun onPackageChanged(packageName: String, user: UserHandle) = onAppsChanged()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onAppsChanged()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onAppsChanged()
    }

    fun start() {
        if (isRegistered) return
        launcherApps.registerCallback(callback)
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        launcherApps.unregisterCallback(callback)
        isRegistered = false
    }
}
