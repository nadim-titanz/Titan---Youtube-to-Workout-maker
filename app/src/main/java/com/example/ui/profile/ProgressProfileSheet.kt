package com.example.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.InteractiveBodyHeatmapDisplay
import com.example.ui.components.MusclePart
import com.example.ui.components.parseTargetMuscles
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitCardBackground
import com.example.ui.theme.FitCardElevated
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Computes 4-day personal heatmap intensity from workout logs and workouts.
 * Resets every 4 days (decaying load window).
 */
fun calculate4DayMuscleHeatmap(
    logs: List<WorkoutLogEntity>,
    workouts: List<WorkoutWithExercises>,
    customCycleStartTimestamp: Long? = null
): Map<MusclePart, Float> {
    val now = System.currentTimeMillis()
    val fourDaysMillis = TimeUnit.DAYS.toMillis(4)
    val cycleCutoff = customCycleStartTimestamp?.coerceAtLeast(now - fourDaysMillis) ?: (now - fourDaysMillis)

    val validLogs = logs.filter { it.completedAt >= cycleCutoff }
    val muscleScores = mutableMapOf<MusclePart, Float>()

    for (log in validLogs) {
        val ageMillis = (now - log.completedAt).coerceAtLeast(0)
        val dayIndex = (ageMillis / TimeUnit.DAYS.toMillis(1)).toInt() // 0, 1, 2, 3

        // Decay multiplier over 4 days: Day 0 = 1.0, Day 1 = 0.75, Day 2 = 0.50, Day 3 = 0.25
        val decayFactor = when (dayIndex) {
            0 -> 1.0f
            1 -> 0.75f
            2 -> 0.50f
            3 -> 0.25f
            else -> 0.0f
        }

        // Find workout muscle group or parse title
        val matchingWorkout = workouts.find { it.workout.id == log.workoutId }
        val categoryStr = matchingWorkout?.workout?.category ?: log.workoutTitle
        val workoutMuscles = parseTargetMuscles(categoryStr)

        for ((part, intensity) in workoutMuscles) {
            val scoreToAdd = intensity * decayFactor * (log.totalSetsCompleted.coerceAtLeast(1) / 3.0f).coerceIn(0.5f, 1.2f)
            muscleScores[part] = ((muscleScores[part] ?: 0f) + scoreToAdd).coerceAtMost(1.0f)
        }
    }

    return muscleScores
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressProfileSheet(
    currentUser: UserProfile? = null,
    stats: WorkoutStats,
    logs: List<WorkoutLogEntity>,
    workouts: List<WorkoutWithExercises>,
    onDismiss: () -> Unit,
    onSignInRequested: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customCycleStart by remember { mutableLongStateOf(0L) }
    var selectedMuscleDetail by remember { mutableStateOf<MusclePart?>(null) }

    // Calculate current cycle progress (4 days)
    val now = System.currentTimeMillis()
    val cycleEffectiveStart = if (customCycleStart > 0) customCycleStart else (now - (now % TimeUnit.DAYS.toMillis(4)))
    val elapsedSinceCycleStart = now - cycleEffectiveStart
    val currentCycleDay = ((elapsedSinceCycleStart / TimeUnit.DAYS.toMillis(1)) % 4).toInt() + 1
    val millisUntilNextReset = (TimeUnit.DAYS.toMillis(4) - (elapsedSinceCycleStart % TimeUnit.DAYS.toMillis(4))).coerceAtLeast(0)
    val hoursUntilReset = TimeUnit.MILLISECONDS.toHours(millisUntilNextReset)
    val daysUntilReset = (hoursUntilReset / 24) + 1

    val heatmapData = remember(logs, workouts, customCycleStart) {
        calculate4DayMuscleHeatmap(
            logs = logs,
            workouts = workouts,
            customCycleStartTimestamp = if (customCycleStart > 0) customCycleStart else null
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FitBlack,
        dragHandle = null,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF26262B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = FitRedAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "MY PROFILE & PROGRESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = FitTextSecondary
                            )
                            Text(
                                text = "Personal Heatmap",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = FitWhite
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26262B))
                            .testTag("close_progress_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FitWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Athlete Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val isSignedIn = currentUser?.isSignedIn == true
                                val userName = currentUser?.name ?: "Guest Athlete"
                                val userPhoto = currentUser?.photoUrl
                                val userEmail = currentUser?.email

                                if (isSignedIn && !userPhoto.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = userPhoto,
                                        contentDescription = userName,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, FitRedAccent, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(FitRedAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = FitWhite
                                            )
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = userName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FitWhite
                                        )
                                    )
                                    Text(
                                        text = if (isSignedIn) userEmail ?: "Signed in via Google" else "Guest Athlete",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FitTextSecondary
                                    )
                                }
                            }

                            if (currentUser?.isSignedIn == true) {
                                OutlinedButton(
                                    onClick = onSignOut,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383842))
                                ) {
                                    Text("Sign Out", color = FitTextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Button(
                                    onClick = onSignInRequested,
                                    colors = ButtonDefaults.buttonColors(containerColor = FitWhite, contentColor = FitBlack),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Sign In", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Quick Stats Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1B1B22))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${logs.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = FitWhite))
                                Text(text = "Workouts", style = MaterialTheme.typography.labelSmall, color = FitTextSecondary)
                            }
                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFF2E2E38)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${stats.setsTotalSoFar}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFFF59E0B)))
                                Text(text = "Total Sets", style = MaterialTheme.typography.labelSmall, color = FitTextSecondary)
                            }
                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFF2E2E38)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${stats.streakDays}d", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = FitRedAccent))
                                Text(text = "Streak", style = MaterialTheme.typography.labelSmall, color = FitTextSecondary)
                            }
                        }
                    }
                }
            }

            // 4-Day Reset Cycle Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191811)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3816))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timelapse,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "4-Day Heatmap Reset Cycle",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2E230B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Day $currentCycleDay / 4",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Your muscular heatmap dynamically tracks fatigue and recovery over 4 days. Muscle load decays gradually and resets every 4 days to ensure optimal muscle group recovery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitWhite.copy(alpha = 0.85f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next auto reset in ~$daysUntilReset days ($hoursUntilReset hrs)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = FitTextSecondary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        customCycleStart = System.currentTimeMillis()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Reset Heatmap",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Anatome Heatmap Canvas (Anterior & Posterior)
            item {
                InteractiveBodyHeatmapDisplay(
                    muscleIntensities = heatmapData,
                    onMuscleSelected = { selectedMuscleDetail = it }
                )
            }

            // Muscle Recovery Status Breakdown
            item {
                Text(
                    text = "TARGETED MUSCLE GROUPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = FitTextSecondary
                )
            }

            // Grouping cards: Upper Body, Core, Lower Body
            val upperMuscles = listOf(MusclePart.CHEST, MusclePart.SHOULDERS_FRONT, MusclePart.BICEPS, MusclePart.TRICEPS, MusclePart.LATS, MusclePart.UPPER_BACK)
            val coreMuscles = listOf(MusclePart.ABS, MusclePart.OBLIQUES, MusclePart.LOWER_BACK)
            val lowerMuscles = listOf(MusclePart.QUADS, MusclePart.HAMSTRINGS, MusclePart.GLUTES, MusclePart.CALVES)

            item {
                MuscleCategoryCard(
                    categoryTitle = "Upper Body & Arms",
                    muscles = upperMuscles,
                    intensities = heatmapData
                )
            }

            item {
                MuscleCategoryCard(
                    categoryTitle = "Core & Abdominals",
                    muscles = coreMuscles,
                    intensities = heatmapData
                )
            }

            item {
                MuscleCategoryCard(
                    categoryTitle = "Lower Body & Legs",
                    muscles = lowerMuscles,
                    intensities = heatmapData
                )
            }

            // Heatmap Logs History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEATMAP WORKOUT LOGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = FitTextSecondary
                    )

                    Text(
                        text = "${logs.size} recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitTextMuted
                    )
                }
            }

            if (logs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = FitTextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No workouts logged yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = FitWhite
                            )
                            Text(
                                text = "Complete an exercise session to light up your personal muscle heatmap!",
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTextMuted
                            )
                        }
                    }
                }
            } else {
                items(logs.take(10), key = { it.id }) { log ->
                    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
                    val dateStr = dateFormat.format(Date(log.completedAt))
                    val targetMuscles = parseTargetMuscles(log.workoutTitle)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262630))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.workoutTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FitWhite
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${log.durationSeconds / 60}m ${log.durationSeconds % 60}s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = FitRedAccent
                                )
                            }

                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = FitTextSecondary
                            )

                            // Muscle tags
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                targetMuscles.keys.take(3).forEach { muscle ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF22222A))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = muscle.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF22222A))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${log.totalSetsCompleted} sets • ${log.totalRepsCompleted} reps",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = FitWhite
                                    )
                                }
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
}

@Composable
private fun MuscleCategoryCard(
    categoryTitle: String,
    muscles: List<MusclePart>,
    intensities: Map<MusclePart, Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = categoryTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FitWhite
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                muscles.forEach { muscle ->
                    val load = ((intensities[muscle] ?: 0f) * 100).toInt()
                    val (statusText, statusColor) = when {
                        load >= 80 -> "Peak Load (Fatigued)" to Color(0xFFF59E0B)
                        load >= 40 -> "Active (Recovering)" to Color(0xFFFB923C)
                        load > 0 -> "Light Load (Almost Ready)" to Color(0xFF60A5FA)
                        else -> "Fresh (100% Rested)" to Color(0xFF4ADE80)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = muscle.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = FitWhite
                                )
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = statusColor
                            )
                        }

                        // Load gauge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22222A))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$load%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
