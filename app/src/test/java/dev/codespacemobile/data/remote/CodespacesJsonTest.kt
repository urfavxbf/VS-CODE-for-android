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

    @Test fun `codespace without web url remains valid`() {
        val dto = moshi.adapter(CodespaceDto::class.java)
            .fromJson("""{"name":"preparing-space","state":"Starting"}""")!!
        assertNull(dto.webUrl)
        assertEquals("Starting", dto.state)
    }
}
