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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Coordinates the small amount of state shared by the home screen, drawer, and settings. */
class LauncherViewModel(
    private val applicationsRepository: ApplicationsRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                _uiState.update { state -> state.copy(preferences = preferences) }
            }
        }
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoadingApps = true, appLoadError = false) }
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
        }
    }

    fun launchApp(app: LaunchableApp): Boolean = applicationsRepository.launchApp(app)

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(app.key)
        }
    }

    fun setUse24HourClock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUse24HourClock(enabled)
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
