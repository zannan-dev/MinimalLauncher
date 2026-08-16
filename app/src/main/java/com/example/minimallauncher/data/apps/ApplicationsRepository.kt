package com.example.minimallauncher.data.apps

import com.example.minimallauncher.domain.LaunchableApp

interface ApplicationsRepository {
    /** Loads all activities that the user can start from a launcher. */
    suspend fun loadApps(): List<LaunchableApp>

    /** Returns false if the target activity is no longer available. */
    fun launchApp(app: LaunchableApp): Boolean
}
