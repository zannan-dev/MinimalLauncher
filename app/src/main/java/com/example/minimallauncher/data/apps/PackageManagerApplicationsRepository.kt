package com.example.minimallauncher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.sortApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android platform implementation that performs package-manager work off the main thread. */
class PackageManagerApplicationsRepository(context: Context) : ApplicationsRepository {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager

    override suspend fun loadApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = queryLaunchableActivities(launcherIntent)
            .mapNotNull { resolveInfo -> resolveInfo.toLaunchableApp() }
            .filterNot { app -> app.packageName == applicationContext.packageName }
            .distinctBy { app -> app.key }

        sortApps(apps)
    }

    override fun launchApp(app: LaunchableApp): Boolean = try {
        val intent = Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(intent)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: android.content.ActivityNotFoundException) {
        false
    }

    private fun ResolveInfo.toLaunchableApp(): LaunchableApp? {
        val activityInfo = activityInfo ?: return null
        val label = loadLabel(packageManager)?.toString()?.trim().orEmpty()
        return LaunchableApp(
            packageName = activityInfo.packageName,
            activityName = activityInfo.name,
            label = label.ifBlank { activityInfo.packageName },
        )
    }

    private fun queryLaunchableActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            // This overload is required for Android 12 and lower, where the typed flags API does not exist.
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}
