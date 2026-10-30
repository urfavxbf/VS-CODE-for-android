package dev.codespacemobile.ui.editor

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import dev.codespacemobile.domain.CodespaceUrlPolicy

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pane by remember { mutableStateOf(EditorScript.Pane.EDITOR) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var browserHint by remember { mutableStateOf(false) }
    var signInRequired by remember { mutableStateOf(false) }
    val trusted = remember(url) { CodespaceUrlPolicy.isTrusted(url) }

    fun openInBrowser() {
        if (!trusted) return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            error = "No browser found. Install Chrome or another browser and retry."
        }
    }

    BackHandler { onBack() }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "Codespace" }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::openInBrowser, enabled = trusted) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in external browser")
                    }
                }
            )
        },
        bottomBar = {
            if (trusted) NavigationBar {
                val pages = listOf(
                    EditorScript.Pane.EDITOR to "Code",
                    EditorScript.Pane.FILES to "Files",
                    EditorScript.Pane.SOURCE_CONTROL to "Git",
                    EditorScript.Pane.TERMINAL to "Terminal"
                )
                pages.forEach { (destination, label) ->
                    NavigationBarItem(
                        selected = pane == destination,
                        onClick = {
                            pane = destination
                            webView?.evaluateJavascript(EditorScript.activate(destination), null)
                        },
                        label = { Text(label) },
                        icon = {
                            Icon(
                                when (destination) {
                                    EditorScript.Pane.EDITOR -> Icons.Default.Code
                                    EditorScript.Pane.FILES -> Icons.Default.Folder
                                    EditorScript.Pane.SOURCE_CONTROL -> Icons.Default.Source
                                    EditorScript.Pane.TERMINAL -> Icons.Default.Terminal
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (!trusted) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Untrusted editor URL. Open your Codespace from the dashboard.")
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                val recoveryMessage = when {
                    signInRequired -> "GitHub sign-in is required for the hosted editor. " +
                        "Your PAT connects the dashboard only; the editor needs its own browser session."
                    error != null -> error
                    browserHint -> "The VS Code editor did not appear in the embedded view. " +
                        "Open it in your browser for full GitHub sign-in and editing."
                    else -> null
                }
                recoveryMessage?.let { message ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::openInBrowser) { Text("Open in browser") }
                            OutlinedButton(onClick = {
                                signInRequired = false
                                browserHint = false
                                error = null
                                loading = true
                                webView?.reload()
                            }) { Text("Retry") }
                        }
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = false
                                settings.loadWithOverviewMode = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest
                                    ): Boolean {
                                        val destination = request.url.toString()
                                        if (request.isForMainFrame &&
                                            CodespaceUrlPolicy.isSignInPage(destination)) {
                                            signInRequired = true
                                            browserHint = true
                                            loading = false
                                            return true
                                        }
                                        if (CodespaceUrlPolicy.isTrusted(destination)) return false
                                        if (request.isForMainFrame && request.url.scheme == "https") {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                        }
                                        return true
                                    }

                                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                                        super.onPageFinished(view, loadedUrl)
                                        loading = false
                                        if (CodespaceUrlPolicy.isSignInPage(loadedUrl)) {
                                            signInRequired = true
                                            browserHint = true
                                        } else if (CodespaceUrlPolicy.isEditorUrl(loadedUrl)) {
                                            // Only make non-destructive touch improvements.
                                            // Never hide the hosted workbench's panes.
                                            view.evaluateJavascript(EditorScript.BOOTSTRAP, null)
                                            view.postDelayed({
                                                if (view.url == loadedUrl && !signInRequired) {
                                                    view.evaluateJavascript(EditorScript.READINESS_CHECK) { ready ->
                                                        if (ready != "true") browserHint = true
                                                    }
                                                }
                                            }, 9000L)
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView, request: WebResourceRequest,
                                        webError: WebResourceError
                                    ) {
                                        if (request.isForMainFrame) {
                                            error = "Editor connection failed. The browser can retry GitHub sign-in."
                                            browserHint = true
                                            loading = false
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView, handler: SslErrorHandler, sslError: SslError
                                    ) {
                                        handler.cancel()
                                        error = "Secure connection failed. The unsafe page was blocked."
                                        browserHint = true
                                        loading = false
                                    }
                                }
                                loadUrl(url)
                                webView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (loading) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Connecting to VS Code…")
                        }
                    }
                }
            }
        }
    }
}
