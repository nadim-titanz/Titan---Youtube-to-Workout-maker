package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val orderIndex: Int = 0,
    val sets: Int = 3,
    val reps: String = "12", // e.g. "12" or "To Failure"
    val durationSeconds: Int = 45, // Work timer in seconds (e.g. 45s for timed exercises, 0 for reps-only)
    val restSeconds: Int = 30, // Rest timer in seconds
    val timestampSeconds: Int = 0, // Video timestamp in seconds (e.g. 75 for 01:15)
    val timestampFormatted: String = "00:00", // e.g. "01:15"
    val videoId: String = "", // YouTube video ID for frame visual
    val muscleGroup: String = "Core", // Chest, Triceps, Abs, Quads, etc.
    val equipment: String = "Bodyweight", // Dumbbells, Mat, Barbell, None
    val formTips: String = "Maintain tight core and steady controlled breathing.",
    val isCompleted: Boolean = false
)
