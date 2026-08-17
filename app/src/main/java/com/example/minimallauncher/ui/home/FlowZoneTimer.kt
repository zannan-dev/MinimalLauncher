package com.example.minimallauncher.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.ui.FlowZonePhase
import com.example.minimallauncher.ui.FlowZoneState

@SuppressLint("DefaultLocale")
@Composable
fun FlowZoneTimer(
    state: FlowZoneState,
    onToggleTimer: () -> Unit,
    onSkipPhase: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val phaseLabel = when (state.phase) {
            FlowZonePhase.FOCUS -> "Flow"
            FlowZonePhase.BREAK -> "Break"
            FlowZonePhase.LONG_BREAK -> "Long Break"
        }

        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        Text(
            text = timeString,
            fontSize = 96.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        state.message?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Cycles dots
        Row(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0..3) {
                val cycleMod = state.completedCycles % 4
                val isActiveFocus = state.phase == FlowZonePhase.FOCUS && i == cycleMod
                val isCompleted = if (state.phase == FlowZonePhase.LONG_BREAK) {
                    true
                } else {
                    i < cycleMod
                }
                
                val progress = if (isActiveFocus && state.totalSeconds > 0) {
                    state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
                } else {
                    0f
                }
                
                val targetWidth = if (isActiveFocus) {
                    12.dp + (48.dp - 12.dp) * progress
                } else {
                    12.dp
                }
                
                val animatedWidth by animateDpAsState(
                    targetValue = targetWidth,
                    label = "dot_width"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = animatedWidth, height = 12.dp)
                        .background(
                            color = if (isActiveFocus || isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        // Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (state.isRunning || state.remainingSeconds < (if (state.phase == FlowZonePhase.FOCUS) 25 * 60 else 5 * 60)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onReset() }
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(32.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .clickable { onToggleTimer() }
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isRunning) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            if (state.isRunning || state.remainingSeconds < (if (state.phase == FlowZonePhase.FOCUS) 25 * 60 else 5 * 60)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Skip",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onSkipPhase() }
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
    }
}
