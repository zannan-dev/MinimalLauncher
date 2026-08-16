package com.example.minimallauncher.ui.settings

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
    theme: ThemePreference,
    onUse24HourClockChanged: (Boolean) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onAutoOpenKeyboardChanged: (Boolean) -> Unit,
    onThemeChanged: (ThemePreference) -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
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
                HorizontalDivider()
                Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                Row {
                    ThemePreference.entries.forEach { choice ->
                        TextButton(onClick = { onThemeChanged(choice) }) {
                            Text(if (choice == theme) "• ${choice.label}" else choice.label)
                        }
                    }
                }
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = onOpenDefaultLauncherSettings, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Default launcher settings")
                }
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                Text("About", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                Text("Minimal Launcher is an offline, distraction-free Android home screen.")
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
            .padding(vertical = 12.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}
