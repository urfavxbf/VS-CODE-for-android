package dev.codespacemobile.ui.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.codespacemobile.data.remote.RepositoryContentDto
import dev.codespacemobile.data.repository.GitHubFileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class RepositoryEditorState(
    val repositoryName: String = "",
    val branch: String = "",
    val directory: String = "",
    val entries: List<RepositoryContentDto> = emptyList(),
    val file: GitHubFileRepository.FileText? = null,
    val editedText: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    val hasUnsavedChanges: Boolean get() = file?.text != null && file.text != editedText
}

@HiltViewModel
class RepositoryEditorViewModel @Inject constructor(
    private val repository: GitHubFileRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(RepositoryEditorState())
    val state: StateFlow<RepositoryEditorState> = mutableState.asStateFlow()

    private var location: GitHubFileRepository.Location? = null

    fun openRepository(name: String) {
        if (mutableState.value.repositoryName == name) return
        viewModelScope.launch {
            mutableState.value = RepositoryEditorState(repositoryName = name, loading = true)
            try {
                val parsed = repository.parseLocation(name)
                location = parsed
                val branch = repository.defaultBranch(parsed)
                val entries = repository.list(parsed, branch, "")
                mutableState.value = mutableState.value.copy(
                    branch = branch, entries = entries, loading = false
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false, error = describe(error)
                )
            }
        }
    }

    fun openDirectory(path: String) {
        val target = location ?: return
        val branch = mutableState.value.branch
        if (branch.isEmpty() || mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                loading = true, error = null, notice = null
            )
            try {
                val entries = repository.list(target, branch, path)
                mutableState.value = mutableState.value.copy(
                    directory = path, entries = entries, file = null,
                    editedText = "", loading = false
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false, error = describe(error)
                )
            }
        }
    }

    fun openFile(path: String) {
        val target = location ?: return
        val branch = mutableState.value.branch
        if (branch.isEmpty() || mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                loading = true, error = null, notice = null
            )
            try {
                val file = repository.read(target, branch, path)
                mutableState.value = mutableState.value.copy(
                    file = file, editedText = file.text, loading = false
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false, error = describe(error)
                )
            }
        }
    }

    fun edit(text: String) {
        mutableState.value = mutableState.value.copy(
            editedText = text, notice = null
        )
    }

    fun closeFile() {
        mutableState.value = mutableState.value.copy(
            file = null, editedText = "", error = null, notice = null
        )
    }

    fun goUp() {
        val current = mutableState.value
        if (current.file != null) {
            closeFile()
            return
        }
        if (current.directory.isNotEmpty()) {
            openDirectory(current.directory.substringBeforeLast('/', ""))
        }
    }

    fun save(message: String) {
        val current = mutableState.value
        val target = location ?: return
        val file = current.file ?: return
        if (current.saving || !current.hasUnsavedChanges) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                saving = true, error = null, notice = null
            )
            try {
                val saved = repository.commit(target, current.branch, file, current.editedText, message)
                mutableState.value = mutableState.value.copy(
                    file = saved, saving = false,
                    notice = "Committed to " + current.branch + "."
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    saving = false, error = describe(error)
                )
            }
        }
    }

    private fun describe(error: Exception): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "GitHub token expired or invalid. Reconnect from the dashboard."
                403 -> "GitHub denied repository access. The token needs Contents read/write permission for this repository."
                404 -> "Repository or file not accessible. Check token repository permissions."
                409, 422 -> "File changed remotely or commit was rejected. Refresh the file before retrying."
                else -> "GitHub API error (" + error.code() + ")."
            }
            is IllegalArgumentException -> error.message ?: "Invalid repository file."
            else -> "Unable to load or save. Check your connection and retry."
        }
    }
}
