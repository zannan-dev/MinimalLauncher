package com.example.minimallauncher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserManager
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.sortApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageManagerApplicationsRepository(context: Context) : ApplicationsRepository {
    private val applicationContext = context.applicationContext
    private val launcherApps =
        applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager =
        applicationContext.getSystemService(Context.USER_SERVICE) as UserManager

    override suspend fun loadApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<LaunchableApp>()

        try {
            for (profile in userManager.userProfiles) {
                // Regular apps
                launcherApps.getActivityList(null, profile).forEach { activityInfo ->
                    val appPackage = activityInfo.applicationInfo.packageName
                    if (appPackage != applicationContext.packageName) {
                        apps.add(
                            LaunchableApp(
                                packageName = appPackage,
                                activityName = activityInfo.componentName.className,
                                label = activityInfo.label?.toString()?.trim().orEmpty().ifBlank { appPackage },
                                userHandle = profile,
                                isPinnedShortcut = false
                            )
                        )
                    }
                }

                // Pinned shortcuts
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && launcherApps.hasShortcutHostPermission()) {
                    val query = LauncherApps.ShortcutQuery().apply {
                        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                    }
                    try {
                        launcherApps.getShortcuts(query, profile)?.forEach { shortcut ->
                            if (shortcut.isPinned) {
                                apps.add(
                                    LaunchableApp(
                                        packageName = shortcut.`package`,
                                        activityName = shortcut.id, // Using id as activityName for shortcuts
                                        label = shortcut.shortLabel?.toString() ?: shortcut.id,
                                        userHandle = profile,
                                        isPinnedShortcut = true
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore exceptions from fetching shortcuts
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore exceptions
        }

        sortApps(apps.distinctBy { it.key })
    }

    override fun launchApp(app: LaunchableApp): Boolean = try {
        if (app.isPinnedShortcut) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launcherApps.startShortcut(app.packageName, app.activityName, null, null, app.userHandle)
                true
            } else {
                false
            }
        } else {
            val component = ComponentName(app.packageName, app.activityName)
            launcherApps.startMainActivity(component, app.userHandle, null, null)
            true
        }
    } catch (_: SecurityException) {
        false
    } catch (_: android.content.ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
