package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GeoBorderLilac
import com.example.ui.theme.GeoBorderMuted
import com.example.ui.theme.GeoCardWhite
import com.example.ui.theme.GeoIvoryBackground
import com.example.ui.theme.GeoOnPrimaryContainer
import com.example.ui.theme.GeoPrimaryContainer
import com.example.ui.theme.GeoPrimaryPurple
import com.example.ui.theme.GeoSecondaryContainer
import com.example.ui.theme.GeoSurfaceContainer
import com.example.ui.theme.GeoTextDark
import com.example.ui.theme.GeoTextMuted
import com.example.ui.theme.GeoTextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeLinkInputSheet(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onSubmitSingleUrl: (String, String?) -> Unit,
    onSubmitMultipleUrls: (List<String>, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Single Video, 1: Multi-Link Merger
    var singleUrlInput by remember { mutableStateOf("") }
    var customFocusInput by remember { mutableStateOf("") }

    val multiUrlList = remember {
        mutableStateListOf(
            "https://www.youtube.com/watch?v=IODxDxX7oi4",
            "https://www.youtube.com/watch?v=AnYl6Nk9GOA"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GeoCardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEW WORKOUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = GeoPrimaryPurple
                    )
                    Text(
                        text = "YouTube to Workout Maker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = GeoTextDark
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = GeoTextDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row (Single Video vs Merge Multiple Videos)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GeoSurfaceContainer,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GeoPrimaryPurple,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "Single Video",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    selectedContentColor = GeoPrimaryPurple,
                    unselectedContentColor = GeoTextMuted
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "Merge Multiple Links",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    selectedContentColor = GeoPrimaryPurple,
                    unselectedContentColor = GeoTextMuted
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                // SINGLE URL FLOW
                OutlinedTextField(
                    value = singleUrlInput,
                    onValueChange = { singleUrlInput = it },
                    label = { Text("Paste YouTube Video URL") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = GeoPrimaryPurple) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    singleUrlInput = clip.getItemAt(0).text.toString()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Clipboard", tint = GeoPrimaryPurple)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryPurple,
                        unfocusedBorderColor = GeoBorderLilac,
                        focusedLabelColor = GeoPrimaryPurple,
                        focusedTextColor = GeoTextDark,
                        unfocusedTextColor = GeoTextDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_single_url_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customFocusInput,
                    onValueChange = { customFocusInput = it },
                    label = { Text("Optional Goal / Focus Area") },
                    placeholder = { Text("e.g. Chest & Triceps hypertrophy, 15 min") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryPurple,
                        unfocusedBorderColor = GeoBorderLilac,
                        focusedLabelColor = GeoPrimaryPurple,
                        focusedTextColor = GeoTextDark,
                        unfocusedTextColor = GeoTextDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick sample video chips
                Text(
                    text = "OR CHOOSE A POPULAR FITNESS VIDEO TO TEST:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = GeoTextMuted,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val sampleVideos = listOf(
                    Pair("10 Min Push-Up Mastery", "https://www.youtube.com/watch?v=IODxDxX7oi4"),
                    Pair("7 Min Six-Pack Torch", "https://www.youtube.com/watch?v=AnYl6Nk9GOA"),
                    Pair("15 Min Full Body Dumbbell", "https://www.youtube.com/watch?v=cbKkJWYCEo0"),
                    Pair("20 Min HIIT Cardio Blast", "https://www.youtube.com/watch?v=M0uO8X3_tEA")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleVideos.forEach { (name, url) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GeoSurfaceContainer)
                                .border(1.dp, GeoBorderLilac, RoundedCornerShape(12.dp))
                                .clickable { singleUrlInput = url }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = GeoPrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GeoTextDark
                                    )
                                }
                                Text(
                                    text = "SELECT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GeoPrimaryPurple
                                )
                            }
                        }
                    }
                }
            } else {
                // MULTI-LINK MERGER FLOW
                Text(
                    text = "Paste multiple YouTube URLs to combine into a superset routine:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeoTextMuted,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                multiUrlList.forEachIndexed { index, url ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { multiUrlList[index] = it },
                            label = { Text("Video Link #${index + 1}") },
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeoPrimaryPurple,
                                unfocusedBorderColor = GeoBorderLilac,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        if (multiUrlList.size > 1) {
                            IconButton(
                                onClick = { multiUrlList.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Link", tint = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { multiUrlList.add("") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoPrimaryContainer,
                        contentColor = GeoOnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Another YouTube Link", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customFocusInput,
                    onValueChange = { customFocusInput = it },
                    label = { Text("Combined Routine Goal (Optional)") },
                    placeholder = { Text("e.g. Chest Warmup + Core Finisher") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimaryPurple,
                        unfocusedBorderColor = GeoBorderLilac,
                        focusedTextColor = GeoTextDark,
                        unfocusedTextColor = GeoTextDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Generate Button
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        onSubmitSingleUrl(singleUrlInput, customFocusInput.takeIf { it.isNotBlank() })
                    } else {
                        onSubmitMultipleUrls(multiUrlList.toList(), customFocusInput.takeIf { it.isNotBlank() })
                    }
                },
                enabled = !isGenerating && (
                        (selectedTab == 0 && singleUrlInput.isNotBlank()) ||
                                (selectedTab == 1 && multiUrlList.any { it.isNotBlank() })
                        ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_workout_submit_btn"),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoPrimaryPurple,
                    contentColor = GeoTextWhite,
                    disabledContainerColor = Color(0xFFDDDDDD),
                    disabledContentColor = Color(0xFF888888)
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("EXTRACTING WORKOUT WITH AI...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTab == 0) "GENERATE WORKOUT" else "MERGE & GENERATE ROUTINE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

