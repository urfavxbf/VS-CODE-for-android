package dev.codespacemobile.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Protects the API contract used by the native dashboard. */
class CodespacesJsonTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test fun `codespaces list accepts real GitHub field names`() {
        val adapter = moshi.adapter(CodespacesResponse::class.java)
        val response = adapter.fromJson("""{
            "total_count": 1,
            "codespaces": [{
                "name": "my-space-abc", "display_name": "Mobile dev", "state": "Available",
                "web_url": "https://my-space-abc.github.dev",
                "repository": {"full_name": "octocat/Hello-World"}
            }]
        }""")!!
        assertEquals(1, response.codespaces.size)
        assertEquals("Mobile dev", response.codespaces.single().displayName)
        assertEquals("octocat/Hello-World", response.codespaces.single().repository?.fullName)
    }

    @Test fun `repository directory parses files and folders`() {
        val adapter = moshi.adapter<List<RepositoryContentDto>>(
            com.squareup.moshi.Types.newParameterizedType(
                List::class.java, RepositoryContentDto::class.java
            )
        )
        val list = adapter.fromJson("""[
          {"name":"src","path":"src","type":"dir","sha":"abc123","size":0},
          {"name":"Main.kt","path":"Main.kt","type":"file","sha":"def456","size":120}
        ]""")!!
        assertEquals("dir", list[0].type)
        assertEquals("src", list[0].path)
        assertEquals("file", list[1].type)
        assertEquals("Main.kt", list[1].name)
    }

    @Test fun `repository file and save response retain GitHub blob sha`() {
        val file = moshi.adapter(RepositoryContentDto::class.java)
            .fromJson("""{"name":"hello.kt","path":"src/hello.kt","type":"file",
              "sha":"old-sha","size":6,"encoding":"base64","content":"SGVsbG8K"}""")!!
        assertEquals("old-sha", file.sha)
        assertEquals("base64", file.encoding)
        val save = moshi.adapter(UpdateRepositoryFileResponse::class.java)
            .fromJson("""{"content":{"name":"hello.kt","path":"src/hello.kt",
              "type":"file","sha":"new-sha"}}""")!!
        assertEquals("new-sha", save.content?.sha)
    }

    @Test fun `codespace without web url remains valid`() {
        val dto = moshi.adapter(CodespaceDto::class.java)
            .fromJson("""{"name":"preparing-space","state":"Starting"}""")!!
        assertNull(dto.webUrl)
        assertEquals("Starting", dto.state)
    }
}
