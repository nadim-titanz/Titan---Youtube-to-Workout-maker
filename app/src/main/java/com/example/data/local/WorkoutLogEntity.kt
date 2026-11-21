package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long = 0,
    val workoutTitle: String,
    val completedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val totalSetsCompleted: Int = 0,
    val totalRepsCompleted: Int = 0,
    val totalExercisesCompleted: Int = 0,
    val caloriesBurned: Int = 0
)
