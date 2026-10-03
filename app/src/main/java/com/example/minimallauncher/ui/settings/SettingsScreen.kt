package com.example.minimallauncher.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.material3.FilterChip
import com.example.minimallauncher.ui.motion.LauncherMotion
import com.example.minimallauncher.ui.motion.launcherPressFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.height
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp

@Composable
fun SettingsScreen(
    showDate: Boolean,
    autoOpenKeyboard: Boolean,
    doubleTapToLock: Boolean,
    showStatusBar: Boolean,
    isIntentionalPilotEnabled: Boolean,
    theme: ThemePreference,
    onShowDateChanged: (Boolean) -> Unit,
    onAutoOpenKeyboardChanged: (Boolean) -> Unit,
    onDoubleTapToLockChanged: (Boolean) -> Unit,
    onShowStatusBarChanged: (Boolean) -> Unit,
    onIntentionalPilotEnabledChanged: (Boolean) -> Unit,
    onSelectIntentionalPilotApps: () -> Unit,
    onThemeChanged: (ThemePreference) -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                SettingsSectionTitle("General")
                PreferenceToggle(
                    title = "Show date",
                    checked = showDate,
                    onCheckedChange = onShowDateChanged,
                )
                PreferenceToggle(
                    title = "Auto-open keyboard in app drawer",
                    checked = autoOpenKeyboard,
                    onCheckedChange = onAutoOpenKeyboardChanged,
                )
                PreferenceToggle(
                    title = "Double tap to lock screen",
                    checked = doubleTapToLock,
                    onCheckedChange = onDoubleTapToLockChanged,
                )
                PreferenceToggle(
                    title = "Show status bar",
                    checked = showStatusBar,
                    onCheckedChange = onShowStatusBarChanged,
                )

                PreferenceToggle(
                    title = "Intentional Pilot",
                    subtitle = "Pause for 3 seconds to consider why you are opening distracting apps",
                    checked = isIntentionalPilotEnabled,
                    onCheckedChange = onIntentionalPilotEnabledChanged,
                )
                AnimatedVisibility(
                    visible = isIntentionalPilotEnabled,
                    enter = expandVertically(LauncherMotion.settle()) + fadeIn(LauncherMotion.fade()),
                    exit = shrinkVertically(LauncherMotion.settle()) + fadeOut(LauncherMotion.fade()),
                ) {
                    PreferenceRow(
                        title = "Select apps to delay",
                        subtitle = "Choose which apps ask you to pause before opening.",
                        onClick = onSelectIntentionalPilotApps
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionTitle("Theme")
                Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                    ThemePreference.entries.forEach { choice ->
                        FilterChip(
                            selected = choice == theme,
                            onClick = { onThemeChanged(choice) },
                            label = { Text(choice.label) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionTitle("System")
                PreferenceRow(
                    title = "Default launcher settings",
                    onClick = onOpenDefaultLauncherSettings
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionTitle("About")
                Text(
                    text = "Minimal Launcher is an offline, distraction-free Android home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                )
            }
        }
    }
}

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.SYSTEM -> "System"
        ThemePreference.LIGHT -> "Light"
        ThemePreference.DARK -> "Dark"
    }

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun PreferenceToggle(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .launcherPressFeedback(interactions)
            .semantics { role = Role.Switch }
            .toggleable(value = checked, interactionSource = interactions,
                indication = androidx.compose.material3.ripple(), role = Role.Switch,
                onValueChange = onCheckedChange)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .launcherPressFeedback(interactions)
            .clickable(interactionSource = interactions, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
