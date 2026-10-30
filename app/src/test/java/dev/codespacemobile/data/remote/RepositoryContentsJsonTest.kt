package dev.codespacemobile.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Guards the GitHub Contents API schema used by the in-app editor. */
class RepositoryContentsJsonTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test fun directoryEntriesDecodeFromGitHubResponse() {
        val json = """[
            {"name":"src", "path":"src", "type":"dir", "sha":"abc", "size":0},
            {"name":"README.md", "path":"README.md", "type":"file", "sha":"def", "size":17}
        ]"""
        val type = com.squareup.moshi.Types.newParameterizedType(
            List::class.java, RepositoryContentDto::class.java
        )
        val entries = moshi.adapter<List<RepositoryContentDto>>(type).fromJson(json)!!
        assertEquals(2, entries.size)
        assertEquals("dir", entries[0].type)
        assertEquals("README.md", entries[1].path)
    }

    @Test fun fileUsesBase64AndShaForConflictSafety() {
        val file = moshi.adapter(RepositoryContentDto::class.java)
            .fromJson("""{
                "name":"README.md","path":"README.md","type":"file",
                "sha":"blob-sha", "size":5,"encoding":"base64",
                "content":"aGVsbG8=\n"
            }""")!!
        assertEquals("base64", file.encoding)
        assertEquals("blob-sha", file.sha)
        assertNotNull(file.content)
    }

    @Test fun commitBodyIncludesOriginalShaAndBranch() {
        val request = UpdateRepositoryFileRequest(
            message = "Change README", content = "aGVsbG8=", sha = "old-sha",
            branch = "main"
        )
        val json = moshi.adapter(UpdateRepositoryFileRequest::class.java)
            .toJson(request)
        assertEquals(true, json.contains("\"sha\":\"old-sha\""))
        assertEquals(true, json.contains("\"branch\":\"main\""))
    }
}
