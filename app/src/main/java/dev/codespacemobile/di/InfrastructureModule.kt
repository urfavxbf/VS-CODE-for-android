package dev.codespacemobile.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.codespacemobile.auth.SecureTokenStore
import dev.codespacemobile.data.local.CodespaceDao
import dev.codespacemobile.data.local.CodespaceDatabase
import dev.codespacemobile.data.remote.GitHubApi
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InfrastructureModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): CodespaceDatabase =
        Room.databaseBuilder(context, CodespaceDatabase::class.java, "codespace_cache_v1.db")
            .build()

    @Provides
    fun dao(database: CodespaceDatabase): CodespaceDao = database.dao()

    @Provides
    @Singleton
    fun okHttp(tokens: SecureTokenStore): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                // Never attach credentials to non-GitHub hosts or a redirect.
                val builder = original.newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                if (original.url.host == "api.github.com" && original.header("Authorization") == null) {
                    tokens.read()?.let { builder.header("Authorization", "Bearer $it") }
                }
                chain.proceed(builder.build())
            })
            .followRedirects(false)
            .build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient): GitHubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            )
        )
        .build()
        .create(GitHubApi::class.java)
}
