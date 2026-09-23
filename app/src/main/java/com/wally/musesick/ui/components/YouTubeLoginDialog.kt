package com.wally.musesick.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val YT_MUSIC_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2F%3Fapp%3Ddesktop"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginDialog(
    onLoginSuccess: (cookie: String, fallbackName: String?, fallbackAvatarUrl: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isRecoveringConnection by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }
    var latestDetectedCookie by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastUrl by remember { mutableStateOf<String?>(null) }

    fun getCombinedYouTubeCookie(): String? {
        val cm = CookieManager.getInstance()
        val c1 = cm.getCookie("https://music.youtube.com").orEmpty()
        val c2 = cm.getCookie("https://www.youtube.com").orEmpty()
        val c3 = cm.getCookie("https://m.youtube.com").orEmpty()
        val c4 = cm.getCookie("https://accounts.youtube.com").orEmpty()
        val c5 = cm.getCookie("https://accounts.google.com").orEmpty()
        val c6 = cm.getCookie("https://www.google.com").orEmpty()

        val cookieMap = LinkedHashMap<String, String>()
        listOf(c6, c5, c4, c3, c2, c1).forEach { raw ->
            raw.split(";").forEach { part ->
                val trimmed = part.trim()
                val eq = trimmed.indexOf('=')
                if (eq > 0) {
                    val k = trimmed.substring(0, eq).trim()
                    val v = trimmed.substring(eq + 1).trim()
                    if (k.isNotEmpty() && v.isNotEmpty()) {
                        cookieMap[k] = v
                    }
                }
            }
        }
        if (cookieMap.isEmpty()) return null
        return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    fun hasValidAuthCookie(cookieStr: String?): Boolean {
        if (cookieStr.isNullOrBlank()) return false
        val hasSapisid = cookieStr.contains("SAPISID=") ||
                cookieStr.contains("__Secure-3PAPISID=") ||
                cookieStr.contains("__Secure-1PAPISID=")
        val hasSid = cookieStr.contains("SID=") ||
                cookieStr.contains("__Secure-1PSID=") ||
                cookieStr.contains("__Secure-3PSID=")
        return hasSapisid && hasSid
    }

    fun tryCompleteIfAuthenticated(url: String? = lastUrl, forceIfCookiePresent: Boolean = false): Boolean {
        if (hasCompleted) return true
        val combined = getCombinedYouTubeCookie()
        if (hasValidAuthCookie(combined)) {
            latestDetectedCookie = combined
            val isOnYouTubeOrCheckCookie = url != null && (
                (url.contains("youtube.com") && !url.contains("accounts.google.com")) ||
                url.contains("accounts.google.com/CheckCookie") ||
                url.contains("accounts.youtube.com/accounts/SetSID") ||
                url.contains("myaccount.google.com")
            )
            if (forceIfCookiePresent || isOnYouTubeOrCheckCookie) {
                hasCompleted = true
                CookieManager.getInstance().flush()
                onLoginSuccess(combined!!, null, null)
                return true
            }
        }
        return false
    }

    // Automatically recover when user returns to the app after approving Google 2-Step Verification prompt
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !hasCompleted) {
                if (!tryCompleteIfAuthenticated(forceIfCookiePresent = false)) {
                    if (isRecoveringConnection) {
                        webViewRef?.let { wv ->
                            val target = lastUrl ?: YT_MUSIC_LOGIN_URL
                            if (target.contains("music.youtube.com")) {
                                wv.loadUrl("https://www.youtube.com/")
                            } else {
                                wv.reload()
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Login with YouTube Music",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRecoveringConnection) "Completing 2-Step Verification..." else "Sign in with your Google Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (!tryCompleteIfAuthenticated(forceIfCookiePresent = true)) {
                                    isRecoveringConnection = false
                                    webViewRef?.reload()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (latestDetectedCookie != null && !hasCompleted) {
                            TextButton(
                                onClick = {
                                    hasCompleted = true
                                    CookieManager.getInstance().flush()
                                    onLoginSuccess(latestDetectedCookie!!, null, null)
                                }
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isLoading || isRecoveringConnection) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                webViewRef = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    // Strip "; wv" so Google OAuth allows sign-in inside WebView
                                    val defaultUa = userAgentString ?: ""
                                    userAgentString = defaultUa
                                        .replace("; wv", "")
                                        .replace("Version/4.0 ", "")
                                        .ifBlank {
                                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                        }
                                }

                                webViewClient = object : WebViewClient() {
                                    private val mainHandler = Handler(Looper.getMainLooper())

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (!url.isNullOrBlank() && url != "about:blank") {
                                            lastUrl = url
                                        }
                                        isLoading = true
                                        if (tryCompleteIfAuthenticated(url, forceIfCookiePresent = false)) {
                                            view?.stopLoading()
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        if (url != "about:blank") {
                                            isRecoveringConnection = false
                                        }
                                        tryCompleteIfAuthenticated(url, forceIfCookiePresent = false)
                                    }

                                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                        super.doUpdateVisitedHistory(view, url, isReload)
                                        if (!url.isNullOrBlank() && url != "about:blank") {
                                            lastUrl = url
                                        }
                                        if (tryCompleteIfAuthenticated(url, forceIfCookiePresent = false)) {
                                            view?.stopLoading()
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return false
                                        // Block intent:// or vnd.youtube:// deep links from leaving the WebView
                                        if (!reqUrl.startsWith("http://") && !reqUrl.startsWith("https://")) {
                                            tryCompleteIfAuthenticated(reqUrl, forceIfCookiePresent = true)
                                            return true
                                        }
                                        // If Google redirects to music.youtube.com after login, check cookies or redirect to www.youtube.com
                                        // to avoid mobile ISP ERR_CONNECTION_RESET on music.youtube.com
                                        if (reqUrl.contains("music.youtube.com")) {
                                            if (tryCompleteIfAuthenticated(reqUrl, forceIfCookiePresent = true)) {
                                                return true
                                            }
                                            view?.loadUrl("https://www.youtube.com/")
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            val failingUrl = request.url?.toString()
                                            // 1. If cookies were already set during 2FA, finish immediately!
                                            if (tryCompleteIfAuthenticated(failingUrl, forceIfCookiePresent = true)) {
                                                return
                                            }
                                            // 2. Otherwise hide the raw Android error screen and auto-retry after 2FA resume
                                            isRecoveringConnection = true
                                            view?.loadUrl("about:blank")
                                            mainHandler.postDelayed({
                                                if (!hasCompleted) {
                                                    if (!tryCompleteIfAuthenticated(failingUrl, forceIfCookiePresent = true)) {
                                                        val retryTarget = when {
                                                            failingUrl == null -> YT_MUSIC_LOGIN_URL
                                                            failingUrl.contains("music.youtube.com") -> "https://www.youtube.com/"
                                                            else -> failingUrl
                                                        }
                                                        view?.loadUrl(retryTarget)
                                                    }
                                                }
                                            }, 800L)
                                        }
                                    }
                                }

                                loadUrl(YT_MUSIC_LOGIN_URL)
                            }
                        }
                    )

                    if (isRecoveringConnection) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.padding(top = 12.dp))
                                Text(
                                    text = "Completing Google Verification...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
