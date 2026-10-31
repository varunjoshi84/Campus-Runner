package com.example.campusrunner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "OPEN" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "ACCEPTED" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        "BOUGHT" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "DELIVERED" -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        "CANCELLED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color.LightGray to Color.DarkGray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = textColor,
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
