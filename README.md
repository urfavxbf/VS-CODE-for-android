# VS Code for Android — Codespaces

Native Android dashboard for GitHub Codespaces plus a full-width, single-pane editor surface. This is a **new** app, not a rename of the older VS Code Mobile WebView fork.

> Source repository: https://github.com/urfavxbf/VS-CODE-for-android. GitHub Actions must pass before a debug APK can be considered build-verified. Live Codespaces editor/login functionality still needs a device test.

## Features

- Kotlin, Jetpack Compose, Material 3, MVVM, Hilt, Retrofit and Room.
- Official GitHub REST Codespaces API: list, create (by repository/ref), start, stop.
- Native Cards with loading, error, empty and cached/offline states.
- Personal access token is encrypted using AES-256-GCM with an Android Keystore key; never hardcoded, never injected into WebView.
- Native editor view with full-width WebView and a bottom tab bar for Code, Files, Git, Terminal. Best-effort JS supports single-pane VS Code web UI.
- Browser escape hatch: open the same Codespace in a trusted external browser if WebView login or editor compatibility breaks.
- Secure HTTPS-only networking, strict editor URL validation, back-up disabled.
- GitHub Actions builds the debug APK with lint, JVM tests and emulator launch smoke test; artifact includes SHA-256 checksum.

## Permissions / Authentication

Sign-in uses a **GitHub personal access token** in v0.1 because reliable first-party OAuth requires creating a GitHub OAuth App and configuring a public `client_id`. There are no embedded OAuth credentials.

- Classic PAT: `codespace` scope. Create at https://github.com/settings/tokens.
- Fine-grained token: grant **Codespaces (read/write)** as needed plus **Codespaces lifecycle admin (write)** for start/stop. Select only the repositories needed; GitHub's token scopes are checked server-side.
- Token is validated against `GET /user` before being saved. A valid login token may still lack Codespaces privileges, in which case the dashboard returns a safe permission error.
- If the hosted editor requires browser sign-in, sign in separately in the editor or choose **Open in external browser**. The REST token is *never shared* with the embedded browser.

**Security note:** A personal token can grant sensitive access. Only enter tokens you generated yourself, and revoke a compromised token in GitHub settings. Do not paste tokens into repository files, issues, screenshots, or chat.

## Troubleshooting: Codespace list works, but editor is blank

If your GitHub token connects and shows Codespaces but tapping **Try in app (beta)**
opens an empty screen, this does **not** mean your token is invalid.

- The personal access token authenticates the **REST dashboard only**.
  The hosted VS Code editor needs a separate GitHub browser login/session.
- Use **Open in browser** on the Codespace card (recommended). Sign in to
  GitHub in Chrome/your default browser if asked.
- The embedded editor is an optional beta. It never receives your PAT.
  If it cannot reach the VS Code workbench, it now shows **Open in browser**
  and **Retry**, rather than silently displaying an empty WebView.
- We removed invasive CSS that previously hid all desktop VS Code panels;
  the browser-hosted editor controls its own workspace layout.

A successful CI test only verifies the Android build and emulator startup.
It cannot prove that your private Codespace editor works on your actual phone.

## Native repository file editor (no browser redirect)

Open a Codespace card and tap **Edit repository files**. The Compose app can
browse folders, open UTF-8 text files up to 512 KiB, make changes, and create an
explicit Git commit on that repository's **default branch**. The commit requires
a confirmation and a message; unsaved edits are guarded by a discard dialog.

**Important:** This edits files on GitHub, not the running Codespace VM's working
tree. Changes in an uncommitted Codespace are not visible in this editor, and a
GitHub commit does not automatically update a running VM's local branch.
The terminal and live debugging environment still require a separate secure
workspace connection.

