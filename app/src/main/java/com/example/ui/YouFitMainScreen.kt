package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutWithExercises
import com.example.ui.components.GoogleAuthDialog
import com.example.ui.components.PushrHeader
import com.example.ui.components.TitanzBottomNavBar
import com.example.ui.components.TitanzTab
import com.example.ui.components.WeeklyConsistencyCard
import com.example.ui.components.WorkoutCard
import com.example.ui.components.YoutubeLinkPasteCard
import com.example.ui.edit.EditWorkoutScreen
import com.example.ui.history.WorkoutHistorySheet
import com.example.ui.player.ActiveWorkoutPlayer
import com.example.ui.profile.ProgressProfileSheet
import com.example.ui.progress.MyProgressView
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite

@Composable
fun YouFitMainScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val playerEngineMode by viewModel.playerEngineMode.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val allRawWorkouts by viewModel.rawWorkouts.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()

    val isGenerating by viewModel.isGenerating.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val editingWorkout by viewModel.editingWorkout.collectAsState()
    val showHistorySheet by viewModel.showHistorySheet.collectAsState()

    var currentTab by remember { mutableStateOf(TitanzTab.DASHBOARD) }
    var selectedWorkoutForOverview by remember { mutableStateOf<WorkoutWithExercises?>(null) }
    var showExitAppDialog by remember { mutableStateOf(false) }
    var showGoogleAuthDialog by remember { mutableStateOf(false) }
    var showProgressProfileSheet by remember { mutableStateOf(false) }
    var passedAutoToggle by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showGoogleAuthDialog) {
        GoogleAuthDialog(
            currentUser = currentUser,
            onSignInWithGoogle = {
                viewModel.signInWithGoogle { success ->
                    if (success) {
                        showGoogleAuthDialog = false
                    }
                }
            },
            onQuickSignIn = { name, email ->
                viewModel.quickSignIn(name, email)
                showGoogleAuthDialog = false
            },
            onSignOut = {
                viewModel.signOut()
            },
            onDismiss = { showGoogleAuthDialog = false }
        )
    }

    // Modal Screen 1: Active Workout Player (Screenshot 2)
    if (activeWorkout != null) {
        ActiveWorkoutPlayer(
            workoutWithExercises = activeWorkout!!,
            initialAutoToggle = passedAutoToggle,
            onClose = { viewModel.closeWorkoutPlayer() },
            currentUser = currentUser,
            onSignInRequested = { showGoogleAuthDialog = true },
            playerMode = playerEngineMode,
            onPlayerModeChanged = { viewModel.setPlayerEngineMode(it) },
            onFinishSession = { workoutId, title, duration, sets, reps, exercises ->
                viewModel.recordCompletedWorkout(workoutId, title, duration, sets, reps, exercises)
            }
        )
        return
    }

    // Modal Screen 2: Workout Overview / Detail Screen (Screenshot 1)
    if (selectedWorkoutForOverview != null) {
        WorkoutDetailScreen(
            workoutWithExercises = selectedWorkoutForOverview!!,
            onBack = { selectedWorkoutForOverview = null },
            currentUser = currentUser,
            onSignInRequested = { showGoogleAuthDialog = true },
            playerMode = playerEngineMode,
            onPlayerModeChanged = { viewModel.setPlayerEngineMode(it) },
            onOpenHeatmapProgress = { showProgressProfileSheet = true },
            onStartWorkout = { autoToggle ->
                passedAutoToggle = autoToggle
                val targetWorkout = selectedWorkoutForOverview
                selectedWorkoutForOverview = null
                if (targetWorkout != null) {
                    viewModel.openWorkoutPlayer(targetWorkout)
                }
            }
        )
        return
    }

    // Modal Screen 3: Edit Screen
    if (editingWorkout != null) {
        EditWorkoutScreen(
            workoutWithExercises = editingWorkout!!,
            onClose = { viewModel.closeWorkoutEditor() },
            onSaveChanges = { updatedWorkout, updatedExercises ->
                viewModel.saveWorkoutChanges(updatedWorkout, updatedExercises)
            }
        )
        return
    }

    // Handle back button on root screen with confirmation so user doesn't accidentally exit
    BackHandler(enabled = true) {
        if (showProgressProfileSheet) {
            showProgressProfileSheet = false
        } else if (showHistorySheet) {
            viewModel.showHistorySheet.value = false
        } else if (currentTab != TitanzTab.DASHBOARD) {
            currentTab = TitanzTab.DASHBOARD
        } else {
            showExitAppDialog = true
        }
    }

    if (showExitAppDialog) {
        AlertDialog(
            onDismissRequest = { showExitAppDialog = false },
            title = {
                Text(
                    text = "Exit App?",
                    fontWeight = FontWeight.Bold,
                    color = FitWhite
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to close Titanz?",
                    color = FitTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitAppDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FitRedAccent)
                ) {
                    Text("Exit", color = FitWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitAppDialog = false }) {
                    Text("Stay", color = FitWhite, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1C1C1E)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FitBlack,
        bottomBar = {
            TitanzBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (currentTab) {
            TitanzTab.MY_PROGRESS -> {
                MyProgressView(
                    currentUser = currentUser,
                    stats = stats,
                    logs = allLogs,
                    workouts = allRawWorkouts,
                    onOpenSettings = { showGoogleAuthDialog = true },
                    onOpenAllRecords = { viewModel.showHistorySheet.value = true },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            TitanzTab.DASHBOARD -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Bar with Titanz branding, Profile and History
                    item {
                        PushrHeader(
                            currentUser = currentUser,
                            onOpenProfile = { showGoogleAuthDialog = true },
                            onOpenProgressHeatmap = { currentTab = TitanzTab.MY_PROGRESS },
                            onOpenHistory = { viewModel.showHistorySheet.value = true }
                        )
                    }

                    // 1. Weekly Consistency Card
                    item {
                        WeeklyConsistencyCard(
                            stats = stats,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    // 2. Direct YouTube Link Paste Card
                    item {
                        YoutubeLinkPasteCard(
                            isGenerating = isGenerating,
                            onExtractWorkout = { url, focus ->
                                viewModel.createWorkoutFromYoutube(url, focus) { newId ->
                                    // Open newly created workout detail screen automatically
                                    val newWorkout = allRawWorkouts.find { it.workout.id == newId }
                                    if (newWorkout != null) {
                                        selectedWorkoutForOverview = newWorkout
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    // 3. Saved Workouts Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YOUR WORKOUTS (${allRawWorkouts.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = FitTextSecondary
                            )
                        }
                    }

                    // 4. Saved Workout Cards or Clean Empty State
                    if (allRawWorkouts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262A))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF26262B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SmartDisplay,
                                            contentDescription = null,
                                            tint = FitWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "No workouts yet",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = FitWhite
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Paste a YouTube workout video link above to extract all exercises, sets, reps and follow along with live video demonstrations.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FitTextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(allRawWorkouts, key = { it.workout.id }) { item ->
                            WorkoutCard(
                                workoutWithExercises = item,
                                onOpenOverview = { selectedWorkoutForOverview = item },
                                onStartWorkout = { viewModel.openWorkoutPlayer(item) },
                                onDeleteWorkout = { viewModel.deleteWorkout(item.workout.id) },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    // Modal Progress Profile & Heatmap Logs Sheet
    if (showProgressProfileSheet) {
        ProgressProfileSheet(
            currentUser = currentUser,
            stats = stats,
            logs = allLogs,
            workouts = allRawWorkouts,
            onSignInRequested = { showGoogleAuthDialog = true },
            onSignOut = { viewModel.signOut() },
            onDismiss = { showProgressProfileSheet = false }
        )
    }

    // Modal History Sheet
    if (showHistorySheet) {
        WorkoutHistorySheet(
            logs = allLogs,
            onDismiss = { viewModel.showHistorySheet.value = false }
        )
    }
}
