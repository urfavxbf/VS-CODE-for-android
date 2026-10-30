package dev.codespacemobile.domain

import java.net.URI
import java.util.Locale

/** Prevents arbitrary redirects from the Codespaces API opening in an embedded WebView. */
object CodespaceUrlPolicy {
    /** A hosted VS Code editor origin; GitHub login pages are not editors. */
    fun isEditorUrl(raw: String?): Boolean {
        if (!isTrusted(raw)) return false
        val host = runCatching { URI(raw).host?.lowercase(Locale.ROOT) }.getOrNull() ?: return false
        return host == "github.dev" || host.endsWith(".github.dev") ||
            host == "githubpreview.dev" || host.endsWith(".githubpreview.dev")
    }

    /** Embedded GitHub login should never be mistaken for a successful editor load. */
    fun isSignInPage(raw: String?): Boolean {
        if (!isTrusted(raw)) return false
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        if (!uri.host.equals("github.com", ignoreCase = true)) return false
        val path = uri.path ?: ""
        return path == "/login" || path.startsWith("/login/") ||
            path == "/sessions" || path.startsWith("/sessions/")
    }

    fun isTrusted(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase(Locale.ROOT) != "https") return false
        if (uri.userInfo != null || uri.port != -1) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        return host == "github.com" || host == "github.dev" ||
            host.endsWith(".github.dev") || host == "githubpreview.dev" ||
            host.endsWith(".githubpreview.dev")
    }
}
