package com.example.ui.player

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.WorkoutWithExercises
import com.example.data.remote.YoutubeMetadataService
import com.example.ui.components.AnatomeExerciseVisualBox
import com.example.ui.components.EmbeddedWorkoutVideoPlayer
import com.example.ui.components.MusclePart
import com.example.ui.components.PlayerEngineMode
import com.example.ui.components.parseTargetMuscles
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite
import kotlinx.coroutines.delay

enum class ActivePlayerPhase {
    WORK, REST, FINISHED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveWorkoutPlayer(
    workoutWithExercises: WorkoutWithExercises,
    initialAutoToggle: Boolean = true,
    onClose: () -> Unit,
    onFinishSession: (workoutId: Long, workoutTitle: String, durationSeconds: Int, totalSets: Int, totalReps: Int, totalExercises: Int) -> Unit,
    currentUser: UserProfile? = null,
    onSignInRequested: () -> Unit = {},
    playerMode: PlayerEngineMode = PlayerEngineMode.CLEAN_VIDEO,
    onPlayerModeChanged: (PlayerEngineMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLeaveConfirmationDialog by remember { mutableStateOf(false) }

    // Intercept back navigation so user doesn't get kicked out accidentally
    BackHandler(enabled = true) {
        showLeaveConfirmationDialog = true
    }

    if (showLeaveConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmationDialog = false },
            title = {
                Text(
                    text = "Leave Workout?",
                    fontWeight = FontWeight.Bold,
                    color = FitWhite
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to stop this workout session and return to the main screen?",
                    color = FitTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmationDialog = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FitRedAccent)
                ) {
                    Text("Leave", color = FitWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmationDialog = false }) {
                    Text("Stay", color = FitWhite, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1C1C1E)
        )
    }

    val context = LocalContext.current
    val workout = workoutWithExercises.workout
    val exercises = workoutWithExercises.sortedExercises

    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(ActivePlayerPhase.WORK) }
    var autoToggle by remember { mutableStateOf(initialAutoToggle) }
    var isSoundEnabled by remember { mutableStateOf(true) }
    var isVideoPlaying by remember { mutableStateOf(true) }
    var isWorkFinishedWaitingForComplete by remember { mutableStateOf(false) }

    val currentExercise = exercises.getOrNull(currentExerciseIndex) ?: exercises.firstOrNull()
    val nextExercise = exercises.getOrNull(currentExerciseIndex + 1)
    val prevExercise = exercises.getOrNull(currentExerciseIndex - 1)

    val currentWorkDuration = remember(currentExercise) {
        if ((currentExercise?.durationSeconds ?: 0) > 0) currentExercise!!.durationSeconds else 40
    }

    var workElapsedSeconds by remember { mutableIntStateOf(0) }
    var restTimerSeconds by remember { mutableIntStateOf(currentExercise?.restSeconds ?: 20) }
    var totalElapsedSeconds by remember { mutableIntStateOf(0) }
    var completedSetsCount by remember { mutableIntStateOf(0) }
    var completedRepsCount by remember { mutableIntStateOf(0) }

