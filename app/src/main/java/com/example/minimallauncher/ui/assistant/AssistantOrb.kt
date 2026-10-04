package com.example.minimallauncher.ui.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.minimallauncher.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.ui.motion.LauncherMotion

/** The supplied artwork preserves the reference strands and glow. Idle stays still. */
@Composable
fun AssistantOrb(
    onClick: () -> Unit,
    description: String,
    diameter: Dp = 96.dp,
    listening: Boolean = false,
    level: Float = 0f,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, LauncherMotion.settle(), label = "Assistant press")
    val energy by animateFloatAsState(if (listening) level.coerceIn(0f, 1f) else 0f,
        LauncherMotion.settle(), label = "Voice energy")
    var restingRotation by remember { mutableFloatStateOf(0f) }
    val rotation = if (listening) {
        val origin = remember { restingRotation }
        val motion = rememberInfiniteTransition(label = "Listening ribbons")
        val angle by motion.animateFloat(origin, origin + 360f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "Ribbon rotation")
        SideEffect { restingRotation = angle }
        angle
    } else restingRotation
    Box(Modifier.size(diameter).graphicsLayer { scaleX = scale; scaleY = scale }
        .semantics { contentDescription = description }
        .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)) {
        // Map the green artwork's intensity to white without changing its texture or contours.
        val monochrome = remember {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0f, 1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )))
        }
        Image(
            painter = painterResource(R.drawable.assistant_ring),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = monochrome,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                rotationZ = rotation
                scaleX = 1f + energy * 0.06f
                scaleY = 1f + energy * 0.06f
            }.drawWithContent {
                drawContent()
                drawRect(Brush.radialGradient(
                    0f to Color.White, 0.78f to Color.White, 1f to Color.Transparent,
                    center = center, radius = size.minDimension / 2f,
                ), blendMode = BlendMode.DstIn)
            },
        )
    }
}
