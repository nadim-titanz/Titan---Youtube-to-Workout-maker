package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.auth.UserProfile
import com.example.data.remote.YoutubeMetadataService
import com.example.ui.theme.FitBlack
import com.example.ui.theme.FitCardBackground
import com.example.ui.theme.FitCardElevated
import com.example.ui.theme.FitRedAccent
import com.example.ui.theme.FitTextMuted
import com.example.ui.theme.FitTextPrimary
import com.example.ui.theme.FitTextSecondary
import com.example.ui.theme.FitWhite

enum class PlayerEngineMode {
    CLEAN_VIDEO,    // Full frame YouTube video with 0 website UI (pure video only)
    WEB_FRAME,      // Direct clean embed frame
    APP_LAUNCHER    // 1-Tap Watch in YouTube App
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWorkoutVideoPlayer(
    videoId: String,
    timestampSeconds: Int = 0,
    isPlaying: Boolean = true,
    exerciseName: String? = null,
    timestampFormatted: String? = null,
    currentUser: UserProfile? = null,
    onSignInRequested: () -> Unit = {},
    currentMode: PlayerEngineMode = PlayerEngineMode.CLEAN_VIDEO,
    onModeChanged: (PlayerEngineMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isPlayerLoading by remember { mutableStateOf(true) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadCount by remember { mutableIntStateOf(0) }

    fun launchExternalYouTube(targetTimestampSec: Int = timestampSeconds) {
        if (videoId.isBlank()) return
        val secParam = if (targetTimestampSec > 0) "&t=${targetTimestampSec}s" else ""
        val appUri = Uri.parse("vnd.youtube:$videoId?t=$targetTimestampSec")
        val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoId$secParam")

        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {}
        }
    }

    // Seek or update when timestamp changes
    LaunchedEffect(videoId, timestampSeconds, reloadCount, currentMode) {
        hasPlaybackError = false
        isPlayerLoading = true
        if (webViewInstance != null && videoId.isNotBlank()) {
            if (currentMode == PlayerEngineMode.CLEAN_VIDEO) {
                // Seek inside iframe using YouTube JS postMessage or seek API
                val jsSeek = """
                    (function() {
                        try {
                            var iframe = document.getElementById('youtube-player-frame');
                            if (iframe && iframe.contentWindow) {
                                iframe.contentWindow.postMessage(JSON.stringify({
                                    "event": "command",
                                    "func": "seekTo",
                                    "args": [$timestampSeconds, true]
                                }), "*");
                            }
                        } catch(e) {}
                    })();
                """.trimIndent()
                webViewInstance?.evaluateJavascript(jsSeek, null)
            }
        }
    }

    // Play / Pause video frame on state change
    LaunchedEffect(isPlaying) {
        if (webViewInstance != null) {
            val command = if (isPlaying) "playVideo" else "pauseVideo"
            val jsPlayPause = """
                (function() {
                    try {
                        var iframe = document.getElementById('youtube-player-frame');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "$command",
                                "args": []
                            }), "*");
                        }
                    } catch(e) {}
                })();
            """.trimIndent()
            webViewInstance?.evaluateJavascript(jsPlayPause, null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FitCardBackground)
    ) {
        // Main 16:9 Video Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(FitBlack),
            contentAlignment = Alignment.Center
        ) {
            when (currentMode) {
                PlayerEngineMode.CLEAN_VIDEO -> {
                    // Full-Frame Embedded Video (Only Video, NO YouTube website UI, search bar, or comments)
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("clean_video_player"),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(0xFF000000.toInt())

                                CookieManager.getInstance().setAcceptCookie(true)
                                try {
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                } catch (e: Exception) {}

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun getDefaultVideoPoster(): Bitmap? {
                                        return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isPlayerLoading = false
                                    }
                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        if (request?.isForMainFrame == true) {
                                            hasPlaybackError = true
                                            isPlayerLoading = false
                                        }
                                    }
                                }

                                // Full frame clean HTML wrapper that eliminates all margins, padding, and website UI
                                val cleanHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                            * { margin: 0; padding: 0; box-sizing: border-box; }
                                            html, body {
                                                width: 100%;
                                                height: 100%;
                                                background: #000000;
                                                overflow: hidden;
                                                display: flex;
                                                align-items: center;
                                                justify-content: center;
                                            }
                                            iframe {
                                                position: absolute;
                                                top: 0;
                                                left: 0;
                                                width: 100%;
                                                height: 100%;
                                                border: 0;
                                                outline: none;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe 
                                            id="youtube-player-frame"
                                            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&start=$timestampSeconds&controls=1&rel=0&modestbranding=1&enablejsapi=1&fs=0"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                            allowfullscreen>
                                        </iframe>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("https://www.youtube-nocookie.com", cleanHtml, "text/html", "UTF-8", "https://www.youtube-nocookie.com")
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            // If videoId or timestamp changes
                            val cleanHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        html, body {
                                            width: 100%;
                                            height: 100%;
                                            background: #000000;
                                            overflow: hidden;
                                            display: flex;
                                            align-items: center;
                                            justify-content: center;
                                        }
                                        iframe {
                                            position: absolute;
                                            top: 0;
                                            left: 0;
                                            width: 100%;
                                            height: 100%;
                                            border: 0;
                                            outline: none;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <iframe 
                                        id="youtube-player-frame"
                                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&start=$timestampSeconds&controls=1&rel=0&modestbranding=1&enablejsapi=1&fs=0"
                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                        allowfullscreen>
                                    </iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            view.loadDataWithBaseURL("https://www.youtube-nocookie.com", cleanHtml, "text/html", "UTF-8", "https://www.youtube-nocookie.com")
                        }
                    )
                }

                PlayerEngineMode.WEB_FRAME -> {
                    // Direct YouTube Embed Frame URL (fills entire container)
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("web_frame_player"),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(0xFF000000.toInt())
                                CookieManager.getInstance().setAcceptCookie(true)
                                try {
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                } catch (e: Exception) {}

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isPlayerLoading = false
                                        // Inject CSS to ensure absolute full screen frame with no headers
                                        val cssInject = """
                                            javascript:(function() {
                                                var css = 'html, body, #player, .html5-video-player, video { width: 100% !important; height: 100% !important; margin: 0 !important; padding: 0 !important; background: #000 !important; }';
                                                var head = document.head || document.getElementsByTagName('head')[0];
                                                var style = document.createElement('style');
                                                style.type = 'text/css';
                                                style.appendChild(document.createTextNode(css));
                                                head.appendChild(style);
                                            })()
                                        """.trimIndent()
                                        view?.loadUrl(cssInject)
                                    }
                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        if (request?.isForMainFrame == true) {
                                            hasPlaybackError = true
                                            isPlayerLoading = false
                                        }
                                    }
                                }
                                val directEmbedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&start=$timestampSeconds&controls=1&rel=0&modestbranding=1"
                                val extraHeaders = mapOf(
                                    "Referer" to "https://www.youtube.com",
                                    "Origin" to "https://www.youtube.com"
                                )
                                loadUrl(directEmbedUrl, extraHeaders)
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            val directEmbedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&start=$timestampSeconds&controls=1&rel=0&modestbranding=1"
                            val extraHeaders = mapOf(
                                "Referer" to "https://www.youtube.com",
                                "Origin" to "https://www.youtube.com"
                            )
                            view.loadUrl(directEmbedUrl, extraHeaders)
                        }
                    )
                }

                PlayerEngineMode.APP_LAUNCHER -> {
                    // Visual Poster Card with 1-Tap App Launcher
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FitBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(YoutubeMetadataService.getThumbnailUrl(videoId, "hqdefault"))
                                .crossfade(true)
                                .build(),
                            contentDescription = exerciseName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(FitRedAccent)
                                    .clickable { launchExternalYouTube() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play in YouTube",
                                    tint = FitWhite,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Text(
                                text = "Open in YouTube App",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FitWhite
                            )

                            if (timestampFormatted != null) {
                                Text(
                                    text = "Starts at $timestampFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FitTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Error / Fallback Overlay
            if (hasPlaybackError && currentMode != PlayerEngineMode.APP_LAUNCHER) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FitBlack),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(YoutubeMetadataService.getThumbnailUrl(videoId, "hqdefault"))
                            .crossfade(true)
                            .build(),
                        contentDescription = exerciseName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Button(
                            onClick = { launchExternalYouTube() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FitRedAccent,
                                contentColor = FitWhite
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (timestampFormatted != null) "Watch in YouTube ($timestampFormatted)" else "Watch in YouTube",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = errorMessage ?: "Owner restricted direct embedding • Tap above to watch in YouTube",
                            style = MaterialTheme.typography.labelSmall,
                            color = FitWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Account / Sign-In Quick Banner if guest
        if (currentUser != null && !currentUser.isSignedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222228))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Account",
                        tint = FitRedAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sign in with Google for YouTube sync",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FitWhite
                    )
                }

                Button(
                    onClick = onSignInRequested,
                    colors = ButtonDefaults.buttonColors(containerColor = FitWhite, contentColor = FitBlack),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Sign In", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        } else if (currentUser != null && currentUser.isSignedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF19221C))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Signed in as ${currentUser.name}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4ADE80)
                    )
                )
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FitCardElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FitRedAccent)
                    )
                    Text(
                        text = exerciseName ?: "Exercise Demonstration",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FitWhite
                        ),
                        maxLines = 1
                    )
                    if (!timestampFormatted.isNullOrEmpty()) {
                        Text(
                            text = "• $timestampFormatted",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FitTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Quick Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind to timestamp start
                    IconButton(
                        onClick = {
                            try {
                                val jsSeek = """
                                    (function() {
                                        try {
                                            var iframe = document.getElementById('youtube-player-frame');
                                            if (iframe && iframe.contentWindow) {
                                                iframe.contentWindow.postMessage(JSON.stringify({
                                                    "event": "command",
                                                    "func": "seekTo",
                                                    "args": [$timestampSeconds, true]
                                                }), "*");
                                            }
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                webViewInstance?.evaluateJavascript(jsSeek, null)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Restart Timestamp",
                            tint = FitTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Reload
                    IconButton(
                        onClick = {
                            hasPlaybackError = false
                            reloadCount++
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Video",
                            tint = FitTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Open directly in YouTube App
                    IconButton(
                        onClick = { launchExternalYouTube() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in YouTube App",
                            tint = FitWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Mode Selector Pills (Persisted across exercises)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player Mode:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = FitTextSecondary
                )

                // 1. Clean Video (Full Screen in Frame, Zero YouTube UI)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentMode == PlayerEngineMode.CLEAN_VIDEO) FitWhite else Color(0xFF26262B))
                        .clickable { onModeChanged(PlayerEngineMode.CLEAN_VIDEO) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("mode_clean_video")
                ) {
                    Text(
                        text = "Full Frame",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (currentMode == PlayerEngineMode.CLEAN_VIDEO) FitBlack else FitWhite
                    )
                }

                // 2. Direct Embed Frame
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentMode == PlayerEngineMode.WEB_FRAME) FitWhite else Color(0xFF26262B))
                        .clickable { onModeChanged(PlayerEngineMode.WEB_FRAME) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("mode_web_frame")
                ) {
                    Text(
                        text = "Web Embed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (currentMode == PlayerEngineMode.WEB_FRAME) FitBlack else FitWhite
                    )
                }

                // 3. YouTube App
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentMode == PlayerEngineMode.APP_LAUNCHER) FitRedAccent else Color(0xFF26262B))
                        .clickable { onModeChanged(PlayerEngineMode.APP_LAUNCHER) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("mode_app_launcher")
                ) {
                    Text(
                        text = "YouTube App",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = FitWhite
                    )
                }
            }
        }
    }
}
