package com.example.minimallauncher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.movedFavorite
import kotlin.math.roundToInt

@Composable
fun HomeFavorites(
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onMoveFavorite: (LaunchableApp, LaunchableApp) -> Unit,
    onRemoveFavorite: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hasMoved by remember { mutableStateOf(false) }
    var dragOrigin by remember { mutableStateOf<Rect?>(null) }
    var previewOrder by remember { mutableStateOf<List<String>?>(null) }
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }
    var containerBounds by remember { mutableStateOf<Rect?>(null) }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    val listState = rememberLazyListState()
    val overTrash = draggingKey != null && trashBounds?.contains(dragPosition) == true
    val appsByKey = apps.associateBy { it.key }
    val visibleApps = previewOrder?.mapNotNull(appsByKey::get) ?: apps

    LaunchedEffect(apps.map { it.key }) {
        if (draggingKey == null && previewOrder == apps.map { it.key }) previewOrder = null
    }

    Box(modifier = modifier.onGloballyPositioned { containerBounds = it.boundsInRoot() }) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(bottom = 72.dp),
            userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        ) {
            items(visibleApps, key = { it.key }) { app ->
                    val isDragging = draggingKey == app.key
                    Row(
                        modifier = Modifier.animateItem().fillMaxWidth().heightIn(min = 48.dp)
                            .onGloballyPositioned { coordinates ->
                                if (!isDragging) rowBounds[app.key] = coordinates.boundsInRoot()
                            }
                            .graphicsLayer {
                                alpha = if (isDragging) 0f else 1f
                            }
                            .clickable { onLaunchApp(app) }
                            .pointerInput(app.key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        val origin = rowBounds[app.key]
                                        dragOrigin = origin
                                        draggingKey = app.key
                                        previewOrder = apps.map { it.key }
                                        dragOffset = Offset.Zero
                                        hasMoved = false
                                        dragPosition = origin?.topLeft?.plus(start) ?: start
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                        dragPosition += amount
                                        if (amount != Offset.Zero) hasMoved = true
                                        val order = previewOrder ?: apps.map { it.key }
                                        val fromIndex = order.indexOf(app.key)
                                        val hoveredKey = order.firstOrNull { key ->
                                            key != app.key && rowBounds[key]?.contains(dragPosition) == true
                                        }
                                        val targetIndex = order.indexOf(hoveredKey)
                                        if (hoveredKey != null &&
                                            ((amount.y > 0 && targetIndex > fromIndex) ||
                                                (amount.y < 0 && targetIndex < fromIndex))
                                        ) {
                                            previewOrder = movedFavorite(order, app.key, hoveredKey)
                                        }
                                    },
                                    onDragEnd = {
                                        val order = previewOrder ?: apps.map { it.key }
                                        if (trashBounds?.contains(dragPosition) == true) {
                                            previewOrder = order - app.key
                                            onRemoveFavorite(app)
                                        } else {
                                            val finalIndex = order.indexOf(app.key)
                                            if (finalIndex != apps.indexOf(app) && finalIndex in apps.indices) {
                                                onMoveFavorite(app, apps[finalIndex])
                                            } else {
                                                previewOrder = null
                                            }
                                        }
                                        draggingKey = null
                                        dragOrigin = null
                                        dragOffset = Offset.Zero
                                        hasMoved = false
                                    },
                                    onDragCancel = {
                                        draggingKey = null
                                        dragOrigin = null
                                        previewOrder = null
                                        dragOffset = Offset.Zero
                                        hasMoved = false
                                    },
                                )
                            }
                            .semantics { contentDescription = "Favorite ${app.label}" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(app.label, fontSize = 18.sp, color = Color.White)
                    }
            }
        }

        if (draggingKey != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).size(64.dp).zIndex(1f)
                    .onGloballyPositioned { trashBounds = it.boundsInRoot() }
                    .semantics { contentDescription = "Remove favorite" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (overTrash) Color(0xFFFF7777) else Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        val draggedApp = apps.firstOrNull { it.key == draggingKey }
        val origin = dragOrigin
        val container = containerBounds
        if (hasMoved && draggedApp != null && origin != null && container != null) {
            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (origin.left - container.left).roundToInt(),
                            (origin.top - container.top).roundToInt(),
                        )
                    }
                    .graphicsLayer {
                        translationX = dragOffset.x
                        translationY = dragOffset.y
                        alpha = 0.55f
                    }
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(draggedApp.label, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
