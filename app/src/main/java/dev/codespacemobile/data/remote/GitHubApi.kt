package dev.codespacemobile.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class GitHubUserDto(val login: String)

data class CodespacesResponse(val codespaces: List<CodespaceDto> = emptyList())

data class RepoDto(@Json(name = "full_name") val fullName: String = "")

data class CodespaceDto(
    val name: String,
    val state: String = "Unknown",
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "web_url") val webUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val repository: RepoDto? = null
)

data class CreateCodespaceRequest(val ref: String)

data class RepositoryMetadataDto(
    @Json(name = "default_branch") val defaultBranch: String
)

data class RepositoryEntryDto(
    val name: String = "",
    val path: String = "",
    val type: String = "",
    val sha: String = "",
    val size: Long = 0,
    val content: String? = null,
    val encoding: String? = null
)

data class RepositorySaveRequest(
    val message: String,
    val content: String,
    val sha: String,
    val branch: String
)

data class RepositorySaveResponse(
    val content: RepositoryEntryDto? = null
)

interface GitHubApi {
    @GET("repos/{owner}/{repo}")
    suspend fun repositoryMetadata(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): RepositoryMetadataDto

    @GET("repos/{owner}/{repo}/contents")
    suspend fun repositoryRoot(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String
    ): List<RepositoryEntryDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun repositoryDirectory(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String
    ): List<RepositoryEntryDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun repositoryFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String
    ): RepositoryEntryDto

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun saveRepositoryFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: RepositorySaveRequest
    ): Response<RepositorySaveResponse>

    @GET("user")
    suspend fun validateToken(@Header("Authorization") bearer: String): GitHubUserDto

    @GET("user/codespaces")
    suspend fun listCodespaces(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 100
    ): CodespacesResponse

    @POST("repos/{owner}/{repo}/codespaces")
    suspend fun createCodespace(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body payload: CreateCodespaceRequest
    ): CodespaceDto

    @POST("user/codespaces/{name}/start")
    suspend fun startCodespace(@Path("name") name: String): CodespaceDto

    @POST("user/codespaces/{name}/stop")
    suspend fun stopCodespace(@Path("name") name: String): CodespaceDto

}

