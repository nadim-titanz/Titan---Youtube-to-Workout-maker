package com.example.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.model.WorkoutWithExercises
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite

@Composable
fun EditWorkoutScreen(
    workoutWithExercises: WorkoutWithExercises,
    onClose: () -> Unit,
    onSaveChanges: (WorkoutEntity, List<WorkoutExerciseEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept back button
    BackHandler(enabled = true) {
        onClose()
    }

    var title by remember { mutableStateOf(workoutWithExercises.workout.title) }
    var category by remember { mutableStateOf(workoutWithExercises.workout.category) }
    var difficulty by remember { mutableStateOf(workoutWithExercises.workout.difficulty) }

    val exerciseList = remember {
        mutableStateListOf<WorkoutExerciseEntity>().apply {
            addAll(workoutWithExercises.sortedExercises)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitBlack)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("edit_workout_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26262B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = FitWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Edit Workout",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = FitWhite
                )
            }

            Button(
                onClick = {
                    val updatedWorkout = workoutWithExercises.workout.copy(
                        title = title.trim(),
                        category = category.trim(),
                        difficulty = difficulty.trim()
                    )
                    onSaveChanges(updatedWorkout, exerciseList.toList())
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FitWhite,
                    contentColor = FitBlack
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "WORKOUT DETAILS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = FitTextSecondary
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title", color = FitTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FitWhite,
                                unfocusedBorderColor = Color(0xFF2E2E34),
                                focusedTextColor = FitWhite,
                                unfocusedTextColor = FitWhite
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category", color = FitTextSecondary) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FitWhite,
                                    unfocusedBorderColor = Color(0xFF2E2E34),
                                    focusedTextColor = FitWhite,
                                    unfocusedTextColor = FitWhite
                                )
                            )

                            OutlinedTextField(
                                value = difficulty,
                                onValueChange = { difficulty = it },
                                label = { Text("Difficulty", color = FitTextSecondary) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FitWhite,
                                    unfocusedBorderColor = Color(0xFF2E2E34),
                                    focusedTextColor = FitWhite,
                                    unfocusedTextColor = FitWhite
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXERCISES (${exerciseList.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = FitTextSecondary
                    )

                    Button(
                        onClick = {
                            val newEx = WorkoutExerciseEntity(
                                workoutId = workoutWithExercises.workout.id,
                                name = "New Exercise",
                                sets = 1,
                                reps = "40 Seconds",
                                durationSeconds = 40,
                                restSeconds = 20,
                                orderIndex = exerciseList.size,
                                videoId = workoutWithExercises.workout.youtubeVideoId,
                                timestampSeconds = 0,
                                timestampFormatted = "0:00"
                            )
                            exerciseList.add(newEx)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF26262B),
                            contentColor = FitWhite
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Exercise", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            itemsIndexed(exerciseList, key = { _, ex -> ex.id }) { index, ex ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${index + 1} ${ex.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FitWhite
                            )

                            Row {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val item = exerciseList.removeAt(index)
                                            exerciseList.add(index - 1, item)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = FitTextSecondary)
                                    }
                                }
                                if (index < exerciseList.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val item = exerciseList.removeAt(index)
                                            exerciseList.add(index + 1, item)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = FitTextSecondary)
                                    }
                                }
                                IconButton(
                                    onClick = { exerciseList.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FitRedAccent)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ex.name,
                            onValueChange = { newName ->
                                exerciseList[index] = ex.copy(name = newName)
                            },
                            label = { Text("Exercise Name", color = FitTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FitWhite,
                                unfocusedBorderColor = Color(0xFF2E2E34),
                                focusedTextColor = FitWhite,
                                unfocusedTextColor = FitWhite
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (ex.durationSeconds > 0) "${ex.durationSeconds}s" else ex.reps,
                                onValueChange = { newVal ->
                                    val secs = newVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    exerciseList[index] = ex.copy(reps = newVal, durationSeconds = secs)
                                },
                                label = { Text("Target / Time", color = FitTextSecondary) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FitWhite,
                                    unfocusedBorderColor = Color(0xFF2E2E34),
                                    focusedTextColor = FitWhite,
                                    unfocusedTextColor = FitWhite
                                )
                            )

                            OutlinedTextField(
                                value = "${ex.restSeconds}s",
                                onValueChange = { newVal ->
                                    val r = newVal.filter { it.isDigit() }.toIntOrNull() ?: 20
                                    exerciseList[index] = ex.copy(restSeconds = r)
                                },
                                label = { Text("Rest", color = FitTextSecondary) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FitWhite,
                                    unfocusedBorderColor = Color(0xFF2E2E34),
                                    focusedTextColor = FitWhite,
                                    unfocusedTextColor = FitWhite
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
