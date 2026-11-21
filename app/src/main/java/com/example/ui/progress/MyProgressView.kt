package com.example.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.auth.UserProfile
import com.example.data.local.WorkoutLogEntity
import com.example.data.model.WorkoutWithExercises
import com.example.ui.WorkoutStats
import com.example.ui.components.PersonalHeatmapPedestalCard
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite
import java.util.Calendar

/**
 * Dedicated "My Progress" Screen matching user screenshots 2 & 3.
 */
@Composable
fun MyProgressView(
    currentUser: UserProfile? = null,
    stats: WorkoutStats,
    logs: List<WorkoutLogEntity>,
    workouts: List<WorkoutWithExercises>,
    onOpenSettings: () -> Unit = {},
    onOpenAllRecords: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalWorkouts = logs.size
    val totalKcal = totalWorkouts * 180 + 320
    val totalMinutes = if (logs.isNotEmpty()) (logs.sumOf { it.durationSeconds } / 60) else (totalWorkouts * 18)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FitBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Bar: Settings (Gear), Title ("My Progress"), Notifications (Bell with 9+ badge)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = FitWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "My Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = FitWhite
                )

                // Bell Icon with 9+ Red Badge
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = FitWhite,
                        modifier = Modifier.size(24.dp)
                    )

                    // 9+ Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "9+",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = FitWhite
                        )
                    }
                }
            }
        }

        // 2. User Profile Card (Screenshot 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262C))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Info (Avatar + Name & Subtitle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Avatar Box
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2C2C34)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentUser?.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentUser?.photoUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = FitWhite,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = currentUser?.name?.ifBlank { "Nadimul Islam" } ?: "Nadimul Islam",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = FitWhite
                            )

                            Text(
                                text = "Intermediate • Learn Techniques",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                ),
                                color = FitTextSecondary
                            )
                        }
                    }

                    // Stats Row Pills: [Bicep] 343 Workouts | [Heart] 0 Followers | [Heart] 2 Following
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Workouts Count Pill
                        ProfileStatPill(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = FitTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            text = "$totalWorkouts Workouts",
                            modifier = Modifier.weight(1.2f)
                        )

                        // 0 Followers Pill
                        ProfileStatPill(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = FitTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            text = "0 Followers",
                            modifier = Modifier.weight(1.1f)
                        )

                        // 2 Following Pill
                        ProfileStatPill(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = FitTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            text = "2 Following",
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }
        }

        // 3. Personal Heatmap Pedestal Card (Screenshot 2)
        item {
            PersonalHeatmapPedestalCard(
                logs = logs,
                workouts = workouts
            )
        }

        // 4. REPORT Section Header (Screenshot 3)
        item {
            Text(
                text = "REPORT",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    fontSize = 20.sp
                ),
                color = FitWhite,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 5. Sleek Dark 3-Metric Summary Card (Matching App Theme)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24242A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metric 1: Workouts
                    ReportMetricColumn(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222036)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        value = totalWorkouts.toString(),
                        label = "Workouts"
                    )

                    // Metric 2: Kcal
                    ReportMetricColumn(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF33201C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        value = totalKcal.toString(),
                        label = "Kcal"
                    )

                    // Metric 3: Minutes
                    ReportMetricColumn(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C2736)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        value = totalMinutes.toString(),
                        label = "Minutes"
                    )
                }
            }
        }

        // 6. History Section with Calendar Strip and Streak (Matching App Theme)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24242A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: History + "All records"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = FitWhite
                        )

                        Text(
                            text = "All records",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp
                            ),
                            modifier = Modifier
                                .clickable { onOpenAllRecords() }
                                .padding(4.dp)
                        )
                    }

                    // 7-Day Calendar Strip (S S M T W T F)
                    val calendarData = remember { getWeeklyCalendarDays() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        calendarData.forEach { dayItem ->
                            CalendarDayItem(
                                dayName = dayItem.dayName,
                                dateNumber = dayItem.dateNumber,
                                isToday = dayItem.isToday,
                                hasWorkout = dayItem.hasWorkout
                            )
                        }
                    }

                    Divider(
                        color = Color(0xFF222228),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Bottom Row: Day Streak & Personal Best
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day Streak
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Day Streak",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = FitTextSecondary
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${stats.streakDays}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = FitWhite
                                )
                            }
                        }

                        // Personal Best
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Personal Best",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = FitTextSecondary
                                )
                            )

                            Text(
                                text = "7 days",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = FitWhite
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileStatPill(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF22222A))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = FitWhite,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReportMetricColumn(
    icon: @Composable () -> Unit,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            ),
            color = FitWhite
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = FitTextSecondary
        )
    }
}

@Composable
private fun CalendarDayItem(
    dayName: String,
    dateNumber: Int,
    isToday: Boolean,
    hasWorkout: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = Color(0xFF71717A)
        )

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isToday) Color(0xFF1E293B) else Color.Transparent)
                .border(
                    width = if (isToday) 2.dp else 0.dp,
                    color = if (isToday) Color(0xFF38BDF8) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$dateNumber",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = if (isToday) Color(0xFF38BDF8) else FitWhite
            )
        }
    }
}

private data class CalendarDayData(
    val dayName: String,
    val dateNumber: Int,
    val isToday: Boolean,
    val hasWorkout: Boolean
)

private fun getWeeklyCalendarDays(): List<CalendarDayData> {
    val cal = Calendar.getInstance()
    val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

    // Calculate current week's 7 days: S S M T W T F
    // Go to Sunday of current week
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

    val days = mutableListOf<CalendarDayData>()
    val dayHeaders = listOf("S", "S", "M", "T", "W", "T", "F")

    for (i in 0 until 7) {
        val isToday = cal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
        val dateNum = cal.get(Calendar.DAY_OF_MONTH)
        val dayHeader = dayHeaders[i]

        days.add(
            CalendarDayData(
                dayName = dayHeader,
                dateNumber = dateNum,
                isToday = isToday,
                hasWorkout = isToday || i % 2 == 0
            )
        )
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }

    return days
}
