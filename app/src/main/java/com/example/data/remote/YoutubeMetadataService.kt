package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

data class YoutubeVideoChapter(
    val title: String,
    val timestampSeconds: Int,
    val timestampFormatted: String,
    val durationSeconds: Int = 40,
    val restSeconds: Int = 20,
    val repsOrTime: String = "40 Seconds"
)

data class YoutubeVideoInfo(
    val videoId: String,
    val title: String,
    val authorName: String,
    val thumbnailUrl: String,
    val rawUrl: String,
    val description: String = "",
    val durationSeconds: Int = 0,
    val transcript: String = "",
    val parsedChapters: List<YoutubeVideoChapter> = emptyList()
)

object YoutubeMetadataService {
    private const val TAG = "YoutubeMetadataService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val YOUTUBE_PATTERNS = listOf(
        Pattern.compile("(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.)?youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("^([a-zA-Z0-9_-]{11})$")
    )

    fun extractVideoId(url: String): String? {
        val trimmed = url.trim()
        for (pattern in YOUTUBE_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun extractMultipleVideoIds(text: String): List<String> {
        val lines = text.split(Regex("[\n,;]+"))
        val ids = mutableListOf<String>()
        for (line in lines) {
            val id = extractVideoId(line.trim())
            if (id != null && !ids.contains(id)) {
                ids.add(id)
            }
        }
        return ids
    }

    fun getThumbnailUrl(videoId: String, quality: String = "hqdefault"): String {
        return "https://img.youtube.com/vi/$videoId/$quality.jpg"
    }

    fun getMaxResThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }

    suspend fun fetchVideoMetadata(videoId: String): YoutubeVideoInfo = withContext(Dispatchers.IO) {
        val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"
        val oembedUrl = "https://www.youtube.com/oembed?url=$youtubeUrl&format=json"

        var title = "YouTube Workout Video"
        var author = "Fitness Coach"
        var description = ""
        var durationSec = 0
        var transcriptText = ""
        val chapters = mutableListOf<YoutubeVideoChapter>()

        // 1. Fetch oEmbed for clean creator name and official title
        try {
            val oembedReq = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val oembedResp = client.newCall(oembedReq).execute()
            if (oembedResp.isSuccessful) {
                val body = oembedResp.body?.string()
                if (!body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    title = json.optString("title", title)
                    author = json.optString("author_name", author)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "OEmbed fetch failed for $videoId", e)
        }

        // 2. Fetch full HTML page to parse description, chapters, and captions/transcript
        try {
            val pageReq = Request.Builder()
                .url("https://www.youtube.com/watch?v=$videoId")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            val pageResp = client.newCall(pageReq).execute()
            if (pageResp.isSuccessful) {
                val html = pageResp.body?.string() ?: ""

                // Extract lengthSeconds
                val lenMatch = Regex("\"lengthSeconds\":\"(\\d+)\"").find(html)
                if (lenMatch != null) {
                    durationSec = lenMatch.groupValues[1].toIntOrNull() ?: 0
                }

                // Extract description from JSON schemas inside page
                val descMatch = Regex("\"shortDescription\":\"(.*?)\"").find(html)
                if (descMatch != null) {
                    description = unescapeJsonString(descMatch.groupValues[1])
                }

                // Extract structured chapters from macroMarkersListItemRenderer if present in HTML
                val structuredChapters = extractStructuredChaptersFromHtml(html)
                if (structuredChapters.isNotEmpty()) {
                    chapters.addAll(structuredChapters)
                }

                // If description is present and no structured chapters were found, parse from description text
                if (chapters.isEmpty() && description.isNotBlank()) {
                    val parsed = parseChaptersFromText(description)
                    if (parsed.isNotEmpty()) {
                        chapters.addAll(parsed)
                    }
                }

                // Extract captions / transcript if available
                transcriptText = extractTranscriptFromHtml(html)
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTML page metadata fetch failed for $videoId", e)
        }

        // Fallback title cleaning if still generic
        if (title.isBlank() || title == "YouTube Workout Video") {
            title = "Workout Session ($videoId)"
        }

        YoutubeVideoInfo(
            videoId = videoId,
            title = title,
            authorName = author,
            thumbnailUrl = getThumbnailUrl(videoId),
            rawUrl = youtubeUrl,
            description = description,
            durationSeconds = durationSec,
            transcript = transcriptText,
            parsedChapters = chapters
        )
    }

    private fun extractTranscriptFromHtml(html: String): String {
        try {
            // Find captionTracks in JSON
            val captionBaseUrlMatch = Regex(""""captionTracks"\s*:\s*\[\s*\{[^}]*?"baseUrl"\s*:\s*"([^"]+)"""").find(html)
            if (captionBaseUrlMatch != null) {
                val rawCaptionUrl = unescapeJsonString(captionBaseUrlMatch.groupValues[1])
                val fullCaptionUrl = if (rawCaptionUrl.contains("fmt=")) rawCaptionUrl else "$rawCaptionUrl&fmt=json3"

                val capReq = Request.Builder()
                    .url(fullCaptionUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val capResp = client.newCall(capReq).execute()
                if (capResp.isSuccessful) {
                    val capBody = capResp.body?.string() ?: ""
                    if (capBody.startsWith("{")) {
                        // json3 format
                        val json = JSONObject(capBody)
                        val events = json.optJSONArray("events") ?: JSONArray()
                        val sb = StringBuilder()
                        for (i in 0 until events.length()) {
                            val ev = events.getJSONObject(i)
                            val startMs = ev.optLong("tStartMs", 0L)
                            val segs = ev.optJSONArray("segs")
                            if (segs != null) {
                                val text = (0 until segs.length()).joinToString("") { segs.getJSONObject(it).optString("utf8", "") }.trim()
                                if (text.isNotBlank() && text != "\n") {
                                    val startSec = (startMs / 1000).toInt()
                                    sb.append("[${formatSecondsToTimestamp(startSec)}] $text\n")
                                }
                            }
                        }
                        return sb.toString()
                    } else if (capBody.contains("<text")) {
                        // XML format
                        val xmlRegex = Regex("""<text start="([\d\.]+)"[^>]*>(.*?)</text>""")
                        val matches = xmlRegex.findAll(capBody)
                        val sb = StringBuilder()
                        for (m in matches) {
                            val startSec = m.groupValues[1].toDoubleOrNull()?.toInt() ?: 0
                            val text = m.groupValues[2].replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").trim()
                            if (text.isNotBlank()) {
                                sb.append("[${formatSecondsToTimestamp(startSec)}] $text\n")
                            }
                        }
                        return sb.toString()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Transcript extraction failed", e)
        }
        return ""
    }

    private fun unescapeJsonString(input: String): String {
        return input
            .replace("\\n", "\n")
            .replace("\\r", "")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
    }

    private fun extractStructuredChaptersFromHtml(html: String): List<YoutubeVideoChapter> {
        val list = mutableListOf<YoutubeVideoChapter>()
        try {
            // Regex match macroMarkersListItemRenderer objects
            // "macroMarkersListItemRenderer":{"title":{"simpleText":"Diamond Push Ups"},"timeDescription":{"simpleText":"0:45"}...
            val markerRegex = Regex(""""macroMarkersListItemRenderer"\s*:\s*\{"title"\s*:\s*\{"simpleText"\s*:\s*"([^"]+)"\}\s*,\s*"timeDescription"\s*:\s*\{"simpleText"\s*:\s*"([^"]+)"""")
            val matches = markerRegex.findAll(html)
            for (match in matches) {
                val chTitle = unescapeJsonString(match.groupValues[1]).trim()
                val chTime = match.groupValues[2].trim()
                val seconds = parseTimestampToSeconds(chTime)

                if (isExerciseTitleValid(chTitle)) {
                    list.add(
                        YoutubeVideoChapter(
                            title = cleanExerciseName(chTitle),
                            timestampSeconds = seconds,
                            timestampFormatted = formatSecondsToTimestamp(seconds),
                            durationSeconds = 40,
                            restSeconds = 20,
                            repsOrTime = "40 Seconds"
                        )
                    )
                }
            }

            // Also match chapterRenderer style
            if (list.isEmpty()) {
                val chapterRegex = Regex(""""chapterRenderer"\s*:\s*\{"title"\s*:\s*\{"simpleText"\s*:\s*"([^"]+)"\}\s*,\s*"timeRangeStartMillis"\s*:\s*(\d+)""")
                val chMatches = chapterRegex.findAll(html)
                for (match in chMatches) {
                    val chTitle = unescapeJsonString(match.groupValues[1]).trim()
                    val startMillis = match.groupValues[2].toLongOrNull() ?: 0L
                    val seconds = (startMillis / 1000).toInt()

                    if (isExerciseTitleValid(chTitle)) {
                        list.add(
                            YoutubeVideoChapter(
                                title = cleanExerciseName(chTitle),
                                timestampSeconds = seconds,
                                timestampFormatted = formatSecondsToTimestamp(seconds),
                                durationSeconds = 40,
                                restSeconds = 20,
                                repsOrTime = "40 Seconds"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed extracting structured chapters", e)
        }
        return list
    }

    fun parseChaptersFromText(text: String): List<YoutubeVideoChapter> {
        val list = mutableListOf<YoutubeVideoChapter>()
        val lines = text.lines()
        val timestampRegex = Regex("""(?:(\d{1,2}):)?(\d{1,2}):(\d{2})""")

        for (line in lines) {
            val match = timestampRegex.find(line)
            if (match != null) {
                val tsString = match.value
                val seconds = parseTimestampToSeconds(tsString)
                var name = line.replace(match.value, "").trim()
                name = cleanExerciseName(name)

                if (isExerciseTitleValid(name)) {
                    var workDuration = 40
                    var restDuration = 20
                    var repsOrTime = "40 Seconds"

                    val workMatch = Regex("""(\d+)\s*(?:s|sec|seconds)\s*(?:work|on)?""", RegexOption.IGNORE_CASE).find(line)
                    if (workMatch != null) {
                        workDuration = workMatch.groupValues[1].toIntOrNull() ?: 40
                        repsOrTime = "$workDuration Seconds"
                    }

                    val restMatch = Regex("""(?:rest|break|off)\s*(?:for\s*)?(\d+)\s*(?:s|sec|seconds)?""", RegexOption.IGNORE_CASE).find(line)
                    if (restMatch != null) {
                        restDuration = restMatch.groupValues[1].toIntOrNull() ?: 20
                    }

                    val repsMatch = Regex("""(\d+)\s*reps?""", RegexOption.IGNORE_CASE).find(line)
                    if (repsMatch != null) {
                        val count = repsMatch.groupValues[1]
                        repsOrTime = "$count Reps"
                        workDuration = 0
                    }

                    list.add(
                        YoutubeVideoChapter(
                            title = name,
                            timestampSeconds = seconds,
                            timestampFormatted = formatSecondsToTimestamp(seconds),
                            durationSeconds = workDuration,
                            restSeconds = restDuration,
                            repsOrTime = repsOrTime
                        )
                    )
                }
            }
        }

        // If no timestamps were found, check if there's a numbered list of exercises (e.g. 1. Push-ups, 2. Squats)
        if (list.isEmpty()) {
            val numberedRegex = Regex("""^\s*(?:#|exercise\s*)?(\d{1,2})[\.\)\-:]\s*(.+)""", RegexOption.IGNORE_CASE)
            var autoTimestamp = 15
            for (line in lines) {
                val nMatch = numberedRegex.find(line)
                if (nMatch != null) {
                    val exName = cleanExerciseName(nMatch.groupValues[2])
                    if (isExerciseTitleValid(exName)) {
                        list.add(
                            YoutubeVideoChapter(
                                title = exName,
                                timestampSeconds = autoTimestamp,
                                timestampFormatted = formatSecondsToTimestamp(autoTimestamp),
                                durationSeconds = 40,
                                restSeconds = 20,
                                repsOrTime = "40 Seconds"
                            )
                        )
                        autoTimestamp += 60
                    }
                }
            }
        }

        return list
    }

    private fun isExerciseTitleValid(title: String): Boolean {
        val lower = title.lowercase()
        if (title.length < 3) return false
        if (lower.contains("http") || lower.contains("www.") || lower.contains("instagram") ||
            lower.contains("subscribe") || lower.contains("patreon") || lower.contains("merch") ||
            lower.contains("sponsor") || lower.contains("tiktok") || lower.contains("twitter") ||
            lower.contains("discount") || lower.contains("free app") || lower.contains("music by")
        ) {
            return false
        }
        // Exclude pure intro/outro if desired, but keep if user wants full flow
        if (lower == "intro" || lower == "introduction" || lower == "outro" || lower == "subscribe") {
            return false
        }
        return true
    }

    private fun cleanExerciseName(rawName: String): String {
        return rawName
            .replace(Regex("""^\s*[\(\[\{]?\d{1,2}:\d{2}(?::\d{2})?[\)\]\}]?\s*"""), "")
            .replace(Regex("""\s*[\(\[\{]?\d{1,2}:\d{2}(?::\d{2})?[\)\]\}]?\s*$"""), "")
            .trimStart('-', ':', '•', '|', '>', ' ', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.', ')', '(', '[', ']', '#')
            .trimEnd('-', ':', '•', '|', '<', ' ', '.', ')', '(', '[', ']')
            .trim()
    }

    fun formatSecondsToTimestamp(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    fun parseTimestampToSeconds(timestamp: String): Int {
        val parts = timestamp.trim().split(":")
        return when (parts.size) {
            1 -> parts[0].toIntOrNull() ?: 0
            2 -> {
                val mins = parts[0].toIntOrNull() ?: 0
                val secs = parts[1].toIntOrNull() ?: 0
                mins * 60 + secs
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: 0
                val mins = parts[1].toIntOrNull() ?: 0
                val secs = parts[2].toIntOrNull() ?: 0
                hours * 3600 + mins * 60 + secs
            }
            else -> 0
        }
    }
}
