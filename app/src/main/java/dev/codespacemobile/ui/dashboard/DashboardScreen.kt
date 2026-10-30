package dev.codespacemobile.ui.dashboard

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.codespacemobile.domain.Codespace
import dev.codespacemobile.domain.CodespaceUrlPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onOpen: (Codespace) -> Unit, onEditRepo: (Codespace) -> Unit) {
    val state by viewModel.state.collectAsState()
    var stopCandidate by remember { mutableStateOf<Codespace?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("My Codespaces") },
            actions = {
                if (state.authenticated) {
                    IconButton(onClick = { showCreate = true }, enabled = !state.loading) {
                        Icon(Icons.Default.Add, contentDescription = "Create Codespace")
                    }
                    IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = viewModel::disconnect, enabled = !state.loading) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            state.error?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
            if (!state.authenticated) {
                ConnectScreen(loading = state.loading, onConnect = viewModel::connect)
            } else if (state.items.isEmpty() && !state.loading) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("No Codespaces found", style = MaterialTheme.typography.titleLarge)
                    Text("Create a Codespace on GitHub, then tap Refresh.")
                    Spacer(Modifier.height(12.dp))
                    val context = LocalContext.current
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/codespaces")))
                    }) { Text("Open GitHub Codespaces") }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            if (state.hasSynced) "Connected to GitHub" else "Cached Codespaces · Refresh to update",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(state.items, key = { it.name }) { item ->
                        CodespaceCard(
                            item = item,
                            busy = state.busyCodeSpace == item.name,
                            onOpen = { onOpen(item) },
                            onEditRepo = { onEditRepo(item) },
                            onStart = { viewModel.changeState(item, true) },
                            onStop = { stopCandidate = item }
                        )
                    }
                }
            }
        }
    }
    if (showCreate) {
        var repoName by remember { mutableStateOf("") }
        var branch by remember { mutableStateOf("main") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create Codespace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Codespaces may incur GitHub usage charges.")
                    OutlinedTextField(
                        value = repoName, onValueChange = { repoName = it.trim() },
                        label = { Text("owner/repository") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = branch, onValueChange = { branch = it.trim() },
                        label = { Text("Git ref or branch") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreate = false
                    viewModel.create(repoName, branch)
                }, enabled = repoName.count { it == '/' } == 1 && branch.isNotBlank()) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
    stopCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { stopCandidate = null },
            title = { Text("Stop Codespace?") },
            text = { Text("Stop ${target.title}? Unsaved editor activity may be interrupted.") },
            confirmButton = {
                TextButton(onClick = {
                    stopCandidate = null
                    viewModel.changeState(target, false)
                }) { Text("Stop") }
            },
            dismissButton = { TextButton(onClick = { stopCandidate = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ConnectScreen(loading: Boolean, onConnect: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.Code, contentDescription = null)
        Text("Connect GitHub", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Use a GitHub personal access token with Codespaces read and lifecycle permissions. " +
                "Your token is encrypted by Android Keystore and sent only to api.github.com.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it.trim() },
            label = { Text("Personal access token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onConnect(token)
                token = ""
            },
            enabled = token.length >= 20 && !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Connect securely") }
        val context = LocalContext.current
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/tokens")))
        }) { Text("Manage GitHub tokens") }
        Text(
            "For classic tokens, request the 'codespace' scope. " +
                "Fine-grained tokens require Codespaces read and Codespaces lifecycle admin write permissions. " +
                "The editor may ask for a separate browser sign-in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun CodespaceCard(
    item: Codespace,
    busy: Boolean,
    onOpen: () -> Unit,
    onEditRepo: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.repository, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Status: ${item.state}", style = MaterialTheme.typography.labelMedium)
            if (item.repository.isNotBlank()) {
                Button(onClick = onEditRepo, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Text(" Edit repository files")
                }
                Text(
                    "Edit and commit GitHub repository files natively. Live Codespace files and terminal are separate; requires GitHub Contents permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            val context = LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.isAvailable) {
                    val canOpen = !busy && CodespaceUrlPolicy.isTrusted(item.webUrl)
                    Button(
                        onClick = {
                            item.webUrl?.let { link ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                            }
                        },
                        enabled = canOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                        Text(" Open in browser")
                    }
                    OutlinedButton(
                        onClick = onOpen,
                        enabled = canOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Text(" Try in app (beta)")
                    }
                    Text(
                        "The GitHub editor needs a browser sign-in separate from your API token.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedButton(onClick = onStop, enabled = !busy) {
                        Icon(Icons.Default.Stop, null)
                        Text(" Stop Codespace")
                    }
                } else if (item.isStopped) {
                    Button(onClick = onStart, enabled = !busy) {
                        Icon(Icons.Default.PlayArrow, null)
                        Text(" Start")
                    }
                } else {
                    Text("Changing state · Refresh later", style = MaterialTheme.typography.bodySmall)
                }
                if (busy) CircularProgressIndicator()
            }
        }
    }
}
