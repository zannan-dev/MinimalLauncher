package com.example.minimallauncher.ui.apps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.domain.LaunchableApp
import kotlinx.coroutines.delay

@Composable
fun IntentionalPilotScreen(
    app: LaunchableApp,
    delaySeconds: Int,
    onLaunchApp: () -> Unit,
    onCancel: () -> Unit,
) {
    var remainingSeconds by remember { mutableStateOf(delaySeconds) }
    var instructionText by remember { mutableStateOf("Breathe in...") }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    
    // Core breathing cycle (0.0 to 1.0)
    val breathingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_progress"
    )

    LaunchedEffect(delaySeconds) {
        remainingSeconds = delaySeconds
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }
    
    LaunchedEffect(breathingProgress) {
        if (breathingProgress > 0.95f) {
            instructionText = "Breathe out..."
        } else if (breathingProgress < 0.05f) {
            instructionText = "Breathe in..."
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = instructionText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(0.8f + (breathingProgress * 0.2f))
                        .alpha(0.1f + (breathingProgress * 0.1f))
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(0.7f + (breathingProgress * 0.3f))
                        .alpha(0.2f + (breathingProgress * 0.15f))
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
                // Inner ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(0.6f + (breathingProgress * 0.4f))
                        .alpha(0.3f + (breathingProgress * 0.2f))
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }

            Text(
                text = "What is the purpose of opening ${app.label}?\nIs it necessary?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onCancel) {
                    Text("No")
                }
                Button(
                    onClick = onLaunchApp,
                    enabled = remainingSeconds == 0
                ) {
                    Text(if (remainingSeconds > 0) remainingSeconds.toString() else "Yes")
                }
            }
        }
    }
}
