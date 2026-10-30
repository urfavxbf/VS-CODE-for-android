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

    fun activate(pane: Pane): String {
        val target = when (pane) {
            Pane.EDITOR -> "editor"
            Pane.FILES, Pane.SOURCE_CONTROL -> "sidebar"
            Pane.TERMINAL -> "panel"
        }
        val action = when (pane) {
            Pane.EDITOR -> ""
            Pane.FILES -> "workbench.view.explorer"
            Pane.SOURCE_CONTROL -> "workbench.view.scm"
            Pane.TERMINAL -> "workbench.action.terminal.toggleTerminal"
        }
        return """
            (function () {
              var command = '$action';
              if (command) {
                var root = document.querySelector('.monaco-workbench');
                var candidate = root && root.querySelector(
                  '[id="' + command + '"], [data-id="' + command + '"]'
                );
                if (candidate) candidate.click();
              }
              // Keep hosted VS Code layout intact; do not hide its panes.
            })();
        """.trimIndent()
    }
}
