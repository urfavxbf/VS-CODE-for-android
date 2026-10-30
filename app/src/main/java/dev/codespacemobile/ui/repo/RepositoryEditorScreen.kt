package dev.codespacemobile.ui.repo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
 * First-party Android code editor for the GitHub repository in each Codespace.
 * It saves explicit Git commits to the repository's default branch; this is NOT
 * a connection to the running VM's local filesystem or terminal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryEditorScreen(
    repositoryName: String,
    onBack: () -> Unit,
    model: RepositoryEditorViewModel = hiltViewModel()
) {
    val state by model.state.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmCommit by remember { mutableStateOf(false) }
    var commitMessage by remember(state.file?.path) {
        mutableStateOf("Edit " + (state.file?.path ?: "file") + " from Android")
    }

    LaunchedEffect(repositoryName) { model.openRepository(repositoryName) }

    fun navigateBack() {
        if (state.hasUnsavedChanges) {
            confirmDiscard = true
        } else if (state.file != null || state.directory.isNotEmpty()) {
            model.goUp()
        } else {
            onBack()
        }
    }

    BackHandler { navigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Code · " + repositoryName, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            if (state.branch.isNotEmpty()) "Branch: " + state.branch
                            else "GitHub repository",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.loading || state.saving) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text(if (state.saving) "Committing…" else "Loading repository…")
                }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                if (state.file == null) {
                    TextButton(onClick = {
                        if (state.branch.isNotBlank()) model.openDirectory(state.directory)
                        else model.openRepository(repositoryName)
                    }) { Text("Retry") }
                }
            }
            state.notice?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            val file = state.file
            if (file != null) {
                Text(
                    file.path,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "GitHub repository file · not the running Codespace filesystem",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(
                    value = state.editedText,
                    onValueChange = model::edit,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    readOnly = state.saving,
                    label = { Text("Edit source code") },
                    singleLine = false
                )
                Button(
                    onClick = { confirmCommit = true },
                    enabled = state.hasUnsavedChanges && !state.saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(" Commit changes to " + state.branch)
                }
            } else if (!state.loading && state.branch.isNotEmpty()) {
                Text(
                    if (state.directory.isEmpty()) "Repository files"
                    else state.directory,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Browse and edit source files directly in this app. " +
                        "Saving creates a commit on the repository's default branch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                HorizontalDivider()
                if (state.entries.isEmpty()) {
                    Text("This directory is empty.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(state.entries, key = { it.path }) { entry ->
                            ListItem(
                                headlineContent = {
                                    Text(entry.name, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(if (entry.type == "dir") "Folder" else "File")
                                },
                                leadingContent = {
                                    Icon(
                                        if (entry.type == "dir") Icons.Default.Folder
                                        else Icons.Default.Description,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (entry.type == "dir") model.openDirectory(entry.path)
                                    else model.openFile(entry.path)
                                }
                            )
                            HorizontalDivider()
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
            text = { Text("Your changes have not been committed to GitHub.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    model.closeFile()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            }
        )
    }

    if (confirmCommit) {
        AlertDialog(
            onDismissRequest = { confirmCommit = false },
            title = { Text("Commit to " + state.branch + "?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This updates the GitHub repository immediately. " +
                            "It does not save uncommitted changes inside the running Codespace."
                    )
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it.take(200) },
                        label = { Text("Commit message") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmCommit = false
                        model.save(commitMessage)
                    },
                    enabled = commitMessage.isNotBlank() && !state.saving
                ) { Text("Commit") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCommit = false }) { Text("Cancel") }
            }
        )
    }
}
