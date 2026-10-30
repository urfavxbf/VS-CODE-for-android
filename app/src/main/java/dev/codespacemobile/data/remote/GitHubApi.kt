package dev.codespacemobile.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

interface GitHubApi {
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
