package com.example.campusrunner.ui.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusrunner.data.remote.RunDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    run: RunDto,
    onDismiss: () -> Unit,
    onSubmit: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var item by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTip by remember { mutableStateOf(20) }

    val tipOptions = listOf(10, 20, 30)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Request Item", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Runner", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            // Runner Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👨‍🎓", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = run.runnerName ?: "Kamal S.", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "At: ${run.storeName}", fontSize = 13.sp, color = Color.Gray)
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Item Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Item Name Input
            OutlinedTextField(
                value = item,
                onValueChange = { item = it },
                label = { Text("Item Name *") },
                placeholder = { Text("Notebook (A4)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("Any brand, 200 pages") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Tip Amount (₹)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Tip Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tipOptions.forEach { tip ->
                    val isSelected = selectedTip == tip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF3F4F6),
                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFEA580C)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTip = tip }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹$tip",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFEA580C) else Color.DarkGray,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Custom Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Custom", fontWeight = FontWeight.Medium, color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Send Request Button
            Button(
                onClick = {
                    if (item.isNotBlank()) {
                        onSubmit(item, selectedTip)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(text = "Send Request", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
