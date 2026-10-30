package dev.codespacemobile.ui.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorScriptTest {
    @Test fun bootstrapIsNonDestructiveAndDoesNotInjectCredentials() {
        val script = EditorScript.BOOTSTRAP
        assertTrue(script.contains("min-height: 36px"))
        assertFalse("Never hide the editor", script.contains("visibility: hidden"))
        assertFalse("Never hide the editor", script.contains("pointer-events: none"))
        assertFalse("Never overlay panels", script.contains("position: absolute"))
        assertFalse(script.contains("addJavascriptInterface"))
        assertFalse(script.contains("Authorization"))
    }

    @Test fun aMissingWorkbenchWillShowRecoveryInsteadOfBlankingPage() {
        assertTrue(EditorScript.READINESS_CHECK.contains("monaco-workbench"))
        assertTrue(EditorScript.READINESS_CHECK.contains("return false"))
        assertTrue(EditorScript.READINESS_CHECK.contains("getBoundingClientRect"))
    }

    @Test fun tabsKeepUsingRealVsCodeControls() {
        assertTrue(EditorScript.activate(EditorScript.Pane.EDITOR).contains("focusActiveEditorGroup"))
        assertTrue(EditorScript.activate(EditorScript.Pane.FILES).contains("workbench.view.explorer"))
        assertTrue(EditorScript.activate(EditorScript.Pane.SOURCE_CONTROL).contains("workbench.view.scm"))
        assertTrue(EditorScript.activate(EditorScript.Pane.TERMINAL).contains("workbench.action.terminal.toggleTerminal"))
    }
}
