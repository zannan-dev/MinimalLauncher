package com.example.minimallauncher.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.apps.ApplicationsRepository
import com.example.minimallauncher.data.apps.PackageManagerApplicationsRepository
import com.example.minimallauncher.data.preferences.DataStoreLauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.LauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Coordinates the small amount of state shared by the home screen, search, and settings. */
class LauncherViewModel(
    private val applicationsRepository: ApplicationsRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    // Retain Home requests even while the activity is stopped or its UI is being recreated.
    private val _homeRequests = MutableStateFlow(0L)
    val homeRequests: StateFlow<Long> = _homeRequests.asStateFlow()
    private var appRefreshJob: Job? = null
    private var refreshPending = false

    fun refreshAppsOnForeground() {
        if (appRefreshJob?.isActive == true && _uiState.value.isLoadingApps) return
        refreshApps()
    }

    fun onHomePressed() {
        _homeRequests.update { it + 1 }
    }

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                _uiState.update { state -> state.copy(preferences = preferences) }
            }
        }

        refreshApps()
    }

    fun refreshApps() {
        if (appRefreshJob?.isActive == true) {
            refreshPending = true
            return
        }
        appRefreshJob = viewModelScope.launch {
            do {
                refreshPending = false
                _uiState.update { state -> state.copy(isLoadingApps = state.apps.isEmpty(), appLoadError = false) }
                try {
                    val apps = applicationsRepository.loadApps()
                    _uiState.update { state ->
                        state.copy(apps = apps, isLoadingApps = false)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    _uiState.update { state -> state.copy(isLoadingApps = false, appLoadError = true) }
                }
            } while (refreshPending)
        }
    }

    fun launchApp(app: LaunchableApp): Boolean = applicationsRepository.launchApp(app)

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(app.key)
        }
    }

    fun removeFavorite(app: LaunchableApp) {
        viewModelScope.launch { preferencesRepository.removeFavorite(app.key) }
    }

    fun moveFavorite(from: LaunchableApp, to: LaunchableApp) {
        viewModelScope.launch { preferencesRepository.moveFavorite(from.key, to.key) }
    }

    fun toggleIntentionalPilotApp(app: LaunchableApp) {
        viewModelScope.launch {
            preferencesRepository.toggleIntentionalPilotApp(app.key)
        }
    }

    fun setShowDate(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowDate(enabled)
        }
    }

    fun setAutoOpenKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoOpenKeyboard(enabled)
        }
    }

    fun setDoubleTapToLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDoubleTapToLock(enabled)
        }
    }

    fun setShowStatusBar(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowStatusBar(enabled)
        }
    }

    fun setIntentionalPilotEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIntentionalPilotEnabled(enabled)
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }
}

class LauncherViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return LauncherViewModel(
            applicationsRepository = PackageManagerApplicationsRepository(applicationContext),
            preferencesRepository = DataStoreLauncherPreferencesRepository(applicationContext),
        ) as T
    }
}
