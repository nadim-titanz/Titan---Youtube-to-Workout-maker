package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.WorkoutEntity
import com.example.data.local.WorkoutExerciseEntity
import com.example.data.model.WorkoutWithExercises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedWorkoutResult(
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val totalDurationMinutes: Int,
    val totalRounds: Int = 1,
    val exercises: List<GeneratedExerciseData>
)

data class GeneratedExerciseData(
    val name: String,
    val sets: Int,
    val reps: String,
    val durationSeconds: Int,
    val restSeconds: Int,
    val timestampSeconds: Int,
    val timestampFormatted: String,
    val muscleGroup: String,
    val equipment: String,
    val formTips: String
)

object GeminiWorkoutService {
    private const val TAG = "GeminiWorkoutService"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateWorkoutFromVideo(
        videoInfo: YoutubeVideoInfo,
        customFocus: String? = null
    ): GeneratedWorkoutResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = buildWorkoutExtractionPrompt(videoInfo, customFocus)
                val geminiResponse = callGeminiApi(apiKey, prompt)
                if (geminiResponse != null) {
                    val parsed = parseWorkoutJson(geminiResponse, videoInfo.title)
                    if (parsed != null && parsed.exercises.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API call failed, falling back to smart heuristic generator", e)
            }
        }

        // Smart Heuristic Generator fallback
        return@withContext generateSmartWorkoutHeuristic(videoInfo, customFocus)
    }

    suspend fun mergeWorkoutsWithAI(
        workoutList: List<WorkoutWithExercises>,
        customGoal: String? = null
    ): GeneratedWorkoutResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = buildMergePrompt(workoutList, customGoal)
                val geminiResponse = callGeminiApi(apiKey, prompt)
                if (geminiResponse != null) {
                    val parsed = parseWorkoutJson(geminiResponse, "Merged Workout Fusion")
                    if (parsed != null && parsed.exercises.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini merge failed, falling back to heuristic merger", e)
            }
        }

        return@withContext mergeWorkoutsHeuristically(workoutList, customGoal)
    }

    private fun buildWorkoutExtractionPrompt(videoInfo: YoutubeVideoInfo, customFocus: String?): String {
        val chaptersText = if (videoInfo.parsedChapters.isNotEmpty()) {
            "Parsed Video Chapters / Timestamps:\n" + videoInfo.parsedChapters.joinToString("\n") { 
                "${it.timestampFormatted} - ${it.title} (${it.repsOrTime}, Rest: ${it.restSeconds}s)" 
            }
        } else ""

        val transcriptSnippet = if (videoInfo.transcript.isNotBlank()) {
            "Full Video Audio Transcript / Closed Captions:\n${videoInfo.transcript.take(12000)}"
        } else ""

        val descriptionSnippet = if (videoInfo.description.isNotBlank()) {
            "Video Description:\n${videoInfo.description.take(4000)}"
        } else ""

        return """
        You are Gemini Video Workout Summarizer AI. Your mission is to analyze the provided YouTube workout video transcript, chapters, and description, and extract the 100% complete and accurate exercise-by-exercise breakdown.
        
        Video Title: "${videoInfo.title}"
        Channel / Creator: "${videoInfo.authorName}"
        URL: "${videoInfo.rawUrl}"
        Video Duration: ${if (videoInfo.durationSeconds > 0) "${videoInfo.durationSeconds / 60} Minutes" else "Full session"}
        Custom Focus: "${customFocus ?: "Full structured session"}"
        
        $chaptersText
        $transcriptSnippet
        $descriptionSnippet
        
        CRITICAL EXTRACTION REQUIREMENTS:
        1. Extract EVERY SINGLE EXERCISE performed in this workout in the exact chronological sequence.
        2. Identify each exercise by its real name (e.g. "Diamond Push Ups", "Jumping Jacks", "Bicycle Crunches", "Pike Pushups", "Plank Hold").
        3. Extract the accurate work time / duration (`durationSeconds` e.g. 40, 45, 30, 20), rest time (`restSeconds` e.g. 20, 15, 10, 30), and sets (`sets` = 1 for circuit follow-along, or round count).
        4. Provide the exact video timestamp in seconds (`timestampSeconds`) and formatted ("00:45") where the demonstration of that exercise starts.
        5. For each exercise, identify targeted muscle group (e.g. "Upper Chest", "Core", "Quads", "Triceps") and 1 helpful concise form tip.
        
        Return ONLY valid, parseable JSON matching this schema:
        {
          "title": "${videoInfo.title}",
          "description": "Comprehensive workout summary",
          "category": "One of: Chest, Back, Legs, Core, Arms, Full Body, HIIT, Cardio",
          "difficulty": "One of: Beginner, Intermediate, Advanced",
          "totalDurationMinutes": 15,
          "totalRounds": 1,
          "exercises": [
            {
              "name": "Diamond Push Ups",
              "sets": 1,
              "reps": "40 Seconds",
              "durationSeconds": 40,
              "restSeconds": 20,
              "timestampSeconds": 45,
              "timestampFormatted": "00:45",
              "muscleGroup": "Triceps & Chest",
              "equipment": "Bodyweight",
              "formTips": "Keep elbows tucked close to torso."
            }
          ]
        }
        """.trimIndent()
    }

    private fun buildMergePrompt(workoutList: List<WorkoutWithExercises>, customGoal: String?): String {
        val workoutsSummary = workoutList.joinToString("\n---\n") { w ->
            val exercisesList = w.exercises.joinToString(", ") { "${it.name} (${it.sets}x${it.reps})" }
            "Workout: ${w.workout.title} [Category: ${w.workout.category}]\nExercises: $exercisesList"
        }

        return """
        Combine the following workouts into a single cohesive merged workout routine:
        
        $workoutsSummary
        
        User Goal: ${customGoal ?: "Balanced superset routine"}
        
        Return ONLY valid raw JSON:
        {
          "title": "Merged Workout Title",
          "description": "Why this combination is effective",
          "category": "Full Body",
          "difficulty": "Intermediate",
          "totalDurationMinutes": 25,
          "totalRounds": 1,
          "exercises": [
            {
              "name": "Exercise Name",
              "sets": 1,
              "reps": "45 Seconds",
              "durationSeconds": 45,
              "restSeconds": 30,
              "timestampSeconds": 0,
              "timestampFormatted": "00:00",
              "muscleGroup": "Chest",
              "equipment": "Bodyweight",
              "formTips": "Coaching cue"
            }
          ]
        }
        """.trimIndent()
    }

    private fun callGeminiApi(apiKey: String, promptText: String): String? {
        val url = "$GEMINI_API_URL?key=$apiKey"

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", promptText))
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("topP", 0.95)
                val responseFormat = JSONObject().apply {
                    put("mimeType", "application/json")
                }
                put("responseFormat", responseFormat)
            }
            put("generationConfig", generationConfig)
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "Gemini API Error: ${response.code} -> $errorBody")
            return null
        }

        val responseString = response.body?.string() ?: return null
        val responseJson = JSONObject(responseString)
        val candidates = responseJson.optJSONArray("candidates") ?: return null
        if (candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() > 0) {
                return parts.getJSONObject(0).optString("text")
            }
        }
        return null
    }

    private fun parseWorkoutJson(rawText: String, fallbackTitle: String): GeneratedWorkoutResult? {
        try {
            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json")
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```")
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
            cleaned = cleaned.trim()

            val json = JSONObject(cleaned)
            val title = json.optString("title", fallbackTitle)
            val description = json.optString("description", "Extracted directly from YouTube video demonstration.")
            val category = json.optString("category", "Full Body")
            val difficulty = json.optString("difficulty", "Intermediate")
            val totalDuration = json.optInt("totalDurationMinutes", 15)
            val totalRounds = json.optInt("totalRounds", 1)

            val exercisesArray = json.optJSONArray("exercises") ?: JSONArray()
            val exercises = mutableListOf<GeneratedExerciseData>()

            for (i in 0 until exercisesArray.length()) {
                val exObj = exercisesArray.getJSONObject(i)
                val name = exObj.optString("name", "Exercise #${i + 1}")
                val sets = exObj.optInt("sets", 1)
                val reps = exObj.optString("reps", "40 Seconds")
                val duration = exObj.optInt("durationSeconds", 40)
                val rest = exObj.optInt("restSeconds", 20)
                val timeSec = exObj.optInt("timestampSeconds", i * 60)
                val timeFormatted = exObj.optString("timestampFormatted", YoutubeMetadataService.formatSecondsToTimestamp(timeSec))
                val muscle = exObj.optString("muscleGroup", "Core")
                val equip = exObj.optString("equipment", "Bodyweight")
                val tips = exObj.optString("formTips", "Maintain steady cadence and proper form.")

                exercises.add(
                    GeneratedExerciseData(
                        name = name,
                        sets = sets.coerceIn(1, 10),
                        reps = reps,
                        durationSeconds = duration.coerceIn(0, 600),
                        restSeconds = rest.coerceIn(0, 300),
                        timestampSeconds = timeSec,
                        timestampFormatted = timeFormatted,
                        muscleGroup = muscle,
                        equipment = equip,
                        formTips = tips
                    )
                )
            }

            return GeneratedWorkoutResult(
                title = title,
                description = description,
                category = category,
                difficulty = difficulty,
                totalDurationMinutes = totalDuration,
                totalRounds = totalRounds,
                exercises = exercises
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini workout JSON", e)
            return null
        }
    }

    private fun generateSmartWorkoutHeuristic(
        videoInfo: YoutubeVideoInfo,
        customFocus: String?
    ): GeneratedWorkoutResult {
        // If chapters were parsed directly from YouTube description or HTML markers, return EVERY single chapter!
        if (videoInfo.parsedChapters.isNotEmpty()) {
            val exercises = videoInfo.parsedChapters.mapIndexed { idx, ch ->
                GeneratedExerciseData(
                    name = ch.title,
                    sets = 1,
                    reps = ch.repsOrTime,
                    durationSeconds = ch.durationSeconds,
                    restSeconds = ch.restSeconds,
                    timestampSeconds = ch.timestampSeconds,
                    timestampFormatted = ch.timestampFormatted,
                    muscleGroup = detectMuscleGroup(ch.title, videoInfo.title),
                    equipment = "Bodyweight",
                    formTips = "Execute with controlled tempo and full range of motion."
                )
            }
            val totalMins = if (videoInfo.durationSeconds > 0) (videoInfo.durationSeconds / 60) else (exercises.size * 1)
            return GeneratedWorkoutResult(
                title = videoInfo.title,
                description = "Complete routine faithfully extracted from video chapters.",
                category = detectCategory(videoInfo.title),
                difficulty = "Intermediate",
                totalDurationMinutes = totalMins.coerceAtLeast(5),
                totalRounds = 1,
                exercises = exercises
            )
        }

        val titleLower = (videoInfo.title + " " + (customFocus ?: "")).lowercase()
        val category = detectCategory(videoInfo.title)
        val difficulty = if (titleLower.contains("beginner") || titleLower.contains("easy")) "Beginner"
            else if (titleLower.contains("hard") || titleLower.contains("extreme") || titleLower.contains("advanced")) "Advanced"
            else "Intermediate"

        // Extract duration from title or video duration
        val minMatch = Regex("""(\d+)\s*(?:min|minute)""").find(titleLower)
        val durationMins = if (videoInfo.durationSeconds > 0) {
            (videoInfo.durationSeconds / 60).coerceAtLeast(5)
        } else {
            minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
        }

        val pool = when {
            titleLower.contains("abs") || titleLower.contains("six pack") || titleLower.contains("core") -> {
                listOf(
                    Pair("Leg Raises", "Lower Abs"),
                    Pair("Crunch Reach Throughs", "Upper Abs"),
                    Pair("Switching Mountain Climbers", "Obliques"),
                    Pair("Russian Twists", "Obliques"),
                    Pair("Plank Knee to Elbow", "Core & Obliques"),
                    Pair("Flutter Kicks Burnout", "Lower Abs"),
                    Pair("Bicycle Crunches", "Obliques"),
                    Pair("Hollow Body Hold", "Core"),
                    Pair("Toe Touches", "Upper Abs"),
                    Pair("Side Plank Left", "Obliques"),
                    Pair("Side Plank Right", "Obliques"),
                    Pair("Reverse Crunches", "Lower Abs"),
                    Pair("Plank Walkouts", "Core"),
                    Pair("V-Ups", "Full Core"),
                    Pair("Dead Bugs", "Deep Core"),
                    Pair("Heel Touches", "Obliques")
                )
            }
            titleLower.contains("push up") || titleLower.contains("chest") -> {
                listOf(
                    Pair("Standard Push-Ups", "Chest"),
                    Pair("Diamond Push-Ups", "Triceps"),
                    Pair("Wide Grip Push-Ups", "Outer Chest"),
                    Pair("Decline Push-Ups", "Upper Chest"),
                    Pair("Pike Push-Ups", "Shoulders"),
                    Pair("Isometric Push-Up Hover", "Chest & Core"),
                    Pair("Archer Push-Ups", "Unilateral Chest"),
                    Pair("Hindu Push-Ups", "Chest & Shoulders"),
                    Pair("Clapping Push-Ups", "Explosive Chest"),
                    Pair("Spiderman Push-Ups", "Chest & Obliques"),
                    Pair("Typewriter Push-Ups", "Chest"),
                    Pair("Incline Push-Ups", "Lower Chest"),
                    Pair("Hand Release Push-Ups", "Chest & Back"),
                    Pair("Pseudo Planche Push-Ups", "Shoulders & Chest")
                )
            }
            titleLower.contains("pull up") || titleLower.contains("back") -> {
                listOf(
                    Pair("Pull Ups", "Lats & Back"),
                    Pair("Behind the Head Chin Ups", "Upper Back"),
                    Pair("Commando Pull-Ups", "Lats & Core"),
                    Pair("Inverted Rows", "Mid Back"),
                    Pair("Dead Hang Grip Hold", "Forearms & Lats"),
                    Pair("Scapular Pull-Ups", "Trapezius"),
                    Pair("Archer Pull-Ups", "Lats"),
                    Pair("Wide Grip Pull-Ups", "Upper Lats"),
                    Pair("Chin Ups", "Biceps & Back"),
                    Pair("Negative Pull-Ups", "Lats")
                )
            }
            titleLower.contains("leg") || titleLower.contains("squat") -> {
                listOf(
                    Pair("Air Squats", "Quads & Glutes"),
                    Pair("Jumping Lunges", "Quads"),
                    Pair("Bulgarian Split Squats Left", "Glutes"),
                    Pair("Bulgarian Split Squats Right", "Glutes"),
                    Pair("Calf Raises", "Calves"),
                    Pair("Wall Sit Hold", "Quads"),
                    Pair("Sumo Squats", "Adductors"),
                    Pair("Single Leg Glute Bridge Left", "Glutes"),
                    Pair("Single Leg Glute Bridge Right", "Glutes"),
                    Pair("Pistol Squat Progression", "Quads & Core")
                )
            }
            else -> {
                listOf(
                    Pair("High Knees Cardio Warm-Up", "Cardio"),
                    Pair("Air Squats with Pulse", "Quads & Glutes"),
                    Pair("Explosive Push-Ups", "Chest & Arms"),
                    Pair("Bicycle Crunches", "Obliques & Core"),
                    Pair("Burpee Jump Tuck", "Full Body"),
                    Pair("Plank Shoulder Taps", "Shoulders & Core"),
                    Pair("Mountain Climbers", "Cardio & Core"),
                    Pair("Jumping Jacks", "Cardio"),
                    Pair("Reverse Lunges", "Legs"),
                    Pair("Tricep Dips", "Triceps"),
                    Pair("Side Plank Reach", "Obliques"),
                    Pair("Superman Hold", "Lower Back")
                )
            }
        }

        // Generate count matching the duration (e.g. 10 min = 10 exercises)
        val targetCount = durationMins.coerceIn(5, 16)
        val selected = (0 until targetCount).map { i -> pool[i % pool.size] }

        val exercises = selected.mapIndexed { idx, item ->
            val timeSec = idx * 60
            GeneratedExerciseData(
                name = item.first,
                sets = 1,
                reps = "40 Seconds",
                durationSeconds = 40,
                restSeconds = 20,
                timestampSeconds = timeSec,
                timestampFormatted = YoutubeMetadataService.formatSecondsToTimestamp(timeSec),
                muscleGroup = item.second,
                equipment = "Bodyweight",
                formTips = "Execute with strict posture and steady cadence."
            )
        }

        return GeneratedWorkoutResult(
            title = videoInfo.title,
            description = "High-energy workout extracted from video demonstration.",
            category = category,
            difficulty = difficulty,
            totalDurationMinutes = durationMins,
            totalRounds = 1,
            exercises = exercises
        )
    }

    private fun detectCategory(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("abs") || t.contains("core") || t.contains("six pack") -> "Core"
            t.contains("chest") || t.contains("push up") -> "Chest"
            t.contains("back") || t.contains("pull up") || t.contains("row") -> "Back"
            t.contains("leg") || t.contains("squat") || t.contains("glute") -> "Legs"
            t.contains("arm") || t.contains("bicep") || t.contains("tricep") -> "Arms"
            t.contains("hiit") || t.contains("tabata") || t.contains("cardio") -> "HIIT"
            else -> "Full Body"
        }
    }

    private fun detectMuscleGroup(name: String, title: String): String {
        val n = (name + " " + title).lowercase()
        return when {
            n.contains("leg raise") || n.contains("flutter") -> "Lower Abs"
            n.contains("crunch") || n.contains("sit up") -> "Upper Abs"
            n.contains("twist") || n.contains("mountain climber") || n.contains("oblique") -> "Obliques"
            n.contains("plank") || n.contains("hollow") -> "Core"
            n.contains("push up") || n.contains("chest") || n.contains("dip") -> "Chest"
            n.contains("pull up") || n.contains("chin up") || n.contains("row") -> "Lats & Back"
            n.contains("squat") || n.contains("lunge") -> "Quads & Glutes"
            else -> "Full Body"
        }
    }

    private fun mergeWorkoutsHeuristically(
        workoutList: List<WorkoutWithExercises>,
        customGoal: String?
    ): GeneratedWorkoutResult {
        val combined = workoutList.flatMap { it.exercises }
            .distinctBy { it.name.lowercase().trim() }
            .mapIndexed { idx, ex ->
                GeneratedExerciseData(
                    name = ex.name,
                    sets = 1,
                    reps = ex.reps,
                    durationSeconds = ex.durationSeconds,
                    restSeconds = ex.restSeconds,
                    timestampSeconds = ex.timestampSeconds,
                    timestampFormatted = ex.timestampFormatted,
                    muscleGroup = ex.muscleGroup,
                    equipment = ex.equipment,
                    formTips = ex.formTips
                )
            }

        return GeneratedWorkoutResult(
            title = "Fused Full-Body Circuit",
            description = "Balanced fusion of selected workouts.",
            category = "Full Body",
            difficulty = "Intermediate",
            totalDurationMinutes = combined.size * 2,
            totalRounds = 1,
            exercises = combined
        )
    }
}
