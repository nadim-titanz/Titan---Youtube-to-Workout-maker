package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitCardBackground
import com.example.ui.theme.FitCardElevated
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite

enum class MusclePart(
    val displayName: String,
    val isFront: Boolean,
    val category: String
) {
    CHEST("Chest / Pecs", true, "Upper Body"),
    SHOULDERS_FRONT("Front Deltoids", true, "Upper Body"),
    SHOULDERS_REAR("Rear Deltoids", false, "Upper Body"),
    BICEPS("Biceps", true, "Arms"),
    TRICEPS("Triceps", false, "Arms"),
    FOREARMS("Forearms", true, "Arms"),
    ABS("Upper & Lower Abs", true, "Core"),
    OBLIQUES("Obliques", true, "Core"),
    UPPER_BACK("Trapezius / Upper Back", false, "Back"),
    LATS("Lats / Mid Back", false, "Back"),
    LOWER_BACK("Lower Back", false, "Back"),
    GLUTES("Glutes", false, "Lower Body"),
    QUADS("Quadriceps", true, "Lower Body"),
    HAMSTRINGS("Hamstrings", false, "Lower Body"),
    CALVES("Calves", false, "Lower Body")
}

/**
 * Extracts target muscle intensities (0.0f to 1.0f) from a muscle group or category string.
 */
fun parseTargetMuscles(rawText: String): Map<MusclePart, Float> {
    val text = rawText.lowercase()
    val map = mutableMapOf<MusclePart, Float>()

    fun set(part: MusclePart, intensity: Float) {
        map[part] = (map[part] ?: 0f).coerceAtLeast(intensity)
    }

    if (text.contains("chest") || text.contains("push up") || text.contains("bench") || text.contains("pec") || text.contains("push")) {
        set(MusclePart.CHEST, 1.0f)
        set(MusclePart.SHOULDERS_FRONT, 0.7f)
        set(MusclePart.TRICEPS, 0.6f)
    }
    if (text.contains("tricep") || text.contains("dip") || text.contains("diamond")) {
        set(MusclePart.TRICEPS, 1.0f)
        set(MusclePart.CHEST, 0.5f)
        set(MusclePart.SHOULDERS_FRONT, 0.5f)
    }
    if (text.contains("bicep") || text.contains("curl") || text.contains("chin up")) {
        set(MusclePart.BICEPS, 1.0f)
        set(MusclePart.FOREARMS, 0.6f)
        set(MusclePart.LATS, 0.5f)
    }
    if (text.contains("shoulder") || text.contains("deltoid") || text.contains("pike") || text.contains("press") || text.contains("raise")) {
        set(MusclePart.SHOULDERS_FRONT, 1.0f)
        set(MusclePart.SHOULDERS_REAR, 0.8f)
        set(MusclePart.TRICEPS, 0.4f)
    }
    if (text.contains("back") || text.contains("pull up") || text.contains("row") || text.contains("lat")) {
        set(MusclePart.LATS, 1.0f)
        set(MusclePart.UPPER_BACK, 0.8f)
        set(MusclePart.BICEPS, 0.6f)
        set(MusclePart.LOWER_BACK, 0.5f)
    }
    if (text.contains("trap") || text.contains("shrug") || text.contains("scapular")) {
        set(MusclePart.UPPER_BACK, 1.0f)
        set(MusclePart.SHOULDERS_REAR, 0.7f)
    }
    if (text.contains("abs") || text.contains("crunch") || text.contains("sit up") || text.contains("v-up") || text.contains("hollow") || text.contains("flutter")) {
        set(MusclePart.ABS, 1.0f)
        set(MusclePart.OBLIQUES, 0.6f)
    }
    if (text.contains("oblique") || text.contains("twist") || text.contains("side plank") || text.contains("climber") || text.contains("bicycle")) {
        set(MusclePart.OBLIQUES, 1.0f)
        set(MusclePart.ABS, 0.7f)
    }
    if (text.contains("core") || text.contains("plank") || text.contains("dead bug")) {
        set(MusclePart.ABS, 0.9f)
        set(MusclePart.OBLIQUES, 0.8f)
        set(MusclePart.LOWER_BACK, 0.5f)
    }
    if (text.contains("quad") || text.contains("squat") || text.contains("lunge") || text.contains("wall sit")) {
        set(MusclePart.QUADS, 1.0f)
        set(MusclePart.GLUTES, 0.8f)
        set(MusclePart.CALVES, 0.4f)
    }
    if (text.contains("glute") || text.contains("hip thrust") || text.contains("bridge") || text.contains("kickback")) {
        set(MusclePart.GLUTES, 1.0f)
        set(MusclePart.HAMSTRINGS, 0.7f)
    }
    if (text.contains("hamstring") || text.contains("deadlift") || text.contains("good morning")) {
        set(MusclePart.HAMSTRINGS, 1.0f)
        set(MusclePart.GLUTES, 0.8f)
        set(MusclePart.LOWER_BACK, 0.6f)
    }
    if (text.contains("calf") || text.contains("calves") || text.contains("jump") || text.contains("high knee")) {
        set(MusclePart.CALVES, 1.0f)
        set(MusclePart.QUADS, 0.5f)
    }
    if (text.contains("full body") || text.contains("hiit") || text.contains("burpee") || text.contains("cardio")) {
        set(MusclePart.CHEST, 0.7f)
        set(MusclePart.ABS, 0.8f)
        set(MusclePart.QUADS, 0.8f)
        set(MusclePart.LATS, 0.6f)
        set(MusclePart.CALVES, 0.6f)
    }

    // Default if nothing matched
    if (map.isEmpty()) {
        set(MusclePart.ABS, 0.8f)
        set(MusclePart.CHEST, 0.7f)
    }

    return map
}

