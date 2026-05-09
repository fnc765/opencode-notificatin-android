# OpenCode Notifier for Android

Android companion app for [OpenCode](https://opencode.ai) that delivers push notifications for AI agent events — approval requests, task completion, questions, and errors.

You code with OpenCode in your browser (Web UI or Portal). This app runs on your Android phone, watching for events via SSE. When the AI needs you, you get a notification with action buttons.

## Features

- **Approval notifications** — Approve or deny permission requests directly from the notification
- **Completion notifications** — Know when a session finishes
- **Question notifications** — Get alerted when the AI asks you a question
- **Error notifications** — See errors immediately
- **All projects** — Monitors `/global/event` to receive events from every OpenCode project
- **Portal deep links** — Tapping a notification opens the exact session in [Portal](https://github.com/hosenur/portal)
- **In-app debug log** — Real-time SSE event log with copy-to-clipboard export
- **Dark/Light theme** — Follows system theme, supports Material You
- **Foreground service** — Persistent SSE connection with automatic reconnection

## How It Works

```
[OpenCode Server] ──SSE──> [Android App] ──notification──> [Your Phone]
       │                        │
       │                   Approve/Deny
       │                   reply via HTTP
       └────────────────────────┘
```

The app connects to OpenCode's `/global/event` SSE endpoint and listens for:

| Event | Notification |
|---|---|
| `permission.asked` | Approval needed + [Approve] [Deny] buttons |
| `session.idle` | Task completed (taps to web UI) |
| `session.error` | Error occurred |
| `question.asked` | AI has a question |

## Build

Builds use Docker — no local JDK or Android SDK required.

```bash
docker build -t opencode-notifier .
docker run --rm --entrypoint cat opencode-notifier /opencode-notifier-release.apk > app-release.apk
```

To build with your own signing key, place `opencode-notifier.keystore` and `keystore.properties` in the project root.

For local builds without Docker, generate the Gradle wrapper first:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew assembleRelease
```

## Install

```bash
# Via ADB
adb install app-release.apk

# Or download from your HTTP server
# wget http://your-server:8080/app-release.apk
```

Requires Android 8.0+ (API 26).

## Setup

### 1. Start OpenCode Server

```bash
OPENCODE_SERVER_PASSWORD=your-password opencode web --hostname 0.0.0.0
```

For session-specific deep links, also run [Portal](https://github.com/hosenur/portal):

```bash
bunx openportal
```

### 2. Configure the App

1. Open **OpenCode Notifier** on your phone
2. Go to **Settings**
3. Enter your OpenCode server URL (e.g. `http://192.168.1.100:4096`)
4. Enter username/password (leave empty if no auth)
5. Enter Web UI URL (e.g. `http://192.168.1.100:3000` for Portal)
6. Select **Web UI Type**: Portal or OpenCode Web
7. Tap **Save**

The app will connect automatically via foreground service.

## Releases

Every push to `main` triggers GitHub Actions, which:

1. Auto-bumps the version (`versionCode` incremented, `versionName` set to `YYYY.MM.DD.{code}`)
2. Builds the APK via Docker
3. Creates a **draft release** with the APK attached

To publish a release, go to the [Releases](../../releases) page, review the draft, and click **Publish**. The latest APK is always available under **Assets**.

### 3. Test with Mock Server

A mock server is included for testing notifications without a real OpenCode instance:

```bash
python3 mock_opencode_server.py
# Then point the app to http://your-ip:4196
```

## Requirements

- **Server**: OpenCode CLI installed on a reachable machine
- **Phone**: Android 8.0+ (API 26)
- **Network**: Phone and server must be on the same network (or use Tailscale/VPN)

## Tech Stack

- Kotlin + Jetpack Compose
- OkHttp (SSE streaming)
- Kotlinx Serialization (JSON parsing)
- DataStore (settings persistence)
- Lifecycle Service (foreground service)

## License

MIT — see [LICENSE](LICENSE)

## Disclaimer

This project is not affiliated with [OpenCode](https://github.com/anomalyco/opencode) or the Anomaly team.
