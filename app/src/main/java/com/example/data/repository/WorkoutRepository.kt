package com.example.data.repository

import com.example.data.local.WorkoutDao
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.local.WorkoutLogEntity
import com.example.data.model.WorkoutWithExercises
import com.example.data.remote.GeminiWorkoutService
import com.example.data.remote.GeneratedExerciseData
import com.example.data.remote.YoutubeMetadataService
import com.example.data.remote.YoutubeVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorkoutRepository(
    private val workoutDao: WorkoutDao
) {
    val allWorkouts: Flow<List<WorkoutWithExercises>> = workoutDao.getAllWorkoutsWithExercises()
    val allLogs: Flow<List<WorkoutLogEntity>> = workoutDao.getAllLogs()

    fun getWorkoutById(id: Long): Flow<WorkoutWithExercises?> = workoutDao.getWorkoutWithExercisesById(id)

    suspend fun getWorkoutByIdSync(id: Long): WorkoutWithExercises? = workoutDao.getWorkoutWithExercisesByIdSync(id)

    suspend fun createWorkoutFromYoutubeUrl(
        url: String,
        customFocus: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val videoId = YoutubeMetadataService.extractVideoId(url) ?: "dQw4w9WgXcQ"
        val videoInfo = YoutubeMetadataService.fetchVideoMetadata(videoId)
        val generated = GeminiWorkoutService.generateWorkoutFromVideo(videoInfo, customFocus)

        val workoutEntity = WorkoutEntity(
            title = generated.title,
            description = generated.description,
            sourceYoutubeUrl = url,
            youtubeVideoId = videoId,
            channelTitle = videoInfo.authorName,
            coverImageUrl = videoInfo.thumbnailUrl,
            totalDurationMinutes = generated.totalDurationMinutes,
            difficulty = generated.difficulty,
            category = generated.category,
            isFavorite = false,
            timesCompleted = 0,
            isMerged = false,
            createdAt = System.currentTimeMillis()
        )

        val workoutId = workoutDao.insertWorkout(workoutEntity)

        val exercises = generated.exercises.mapIndexed { index, ex ->
            WorkoutExerciseEntity(
                workoutId = workoutId,
                name = ex.name,
                orderIndex = index,
                sets = ex.sets,
                reps = ex.reps,
                durationSeconds = ex.durationSeconds,
                restSeconds = ex.restSeconds,
                timestampSeconds = ex.timestampSeconds,
                timestampFormatted = ex.timestampFormatted,
                videoId = videoId,
                muscleGroup = ex.muscleGroup,
                equipment = ex.equipment,
                formTips = ex.formTips,
                isCompleted = false
            )
        }

        workoutDao.insertExercises(exercises)
        workoutId
    }

    suspend fun createWorkoutFromMultipleUrls(
        urls: List<String>,
        customGoal: String? = null
    ): Long = withContext(Dispatchers.IO) {
        if (urls.isEmpty()) return@withContext 0L
        if (urls.size == 1) return@withContext createWorkoutFromYoutubeUrl(urls.first(), customGoal)

        val videoInfos = mutableListOf<YoutubeVideoInfo>()
        val intermediateWorkouts = mutableListOf<WorkoutWithExercises>()

        for (url in urls) {
            val videoId = YoutubeMetadataService.extractVideoId(url) ?: continue
            val videoInfo = YoutubeMetadataService.fetchVideoMetadata(videoId)
            videoInfos.add(videoInfo)
            val generated = GeminiWorkoutService.generateWorkoutFromVideo(videoInfo, null)

            val tempWorkout = WorkoutEntity(
                title = generated.title,
                category = generated.category,
                difficulty = generated.difficulty,
                totalDurationMinutes = generated.totalDurationMinutes,
                youtubeVideoId = videoId
            )
            val tempExercises = generated.exercises.mapIndexed { index, ex ->
                WorkoutExerciseEntity(
                    workoutId = 0,
                    name = ex.name,
                    orderIndex = index,
                    sets = ex.sets,
                    reps = ex.reps,
                    durationSeconds = ex.durationSeconds,
                    restSeconds = ex.restSeconds,
                    timestampSeconds = ex.timestampSeconds,
                    timestampFormatted = ex.timestampFormatted,
                    videoId = videoId,
                    muscleGroup = ex.muscleGroup,
                    equipment = ex.equipment,
                    formTips = ex.formTips
                )
            }
            intermediateWorkouts.add(WorkoutWithExercises(tempWorkout, tempExercises))
        }

        val merged = GeminiWorkoutService.mergeWorkoutsWithAI(intermediateWorkouts, customGoal)
        val primaryVideo = videoInfos.firstOrNull()
        val primaryVideoId = primaryVideo?.videoId ?: ""

        val mergedWorkout = WorkoutEntity(
            title = merged.title,
            description = merged.description,
            sourceYoutubeUrl = urls.joinToString(", "),
            youtubeVideoId = primaryVideoId,
            channelTitle = videoInfos.joinToString(" & ") { it.authorName },
            coverImageUrl = primaryVideo?.thumbnailUrl ?: YoutubeMetadataService.getThumbnailUrl(primaryVideoId),
            totalDurationMinutes = merged.totalDurationMinutes,
            difficulty = merged.difficulty,
            category = merged.category,
            isFavorite = false,
            timesCompleted = 0,
            isMerged = true,
            mergedSourceUrls = urls.joinToString(","),
            createdAt = System.currentTimeMillis()
        )

        val workoutId = workoutDao.insertWorkout(mergedWorkout)

        val exercises = merged.exercises.mapIndexed { index, ex ->
            val videoForEx = intermediateWorkouts.firstOrNull { w ->
                w.exercises.any { it.name.equals(ex.name, ignoreCase = true) }
            }?.workout?.youtubeVideoId ?: primaryVideoId

            WorkoutExerciseEntity(
                workoutId = workoutId,
                name = ex.name,
                orderIndex = index,
                sets = ex.sets,
                reps = ex.reps,
                durationSeconds = ex.durationSeconds,
                restSeconds = ex.restSeconds,
                timestampSeconds = ex.timestampSeconds,
                timestampFormatted = ex.timestampFormatted,
                videoId = videoForEx,
                muscleGroup = ex.muscleGroup,
                equipment = ex.equipment,
                formTips = ex.formTips,
                isCompleted = false
            )
        }

        workoutDao.insertExercises(exercises)
        workoutId
    }

    suspend fun mergeSavedWorkouts(
        workoutIds: List<Long>,
        customTitle: String? = null,
        customGoal: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val workouts = workoutIds.mapNotNull { workoutDao.getWorkoutWithExercisesByIdSync(it) }
        if (workouts.isEmpty()) return@withContext 0L

        val mergedResult = GeminiWorkoutService.mergeWorkoutsWithAI(workouts, customGoal)
        val primaryWorkout = workouts.first().workout

        val mergedEntity = WorkoutEntity(
            title = customTitle?.takeIf { it.isNotBlank() } ?: mergedResult.title,
            description = mergedResult.description,
            sourceYoutubeUrl = workouts.map { it.workout.sourceYoutubeUrl }.filter { it.isNotBlank() }.joinToString(", "),
            youtubeVideoId = primaryWorkout.youtubeVideoId,
            channelTitle = workouts.map { it.workout.channelTitle }.filter { it.isNotBlank() }.distinct().joinToString(" & "),
            coverImageUrl = primaryWorkout.coverImageUrl,
            totalDurationMinutes = mergedResult.totalDurationMinutes,
            difficulty = mergedResult.difficulty,
            category = mergedResult.category,
            isFavorite = false,
            timesCompleted = 0,
            isMerged = true,
            mergedSourceUrls = workouts.map { it.workout.id.toString() }.joinToString(","),
            createdAt = System.currentTimeMillis()
        )

        val workoutId = workoutDao.insertWorkout(mergedEntity)

        val exercises = mergedResult.exercises.mapIndexed { index, ex ->
            val sourceVideoId = workouts.firstOrNull { w ->
                w.exercises.any { it.name.equals(ex.name, ignoreCase = true) }
            }?.workout?.youtubeVideoId ?: primaryWorkout.youtubeVideoId

            WorkoutExerciseEntity(
                workoutId = workoutId,
                name = ex.name,
                orderIndex = index,
                sets = ex.sets,
                reps = ex.reps,
                durationSeconds = ex.durationSeconds,
                restSeconds = ex.restSeconds,
                timestampSeconds = ex.timestampSeconds,
                timestampFormatted = ex.timestampFormatted,
                videoId = sourceVideoId,
                muscleGroup = ex.muscleGroup,
                equipment = ex.equipment,
                formTips = ex.formTips,
                isCompleted = false
            )
        }

        workoutDao.insertExercises(exercises)
        workoutId
    }

    suspend fun saveWorkoutChanges(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>
    ) = withContext(Dispatchers.IO) {
        workoutDao.updateWorkout(workout)
        workoutDao.deleteExercisesForWorkout(workout.id)
        val reindexed = exercises.mapIndexed { index, ex ->
            ex.copy(workoutId = workout.id, orderIndex = index)
        }
        workoutDao.insertExercises(reindexed)
    }

    suspend fun deleteWorkout(workoutId: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteExercisesForWorkout(workoutId)
        workoutDao.deleteWorkoutById(workoutId)
    }

    suspend fun deleteAllWorkouts() = withContext(Dispatchers.IO) {
        workoutDao.deleteAllExercises()
        workoutDao.deleteAllWorkouts()
    }

    suspend fun toggleFavorite(workoutId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        workoutDao.setFavorite(workoutId, isFavorite)
    }

    suspend fun recordWorkoutCompletion(
        workoutId: Long,
        workoutTitle: String,
        durationSeconds: Int,
        totalSets: Int,
        totalReps: Int,
        totalExercises: Int
    ): Long = withContext(Dispatchers.IO) {
        val calories = calculateCaloriesBurned(durationSeconds, totalSets)
        val log = WorkoutLogEntity(
            workoutId = workoutId,
            workoutTitle = workoutTitle,
            completedAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            totalSetsCompleted = totalSets,
            totalRepsCompleted = totalReps,
            totalExercisesCompleted = totalExercises,
            caloriesBurned = calories
        )
        if (workoutId > 0) {
            workoutDao.incrementTimesCompleted(workoutId)
        }
        workoutDao.insertLog(log)
    }

    private fun calculateCaloriesBurned(durationSeconds: Int, totalSets: Int): Int {
        // Average metabolic burn estimate for strength/interval training (~8-10 kcal / min + set volume bonus)
        val minutes = (durationSeconds / 60.0).coerceAtLeast(1.0)
        val baseKcal = minutes * 8.5
        val setBonus = totalSets * 6.0
        return (baseKcal + setBonus).toInt()
    }

    suspend fun seedInitialWorkoutsIfEmpty() = withContext(Dispatchers.IO) {
        // Leave completely empty on first launch as requested by user
    }
}