@Composable
fun PersonalHeatmapBanner(
    targetCategory: String = "Core",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val muscleMap = remember(targetCategory) { parseTargetMuscles(targetCategory) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("personal_heatmap_banner"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Front & Back Muscular Anatomy Graphic with active target highlights
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnatomyMannequinCanvas(
                    isFront = true,
                    muscleIntensities = muscleMap,
                    modifier = Modifier.size(width = 46.dp, height = 70.dp)
                )
                AnatomyMannequinCanvas(
                    isFront = false,
                    muscleIntensities = muscleMap,
                    modifier = Modifier.size(width = 46.dp, height = 70.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Target Muscles",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FitTextSecondary
                        )
                    )
                    Text(
                        text = targetCategory.ifBlank { "Full Body" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FitWhite
                        )
                    )
                }
            }

            // Right Pill: "Personal Heatmap" with Arrow
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF26262B))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Heatmap",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FitWhite
                    )
                )

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF383840)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Heatmap",
                        tint = FitWhite,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * High-fidelity Anatomical Body Map rendering distinct muscle groups (Chest, Deltoids, Biceps, Abs, Lats, Quads, etc.)
 */
@Composable
fun AnatomyMannequinCanvas(
    isFront: Boolean,
    muscleIntensities: Map<MusclePart, Float> = emptyMap(),
    onMuscleClicked: ((MusclePart) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Base Slate Mannequin Tone
    val baseBodyColor = Color(0xFF323238)
    val bodyOutlineColor = Color(0xFF1E1E22)

    // Color gradient for intensities (Amber Gold / Warm Orange / Crimson)
    fun getMuscleColor(part: MusclePart): Color {
        val intensity = muscleIntensities[part] ?: 0f
        return when {
            intensity >= 0.85f -> Color(0xFFF59E0B) // Amber Glow / Peak Load
            intensity >= 0.60f -> Color(0xFFFB923C) // Warm Orange / High Load
            intensity >= 0.35f -> Color(0xFFEA580C) // Moderate Load
            intensity > 0.05f -> Color(0xFFB45309) // Light Load
            else -> baseBodyColor
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

/**
 * Interactive Anatome-inspired Dual Body Heatmap Screen Component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveBodyHeatmapDisplay(
    muscleIntensities: Map<MusclePart, Float>,
    onMuscleSelected: (MusclePart) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedViewMode by remember { mutableStateOf("BOTH") } // "FRONT", "BACK", "BOTH"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF26262E), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector (Front / Back / Both)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BODY HEATMAP ANATOMY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = FitTextSecondary
                )
                Text(
                    text = "Muscular Load Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FitWhite
                    )
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222228))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("BOTH" to "All", "FRONT" to "Front", "BACK" to "Back").forEach { (mode, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedViewMode == mode) FitWhite else Color.Transparent)
                            .clickable { selectedViewMode = mode }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedViewMode == mode) FitBlack else FitTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Mannequins Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0C0C0E)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedViewMode == "BOTH" || selectedViewMode == "FRONT") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnatomyMannequinCanvas(
                            isFront = true,
                            muscleIntensities = muscleIntensities,
                            modifier = Modifier.size(width = 110.dp, height = 180.dp)
                        )
                        Text(
                            text = "ANTERIOR (FRONT)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = FitTextSecondary
                        )
                    }
                }

                if (selectedViewMode == "BOTH" || selectedViewMode == "BACK") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnatomyMannequinCanvas(
                            isFront = false,
                            muscleIntensities = muscleIntensities,
                            modifier = Modifier.size(width = 110.dp, height = 180.dp)
                        )
                        Text(
                            text = "POSTERIOR (BACK)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = FitTextSecondary
                        )
                    }
                }
            }
        }

        // Heatmap Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF323238)))
                Text("Rested (0%)", style = MaterialTheme.typography.labelSmall, color = FitTextMuted)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEA580C)))
                Text("Active (50%)", style = MaterialTheme.typography.labelSmall, color = FitTextSecondary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                Text("Peak Load (100%)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF59E0B))
            }
        }

        // Active Muscle Chips
        val activeMuscles = MusclePart.values().filter { (muscleIntensities[it] ?: 0f) > 0.1f }
        if (activeMuscles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeMuscles.forEach { part ->
                    val load = ((muscleIntensities[part] ?: 0f) * 100).toInt()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202026))
                            .clickable { onMuscleSelected(part) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (load >= 80) Color(0xFFF59E0B) else Color(0xFFFB923C))
                            )
                            Text(
                                text = "${part.displayName} • $load%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = FitWhite
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
