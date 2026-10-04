package com.example.minimallauncher.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** A shared, restrained motion language. Compose honors the system animation scale. */
object LauncherMotion {
    fun <T> settle(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 450f,
    )

    fun <T> search(opening: Boolean = true): TweenSpec<T> = tween(
        durationMillis = if (opening) 340 else 260,
        easing = if (opening) CubicBezierEasing(0.2f, 0f, 0.2f, 1f) else FastOutSlowInEasing,
    )

    /** Smoothly stage details within one reversible transition, without delayed animations. */
    fun reveal(progress: Float, start: Float = 0f, end: Float = 1f): Float {
        val fraction = ((progress - start) / (end - start)).coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    fun <T> fade(): TweenSpec<T> = tween(160, easing = FastOutSlowInEasing)
    fun <T> color(): TweenSpec<T> = tween(220, easing = FastOutSlowInEasing)
}

/** Draw-only press feedback without changing the space reserved for each control. */
@Composable
fun Modifier.launcherPressFeedback(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = LauncherMotion.settle(),
        label = "Press response",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
