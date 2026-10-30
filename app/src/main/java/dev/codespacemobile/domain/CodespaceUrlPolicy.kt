package dev.codespacemobile.domain

import java.net.URI
import java.util.Locale

/** Prevents arbitrary redirects from the Codespaces API opening in an embedded WebView. */
object CodespaceUrlPolicy {
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
