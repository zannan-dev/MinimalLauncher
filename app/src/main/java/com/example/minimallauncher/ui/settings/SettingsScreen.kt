package com.example.minimallauncher.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp

@Composable
fun SettingsScreen(
    use24HourClock: Boolean,
    showDate: Boolean,
    autoOpenKeyboard: Boolean,
    doubleTapToLock: Boolean,
    theme: ThemePreference,
    onUse24HourClockChanged: (Boolean) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onAutoOpenKeyboardChanged: (Boolean) -> Unit,
    onDoubleTapToLockChanged: (Boolean) -> Unit,
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
                    title = "24-hour clock",
                    checked = use24HourClock,
                    onCheckedChange = onUse24HourClockChanged,
                )
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionTitle("Theme")
                Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                    ThemePreference.entries.forEach { choice ->
                        TextButton(
                            onClick = { onThemeChanged(choice) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(if (choice == theme) "• ${choice.label}" else choice.label)
                        }
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Switch }
            .toggleable(value = checked, onValueChange = onCheckedChange)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}
