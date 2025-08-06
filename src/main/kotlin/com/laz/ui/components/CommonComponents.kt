package com.laz.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class StatCard(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

data class ActionCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// Add other common components here as needed
