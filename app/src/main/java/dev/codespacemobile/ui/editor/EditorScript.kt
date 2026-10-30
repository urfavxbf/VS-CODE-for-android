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
            html.cm-next .monaco-workbench .part.activitybar,
            html.cm-next .monaco-workbench .part.statusbar { display: none !important; }
            html.cm-next .monaco-workbench .part.sidebar,
            html.cm-next .monaco-workbench .part.editor,
            html.cm-next .monaco-workbench .part.panel,
            html.cm-next .monaco-workbench .part.auxiliarybar {
              position: absolute !important;
              inset: 0 !important;
              min-width: 0 !important;
              width: 100% !important;
              max-width: 100% !important;
              height: 100% !important;
              visibility: hidden !important;
              pointer-events: none !important;
              opacity: 0 !important;
              z-index: 0 !important;
            }
            html.cm-next[data-cm-pane='editor'] .monaco-workbench .part.editor,
            html.cm-next[data-cm-pane='sidebar'] .monaco-workbench .part.sidebar,
            html.cm-next[data-cm-pane='panel'] .monaco-workbench .part.panel {
              visibility: visible !important;
              pointer-events: auto !important;
              opacity: 1 !important;
              z-index: 10 !important;
            }
            html.cm-next .monaco-list-row { min-height: 40px !important; }
            html.cm-next .monaco-workbench .part.sidebar .content,
            html.cm-next .monaco-workbench .part.panel .content { min-width: 0 !important; }
            html.cm-next .breadcrumbs-control { display: none !important; }
          `;
          document.head.appendChild(style);
          document.documentElement.classList.add('cm-next');
          document.documentElement.setAttribute('data-cm-pane', 'editor');
          window.__cmNextSetPane = function (pane) {
            if (['editor', 'sidebar', 'panel'].indexOf(pane) === -1) return;
            document.documentElement.setAttribute('data-cm-pane', pane);
            window.requestAnimationFrame(function () {
              window.dispatchEvent(new Event('resize'));
            });
          };
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
              if (window.__cmNextSetPane) window.__cmNextSetPane('$target');
            })();
        """.trimIndent()
    }
}
