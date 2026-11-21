package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite

/**
 * Helper to determine primary muscle group and perspective (Front / Back) for an exercise.
 */
fun getExerciseMuscleProfile(exerciseName: String, muscleGroup: String): Pair<MusclePart, Boolean> {
    val muscleMap = parseTargetMuscles("$exerciseName $muscleGroup")
    val highestEntry = muscleMap.maxByOrNull { it.value }
    val primaryPart = highestEntry?.key ?: MusclePart.CHEST
    val isFront = primaryPart.isFront
    return Pair(primaryPart, isFront)
}

/**
 * Anatome Workout Visual Box - Renders the exact anatomical muscle highlight visual
 * inspired by Rippy1911/anatome & ExerciseDB anatomy diagrams.
 * Replaces generic video frame screenshots with accurate anatomical diagrams.
 */
@Composable
fun AnatomeExerciseVisualBox(
    exerciseName: String,
    muscleGroup: String = "",
    modifier: Modifier = Modifier,
    showMuscleBadge: Boolean = true,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val muscleMap = remember(exerciseName, muscleGroup) {
        parseTargetMuscles("$exerciseName $muscleGroup")
    }
    val (primaryMuscle, isFrontView) = remember(exerciseName, muscleGroup) {
        getExerciseMuscleProfile(exerciseName, muscleGroup)
    }

    val primaryLabel = remember(primaryMuscle) {
        when (primaryMuscle) {
            MusclePart.CHEST -> "CHEST"
            MusclePart.SHOULDERS_FRONT, MusclePart.SHOULDERS_REAR -> "DELTS"
            MusclePart.BICEPS -> "BICEPS"
            MusclePart.TRICEPS -> "TRICEPS"
            MusclePart.FOREARMS -> "ARMS"
            MusclePart.ABS -> "ABS"
            MusclePart.OBLIQUES -> "CORE"
            MusclePart.UPPER_BACK -> "TRAPS"
            MusclePart.LATS -> "LATS"
            MusclePart.LOWER_BACK -> "BACK"
            MusclePart.GLUTES -> "GLUTES"
            MusclePart.QUADS -> "QUADS"
            MusclePart.HAMSTRINGS -> "HAMS"
            MusclePart.CALVES -> "CALVES"
        }
    }

    val boxModifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1C1C22),
                    Color(0xFF121216)
                )
            )
        )
        .then(
            if (isSelected) {
                Modifier.border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
            } else {
                Modifier.border(1.dp, Color(0xFF26262E), RoundedCornerShape(12.dp))
            }
        )
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        // Anatomical Body Model centered in the square tile
        AnatomyMannequinCanvas(
            isFront = isFrontView,
            muscleIntensities = muscleMap,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )

        // Bottom Target Muscle Pill Badge
        if (showMuscleBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xE6101014))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = FitWhite
                    )
                }
            }
        }
    }
}

/**
 * Anatome Exercise Anatomy Breakdown Modal Dialog
 * Displays dual front/back body mannequins with highlighted muscles and load percentages.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomeExerciseDetailDialog(
    exerciseName: String,
    muscleGroup: String,
    durationSeconds: Int,
    restSeconds: Int,
    reps: String,
    onWatchVideoDemo: () -> Unit,
    onDismiss: () -> Unit
) {
    val muscleMap = remember(exerciseName, muscleGroup) {
        parseTargetMuscles("$exerciseName $muscleGroup")
    }
    val activeMuscles = remember(muscleMap) {
        MusclePart.values().filter { (muscleMap[it] ?: 0f) > 0.1f }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Anatome Tag & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ANATOME MUSCLE MAP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFFFBBF24)
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FitWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Exercise Title
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = FitWhite,
                    textAlign = TextAlign.Center
                )

                // Timing & Form Pills
                val timingText = if (durationSeconds > 0) {
                    "$durationSeconds Seconds Work • $restSeconds Seconds Rest"
                } else {
                    "$reps • $restSeconds Seconds Rest"
                }
                Text(
                    text = timingText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = FitTextSecondary
                )

                // Dual Front & Back Anatomical Model Visualization
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                    border = BorderStroke(1.dp, Color(0xFF26262E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnatomyMannequinCanvas(
                                isFront = true,
                                muscleIntensities = muscleMap,
                                modifier = Modifier.size(width = 80.dp, height = 130.dp)
                            )
                            Text(
                                text = "ANTERIOR (FRONT)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = FitTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(120.dp)
                                .background(Color(0xFF222228))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnatomyMannequinCanvas(
                                isFront = false,
                                muscleIntensities = muscleMap,
                                modifier = Modifier.size(width = 80.dp, height = 130.dp)
                            )
                            Text(
                                text = "POSTERIOR (BACK)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = FitTextSecondary
                            )
                        }
                    }
                }

                // Active Targeted Muscle Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeMuscles.forEach { part ->
                        val load = ((muscleMap[part] ?: 0f) * 100).toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF222228))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (load >= 80) Color(0xFFF59E0B) else Color(0xFFFB923C))
                                )
                                Text(
                                    text = "${part.displayName} • $load%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = FitWhite
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onWatchVideoDemo()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FitWhite, contentColor = FitBlack)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Watch Video Timestamp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF18181C)
    )
}
