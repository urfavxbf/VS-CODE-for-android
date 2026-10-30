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
