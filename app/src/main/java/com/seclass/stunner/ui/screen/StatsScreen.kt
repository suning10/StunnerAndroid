package com.seclass.stunner.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seclass.stunner.model.GoalEvent
import com.seclass.stunner.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.allEvents.collectAsState()
    val zoneCounts by viewModel.zoneCounts.collectAsState()
    val totalGoals by viewModel.totalGoals.collectAsState()
    val totalShots by viewModel.totalShots.collectAsState()
    val accuracy = if (totalShots == 0) 0f else totalGoals.toFloat() / totalShots

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past Performance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryItem("Goals", "$totalGoals")
                        SummaryItem("Shots", "$totalShots")
                        SummaryItem("Accuracy", "${"%.0f".format(accuracy * 100)}%")
                    }
                }
            }

            // Heatmap
            item {
                Text("Goal Zone Heatmap", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                AllTimeZoneHeatmap(counts = zoneCounts)
            }

            // Recent events header
            item {
                Text("Recent Shots", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        "No shots recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(events) { event ->
                    ShotEventRow(event)
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AllTimeZoneHeatmap(counts: IntArray) {
    val maxCount = counts.maxOrNull()?.takeIf { it > 0 } ?: 1
    val zoneLabels = listOf("TL","TC","TR","ML","MC","MR","BL","BC","BR")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.Center) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    val count = counts[index]
                    val intensity = count.toFloat() / maxCount
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Color(0xFF4CAF50).copy(alpha = 0.15f + intensity * 0.85f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$count",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (intensity > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                zoneLabels[index],
                                fontSize = 11.sp,
                                color = if (intensity > 0.5f) Color.White.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

@Composable
private fun ShotEventRow(event: GoalEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // GOAL / MISS badge
        Surface(
            color = if (event.goal) Color(0xFF4CAF50) else Color(0xFFE53935),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = if (event.goal) "GOAL" else "MISS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Zone
        Text(
            text = event.zone ?: "—",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Accuracy
        Text(
            text = "${"%.0f".format(event.accuracy * 100)}%",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Time
        Text(
            text = timeFormat.format(Date(event.createTime)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}
