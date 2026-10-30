package dev.codespacemobile.ui.editor

/**
 * A narrow, best-effort enhancement for VS Code's browser UI.
 * No JavaScript bridge, credential injection, or document access is exposed to Android.
 * The workbench is third-party HTML and its selectors may change independently.
 */
object EditorScript {
    enum class Pane { EDITOR, FILES, SOURCE_CONTROL, TERMINAL }

    const val BOOTSTRAP = """
        (function () {
          'use strict';
          if (window.__codespaceMobileNext) return;
          window.__codespaceMobileNext = true;
          var style = document.createElement('style');
          style.id = 'codespace-mobile-next-layout';
          style.textContent = `
            .monaco-workbench .monaco-list-row { min-height: 36px; }
            .monaco-workbench .part.editor { min-width: 0; }
            .monaco-workbench .part.sidebar .content { min-width: 0; }
            .monaco-workbench .action-label { min-height: 36px; }
          `;
          document.head.appendChild(style);
        })();
    """

    /**
     * Probe page readiness without hiding the hosted UI. A false result triggers
     * an in-app recovery prompt and an external-browser fallback.
     */
    const val READINESS_CHECK = """
        (function () {
          var root = document.querySelector('.monaco-workbench');
          if (!root) return false;
          var editor = root.querySelector('.part.editor, .monaco-editor, .editor-instance');
          return !!(editor && editor.getBoundingClientRect().width > 48);
        })();
    """

    fun activate(pane: Pane): String {
         val action = when (pane) {
            Pane.EDITOR -> "workbench.action.focusActiveEditorGroup"
            Pane.FILES -> "workbench.view.explorer"
            Pane.SOURCE_CONTROL -> "workbench.view.scm"
            Pane.TERMINAL -> "workbench.action.terminal.toggleTerminal"
        }
        return """
            (function () {
              var command = '$action';
              var root = document.querySelector('.monaco-workbench');
              if (!root) return;
              if (command === 'workbench.action.focusActiveEditorGroup') {
                var editor = root.querySelector('.part.editor .monaco-editor textarea.inputarea, .part.editor .monaco-editor');
                if (editor) editor.focus();
                return;
              }
              var candidate = root.querySelector(
                '[id="' + command + '"] .action-label, ' +
                '[id="' + command + '"], ' +
                '[data-id="' + command + '"] .action-label, ' +
                '[data-id="' + command + '"]'
              );
              if (candidate) candidate.click();
              // Never force hidden desktop panels: the hosted workbench manages layout.
            })();
        """.trimIndent()
    }
}
