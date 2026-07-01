package com.aeon.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aeon.app.ui.components.feedback.AeonEmptyState
import com.aeon.app.ui.theme.*

@Composable
fun TaskDetailScreen() {
    DetailPlaceholder("Task Detail", "Manage task properties, subtasks, and deadlines.")
}

@Composable
fun HabitDetailScreen() {
    DetailPlaceholder("Habit Detail", "View habit history, streaks, and adjustment options.")
}

@Composable
fun GoalDetailScreen() {
    DetailPlaceholder("Goal Detail", "Track goal progress, milestones, and linked tasks.")
}

@Composable
fun DetailPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AeonScreenSpacing.Horizontal)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Center
    ) {
        AeonEmptyState(title = title, message = subtitle)
    }
}
