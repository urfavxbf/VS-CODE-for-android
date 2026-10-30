package dev.codespacemobile.ui.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.codespacemobile.data.remote.GitHubApi
import dev.codespacemobile.data.remote.RepositoryEntryDto
import dev.codespacemobile.data.remote.RepositorySaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class RepositoryEditorState(
    val repoName: String = "",
    val branch: String = "",
    val folder: String = "",
    val entries: List<RepositoryEntryDto> = emptyList(),
    val file: RepositoryEntryDto? = null,
    val originalText: String = "",
    val draftText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    val dirty: Boolean get() = file != null && draftText != originalText
}

@HiltViewModel
class RepositoryEditorViewModel @Inject constructor(
    private val api: GitHubApi
) : ViewModel() {
    private val mutable = MutableStateFlow(RepositoryEditorState())
    val state = mutable.asStateFlow()
    private var repoOwner = ""
    private var repoShort = ""

    fun openRepository(fullName: String) {
        if (mutable.value.repoName == fullName) return
        viewModelScope.launch {
            mutable.value = RepositoryEditorState(repoName = fullName, busy = true)
            try {
                val (owner, repo) = RepositoryFileCodec.parseRepo(fullName)
                repoOwner = owner
                repoShort = repo
                val branch = api.repositoryMetadata(owner, repo).defaultBranch
                require(branch.isNotBlank()) { "Repository has no default branch." }
                val entries = api.repositoryRoot(owner, repo, branch)
                mutable.value = mutable.value.copy(
                    branch = branch, entries = sorted(entries), busy = false
                )
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(error = message(e), busy = false)
            }
        }
    }

    fun openFolder(path: String) {
        if (mutable.value.busy || mutable.value.dirty) return
        if (path.isNotEmpty() && !RepositoryFileCodec.validPath(path)) return
        val snapshot = mutable.value
        viewModelScope.launch {
            mutable.value = snapshot.copy(busy = true, error = null, notice = null)
            try {
                val entries = if (path.isEmpty()) {
                    api.repositoryRoot(repoOwner, repoShort, snapshot.branch)
                } else {
                    api.repositoryDirectory(repoOwner, repoShort, path, snapshot.branch)
                }
                mutable.value = mutable.value.copy(
                    folder = path, entries = sorted(entries), file = null, busy = false
                )
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(error = message(e), busy = false)
            }
        }
    }

    fun openFile(entry: RepositoryEntryDto) {
        if (mutable.value.busy || mutable.value.dirty || !RepositoryFileCodec.validPath(entry.path)) return
        val snapshot = mutable.value
        viewModelScope.launch {
            mutable.value = snapshot.copy(busy = true, error = null, notice = null)
            try {
                require(entry.type == "file") { "This item isn't a regular text file." }
                require(entry.size <= RepositoryFileCodec.MAX_EDITABLE_BYTES) {
                    "File exceeds the 512 KiB text editor limit."
                }
                val remote = api.repositoryFile(repoOwner, repoShort, entry.path, snapshot.branch)
                val decoded = RepositoryFileCodec.decodeBase64(
                    remote.content.orEmpty(), remote.encoding, remote.size
                )
                mutable.value = mutable.value.copy(
                    file = remote, originalText = decoded, draftText = decoded, busy = false
                )
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(error = message(e), busy = false)
            }
        }
    }

    fun changeText(next: String) {
        if (mutable.value.busy || mutable.value.file == null) return
        mutable.value = mutable.value.copy(draftText = next, error = null, notice = null)
    }

    fun closeFile(discardUnsaved: Boolean = false) {
        if (mutable.value.dirty && !discardUnsaved) return
        mutable.value = mutable.value.copy(
            file = null, originalText = "", draftText = "", error = null, notice = null
        )
    }

    /** Saves ONLY after explicit confirmation; one GitHub commit on the selected branch. */
    fun commit(message: String) {
        val snapshot = mutable.value
        val file = snapshot.file ?: return
        if (snapshot.busy || !snapshot.dirty || message.isBlank()) return
        viewModelScope.launch {
            mutable.value = snapshot.copy(busy = true, error = null, notice = null)
            try {
                require(RepositoryFileCodec.validPath(file.path))
                val response = api.saveRepositoryFile(
                    repoOwner, repoShort, file.path,
                    RepositorySaveRequest(
                        message = message.trim().take(200),
                        content = RepositoryFileCodec.encodeBase64(snapshot.draftText),
                        sha = file.sha,
                        branch = snapshot.branch
                    )
                )
                if (!response.isSuccessful) throw HttpException(response)
                val newSha = response.body()?.content?.sha?.takeIf { it.isNotBlank() }
                    ?: api.repositoryFile(repoOwner, repoShort, file.path, snapshot.branch).sha
                mutable.value = mutable.value.copy(
                    file = file.copy(sha = newSha),
                    originalText = snapshot.draftText,
                    draftText = snapshot.draftText,
                    busy = false,
                    notice = "Committed to ${snapshot.repoName} / ${snapshot.branch}"
                )
            } catch (e: Exception) {
                // Never discard unsaved work on a network error or a 409 conflict.
                mutable.value = mutable.value.copy(error = message(e), busy = false)
            }
        }
    }

    private fun sorted(list: List<RepositoryEntryDto>): List<RepositoryEntryDto> =
        list.sortedWith(compareBy<RepositoryEntryDto> { it.type != "dir" }
            .thenBy { it.name.lowercase() })

    private fun message(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Token expired or invalid. Reconnect your GitHub account."
            403, 404 -> "Repository access denied. For private repositories, a fine-grained PAT needs Contents read permission. Saving needs Contents write. Your current Codespaces-only token may not be enough."
            409, 422 -> "File changed on GitHub. Your edits are kept here; copy them before reloading the latest version."
            else -> "GitHub API error ${e.code()}. Check your connection and permissions."
        }
        is IllegalArgumentException -> e.message ?: "Unsupported file or repository."
        else -> "Cannot load or save. Check your network connection and retry."
    }
}
