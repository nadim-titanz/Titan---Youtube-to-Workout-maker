package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkoutLogEntity
import com.example.data.model.WorkoutWithExercises
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite
import java.util.concurrent.TimeUnit

enum class RecoveryStage(val color: Color, val label: String) {
    DAY_1(Color(0xFFE53935), "1 Day Ago"),
    DAYS_2_3(Color(0xFFF59E0B), "2–3 Days Ago"),
    DAYS_4_PLUS(Color(0xFFE2E8F0), "4+ Days Ago")
}

/**
 * Calculates recovery stage for each muscle part based on workout history.
 */
fun calculateMuscleRecoveryStages(
    logs: List<WorkoutLogEntity>,
    workouts: List<WorkoutWithExercises>
): Map<MusclePart, RecoveryStage> {
    val now = System.currentTimeMillis()
    val oneDayMillis = TimeUnit.DAYS.toMillis(1)
    val threeDaysMillis = TimeUnit.DAYS.toMillis(3)

    val stages = mutableMapOf<MusclePart, RecoveryStage>()

    // Default all muscles to 4+ Days Ago (Fully Recovered / Light Silver)
    MusclePart.values().forEach { stages[it] = RecoveryStage.DAYS_4_PLUS }

    // Scan recent logs from newest to oldest
    val sortedLogs = logs.sortedByDescending { it.completedAt }
    for (log in sortedLogs) {
        val age = now - log.completedAt
        val matchingWorkout = workouts.find { it.workout.id == log.workoutId }
        val categoryStr = matchingWorkout?.workout?.category ?: log.workoutTitle
        val workoutMuscles = parseTargetMuscles(categoryStr)

        val stageForThisLog = when {
            age <= oneDayMillis -> RecoveryStage.DAY_1
            age <= threeDaysMillis -> RecoveryStage.DAYS_2_3
            else -> RecoveryStage.DAYS_4_PLUS
        }

        for ((part, _) in workoutMuscles) {
            val currentStage = stages[part] ?: RecoveryStage.DAYS_4_PLUS
            // Only upgrade to a more recent status (DAY_1 > DAYS_2_3 > DAYS_4_PLUS)
            if (stageForThisLog == RecoveryStage.DAY_1) {
                stages[part] = RecoveryStage.DAY_1
            } else if (stageForThisLog == RecoveryStage.DAYS_2_3 && currentStage != RecoveryStage.DAY_1) {
                stages[part] = RecoveryStage.DAYS_2_3
            }
        }
    }

    return stages
}

/**
 * High-fidelity Anatomical Body Canvas on a 3D-styled dark pedestal platform.
 * Directly matches the screenshot requested by the user.
 */
@Composable
fun PersonalHeatmapPedestalCard(
    logs: List<WorkoutLogEntity>,
    workouts: List<WorkoutWithExercises>,
    modifier: Modifier = Modifier
) {
    val recoveryStages = remember(logs, workouts) {
        calculateMuscleRecoveryStages(logs, workouts)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222226))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Personal Heatmap" Pill Header
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF28282E))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Personal Heatmap",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = FitWhite
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Figures on Pedestal Platform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background Pedestal Mat Platform
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val w = size.width
                    val h = size.height

                    // Pedestal Base shadow
                    drawRoundRect(
                        color = Color(0xFF0D0D10),
                        topLeft = Offset(w * 0.05f, h * 0.2f),
                        size = Size(w * 0.9f, h * 0.75f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )

                    // Pedestal Top Surface
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF24242A), Color(0xFF1C1C20))
                        ),
                        topLeft = Offset(w * 0.05f, h * 0.1f),
                        size = Size(w * 0.9f, h * 0.7f),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                }

                // Front & Back Anatomical Figures
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Anterior (Front) View
                    AnatomicalFigureView(
                        isFront = true,
                        recoveryStages = recoveryStages,
                        modifier = Modifier
                            .weight(1f)
                            .height(210.dp)
                    )

                    // Posterior (Back) View
                    AnatomicalFigureView(
                        isFront = false,
                        recoveryStages = recoveryStages,
                        modifier = Modifier
                            .weight(1f)
                            .height(210.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Stage Recovery Legend Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecoveryLegendPill(
                    color = RecoveryStage.DAY_1.color,
                    label = RecoveryStage.DAY_1.label
                )

                RecoveryLegendPill(
                    color = RecoveryStage.DAYS_2_3.color,
                    label = RecoveryStage.DAYS_2_3.label
                )

                RecoveryLegendPill(
                    color = RecoveryStage.DAYS_4_PLUS.color,
                    label = RecoveryStage.DAYS_4_PLUS.label
                )
            }
        }
    }
}

@Composable
private fun RecoveryLegendPill(
    color: Color,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF222228))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = FitWhite
            )
        }
    }
}