    // Audio feedback
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
        }
    }

    // Elapsed session total timer loop
    LaunchedEffect(phase) {
        while (phase != ActivePlayerPhase.FINISHED) {
            delay(1000L)
            totalElapsedSeconds++
        }
    }

    // Function to cleanly advance to the next exercise or finish
    fun advanceToNextExercise() {
        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
            val nextEx = exercises[currentExerciseIndex]
            restTimerSeconds = if (nextEx.restSeconds > 0) nextEx.restSeconds else 20
            workElapsedSeconds = 0
            isWorkFinishedWaitingForComplete = false
            isVideoPlaying = true
            phase = ActivePlayerPhase.WORK
            if (isSoundEnabled) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
                } catch (e: Exception) {}
            }
        } else {
            phase = ActivePlayerPhase.FINISHED
            isVideoPlaying = false
            if (isSoundEnabled) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                } catch (e: Exception) {}
            }
            onFinishSession(
                workout.id,
                workout.title,
                totalElapsedSeconds,
                completedSetsCount,
                completedRepsCount,
                exercises.size
            )
        }
    }

    // Function to step back to the previous exercise
    fun goToPreviousExercise() {
        if (currentExerciseIndex > 0) {
            currentExerciseIndex--
            val prevEx = exercises[currentExerciseIndex]
            restTimerSeconds = if (prevEx.restSeconds > 0) prevEx.restSeconds else 20
            workElapsedSeconds = 0
            isWorkFinishedWaitingForComplete = false
            isVideoPlaying = true
            phase = ActivePlayerPhase.WORK
            if (isSoundEnabled) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
                } catch (e: Exception) {}
            }
        }
    }

    // Work exercise timer loop & Auto Toggle handling
    LaunchedEffect(phase, currentExerciseIndex, autoToggle, isWorkFinishedWaitingForComplete) {
        if (phase == ActivePlayerPhase.WORK && !isWorkFinishedWaitingForComplete) {
            while (phase == ActivePlayerPhase.WORK && !isWorkFinishedWaitingForComplete) {
                delay(1000L)
                workElapsedSeconds++

                // When workout timestamp / work duration finishes:
                if (workElapsedSeconds >= currentWorkDuration) {
                    if (isSoundEnabled) {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                        } catch (e: Exception) {}
                    }

                    if (autoToggle) {
                        // SCENARIO 2 (Auto Toggle = ON):
                        // Auto move to rest time, then next workout after rest one finishes while video keeps playing
                        completedSetsCount++
                        val numReps = currentExercise?.reps?.filter { it.isDigit() }?.toIntOrNull() ?: 10
                        completedRepsCount += numReps

                        val restSec = currentExercise?.restSeconds ?: 20
                        if (restSec > 0 && currentExerciseIndex < exercises.size - 1) {
                            restTimerSeconds = restSec
                            phase = ActivePlayerPhase.REST
                        } else {
                            advanceToNextExercise()
                        }
                    } else {
                        // SCENARIO 1 (Auto Toggle = OFF):
                        // Pause the video when workout timestamp has finished.
                        // Waiting for user to click "Complete Workout"
                        isVideoPlaying = false
                        isWorkFinishedWaitingForComplete = true
                    }
                    break
                }
            }
        }
    }

    // Rest countdown loop
    LaunchedEffect(phase, currentExerciseIndex) {
        if (phase == ActivePlayerPhase.REST) {
            while (restTimerSeconds > 0 && phase == ActivePlayerPhase.REST) {
                delay(1000L)
                restTimerSeconds--
                if (restTimerSeconds in 1..3 && isSoundEnabled) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    } catch (e: Exception) {}
                }
            }
            if (restTimerSeconds <= 0 && phase == ActivePlayerPhase.REST) {
                // Rest period finished!
                // Auto resume video & move to next workout
                advanceToNextExercise()
            }
        }
    }

    fun completeCurrentExercise() {
        completedSetsCount++
        val numReps = currentExercise?.reps?.filter { it.isDigit() }?.toIntOrNull() ?: 10
        completedRepsCount += numReps

        if (isSoundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
            } catch (e: Exception) {}
        }

        // If there are more exercises, go to REST
        if (currentExerciseIndex < exercises.size - 1) {
            val restSec = currentExercise?.restSeconds ?: 20
            if (restSec > 0) {
                restTimerSeconds = restSec
                phase = ActivePlayerPhase.REST
            } else {
                advanceToNextExercise()
            }
        } else {
            // Final exercise completed
            phase = ActivePlayerPhase.FINISHED
            isVideoPlaying = false
            onFinishSession(
                workout.id,
                workout.title,
                totalElapsedSeconds,
                completedSetsCount,
                completedRepsCount,
                exercises.size
            )
        }
    }

    val elapsedMins = totalElapsedSeconds / 60
    val elapsedSecs = totalElapsedSeconds % 60
    val formattedTime = "%02d:%02d".format(elapsedMins, elapsedSecs)

    val currentSetLabel = "Exercise ${currentExerciseIndex + 1} of ${exercises.size}"
    val targetMuscles = remember(currentExercise) {
        parseTargetMuscles(currentExercise?.muscleGroup?.ifBlank { currentExercise.name } ?: workout.category)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FitBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar & Navigation Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Close, Title + Elapsed Time, Previous & Sound toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { showLeaveConfirmationDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FitWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = workout.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = FitWhite,
                            maxLines = 1
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = FitTextSecondary
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Previous Exercise Button (CRITICAL User Request)
                        IconButton(
                            onClick = { goToPreviousExercise() },
                            enabled = currentExerciseIndex > 0,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (currentExerciseIndex > 0) Color(0xFF26262B) else Color(0xFF18181A))
                                .testTag("previous_workout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Workout",
                                tint = if (currentExerciseIndex > 0) FitWhite else FitTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Sound Toggle
                        IconButton(
                            onClick = { isSoundEnabled = !isSoundEnabled },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Sound",
                                tint = FitWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Segmented Progress Bar Dashes across top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    exercises.forEachIndexed { index, _ ->
                        val barColor = when {
                            index < currentExerciseIndex -> FitWhite
                            index == currentExerciseIndex -> if (phase == ActivePlayerPhase.REST) FitWhite.copy(alpha = 0.5f) else FitWhite
                            else -> Color(0xFF2C2C2E)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Main Content Area (Work vs Rest Phase)
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                label = "PlayerPhaseTransition"
            ) { targetPhase ->
                when (targetPhase) {
                    ActivePlayerPhase.WORK -> {
                        if (currentExercise != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Status Pill: Work Timer & Target Muscles
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFF18181B))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Target Muscles tag
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF59E0B))
                                        )
                                        Text(
                                            text = currentExercise.muscleGroup.ifBlank { "Target: Active" },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBBF24)
                                            ),
                                            maxLines = 1
                                        )
                                    }

                                    // Work countdown / target time
                                    val workRemaining = (currentWorkDuration - workElapsedSeconds).coerceAtLeast(0)
                                    Text(
                                        text = if (isWorkFinishedWaitingForComplete) "Timestamp Finished • Tap Complete" else "${workRemaining}s Remaining",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = if (isWorkFinishedWaitingForComplete) Color(0xFFF59E0B) else FitWhite
                                    )
                                }

                                // Exercise Title & Auto Toggle Status Pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentExercise.name,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        ),
                                        color = FitWhite,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Quick Auto-Toggle Switch inside player
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF1E1E24))
                                            .clickable { autoToggle = !autoToggle }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (autoToggle) "Auto" else "Manual",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (autoToggle) Color(0xFF4ADE80) else FitTextSecondary
                                            )
                                        )
                                        Switch(
                                            checked = autoToggle,
                                            onCheckedChange = { autoToggle = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = FitWhite,
                                                checkedTrackColor = Color(0xFF22C55E),
                                                uncheckedThumbColor = FitTextMuted,
                                                uncheckedTrackColor = Color(0xFF2E2E36)
                                            ),
                                            modifier = Modifier.size(width = 34.dp, height = 20.dp)
                                        )
                                    }
                                }

                                // Center Demonstration Player with Video Frame
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF121214))
                                ) {
                                    val vId = currentExercise.videoId.ifEmpty { workout.youtubeVideoId }
                                    EmbeddedWorkoutVideoPlayer(
                                        videoId = vId,
                                        timestampSeconds = currentExercise.timestampSeconds,
                                        isPlaying = isVideoPlaying,
                                        exerciseName = currentExercise.name,
                                        timestampFormatted = currentExercise.timestampFormatted,
                                        currentUser = currentUser,
                                        onSignInRequested = onSignInRequested,
                                        currentMode = playerMode,
                                        onModeChanged = onPlayerModeChanged,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    ActivePlayerPhase.REST -> {
                        // Dedicated REST UI with countdown and Next Exercise Preview
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Rest Header & Big Timer
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 10.dp)
                            ) {
                                Text(
                                    text = "REST PERIOD",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = FitTextSecondary
                                )

                                Text(
                                    text = "%02d:%02d".format(restTimerSeconds / 60, restTimerSeconds % 60),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 54.sp
                                    ),
                                    color = FitWhite
                                )

                                // Quick Rest Controls (+10s / Skip)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { restTimerSeconds += 10 },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FitWhite),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383840))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+10s Rest", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { advanceToNextExercise() },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FitWhite, contentColor = FitBlack),
                                        modifier = Modifier.testTag("skip_rest_button")
                                    ) {
                                        Text("Skip Rest ➔", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Next Exercise Focus Card
                            if (nextExercise != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C32))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "COMING UP NEXT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = FitTextSecondary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // Next Exercise Anatome Visual
                                            AnatomeExerciseVisualBox(
                                                exerciseName = nextExercise.name,
                                                muscleGroup = nextExercise.muscleGroup.ifBlank { workout.category },
                                                modifier = Modifier.size(72.dp)
                                            )

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = nextExercise.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = FitWhite
                                                )

                                                val nTiming = if (nextExercise.durationSeconds > 0) {
                                                    "${nextExercise.durationSeconds}s Work • ${nextExercise.restSeconds}s Rest"
                                                } else {
                                                    "${nextExercise.reps} • ${nextExercise.restSeconds}s Rest"
                                                }

                                                Text(
                                                    text = nTiming,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = FitTextSecondary
                                                )

                                                if (nextExercise.muscleGroup.isNotBlank()) {
                                                    Text(
                                                        text = "Target: ${nextExercise.muscleGroup}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color(0xFFFBBF24),
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ActivePlayerPhase.FINISHED -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = FitWhite,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Workout Complete!",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FitWhite
                                )
                                Text(
                                    text = "Great job finishing $totalElapsedSeconds seconds of training.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FitTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Bottom Drawer / Sheet Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drawer Top Drag Handle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF383840))
                    )

                    // Next Exercise Preview Row (when in WORK phase)
                    if (phase == ActivePlayerPhase.WORK && nextExercise != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Anatome Visual Thumbnail
                            AnatomeExerciseVisualBox(
                                exerciseName = nextExercise.name,
                                muscleGroup = nextExercise.muscleGroup.ifBlank { workout.category },
                                modifier = Modifier.size(52.dp),
                                showMuscleBadge = false
                            )

                            // Info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Next Exercise",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FitTextSecondary
                                )
                                Text(
                                    text = nextExercise.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FitWhite,
                                    maxLines = 1
                                )

                                val nTiming = if (nextExercise.durationSeconds > 0) {
                                    "${nextExercise.durationSeconds}s Work • Rest ${nextExercise.restSeconds}s"
                                } else {
                                    "${nextExercise.reps} • Rest ${nextExercise.restSeconds}s"
                                }

                                Text(
                                    text = nTiming,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FitTextSecondary
                                )
                            }
                        }
                    }

                    // Complete Exercise Action Button (and Previous Workout Button in row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Exercise Button in bottom bar
                        if (currentExerciseIndex > 0) {
                            OutlinedButton(
                                onClick = { goToPreviousExercise() },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("prev_exercise_bottom_btn"),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FitWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383842))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Prev", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Complete Exercise Button
                        Button(
                            onClick = {
                                if (phase == ActivePlayerPhase.REST) {
                                    advanceToNextExercise()
                                } else {
                                    completeCurrentExercise()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("complete_exercise_btn"),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (phase == ActivePlayerPhase.REST || isWorkFinishedWaitingForComplete) FitWhite else Color(0xFF2C2C30),
                                contentColor = if (phase == ActivePlayerPhase.REST || isWorkFinishedWaitingForComplete) FitBlack else FitWhite
                            )
                        ) {
                            Text(
                                text = when (phase) {
                                    ActivePlayerPhase.REST -> "Skip Rest • Start Next Exercise ➔"
                                    ActivePlayerPhase.FINISHED -> "Finish Workout"
                                    else -> if (isWorkFinishedWaitingForComplete) "Complete Workout & Rest ➔" else if (currentExerciseIndex == exercises.size - 1) "Complete Final Exercise ✓" else "Complete Workout"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Footer with Round and Exercise indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (autoToggle) "Auto Transition: ON" else "Manual Pause: ON",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (autoToggle) Color(0xFF4ADE80) else Color(0xFFF59E0B)
                        )

                        Text(
                            text = currentSetLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = FitWhite
                        )
                    }
                }
            }
        }
    }
}
