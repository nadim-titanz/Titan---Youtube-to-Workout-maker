package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.auth.UserProfile
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.model.WorkoutWithExercises
import com.example.data.remote.YoutubeMetadataService
import com.example.ui.components.AnatomeExerciseDetailDialog
import com.example.ui.components.AnatomeExerciseVisualBox
import com.example.ui.components.EmbeddedWorkoutVideoPlayer
import com.example.ui.components.PersonalHeatmapBanner
import com.example.ui.components.PlayerEngineMode
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitCardBackground
import com.example.ui.theme.FitCardElevated
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite

@Composable
fun WorkoutDetailScreen(
    workoutWithExercises: WorkoutWithExercises,
    onBack: () -> Unit,
    onStartWorkout: (autoToggle: Boolean) -> Unit,
    onOpenHeatmapProgress: () -> Unit = {},
    currentUser: UserProfile? = null,
    onSignInRequested: () -> Unit = {},
    playerMode: PlayerEngineMode = PlayerEngineMode.CLEAN_VIDEO,
    onPlayerModeChanged: (PlayerEngineMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Intercept hardware/gesture back press to return to main screen instead of exiting the app
    BackHandler(enabled = true) {
        onBack()
    }

    val context = LocalContext.current
    val workout = workoutWithExercises.workout
    val exercises = workoutWithExercises.sortedExercises

    var autoToggleExercise by remember { mutableStateOf(true) }
    var useAnatomeVisuals by remember { mutableStateOf(true) }
    var selectedExerciseForPreview by remember { mutableStateOf<WorkoutExerciseEntity?>(null) }
    var showAnatomeDetailDialog by remember { mutableStateOf(false) }

    // Anatome Exercise Anatomy Breakdown Dialog
    if (showAnatomeDetailDialog && selectedExerciseForPreview != null) {
        val ex = selectedExerciseForPreview!!
        AnatomeExerciseDetailDialog(
            exerciseName = ex.name,
            muscleGroup = ex.muscleGroup.ifBlank { workout.category },
            durationSeconds = ex.durationSeconds,
            restSeconds = ex.restSeconds,
            reps = ex.reps,
            onWatchVideoDemo = {
                // Keep selectedExerciseForPreview set so EmbeddedWorkoutVideoPlayer at top previews this exercise timestamp
            },
            onDismiss = {
                showAnatomeDetailDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FitBlack,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FitBlack)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { onStartWorkout(autoToggleExercise) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_workout_full_btn"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitWhite,
                        contentColor = FitBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Workout",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Navigation & Tags
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FitCardElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = FitWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Difficulty Pill Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2C2C2E))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = workout.difficulty.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = FitWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        ),
                        color = FitWhite
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Toggle Exercise Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto-Toggle Exercise",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = FitWhite
                        )

                        Switch(
                            checked = autoToggleExercise,
                            onCheckedChange = { autoToggleExercise = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FitWhite,
                                checkedTrackColor = Color(0xFF38383A),
                                uncheckedThumbColor = FitTextMuted,
                                uncheckedTrackColor = Color(0xFF1C1C1E)
                            )
                        )
                    }
                }
            }

            // Embedded Video Player Preview OR Hero Image Banner
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (selectedExerciseForPreview != null && workout.youtubeVideoId.isNotEmpty()) {
                        EmbeddedWorkoutVideoPlayer(
                            videoId = workout.youtubeVideoId,
                            timestampSeconds = selectedExerciseForPreview?.timestampSeconds ?: 0,
                            exerciseName = selectedExerciseForPreview?.name ?: workout.title,
                            timestampFormatted = selectedExerciseForPreview?.timestampFormatted,
                            currentUser = currentUser,
                            onSignInRequested = onSignInRequested,
                            currentMode = playerMode,
                            onModeChanged = onPlayerModeChanged,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Hero Image Banner matching layout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(FitCardBackground)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(workout.coverImageUrl.ifEmpty { YoutubeMetadataService.getThumbnailUrl(workout.youtubeVideoId) })
                                    .crossfade(true)
                                    .build(),
                                contentDescription = workout.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Personal Heatmap Banner
            item {
                PersonalHeatmapBanner(
                    targetCategory = workout.category,
                    onClick = onOpenHeatmapProgress,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Round 1 Header Banner with Sets tag & Anatome Visuals Mode Toggle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Round 1",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FitWhite
                            )

                            Text(
                                text = "•",
                                color = FitTextSecondary
                            )

                            Text(
                                text = "${exercises.firstOrNull()?.sets ?: 1} Sets",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = FitWhite
                            )
                        }

                        // Anatome vs Video Visual Switcher Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF24242A))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (useAnatomeVisuals) Color(0xFFF59E0B) else Color.Transparent)
                                    .clickable { useAnatomeVisuals = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Anatome",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (useAnatomeVisuals) FitBlack else FitTextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!useAnatomeVisuals) Color(0xFF383842) else Color.Transparent)
                                    .clickable { useAnatomeVisuals = false }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Video",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (!useAnatomeVisuals) FitWhite else FitTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Exercise List Items with Anatome Workout Visuals
            itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, exercise ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedExerciseForPreview = exercise
                                showAnatomeDetailDialog = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Square Visual Frame (Anatome Muscle Mannequin OR Video Thumbnail)
                        if (useAnatomeVisuals) {
                            AnatomeExerciseVisualBox(
                                exerciseName = exercise.name,
                                muscleGroup = exercise.muscleGroup.ifBlank { workout.category },
                                isSelected = selectedExerciseForPreview?.id == exercise.id,
                                modifier = Modifier.size(72.dp),
                                onClick = {
                                    selectedExerciseForPreview = exercise
                                    showAnatomeDetailDialog = true
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF202024))
                            ) {
                                val vId = exercise.videoId.ifEmpty { workout.youtubeVideoId }
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(YoutubeMetadataService.getThumbnailUrl(vId, "hqdefault"))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = exercise.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Exercise Title & Form Details Subtitle
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = FitWhite
                            )

                            val timingText = if (exercise.durationSeconds > 0) {
                                "${exercise.durationSeconds} Seconds • Rest ${exercise.restSeconds} seconds"
                            } else {
                                "${exercise.reps} • Rest ${exercise.restSeconds} seconds"
                            }

                            Text(
                                text = timingText,
                                style = MaterialTheme.typography.bodySmall,
                                color = FitTextSecondary
                            )
                        }
                    }

                    if (index < exercises.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFF1F1F24),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 88.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
