package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutWithExercisesById(id: Long): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutWithExercisesByIdSync(id: Long): WorkoutWithExercises?

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun getWorkoutCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM workout_logs")
    suspend fun deleteAllLogs()

    @Query("UPDATE workouts SET isFavorite = :isFavorite WHERE id = :workoutId")
    suspend fun setFavorite(workoutId: Long, isFavorite: Boolean)

    @Query("UPDATE workouts SET timesCompleted = timesCompleted + 1 WHERE id = :workoutId")
    suspend fun incrementTimesCompleted(workoutId: Long)

    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: WorkoutExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: WorkoutExerciseEntity)

    @Query("DELETE FROM workout_exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getExercisesForWorkout(workoutId: Long): List<WorkoutExerciseEntity>

    // Logs
    @Query("SELECT * FROM workout_logs ORDER BY completedAt DESC")
    fun getAllLogs(): Flow<List<WorkoutLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLogEntity): Long

    @Query("DELETE FROM workout_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)
}
