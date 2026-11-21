package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.auth.UserProfile
import com.example.data.local.WorkoutDatabase
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.local.WorkoutLogEntity
import com.example.data.model.WorkoutWithExercises
import com.example.data.repository.WorkoutRepository
import com.example.ui.components.PlayerEngineMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class WorkoutStats(
    val workoutsToday: Int,
    val totalRepsToday: Int,
    val totalSetsToday: Int,
    val setsThisWeek: Int,
    val setsThisMonth: Int,
    val setsTotalSoFar: Int,
    val streakDays: Int,
    val activeDaysOfWeek: Set<Int>, // Calendar.SUNDAY .. Calendar.SATURDAY
    val globalSetsCounter: Long
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WorkoutDatabase.getDatabase(application)
    private val repository = WorkoutRepository(database.workoutDao())
    val authManager = AuthManager(application)
    private val prefs = application.getSharedPreferences("workout_forge_prefs", Context.MODE_PRIVATE)

    val playerEngineMode = MutableStateFlow(
        try {
            PlayerEngineMode.valueOf(
                prefs.getString("player_engine_mode", PlayerEngineMode.CLEAN_VIDEO.name) ?: PlayerEngineMode.CLEAN_VIDEO.name
            )
        } catch (e: Exception) {
            PlayerEngineMode.CLEAN_VIDEO
        }
    )

    fun setPlayerEngineMode(mode: PlayerEngineMode) {
        playerEngineMode.value = mode
        prefs.edit().putString("player_engine_mode", mode.name).apply()
    }

    val currentUser: StateFlow<UserProfile> = authManager.currentUser

    fun signInWithGoogle(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle()
            onComplete(result.isSuccess)
        }
    }

    fun quickSignIn(name: String = "Google Athlete", email: String = "athlete@gmail.com") {
        authManager.quickSignIn(name, email)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    val rawWorkouts: StateFlow<List<WorkoutWithExercises>> = repository.allWorkouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<WorkoutLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    val filteredWorkouts: StateFlow<List<WorkoutWithExercises>> = combine(
        rawWorkouts,
        selectedCategory,
        searchQuery
    ) { workouts, category, query ->
        workouts.filter { w ->
            val matchesCategory = when (category) {
                "All" -> true
                "Favorites" -> w.workout.isFavorite
                "Merged" -> w.workout.isMerged
                else -> w.workout.category.equals(category, ignoreCase = true) ||
                        w.exercises.any { it.muscleGroup.equals(category, ignoreCase = true) }
            }
            val matchesQuery = query.isBlank() ||
                    w.workout.title.contains(query, ignoreCase = true) ||
                    w.workout.description.contains(query, ignoreCase = true) ||
                    w.exercises.any { it.name.contains(query, ignoreCase = true) }

            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<WorkoutStats> = allLogs.combine(rawWorkouts) { logs, _ ->
        calculateStats(logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkoutStats(0, 0, 0, 0, 0, 0, 0, emptySet(), 9727929L)
    )

    // UI Dialog & Screen State
    val isGenerating = MutableStateFlow(false)
    val isMerging = MutableStateFlow(false)
    val statusMessage = MutableStateFlow<String?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    // Active Player / Edit Workout Target
    val activeWorkout = MutableStateFlow<WorkoutWithExercises?>(null)
    val editingWorkout = MutableStateFlow<WorkoutWithExercises?>(null)
    val showMergeDialog = MutableStateFlow(false)
    val showCreateSheet = MutableStateFlow(false)
    val showHistorySheet = MutableStateFlow(false)

    init {
        // App starts empty on first launch; user extracts workouts via YouTube link
    }

    fun deleteAllWorkouts() {
        viewModelScope.launch {
            repository.deleteAllWorkouts()
            statusMessage.value = "All workouts cleared."
        }
    }

    fun createWorkoutFromYoutube(
        url: String,
        customFocus: String? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        if (url.isBlank()) {
            errorMessage.value = "Please enter a valid YouTube URL."
            return
        }

        viewModelScope.launch {
            isGenerating.value = true
            errorMessage.value = null
            statusMessage.value = "Analyzing YouTube video & generating exercises..."
            try {
                val newId = repository.createWorkoutFromYoutubeUrl(url.trim(), customFocus)
                statusMessage.value = "Workout created successfully!"
                showCreateSheet.value = false
                onSuccess(newId)
            } catch (e: Exception) {
                errorMessage.value = "Failed to create workout: ${e.localizedMessage ?: e.message}"
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun createWorkoutFromMultipleUrls(
        urls: List<String>,
        customGoal: String? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        val validUrls = urls.map { it.trim() }.filter { it.isNotBlank() }
        if (validUrls.isEmpty()) {
            errorMessage.value = "Please enter at least one YouTube URL."
            return
        }

        viewModelScope.launch {
            isGenerating.value = true
            errorMessage.value = null
            statusMessage.value = "Merging ${validUrls.size} YouTube videos with AI..."
            try {
                val newId = repository.createWorkoutFromMultipleUrls(validUrls, customGoal)
                statusMessage.value = "Merged workout created successfully!"
                showCreateSheet.value = false
                onSuccess(newId)
            } catch (e: Exception) {
                errorMessage.value = "Failed to merge videos: ${e.localizedMessage ?: e.message}"
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun mergeExistingWorkouts(
        workoutIds: List<Long>,
        customTitle: String? = null,
        customGoal: String? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        if (workoutIds.size < 2) {
            errorMessage.value = "Select at least 2 workouts to merge."
            return
        }

        viewModelScope.launch {
            isMerging.value = true
            errorMessage.value = null
            statusMessage.value = "Blending workouts into a unified routine..."
            try {
                val newId = repository.mergeSavedWorkouts(workoutIds, customTitle, customGoal)
                statusMessage.value = "Merged routine created!"
                showMergeDialog.value = false
                onSuccess(newId)
            } catch (e: Exception) {
                errorMessage.value = "Merge failed: ${e.localizedMessage ?: e.message}"
            } finally {
                isMerging.value = false
            }
        }
    }

    fun toggleFavorite(workoutId: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(workoutId, !currentStatus)
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            if (activeWorkout.value?.workout?.id == workoutId) {
                activeWorkout.value = null
            }
            if (editingWorkout.value?.workout?.id == workoutId) {
                editingWorkout.value = null
            }
        }
    }

    fun saveWorkoutChanges(workout: WorkoutEntity, exercises: List<WorkoutExerciseEntity>) {
        viewModelScope.launch {
            repository.saveWorkoutChanges(workout, exercises)
            editingWorkout.value = null
            statusMessage.value = "Workout updated!"
        }
    }

    fun recordCompletedWorkout(
        workoutId: Long,
        workoutTitle: String,
        durationSeconds: Int,
        totalSets: Int,
        totalReps: Int,
        totalExercises: Int
    ) {
        viewModelScope.launch {
            repository.recordWorkoutCompletion(
                workoutId = workoutId,
                workoutTitle = workoutTitle,
                durationSeconds = durationSeconds,
                totalSets = totalSets,
                totalReps = totalReps,
                totalExercises = totalExercises
            )
        }
    }

    fun openWorkoutPlayer(workout: WorkoutWithExercises) {
        activeWorkout.value = workout
    }

    fun closeWorkoutPlayer() {
        activeWorkout.value = null
    }

    fun openWorkoutEditor(workout: WorkoutWithExercises) {
        editingWorkout.value = workout
    }

    fun closeWorkoutEditor() {
        editingWorkout.value = null
    }

    fun clearMessages() {
        statusMessage.value = null
        errorMessage.value = null
    }

    private fun calculateStats(logs: List<WorkoutLogEntity>): WorkoutStats {
        val now = Calendar.getInstance()
        val todayYear = now.get(Calendar.YEAR)
        val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val todayMonth = now.get(Calendar.MONTH)
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)

        var workoutsToday = 0
        var repsToday = 0
        var setsToday = 0
        var setsWeek = 0
        var setsMonth = 0
        var setsTotal = 0
        val activeDays = mutableSetOf<Int>()

        val logCalendar = Calendar.getInstance()

        for (log in logs) {
            logCalendar.timeInMillis = log.completedAt
            val year = logCalendar.get(Calendar.YEAR)
            val dayOfYear = logCalendar.get(Calendar.DAY_OF_YEAR)
            val month = logCalendar.get(Calendar.MONTH)
            val week = logCalendar.get(Calendar.WEEK_OF_YEAR)
            val dayOfWeek = logCalendar.get(Calendar.DAY_OF_WEEK)

            setsTotal += log.totalSetsCompleted

            if (year == todayYear && dayOfYear == todayDayOfYear) {
                workoutsToday++
                repsToday += log.totalRepsCompleted
                setsToday += log.totalSetsCompleted
            }

            if (year == todayYear && week == currentWeek) {
                setsWeek += log.totalSetsCompleted
                activeDays.add(dayOfWeek)
            }

            if (year == todayYear && month == todayMonth) {
                setsMonth += log.totalSetsCompleted
            }
        }

        // Calculate active streak
        var streak = 0
        val checkCal = Calendar.getInstance()
        while (true) {
            val cYear = checkCal.get(Calendar.YEAR)
            val cDay = checkCal.get(Calendar.DAY_OF_YEAR)
            val hadWorkout = logs.any { l ->
                logCalendar.timeInMillis = l.completedAt
                logCalendar.get(Calendar.YEAR) == cYear && logCalendar.get(Calendar.DAY_OF_YEAR) == cDay
            }
            if (hadWorkout) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // If today has no workout yet, check if yesterday had one before breaking
                if (streak == 0 && cDay == todayDayOfYear) {
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    continue
                }
                break
            }
        }

        return WorkoutStats(
            workoutsToday = workoutsToday,
            totalRepsToday = repsToday,
            totalSetsToday = setsToday,
            setsThisWeek = setsWeek,
            setsThisMonth = setsMonth,
            setsTotalSoFar = setsTotal,
            streakDays = streak,
            activeDaysOfWeek = activeDays,
            globalSetsCounter = setsTotal.toLong()
        )
    }
}
