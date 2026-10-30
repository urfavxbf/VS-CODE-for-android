package dev.codespacemobile.ui.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorScriptTest {
    @Test fun bootstrapIsSinglePaneAndHasNoJavaBridge() {
        assertTrue(EditorScript.BOOTSTRAP.contains("data-cm-pane"))
        assertTrue(EditorScript.BOOTSTRAP.contains("pointer-events: none"))
        assertFalse(EditorScript.BOOTSTRAP.contains("addJavascriptInterface"))
    }

    @Test fun tabsMapToCorrectPanels() {
        assertTrue(EditorScript.activate(EditorScript.Pane.EDITOR).contains("'editor'"))
        assertTrue(EditorScript.activate(EditorScript.Pane.FILES).contains("'sidebar'"))
        assertTrue(EditorScript.activate(EditorScript.Pane.SOURCE_CONTROL).contains("workbench.view.scm"))
        assertTrue(EditorScript.activate(EditorScript.Pane.TERMINAL).contains("'panel'"))
    }
}
