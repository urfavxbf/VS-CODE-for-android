package dev.codespacemobile.data.repository

import android.util.Base64
import dev.codespacemobile.data.remote.GitHubApi
import dev.codespacemobile.data.remote.RepositoryContentDto
import dev.codespacemobile.data.remote.UpdateRepositoryFileRequest
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GitHub repository Contents API editor, not the Codespace VM filesystem.
 * Writes only after explicit save/commit confirmation in the UI.
 */
@Singleton
class GitHubFileRepository @Inject constructor(private val api: GitHubApi) {
    companion object {
        const val MAX_EDITABLE_FILE_BYTES = 512 * 1024
        private val COMPONENT = Regex("[A-Za-z0-9_.-]+")
    }

    data class Location(val owner: String, val repo: String) {
        val fullName: String get() = "$owner/$repo"
    }

    data class FileText(val path: String, val sha: String, val text: String)

    fun parseLocation(fullName: String): Location {
        val components = fullName.split("/")
        require(components.size == 2 && components.all {
            it.isNotBlank() && it != "." && it != ".." && COMPONENT.matches(it)
        }) { "A valid GitHub owner/repository is required." }
        return Location(components[0], components[1])
    }

    private fun checkPath(path: String) {
        require(path.isNotBlank() && path.length <= 2000 &&
            path.split('/').all { it.isNotBlank() && it != "." && it != ".." } &&
            !path.contains('\\')) { "Invalid repository path." }
    }

    suspend fun defaultBranch(location: Location): String =
        api.getRepositoryInfo(location.owner, location.repo).defaultBranch

    suspend fun list(location: Location, branch: String, directory: String): List<RepositoryContentDto> {
        val contents = if (directory.isEmpty()) {
            api.listRepositoryRoot(location.owner, location.repo, branch)
        } else {
            checkPath(directory)
            api.listRepositoryDirectory(location.owner, location.repo, directory, branch)
        }
        return contents.filter { it.type == "dir" || it.type == "file" }
            .sortedWith(compareBy<RepositoryContentDto> { it.type != "dir" }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    suspend fun read(location: Location, branch: String, path: String): FileText {
        checkPath(path)
        val result = api.getRepositoryFile(location.owner, location.repo, path, branch)
        require(result.type == "file") { "This is not a regular file." }
        require(result.size <= MAX_EDITABLE_FILE_BYTES) { "Files over 512 KiB are not supported." }
        require(result.encoding == "base64" && result.content != null) {
            "GitHub did not return editable UTF-8 file content."
        }
        val bytes = Base64.decode(result.content, Base64.DEFAULT)
        require(bytes.size <= MAX_EDITABLE_FILE_BYTES && bytes.none { it == 0.toByte() }) {
            "Binary or large files cannot be edited as text."
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return FileText(result.path, result.sha, decoder.decode(ByteBuffer.wrap(bytes)).toString())
    }

    suspend fun commit(
        location: Location, branch: String, file: FileText,
        editedText: String, message: String
    ): FileText {
        checkPath(file.path)
        require(message.isNotBlank() && message.length <= 200) {
            "Enter a commit message (maximum 200 characters)."
        }
        val bytes = editedText.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_EDITABLE_FILE_BYTES) { "Edited file exceeds 512 KiB." }
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val response = api.updateRepositoryFile(
            location.owner, location.repo, file.path,
            UpdateRepositoryFileRequest(message.trim(), encoded, file.sha, branch)
        )
        val sha = response.content?.sha?.takeIf { it.isNotBlank() }
            ?: api.getRepositoryFile(location.owner, location.repo, file.path, branch).sha
        return FileText(file.path, sha, editedText)
    }
}
