package dev.codespacemobile.ui.repository

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Actual native GitHub file editor, not a browser or an embedded VS Code website.
 *
 * Important: this edits the repository branch via GitHub's Contents API, not
 * uncommitted files in a live Codespace container. Each save is one git commit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryEditorScreen(
    repoName: String,
    onBack: () -> Unit,
    viewModel: RepositoryEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmCommit by remember { mutableStateOf(false) }
    var commitMessage by remember(state.file?.path) {
        mutableStateOf("Edit ${state.file?.path ?: "file"} from Android")
    }

    LaunchedEffect(repoName) { viewModel.openRepository(repoName) }

    fun navigateBack() {
        when {
            state.file != null && state.dirty -> confirmDiscard = true
            state.file != null -> viewModel.closeFile()
            state.folder.isNotBlank() -> viewModel.openFolder(
                state.folder.substringBeforeLast('/', missingDelimiterValue = "")
            )
            else -> onBack()
        }
    }

    BackHandler { navigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files · ${repoName}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (state.branch.isNotBlank()) "GitHub branch: ${state.branch}"
                else "Connecting to repository…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Native repository editor. Changes are committed directly to GitHub; this is NOT the live Codespace working tree or terminal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            state.notice?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (state.busy) CircularProgressIndicator()
            val file = state.file
            if (file != null) {
                Text(file.path, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = state.draftText,
                    onValueChange = viewModel::changeText,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    enabled = !state.busy,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    label = { Text("Code · UTF-8") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { confirmCommit = true },
                        enabled = !state.busy && state.dirty
                    ) { Text("Save as commit") }
                    OutlinedButton(onClick = ::navigateBack, enabled = !state.busy) {
                        Text("Close file")
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Text(
                    if (state.folder.isBlank()) "Repository root" else "/${state.folder}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.folder.isNotBlank()) {
                    OutlinedButton(
                        enabled = !state.busy,
                        onClick = {
                            viewModel.openFolder(
                                state.folder.substringBeforeLast('/', missingDelimiterValue = "")
                            )
                        }
                    ) { Text(".. Parent folder") }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.entries, key = { it.path }) { entry ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            onClick = {
                                if (entry.type == "dir") viewModel.openFolder(entry.path)
                                else viewModel.openFile(entry)
                            }
                        ) {
                            Icon(
                                if (entry.type == "dir") Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null
                            )
                            Text("  ${entry.name}", modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard unsaved edits?") },
            text = { Text("Your local changes to this file have not been saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.closeFile(discardUnsaved = true)
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
        )
    }

    if (confirmCommit) {
        AlertDialog(
            onDismissRequest = { confirmCommit = false },
            title = { Text("Commit changes to GitHub?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This creates a commit on ${state.repoName} / ${state.branch}. It does not save to uncommitted Codespace files.")
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = { Text("Commit message") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = commitMessage.isNotBlank() && state.dirty && !state.busy,
                    onClick = {
                        confirmCommit = false
                        viewModel.commit(commitMessage)
                    }
                ) { Text("Commit") }
            },
            dismissButton = { TextButton(onClick = { confirmCommit = false }) { Text("Cancel") } }
        )
    }
}
