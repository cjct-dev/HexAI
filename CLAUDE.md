# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HexAI is an Android mobile application (Kotlin/Jetpack Compose) that serves as a client for OpenAI-compatible LLM API servers. Features a dark hacker-themed UI with black/grey/green colors and glitch effects.

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 13+)
- **Language:** Kotlin with Java 17
- **UI:** Jetpack Compose with Material3

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (signed, minified)
./gradlew assembleRelease

# Install to connected device/emulator
./gradlew installDebug

# Run tests
./gradlew test

# Lint check
./gradlew lint
```

## Architecture

The app follows MVVM with a single-Activity architecture:

```
com.hexai/
├── MainActivity.kt           # Entry point, NavHost setup
├── data/
│   ├── api/
│   │   ├── OpenAIApiService.kt  # Retrofit + SSE streaming + HexApiClient
│   │   └── Models.kt            # DTOs including llama-serve Timings
│   ├── preferences/
│   │   └── SettingsDataStore.kt # DataStore for persistent settings
│   └── repository/
│       └── ChatRepository.kt    # Data access layer
├── ui/
│   ├── screens/
│   │   ├── ChatScreen.kt        # Main chat interface with connection indicator
│   │   └── SettingsScreen.kt    # Server config, model search, ping display
│   ├── components/
│   │   ├── CyberpunkEffects.kt  # Custom themed UI components
│   │   ├── MessageBubble.kt     # Chat message display
│   │   ├── MarkdownRenderer.kt  # Rich text rendering
│   │   └── StatsPanel.kt        # Inference statistics
│   └── theme/                   # Hex colors (black/grey/green), typography
└── viewmodel/
    └── ChatViewModel.kt         # AndroidViewModel with DataStore, health check
```

## Key Implementation Details

**State Management:**
- Single `ChatViewModel` extends `AndroidViewModel` for DataStore access
- All state via `StateFlow`
- Settings persist via DataStore (server URL, API key, model settings)
- Auto-connect on app start if URL is saved

**API Communication:**
- Retrofit for REST calls (model list endpoint)
- Custom OkHttp-based SSE client for streaming responses
- `HexApiClient` for health checks and model management
- Supports both OpenAI `usage` and llama-serve `timings` for stats

**llama-serve Features:**
- Parses `timings` object for accurate tokens/second
- Health check ping every 5 seconds when connected
- Model load/unload support (router mode)

## Key Files for Common Tasks

| Task | Files to Modify |
|------|-----------------|
| UI changes | `ui/screens/*.kt`, `ui/components/*.kt` |
| API/networking | `data/api/OpenAIApiService.kt`, `data/api/Models.kt` |
| State management | `viewmodel/ChatViewModel.kt` |
| Theme/styling | `ui/theme/*.kt` (Hex colors) |
| Persistence | `data/preferences/SettingsDataStore.kt` |
| Add new settings | `SettingsScreen.kt`, `ChatViewModel.kt`, `SettingsDataStore.kt` |

## Theme Colors

The app uses a monochrome hacker aesthetic:
- `HexGreen` (#00FF00) - Primary accent, connection status
- `HexBlack` (#000000) - Background
- `HexDarkGrey` (#0A0A0A) - Surface
- `HexGrey200-500` - UI elements
- `HexTextPrimary` (#E0E0E0) - Main text

## Git Workflow

All code changes should be committed and pushed to the GitHub repository after making modifications.
