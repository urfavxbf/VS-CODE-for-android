package dev.codespacemobile.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodespaceUrlPolicyTest {
    @Test fun permitsGithubCodespaceHosts() {
        assertTrue(CodespaceUrlPolicy.isTrusted("https://github.com/codespaces"))
        assertTrue(CodespaceUrlPolicy.isTrusted("https://nice-cat-1.app.github.dev/"))
        assertTrue(CodespaceUrlPolicy.isTrusted("https://project.githubpreview.dev/"))
    }

    @Test fun rejectsUnexpectedNavigation() {
        assertFalse(CodespaceUrlPolicy.isTrusted("http://github.com"))
        assertFalse(CodespaceUrlPolicy.isTrusted("https://github.com.evil.com"))
        assertFalse(CodespaceUrlPolicy.isTrusted("javascript:alert(1)"))
        assertFalse(CodespaceUrlPolicy.isTrusted("https://github.com@evil.com"))
        assertFalse(CodespaceUrlPolicy.isTrusted("https://evil.com"))
        assertFalse(CodespaceUrlPolicy.isTrusted("https://github.com:1234"))
        assertFalse(CodespaceUrlPolicy.isTrusted(""))
    }
}