**Permissions:** The existing Codespaces-only classic PAT may read public
repositories but cannot commit files. For a classic PAT GitHub requires the
broad `repo` scope to write repository contents. Prefer a **fine-grained PAT**
restricted to the selected repositories with Contents read/write permissions,
alongside the Codespaces permissions needed for the dashboard. Do not share
tokens or add them to Git commits. The native editor explains 403/404 errors
instead of silently opening a browser.

## Edit source code directly in the Android app

The **Edit repository files** action on any Codespace card now opens a native
Compose file explorer and multiline text editor. You can navigate directories,
edit small UTF-8 files and explicitly **Save as a Git commit**. No website or
browser is involved in this flow.

**Important:** this saves changes in the GitHub **repository default branch**,
not in the live Codespace's uncommitted workspace. Codespace-specific changes
and terminal commands are not available via GitHub's Codespaces REST API.
Keep your editor open in the app for repository work; the browser fallback is
only for features of the hosted IDE that are not yet available natively.

### Additional GitHub token permission

The old `codespace`-only PAT is enough to list and start Codespaces, but is
not generally enough to edit repository contents.

Prefer a **fine-grained PAT** restricted to the repository you intend to edit:

- **Contents: Read and write** for saving, or Read-only for browsing.
- **Codespaces** and **Codespaces lifecycle admin** permissions as needed for
  the dashboard, provided those permissions are available for the token type.
- If using a classic token for a **private** repository, the `repo` scope
  may be required. That is a broad permission: prefer a fine-grained token.

Enter the updated token via the app's Sign out / Connect flow. Never commit
tokens or paste them into chat.

### File editing boundaries

- Existing text files only, up to **512 KiB**, UTF-8 encoding.
- Every explicit Save creates one GitHub commit on the displayed branch.
- There is a confirmation dialog containing the branch and commit message.
- A 409 conflict does not silently overwrite a remote file; unsaved edits
  remain on-screen for manual recovery.
- Binary files, large files, unsaved Codespace buffers, and a live terminal
  are **not supported** by this native editor yet.

## Native / Web split

```
Android Compose UI
  ├── Native GitHub Codespaces dashboard ── Retrofit ── api.github.com
  │        ├── SecureTokenStore (Android Keystore AES-GCM)
  │        └── Room (offline cached list)
  └── Full-width editor activity
           ├── native bottom navigation
           ├── WebView (Codespace web_url from GitHub API)
           └── Open in external browser fallback
```

The GitHub Codespaces REST API manages Codespaces **but does not provide editor layout, Monaco editor commands or interactive terminal input**. The WebView enhancement is best-effort because VS Code DOM selectors and authentication requirements can change. Keeping the external browser button prevents users from getting stuck.

## Project setup

- Android Studio with JDK 17 and Android SDK 36.
- Android Gradle Plugin 8.11.1, Kotlin 2.2.21, Gradle 8.13.
- Min Android 8.0 (API 26). Target Android 15 (API 35).

GitHub Actions installs Gradle 8.13 with `gradle/actions/setup-gradle`. The repository includes a SHA-256 verified Gradle wrapper bootstrap that fetches the official wrapper JAR when first invoked; Android Studio can use this wrapper. Always review build tooling and dependencies before trusting a downloaded APK.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. This debug APK is for testing only. A properly signed release requires a private signing key and a release workflow.

## QA checklist

1. Install APK on Android 8.0+, open dashboard with no token.
2. Enter a limited-scope token and confirm login; token should never appear in logs.
3. Confirm list, create, start, stop and error states with a test Codespace.
4. Open a running Codespace; verify Code, Files, Git, Terminal tabs on a **phone**.
5. Verify login / 2FA in the editor and external browser fallback.
6. Test offline cached list, token revocation / 401 and sign out.
7. Test small 360dp screens, device rotation, keyboard resizing, accessibility, and background resume.

## Next work

- Register a GitHub OAuth App and add PKCE/device-flow authorization; replace manual token entry.
- Add feature-specific integration tests for live Codespaces and editor DOM compatibility.
- Add signed release, app icon, translations, accessibility and privacy policy.
