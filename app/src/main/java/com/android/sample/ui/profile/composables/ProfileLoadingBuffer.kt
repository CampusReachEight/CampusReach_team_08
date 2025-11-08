package com.android.sample.ui.profile.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Lightweight skeleton / buffer shown while profile data is loading.
 * Keeps the same test tag used by tests: "profile_loading".
 */
@Composable
fun ProfileLoadingBuffer(modifier: Modifier = Modifier, tag: String = "profile_loading") {
    Box(modifier = modifier.testTag(tag)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header skeleton
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Box(
                            modifier =
                                Modifier
                                    .width(160.dp)
                                    .height(16.dp)
                                    .background(Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier =
                                Modifier
                                    .width(210.dp)
                                    .height(12.dp)
                                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats skeleton row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Column {
                                Box(
                                    modifier =
                                        Modifier
                                            .width(40.dp)
                                            .height(16.dp)
                                            .background(Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier =
                                        Modifier
                                            .width(24.dp)
                                            .height(24.dp)
                                            .background(Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Information placeholders
            repeat(5) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Actions placeholders
            repeat(2) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)))
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Keep the centered spinner overlay (existing small indicator)
        LoadingIndicator(Modifier.align(Alignment.Center))
    }
}