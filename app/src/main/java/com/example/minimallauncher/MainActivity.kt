package com.example.minimallauncher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.minimallauncher.platform.InstalledAppsChangeObserver
import com.example.minimallauncher.ui.LauncherApp
import com.example.minimallauncher.ui.LauncherViewModel
import com.example.minimallauncher.ui.LauncherViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModelFactory(applicationContext)
    }
    private lateinit var appsChangeObserver: InstalledAppsChangeObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appsChangeObserver = InstalledAppsChangeObserver(this, viewModel::refreshApps)
        setContent {
            LauncherApp(
                viewModel = viewModel,
                onOpenDefaultLauncherSettings = ::openDefaultLauncherSettings,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        appsChangeObserver.start()
    }

    override fun onStop() {
        appsChangeObserver.stop()
        super.onStop()
    }

    private fun openDefaultLauncherSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        runCatching { startActivity(intent) }
            .getOrElse { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomePressed()
        }
    }
}
