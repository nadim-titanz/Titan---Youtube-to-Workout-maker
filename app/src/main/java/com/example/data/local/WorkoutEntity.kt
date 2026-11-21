package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val sourceYoutubeUrl: String = "",
    val youtubeVideoId: String = "",
    val channelTitle: String = "",
    val coverImageUrl: String = "",
    val totalDurationMinutes: Int = 15,
    val difficulty: String = "Intermediate", // Beginner, Intermediate, Advanced
    val category: String = "Full Body", // Chest, Back, Legs, Core, Arms, Full Body, HIIT, Cardio
    val isFavorite: Boolean = false,
    val timesCompleted: Int = 0,
    val isMerged: Boolean = false,
    val mergedSourceUrls: String = "", // Comma-separated or JSON of source URLs if merged
    val createdAt: Long = System.currentTimeMillis()
)
