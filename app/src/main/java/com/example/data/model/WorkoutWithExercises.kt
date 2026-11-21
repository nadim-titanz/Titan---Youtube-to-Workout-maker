package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity

data class WorkoutWithExercises(
    @Embedded
    val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<WorkoutExerciseEntity>
) {
    val sortedExercises: List<WorkoutExerciseEntity>
        get() = exercises.sortedBy { it.orderIndex }

    val totalSets: Int
        get() = exercises.sumOf { it.sets }

    val calculatedDurationMinutes: Int
        get() {
            val totalSeconds = exercises.sumOf { (it.durationSeconds + it.restSeconds) * it.sets }
            return if (totalSeconds > 0) (totalSeconds / 60).coerceAtLeast(1) else workout.totalDurationMinutes
        }
}
