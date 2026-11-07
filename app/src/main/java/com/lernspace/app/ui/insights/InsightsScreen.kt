package com.lernspace.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lernspace.app.ui.theme.*

@Composable
fun InsightsScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Learning Insights & Workload",
                style = MaterialTheme.typography.headlineLarge,
                color = LernspaceDark
            )
            Text(
                text = "Reflective metrics without pressure to optimize every minute",
                style = MaterialTheme.typography.bodyMedium,
                color = LernspaceTextSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightMetricCard(title = "Tasks Completed", value = "18", subtitle = "This week (+4 from last week)", modifier = Modifier.weight(1f))
            InsightMetricCard(title = "Study Sessions", value = "12", subtitle = "Total 14.5 hours focused", modifier = Modifier.weight(1f))
            InsightMetricCard(title = "Topic Revisions", value = "7", subtitle = "Self-reported understanding improved", modifier = Modifier.weight(1f))
        }

        // Workload breakdown by subject
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
            color = LernspaceSurfaceCard
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Weekly Study Distribution by Subject",
                    style = MaterialTheme.typography.titleLarge,
                    color = LernspaceDark
                )

                SubjectProgressRow(subject = "Neurobiology", hours = "6.5h", progress = 0.45f, color = AccentTeal)
                SubjectProgressRow(subject = "Advanced Statistics", hours = "4.0h", progress = 0.28f, color = AccentBlue)
                SubjectProgressRow(subject = "Cognitive Psychology", hours = "2.5h", progress = 0.17f, color = AccentAmber)
                SubjectProgressRow(subject = "Organic Chemistry II", hours = "1.5h", progress = 0.10f, color = LernspaceDark)
            }
        }
    }
}

@Composable
fun InsightMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LernspaceBorder, RoundedCornerShape(12.dp)),
        color = LernspaceSurfaceCard
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = LernspaceTextSecondary)
            Text(text = value, style = MaterialTheme.typography.headlineLarge, color = LernspaceDark)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = AccentTeal)
        }
    }
}

@Composable
fun SubjectProgressRow(
    subject: String,
    hours: String,
    progress: Float,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = subject, style = MaterialTheme.typography.bodyLarge, color = LernspaceDark)
            Text(text = hours, style = MaterialTheme.typography.labelMedium, color = LernspaceTextSecondary)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = LernspaceSurfaceLow
        )
    }
}

