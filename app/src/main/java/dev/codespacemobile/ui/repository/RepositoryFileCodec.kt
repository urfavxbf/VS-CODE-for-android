package dev.codespacemobile.ui.repository

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Pure JVM codec used by the native GitHub repository editor.
 * A Codespaces PAT alone does not grant repository Contents write permissions.
 */
object RepositoryFileCodec {
    const val MAX_EDITABLE_BYTES = 512 * 1024

    fun parseRepo(fullName: String): Pair<String, String> {
        val parts = fullName.split("/")
        require(parts.size == 2 && parts.all { it.matches(Regex("[A-Za-z0-9_.-]+")) }) {
            "Invalid repository name"
        }
        require(parts.none { it == "." || it == ".." }) { "Invalid repository path" }
        return parts[0] to parts[1]
    }

    fun validPath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith("/") &&
            path.split("/").all { it.isNotBlank() && it != "." && it != ".." }

    fun decodeBase64(content: String, encoding: String?, size: Long): String {
        require(encoding == "base64") { "GitHub cannot return this file as editable text." }
        require(size in 0..MAX_EDITABLE_BYTES.toLong()) {
            "File is too large for the mobile editor (maximum 512 KiB)."
        }
        val binary = Base64.getMimeDecoder().decode(content)
        require(binary.size <= MAX_EDITABLE_BYTES) { "File is too large to edit." }
        require(binary.none { it.toInt() == 0 }) { "Binary files cannot be edited as text." }
        return binary.toString(StandardCharsets.UTF_8)
    }

    fun encodeBase64(content: String): String {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_EDITABLE_BYTES) { "File exceeds the 512 KiB edit limit." }
        return Base64.getEncoder().encodeToString(bytes)
    }
}
