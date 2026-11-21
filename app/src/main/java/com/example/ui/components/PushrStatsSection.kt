package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WorkoutStats
import com.example.ui.theme.GeoAccentFlame
import com.example.ui.theme.GeoBorderLilac
import com.example.ui.theme.GeoBorderMuted
import com.example.ui.theme.GeoCardWhite
import com.example.ui.theme.GeoOnPrimaryContainer
import com.example.ui.theme.GeoPrimaryContainer
import com.example.ui.theme.GeoPrimaryPurple
import com.example.ui.theme.GeoSurfaceContainer
import com.example.ui.theme.GeoSurfaceContainerLow
import com.example.ui.theme.GeoTextDark
import com.example.ui.theme.GeoTextMuted
import com.example.ui.theme.GeoTextWhite
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PushrStatsSection(
    stats: WorkoutStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Today's Reps / Workouts & Aggregate Stats in Geometric Balance White Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_stats_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GeoCardWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoBorderLilac),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Main Big Stat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${stats.totalRepsToday.coerceAtLeast(stats.workoutsToday)}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 44.sp,
                            letterSpacing = (-1.5).sp
                        ),
                        color = GeoTextDark
                    )

                    Column {
                        Text(
                            text = "push-ups & reps",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = GeoTextDark
                        )
                        Text(
                            text = "today",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = GeoPrimaryPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3 Secondary Stats Row in Geometric Pill Containers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryStatItem(
                        value = "${stats.setsThisWeek}",
                        label = "THIS WEEK",
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryStatItem(
                        value = "${stats.setsThisMonth}",
                        label = "THIS MONTH",
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryStatItem(
                        value = "${stats.setsTotalSoFar}",
                        label = "SO FAR",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 2: "this week" Streak Tracker (S M T W T F S) in Lavender Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weekly_streak_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GeoSurfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoBorderLilac),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: this week + flame streak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Consistency",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = GeoTextDark
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GeoPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = GeoPrimaryPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${stats.streakDays} DAYS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = GeoOnPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf(
                        Triple(Calendar.SUNDAY, "S", stats.activeDaysOfWeek.contains(Calendar.SUNDAY)),
                        Triple(Calendar.MONDAY, "M", stats.activeDaysOfWeek.contains(Calendar.MONDAY)),
                        Triple(Calendar.TUESDAY, "T", stats.activeDaysOfWeek.contains(Calendar.TUESDAY)),
                        Triple(Calendar.WEDNESDAY, "W", stats.activeDaysOfWeek.contains(Calendar.WEDNESDAY)),
                        Triple(Calendar.THURSDAY, "T", stats.activeDaysOfWeek.contains(Calendar.THURSDAY)),
                        Triple(Calendar.FRIDAY, "F", stats.activeDaysOfWeek.contains(Calendar.FRIDAY)),
                        Triple(Calendar.SATURDAY, "S", stats.activeDaysOfWeek.contains(Calendar.SATURDAY))
                    )

                    days.forEach { (_, label, isActive) ->
                        DayStreakItem(label = label, isActive = isActive)
                    }
                }
            }
        }

        // Card 3: Global sets counter (Deep Royal Purple Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("global_counter_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GeoPrimaryPurple),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Global Push-ups & Sets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = GeoTextWhite
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Global",
                            tint = GeoTextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val formattedGlobal = NumberFormat.getNumberInstance(Locale.US).format(stats.globalSetsCounter)
                Text(
                    text = formattedGlobal,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        letterSpacing = (-1.0).sp
                    ),
                    color = GeoTextWhite
                )
            }
        }
    }
}

@Composable
private fun SecondaryStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GeoSurfaceContainerLow)
            .border(1.dp, GeoBorderLilac, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                ),
                color = GeoPrimaryPurple
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 22.sp
                ),
                color = GeoTextDark
            )
        }
    }
}

@Composable
private fun DayStreakItem(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isActive) {
            // Rounded vertical purple pill with checkmark and letter
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GeoPrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = Color.White
                    )
                }
            }
        } else {
            // Inactive subtle dot and letter
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(GeoCardWhite)
                    .border(1.dp, GeoBorderMuted, CircleShape)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = GeoTextMuted
            )
        }
    }
}

