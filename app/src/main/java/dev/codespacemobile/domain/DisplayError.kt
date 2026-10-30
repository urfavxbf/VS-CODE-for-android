package dev.codespacemobile.domain

import retrofit2.HttpException
import java.io.IOException

/** User-facing errors do not expose request headers, cookies, or tokens. */
fun Throwable.toDisplayMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "GitHub credentials expired or invalid. Reconnect your account."
        403 -> "Access denied. Check Codespaces permissions and account limits."
        404 -> "Codespace unavailable or removed."
        409, 422 -> "This Codespace cannot change state right now. Retry shortly."
        429 -> "GitHub request limit reached. Retry later."
        else -> "GitHub returned HTTP ${code()}. Try again."
    }
    is IOException -> "Network unavailable. Showing your last cached Codespaces."
    else -> "Something went wrong. Try again."
}
