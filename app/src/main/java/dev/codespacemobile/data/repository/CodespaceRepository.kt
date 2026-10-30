package dev.codespacemobile.data.repository

import android.webkit.CookieManager
import dev.codespacemobile.auth.SecureTokenStore
import dev.codespacemobile.data.local.CodespaceDao
import dev.codespacemobile.data.local.CodespaceEntity
import dev.codespacemobile.data.remote.CodespaceDto
import dev.codespacemobile.data.remote.CreateCodespaceRequest
import dev.codespacemobile.data.remote.GitHubApi
import dev.codespacemobile.domain.Codespace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodespaceRepository @Inject constructor(
    private val api: GitHubApi,
    private val dao: CodespaceDao,
    private val tokens: SecureTokenStore
) {
    fun isConnected(): Boolean = tokens.read() != null

    fun observe(): Flow<List<Codespace>> = dao.observeAll().map { items ->
        items.map { Codespace(it.name, it.title, it.repoName, it.state, it.webUrl, it.updatedAt) }
    }

    suspend fun connect(rawToken: String) = withContext(Dispatchers.IO) {
        val token = rawToken.trim()
        require(token.length >= 20) { "Invalid token format" }
        api.validateToken("Bearer $token")
        tokens.save(token)
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        check(isConnected()) { "Sign in first" }
        val results = mutableListOf<CodespaceDto>()
        for (page in 1..20) {
            val batch = api.listCodespaces(page).codespaces
            results += batch
            if (batch.size < 100) break
        }
        dao.replaceAll(results.map {
            CodespaceEntity(
                it.name,
                it.displayName?.takeIf(String::isNotBlank) ?: it.name,
                it.repository?.fullName.orEmpty(),
                it.state,
                it.webUrl,
                it.updatedAt
            )
        })
    }

    suspend fun create(repository: String, ref: String) = withContext(Dispatchers.IO) {
        val parts = repository.trim().split("/")
        val valid = Regex("[A-Za-z0-9_.-]+")
        require(parts.size == 2 && parts.all { it.matches(valid) }) { "Use owner/repository." }
        require(ref.isNotBlank() && ref.length <= 255) { "Enter a valid branch or ref." }
        api.createCodespace(parts[0], parts[1], CreateCodespaceRequest(ref.trim()))
        refresh()
    }

    suspend fun start(name: String) = withContext(Dispatchers.IO) {
        api.startCodespace(name)
        refresh()
    }

    suspend fun stop(name: String) = withContext(Dispatchers.IO) {
        api.stopCodespace(name)
        refresh()
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            tokens.clear()
            dao.clear()
        }
        // The browser/editor session is independent of the REST API token.
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}