@Composable
private fun AnatomicalFigureView(
    isFront: Boolean,
    recoveryStages: Map<MusclePart, RecoveryStage>,
    modifier: Modifier = Modifier
) {
    val baseBodyColor = Color(0xFF2C2C34)
    val bodyOutlineColor = Color(0xFF1C1C22)

    fun getMuscleColor(part: MusclePart): Color {
        val stage = recoveryStages[part] ?: RecoveryStage.DAYS_4_PLUS
        return when (stage) {
            RecoveryStage.DAY_1 -> Color(0xFFEF4444) // 1 Day Ago - Red
            RecoveryStage.DAYS_2_3 -> Color(0xFFF59E0B) // 2-3 Days Ago - Amber Gold
            RecoveryStage.DAYS_4_PLUS -> baseBodyColor // 4+ Days Ago - Recovered Slate
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Head
        drawCircle(
            color = baseBodyColor,
            radius = w * 0.13f,
            center = Offset(w * 0.5f, h * 0.10f)
        )
        drawCircle(
            color = bodyOutlineColor,
            radius = w * 0.13f,
            center = Offset(w * 0.5f, h * 0.10f),
            style = Stroke(width = 1.5f)
        )

        // 2. Neck
        val neckColor = if (!isFront) getMuscleColor(MusclePart.UPPER_BACK) else baseBodyColor
        drawRoundRect(
            color = neckColor,
            topLeft = Offset(w * 0.44f, h * 0.17f),
            size = Size(w * 0.12f, h * 0.06f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        if (isFront) {
            // FRONT VIEW MUSCLES

            // Trapezius / Neck base
            val trapsPath = Path().apply {
                moveTo(w * 0.38f, h * 0.20f)
                lineTo(w * 0.62f, h * 0.20f)
                lineTo(w * 0.68f, h * 0.24f)
                lineTo(w * 0.32f, h * 0.24f)
                close()
            }
            drawPath(trapsPath, color = getMuscleColor(MusclePart.UPPER_BACK))

            // Shoulders (Front Deltoids - Left & Right)
            val deltLeft = Path().apply {
                moveTo(w * 0.20f, h * 0.24f)
                lineTo(w * 0.33f, h * 0.24f)
                lineTo(w * 0.30f, h * 0.33f)
                lineTo(w * 0.18f, h * 0.31f)
                close()
            }
            drawPath(deltLeft, color = getMuscleColor(MusclePart.SHOULDERS_FRONT))

            val deltRight = Path().apply {
                moveTo(w * 0.80f, h * 0.24f)
                lineTo(w * 0.67f, h * 0.24f)
                lineTo(w * 0.70f, h * 0.33f)
                lineTo(w * 0.82f, h * 0.31f)
                close()
            }
            drawPath(deltRight, color = getMuscleColor(MusclePart.SHOULDERS_FRONT))

            // Chest (Pectorals Left & Right)
            val chestLeft = Path().apply {
                moveTo(w * 0.33f, h * 0.24f)
                lineTo(w * 0.49f, h * 0.24f)
                lineTo(w * 0.49f, h * 0.35f)
                lineTo(w * 0.31f, h * 0.35f)
                close()
            }
            drawPath(chestLeft, color = getMuscleColor(MusclePart.CHEST))

            val chestRight = Path().apply {
                moveTo(w * 0.51f, h * 0.24f)
                lineTo(w * 0.67f, h * 0.24f)
                lineTo(w * 0.69f, h * 0.35f)
                lineTo(w * 0.51f, h * 0.35f)
                close()
            }
            drawPath(chestRight, color = getMuscleColor(MusclePart.CHEST))

            // Upper & Lower Abs (Core Center)
            val absPath = Path().apply {
                moveTo(w * 0.38f, h * 0.36f)
                lineTo(w * 0.62f, h * 0.36f)
                lineTo(w * 0.59f, h * 0.53f)
                lineTo(w * 0.41f, h * 0.53f)
                close()
            }
            drawPath(absPath, color = getMuscleColor(MusclePart.ABS))

            // Obliques (Sides of Core)
            val obliqueLeft = Path().apply {
                moveTo(w * 0.30f, h * 0.36f)
                lineTo(w * 0.37f, h * 0.36f)
                lineTo(w * 0.40f, h * 0.53f)
                lineTo(w * 0.33f, h * 0.51f)
                close()
            }
            drawPath(obliqueLeft, color = getMuscleColor(MusclePart.OBLIQUES))

            val obliqueRight = Path().apply {
                moveTo(w * 0.70f, h * 0.36f)
                lineTo(w * 0.63f, h * 0.36f)
                lineTo(w * 0.60f, h * 0.53f)
                lineTo(w * 0.67f, h * 0.51f)
                close()
            }
            drawPath(obliqueRight, color = getMuscleColor(MusclePart.OBLIQUES))

            // Biceps (Arms Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.BICEPS),
                topLeft = Offset(w * 0.15f, h * 0.33f),
                size = Size(w * 0.12f, h * 0.14f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.BICEPS),
                topLeft = Offset(w * 0.73f, h * 0.33f),
                size = Size(w * 0.12f, h * 0.14f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Forearms (Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.FOREARMS),
                topLeft = Offset(w * 0.12f, h * 0.48f),
                size = Size(w * 0.11f, h * 0.16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.FOREARMS),
                topLeft = Offset(w * 0.77f, h * 0.48f),
                size = Size(w * 0.11f, h * 0.16f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Pelvis / Hips
            drawRoundRect(
                color = baseBodyColor,
                topLeft = Offset(w * 0.36f, h * 0.54f),
                size = Size(w * 0.28f, h * 0.06f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Quadriceps (Front Thighs Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.QUADS),
                topLeft = Offset(w * 0.27f, h * 0.60f),
                size = Size(w * 0.20f, h * 0.22f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.QUADS),
                topLeft = Offset(w * 0.53f, h * 0.60f),
                size = Size(w * 0.20f, h * 0.22f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Calves Front (Tibialis / Shin & Calves)
            drawRoundRect(
                color = getMuscleColor(MusclePart.CALVES),
                topLeft = Offset(w * 0.29f, h * 0.83f),
                size = Size(w * 0.16f, h * 0.15f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.CALVES),
                topLeft = Offset(w * 0.55f, h * 0.83f),
                size = Size(w * 0.16f, h * 0.15f),
                cornerRadius = CornerRadius(4f, 4f)
            )

        } else {
            // BACK VIEW MUSCLES

            // Upper Traps & Neck
            val upperTraps = Path().apply {
                moveTo(w * 0.35f, h * 0.20f)
                lineTo(w * 0.65f, h * 0.20f)
                lineTo(w * 0.72f, h * 0.27f)
                lineTo(w * 0.50f, h * 0.32f)
                lineTo(w * 0.28f, h * 0.27f)
                close()
            }
            drawPath(upperTraps, color = getMuscleColor(MusclePart.UPPER_BACK))

            // Rear Deltoids (Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.SHOULDERS_REAR),
                topLeft = Offset(w * 0.18f, h * 0.25f),
                size = Size(w * 0.14f, h * 0.09f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.SHOULDERS_REAR),
                topLeft = Offset(w * 0.68f, h * 0.25f),
                size = Size(w * 0.14f, h * 0.09f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Lats (Latissimus Dorsi Left & Right)
            val latLeft = Path().apply {
                moveTo(w * 0.31f, h * 0.28f)
                lineTo(w * 0.49f, h * 0.32f)
                lineTo(w * 0.49f, h * 0.46f)
                lineTo(w * 0.33f, h * 0.44f)
                close()
            }
            drawPath(latLeft, color = getMuscleColor(MusclePart.LATS))

            val latRight = Path().apply {
                moveTo(w * 0.69f, h * 0.28f)
                lineTo(w * 0.51f, h * 0.32f)
                lineTo(w * 0.51f, h * 0.46f)
                lineTo(w * 0.67f, h * 0.44f)
                close()
            }
            drawPath(latRight, color = getMuscleColor(MusclePart.LATS))

            // Lower Back (Erector Spinae)
            val lowerBack = Path().apply {
                moveTo(w * 0.37f, h * 0.46f)
                lineTo(w * 0.63f, h * 0.46f)
                lineTo(w * 0.60f, h * 0.53f)
                lineTo(w * 0.40f, h * 0.53f)
                close()
            }
            drawPath(lowerBack, color = getMuscleColor(MusclePart.LOWER_BACK))

            // Triceps (Arms Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.TRICEPS),
                topLeft = Offset(w * 0.15f, h * 0.33f),
                size = Size(w * 0.12f, h * 0.14f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.TRICEPS),
                topLeft = Offset(w * 0.73f, h * 0.33f),
                size = Size(w * 0.12f, h * 0.14f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Forearms Back
            drawRoundRect(
                color = getMuscleColor(MusclePart.FOREARMS),
                topLeft = Offset(w * 0.12f, h * 0.48f),
                size = Size(w * 0.11f, h * 0.16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.FOREARMS),
                topLeft = Offset(w * 0.77f, h * 0.48f),
                size = Size(w * 0.11f, h * 0.16f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Glutes (Gluteus Maximus Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.GLUTES),
                topLeft = Offset(w * 0.28f, h * 0.54f),
                size = Size(w * 0.20f, h * 0.14f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.GLUTES),
                topLeft = Offset(w * 0.52f, h * 0.54f),
                size = Size(w * 0.20f, h * 0.14f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Hamstrings (Rear Thighs Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.HAMSTRINGS),
                topLeft = Offset(w * 0.28f, h * 0.69f),
                size = Size(w * 0.19f, h * 0.13f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.HAMSTRINGS),
                topLeft = Offset(w * 0.53f, h * 0.69f),
                size = Size(w * 0.19f, h * 0.13f),
                cornerRadius = CornerRadius(5f, 5f)
            )

            // Calves (Gastrocnemius Left & Right)
            drawRoundRect(
                color = getMuscleColor(MusclePart.CALVES),
                topLeft = Offset(w * 0.29f, h * 0.83f),
                size = Size(w * 0.16f, h * 0.15f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = getMuscleColor(MusclePart.CALVES),
                topLeft = Offset(w * 0.55f, h * 0.83f),
                size = Size(w * 0.16f, h * 0.15f),
                cornerRadius = CornerRadius(5f, 5f)
            )
        }
    }
}
