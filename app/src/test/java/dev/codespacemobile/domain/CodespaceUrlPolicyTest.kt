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

    @Test fun identifiesHostedEditorAndSeparateGitHubLogin() {
        assertTrue(CodespaceUrlPolicy.isEditorUrl("https://bright-fox-123.app.github.dev/"))
        assertTrue(CodespaceUrlPolicy.isEditorUrl("https://foo.githubpreview.dev/"))
        assertFalse(CodespaceUrlPolicy.isEditorUrl("https://github.com/login"))
        assertTrue(CodespaceUrlPolicy.isSignInPage("https://github.com/login?return_to=%2Fcodespaces"))
        assertTrue(CodespaceUrlPolicy.isSignInPage("https://github.com/sessions/verified-device"))
        assertFalse(CodespaceUrlPolicy.isSignInPage("https://bright-fox-123.app.github.dev/"))
        assertFalse(CodespaceUrlPolicy.isSignInPage("https://github.com.evil.com/login"))
        assertFalse(CodespaceUrlPolicy.isSignInPage("http://github.com/login"))
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
