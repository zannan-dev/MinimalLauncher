package com.example.minimallauncher.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.apps.ApplicationsRepository
import com.example.minimallauncher.data.apps.PackageManagerApplicationsRepository
import com.example.minimallauncher.data.preferences.DataStoreLauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.LauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Coordinates the small amount of state shared by the home screen, drawer, and settings. */
class LauncherViewModel(
    private val applicationContext: Context,
    private val applicationsRepository: ApplicationsRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _homeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeEvents: SharedFlow<Unit> = _homeEvents.asSharedFlow()

    fun onHomePressed() {
        _homeEvents.tryEmit(Unit)
    }

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                _uiState.update { state -> 
                    var newFState = state.flowZoneState
                    val isAtStart = newFState.remainingSeconds == newFState.totalSeconds
                    
                    if (!preferences.isFlowZoneEnabled && newFState.isRunning) {
                        newFState = newFState.copy(isRunning = false)
                    }

                    if (isAtStart) {
                         if (newFState.phase == FlowZonePhase.FOCUS) {
                             newFState = newFState.copy(
                                 remainingSeconds = preferences.flowZoneFocusMinutes * 60L,
                                 totalSeconds = preferences.flowZoneFocusMinutes * 60L
                             )
                         } else if (newFState.phase == FlowZonePhase.BREAK) {
                             newFState = newFState.copy(
                                 remainingSeconds = preferences.flowZoneBreakMinutes * 60L,
                                 totalSeconds = preferences.flowZoneBreakMinutes * 60L
                             )
                         } else if (newFState.phase == FlowZonePhase.LONG_BREAK) {
                             newFState = newFState.copy(
                                 remainingSeconds = preferences.flowZoneLongBreakMinutes * 60L,
                                 totalSeconds = preferences.flowZoneLongBreakMinutes * 60L
                             )
                         }
                    }
                    
                    state.copy(preferences = preferences, flowZoneState = newFState) 
                }
            }
        }
        
        viewModelScope.launch {
            _uiState
                .map { it.preferences.isFlowZoneEnabled && it.flowZoneState.isRunning }
                .distinctUntilChanged()
                .collectLatest { shouldRun ->
                    if (shouldRun) {
                        while (true) {
                            delay(1000L)
                            val current = _uiState.value.flowZoneState
                            if (current.remainingSeconds > 0) {
                                _uiState.update { it.copy(flowZoneState = current.copy(remainingSeconds = current.remainingSeconds - 1)) }
                            } else {
                                transitionFlowZonePhase()
                            }
                        }
                    }
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

    private val motivationalMessages = listOf(
        "You completed a Flow! Recharge your energy.",
        "Great focus! Take a breather.",
        "Level up! You've earned this break.",
        "Deep work complete. Step away from the screen.",
        "Flow achieved. Time to relax.",
        "Your future self thanks you. Rest now.",
        "Awesome job staying focused! Enjoy your break.",
        "Mission accomplished. Recharge your batteries.",
        "Focus session done. Breathe and stretch.",
        "You're in the zone! Take a well-deserved pause."
    )

    private fun playNotificationSounds(count: Int) {
        viewModelScope.launch {
            try {
                for (i in 1..count) {
                    val player = MediaPlayer.create(applicationContext, R.raw.bell)
                    player.setOnCompletionListener { it.release() }
                    player.start()
                    if (i < count) delay(1000L) // wait before playing next
                }
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    private fun transitionFlowZonePhase() {
        val prefs = _uiState.value.preferences
        _uiState.update { state ->
            val fState = state.flowZoneState
            when (fState.phase) {
                FlowZonePhase.FOCUS -> {
                    val nextCycles = fState.completedCycles + 1
                    if (nextCycles % 4 == 0) {
                        playNotificationSounds(3)
                        state.copy(flowZoneState = fState.copy(
                            phase = FlowZonePhase.LONG_BREAK,
                            remainingSeconds = prefs.flowZoneLongBreakMinutes * 60L,
                            totalSeconds = prefs.flowZoneLongBreakMinutes * 60L,
                            completedCycles = nextCycles,
                            isRunning = false,
                            message = motivationalMessages.random()
                        ))
                    } else {
                        playNotificationSounds(1)
                        state.copy(flowZoneState = fState.copy(
                            phase = FlowZonePhase.BREAK,
                            remainingSeconds = prefs.flowZoneBreakMinutes * 60L,
                            totalSeconds = prefs.flowZoneBreakMinutes * 60L,
                            completedCycles = nextCycles,
                            isRunning = false,
                            message = motivationalMessages.random()
                        ))
                    }
                }
                FlowZonePhase.BREAK, FlowZonePhase.LONG_BREAK -> {
                    state.copy(flowZoneState = fState.copy(
                        phase = FlowZonePhase.FOCUS,
                        remainingSeconds = prefs.flowZoneFocusMinutes * 60L,
                        totalSeconds = prefs.flowZoneFocusMinutes * 60L,
                        isRunning = false,
                        message = null
                    ))
                }
            }
        }
    }

    fun toggleFlowZoneTimer() {
        _uiState.update { state -> 
            state.copy(flowZoneState = state.flowZoneState.copy(isRunning = !state.flowZoneState.isRunning)) 
        }
    }

    fun skipFlowZonePhase() {
        transitionFlowZonePhase()
    }

    fun resetFlowZone() {
        val prefs = _uiState.value.preferences
        _uiState.update { state ->
            state.copy(flowZoneState = FlowZoneState(
                phase = FlowZonePhase.FOCUS,
                remainingSeconds = prefs.flowZoneFocusMinutes * 60L,
                totalSeconds = prefs.flowZoneFocusMinutes * 60L,
                completedCycles = 0,
                isRunning = false,
                message = null
            ))
        }
    }

    fun launchApp(app: LaunchableApp): Boolean = applicationsRepository.launchApp(app)

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(app.key)
        }
    }

    fun toggleIntentionalPilotApp(app: LaunchableApp) {
        viewModelScope.launch {
            preferencesRepository.toggleIntentionalPilotApp(app.key)
        }
    }

    fun setIntentionalPilotDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setIntentionalPilotDelaySeconds(seconds)
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

    fun setFlowZoneEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setFlowZoneEnabled(enabled)
        }
    }

    fun setFlowZoneFocusMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setFlowZoneFocusMinutes(minutes)
        }
    }

    fun setFlowZoneBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setFlowZoneBreakMinutes(minutes)
        }
    }

    fun setFlowZoneLongBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setFlowZoneLongBreakMinutes(minutes)
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
            applicationContext = applicationContext,
            applicationsRepository = PackageManagerApplicationsRepository(applicationContext),
            preferencesRepository = DataStoreLauncherPreferencesRepository(applicationContext),
        ) as T
    }
}
