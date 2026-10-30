package dev.codespacemobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.codespacemobile.data.repository.CodespaceRepository
import dev.codespacemobile.domain.Codespace
import dev.codespacemobile.domain.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val authenticated: Boolean = false,
    val loading: Boolean = false,
    val items: List<Codespace> = emptyList(),
    val busyCodeSpace: String? = null,
    val error: String? = null,
    val hasSynced: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CodespaceRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(DashboardUiState(authenticated = repository.isConnected()))
    val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                mutableState.value = mutableState.value.copy(items = list)
            }
        }
        if (mutableState.value.authenticated) refresh()
    }

    fun connect(token: String) {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                repository.connect(token)
                mutableState.value = mutableState.value.copy(authenticated = true, loading = false)
                refresh()
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    error = if (error is IllegalArgumentException) "Enter a valid token." else error.toDisplayMessage()
                )
            }
        }
    }

    fun refresh() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                repository.refresh()
                mutableState.value = mutableState.value.copy(hasSynced = true)
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(error = error.toDisplayMessage())
            } finally {
                mutableState.value = mutableState.value.copy(loading = false)
            }
        }
    }

    fun create(repositoryName: String, ref: String) {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                repository.create(repositoryName, ref)
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    error = if (error is IllegalArgumentException) error.message
                        else error.toDisplayMessage()
                )
            } finally {
                mutableState.value = mutableState.value.copy(loading = false)
            }
        }
    }

    fun changeState(item: Codespace, start: Boolean) {
        if (mutableState.value.busyCodeSpace != null) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busyCodeSpace = item.name, error = null)
            try {
                if (start) repository.start(item.name) else repository.stop(item.name)
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(error = error.toDisplayMessage())
            } finally {
                mutableState.value = mutableState.value.copy(busyCodeSpace = null)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true)
            try {
                repository.disconnect()
                mutableState.value = DashboardUiState()
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false, error = error.toDisplayMessage()
                )
            }
        }
    }
}
