package com.example.campusrunner.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val badge: String
)

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPage(
            emoji = "🛒 🛵",
            badge = "STEP 1 · REQUEST",
            title = "Get Anything\nfrom Campus",
            description = "Craving snacks from the canteen or need stationery for class? Post a request and get it delivered right to your hostel room."
        ),
        OnboardingPage(
            emoji = "🏃 💰",
            badge = "STEP 2 · RUN & EARN",
            title = "Earn Extra Cash\nOn Your Way",
            description = "Heading to the market or food court? Pick up open requests from nearby students and earn instant tips with zero extra detour."
        ),
        OnboardingPage(
            emoji = "⚡ 🤝",
            badge = "STEP 3 · CONNECT",
            title = "Fast, Safe &\nCommunity-Driven",
            description = "Connect directly with trusted campus peers. Real-time run tracking, transparent pricing, and instant deliveries."
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }
    val isLastPage = currentPage == pages.size - 1

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = { currentPage-- }) {
                        Text("← Back", color = Color.Gray, fontSize = 15.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (!isLastPage) {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Center Dynamic Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_animation"
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Badge
                    Surface(
                        color = Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = page.badge,
                            color = Color(0xFFEA580C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Illustration Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(240.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = page.emoji,
                            fontSize = 72.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom Navigation & Dots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFEA580C) else Color(0xFFE2E8F0)
                                )
                                .clickable { currentPage = index }
                        )
                    }
                }

                // Action Button
                Button(
                    onClick = {
                        if (isLastPage) {
                            onNext()
                        } else {
                            currentPage++
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Get Started 🚀" else "Continue ➔",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
