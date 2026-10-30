package dev.codespacemobile.domain

data class Codespace(
    val name: String,
    val title: String,
    val repository: String,
    val state: String,
    val webUrl: String?,
    val updatedAt: String?
) {
    val isAvailable: Boolean get() = state.equals("Available", ignoreCase = true)
    val isStopped: Boolean get() = state.equals("Shutdown", ignoreCase = true) ||
        state.equals("Stopped", ignoreCase = true)
}
