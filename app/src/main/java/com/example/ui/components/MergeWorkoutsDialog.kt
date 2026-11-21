package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.WorkoutWithExercises
import com.example.ui.theme.GeoBorderLilac
import com.example.ui.theme.GeoBorderMuted
import com.example.ui.theme.GeoCardWhite
import com.example.ui.theme.GeoOnPrimaryContainer
import com.example.ui.theme.GeoPrimaryContainer
import com.example.ui.theme.GeoPrimaryPurple
import com.example.ui.theme.GeoSurfaceContainer
import com.example.ui.theme.GeoTextDark
import com.example.ui.theme.GeoTextMuted
import com.example.ui.theme.GeoTextWhite

@Composable
fun MergeWorkoutsDialog(
    allWorkouts: List<WorkoutWithExercises>,
    initialSelectedId: Long? = null,
    isMerging: Boolean,
    onDismiss: () -> Unit,
    onConfirmMerge: (List<Long>, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIds = remember {
        mutableStateListOf<Long>().apply {
            if (initialSelectedId != null) {
                add(initialSelectedId)
            }
        }
    }

    var customTitle by remember { mutableStateOf("") }
    var customGoal by remember { mutableStateOf("") }

    val selectedWorkouts = allWorkouts.filter { selectedIds.contains(it.workout.id) }
    val totalEstMinutes = selectedWorkouts.sumOf { it.calculatedDurationMinutes }
    val totalExercises = selectedWorkouts.sumOf { it.exercises.size }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("merge_workouts_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GeoCardWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoBorderLilac),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GeoPrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMerge,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Merge Workouts",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = GeoTextDark
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GeoTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select 2 or more saved workouts to combine into a single seamless routine:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeoTextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Workouts Selection List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allWorkouts) { item ->
                        val isChecked = selectedIds.contains(item.workout.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) GeoPrimaryContainer else GeoSurfaceContainer)
                                .border(1.dp, if (isChecked) GeoPrimaryPurple else GeoBorderLilac, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isChecked) {
                                        selectedIds.remove(item.workout.id)
                                    } else {
                                        selectedIds.add(item.workout.id)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.workout.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GeoTextDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${item.workout.category} • ${item.calculatedDurationMinutes} min • ${item.exercises.size} exercises",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GeoTextMuted
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedIds.add(item.workout.id) else selectedIds.remove(item.workout.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GeoPrimaryPurple,
                                    uncheckedColor = GeoBorderMuted,
                                    checkmarkColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Pill
                if (selectedIds.size >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GeoPrimaryContainer)
                            .border(1.dp, GeoBorderLilac, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "✨ Combined: ${selectedIds.size} routines • ~$totalEstMinutes min • ~$totalExercises exercises",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GeoOnPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Custom Merged Title (Optional)") },
                    placeholder = { Text("e.g. Mega Push & Core Fusion") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryPurple,
                        unfocusedBorderColor = GeoBorderLilac,
                        focusedTextColor = GeoTextDark,
                        unfocusedTextColor = GeoTextDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onConfirmMerge(selectedIds.toList(), customTitle.takeIf { it.isNotBlank() }, customGoal.takeIf { it.isNotBlank() })
                    },
                    enabled = selectedIds.size >= 2 && !isMerging,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_merge_btn"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoPrimaryPurple,
                        contentColor = GeoTextWhite,
                        disabledContainerColor = Color(0xFFDDDDDD)
                    )
                ) {
                    if (isMerging) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MERGING WORKOUTS...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CallMerge, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MERGE ${selectedIds.size} WORKOUTS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

