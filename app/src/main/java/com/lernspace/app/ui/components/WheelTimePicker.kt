package com.lernspace.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lernspace.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePicker(
    initialHour: Int = 8,
    initialMinute: Int = 0,
    minuteInterval: Int = 5, // 5 or 1
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hours = remember { (0..23).toList() }
    val minutes = remember(minuteInterval) {
        if (minuteInterval <= 1) {
            (0..59).toList()
        } else {
            (0..55 step minuteInterval).toList()
        }
    }

    val itemHeight = 44.dp
    val visibleItemsCount = 3
    val totalHeight = itemHeight * visibleItemsCount

    val hourIndex = remember(initialHour) { hours.indexOf(initialHour.coerceIn(0, 23)).coerceAtLeast(0) }
    val nearestMinute = remember(initialMinute, minutes) {
        minutes.minByOrNull { Math.abs(it - initialMinute) } ?: minutes.first()
    }
    val minuteIndex = remember(nearestMinute, minutes) { minutes.indexOf(nearestMinute).coerceAtLeast(0) }

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = hourIndex)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = minuteIndex)

    val hourFlingBehavior = rememberSnapFlingBehavior(lazyListState = hourListState)
    val minuteFlingBehavior = rememberSnapFlingBehavior(lazyListState = minuteListState)

    val currentSelectedHour by remember {
        derivedStateOf {
            val idx = hourListState.firstVisibleItemIndex
            hours.getOrElse(idx) { 8 }
        }
    }

    val currentSelectedMinute by remember {
        derivedStateOf {
            val idx = minuteListState.firstVisibleItemIndex
            minutes.getOrElse(idx) { 0 }
        }
    }

    LaunchedEffect(currentSelectedHour, currentSelectedMinute) {
        onTimeChanged(currentSelectedHour, currentSelectedMinute)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight bar in the middle
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = LernspaceSurfaceLow,
            border = BorderStroke(1.dp, LernspaceBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(itemHeight)
        ) {}

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Hours Column ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(totalHeight),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = hourListState,
                    flingBehavior = hourFlingBehavior,
                    contentPadding = PaddingValues(vertical = itemHeight),
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(hours) { index, hour ->
                        val isCenter = hourListState.firstVisibleItemIndex == index
                        Box(
                            modifier = Modifier
                                .height(itemHeight)
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        hourListState.animateScrollToItem(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", hour),
                                fontSize = if (isCenter) 22.sp else 16.sp,
                                fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCenter) LernspaceDark else LernspaceTextSecondary,
                                modifier = Modifier.alpha(if (isCenter) 1f else 0.45f)
                            )
                        }
                    }
                }
            }

            // Colon separator
            Text(
                text = ":",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LernspaceDark,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // ── Minutes Column ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(totalHeight),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = minuteListState,
                    flingBehavior = minuteFlingBehavior,
                    contentPadding = PaddingValues(vertical = itemHeight),
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(minutes) { index, minute ->
                        val isCenter = minuteListState.firstVisibleItemIndex == index
                        Box(
                            modifier = Modifier
                                .height(itemHeight)
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        minuteListState.animateScrollToItem(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", minute),
                                fontSize = if (isCenter) 22.sp else 16.sp,
                                fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCenter) LernspaceDark else LernspaceTextSecondary,
                                modifier = Modifier.alpha(if (isCenter) 1f else 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal Dialog popup that lets the user scroll to set time in seconds,
 * with quick interval switch (1 minute vs 5 minute) and duration shortcuts.
 */
@Composable
fun WheelTimePickerDialog(
    title: String = "Pilih Waktu",
    initialTime: String = "08:00",
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initialH = remember(initialTime) { initialTime.split(":").firstOrNull()?.toIntOrNull() ?: 8 }
    val initialM = remember(initialTime) { initialTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0 }

    var selectedHour by remember { mutableIntStateOf(initialH) }
    var selectedMinute by remember { mutableIntStateOf(initialM) }
    var minuteStep by remember { mutableIntStateOf(5) } // 5 or 1

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LernspaceDark
                    )

                    // 1m vs 5m toggle
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LernspaceSurfaceLow,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.clickable {
                            minuteStep = if (minuteStep == 5) 1 else 5
                        }
                    ) {
                        Text(
                            text = if (minuteStep == 5) "Step: 5 Menit" else "Step: 1 Menit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Digital Display Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LernspaceSurfaceLow,
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = LernspaceDark,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Scroll Wheel
                WheelTimePicker(
                    initialHour = initialH,
                    initialMinute = initialM,
                    minuteInterval = minuteStep,
                    onTimeChanged = { h, m ->
                        selectedHour = h
                        selectedMinute = m
                    }
                )

                // Quick Minutes Adjustment buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-15, -5, +5, +15).forEach { delta ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LernspaceSurfaceLow,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier.clickable {
                                val total = (selectedHour * 60 + selectedMinute + delta + 1440) % 1440
                                selectedHour = total / 60
                                selectedMinute = total % 60
                            }
                        ) {
                            Text(
                                text = if (delta > 0) "+${delta}m" else "${delta}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = LernspaceTextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val timeStr = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                            onTimeSelected(timeStr)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LernspaceDark, contentColor = Color.White)
                    ) {
                        Text("Pilih Waktu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
