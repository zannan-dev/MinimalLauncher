package com.example.minimallauncher.ui.apps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.domain.LaunchableApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IntentionalPilotScreen(
    app: LaunchableApp,
    delaySeconds: Int,
    onLaunchApp: () -> Unit,
    onCancel: () -> Unit,
) {
    var remainingSeconds by remember { mutableStateOf(delaySeconds) }
    var breathPhase by remember { mutableStateOf("Get ready") }
    var phaseSecondsLeft by remember { mutableStateOf(3) }

    val breathingProgress = remember { Animatable(0f) }

    LaunchedEffect(delaySeconds) {
        remainingSeconds = delaySeconds
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }
    
    LaunchedEffect(Unit) {
        launch {
            breathPhase = "Get ready"
            for (i in 3 downTo 1) {
                phaseSecondsLeft = i
                delay(1000L)
            }
            while (true) {
                breathPhase = "Inhale"
                for (i in 4 downTo 1) {
                    phaseSecondsLeft = i
                    delay(1000L)
                }
                breathPhase = "Exhale"
                for (i in 7 downTo 1) {
                    phaseSecondsLeft = i
                    delay(1000L)
                }
            }
        }
        
        launch {
            delay(3000L)
            while (true) {
                breathingProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
                )
                breathingProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 7000, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(340.dp)
                        .scale(0.85f + (breathingProgress.value * 0.2f))
                        .alpha(0.05f + (breathingProgress.value * 0.1f))
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(0.8f + (breathingProgress.value * 0.3f))
                        .alpha(0.1f + (breathingProgress.value * 0.15f))
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
                // Inner ring
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(0.75f + (breathingProgress.value * 0.4f))
                        .alpha(0.2f + (breathingProgress.value * 0.2f))
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )

                Text(
                    text = "$breathPhase\n$phaseSecondsLeft",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(0.9f)
                        .scale(0.85f + (breathingProgress.value * 0.15f))
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Do you really need to open ${app.label}?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp).alpha(0.9f),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Take a moment to decide.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 40.dp).alpha(0.7f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("No, go back", fontSize = 16.sp)
                    }
                    Button(
                        onClick = onLaunchApp,
                        enabled = remainingSeconds == 0,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (remainingSeconds > 0) "Wait ($remainingSeconds)" else "Yes, open",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
