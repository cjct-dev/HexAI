# HexAI

A sleek Android LLM chat client with a dark hacker aesthetic. Connect to llama.cpp, llama-serve, or any OpenAI-compatible API server.

## Features

- **Dark Hacker Theme**: Black, grey, and terminal green color scheme
- **llama-serve Compatibility**: Full support for llama.cpp server timing statistics
- **Streaming Responses**: Real-time token streaming with SSE
- **Inference Stats**: Tokens/second, time to first token, prompt/completion counts
- **Model Search**: Filter models in the selection dropdown
- **Model Management**: Load/unload models on llama-serve router mode
- **Connection Monitor**: Live ping latency and connection status indicator
- **Persistent Settings**: Server URL, API key, and model settings saved automatically
- **Thinking Tokens**: Support for models with reasoning/thinking output
- **Chat Export/Import**: Save and load conversations as Markdown

## Installation

Download the latest APK from the [Releases](../../releases) page and install on your Android device.

Requirements:
- Android 8.0 (API 26) or higher
- An LLM server (llama.cpp, Ollama, vLLM, etc.)

## Server Setup

### llama.cpp / llama-serve

```bash
# Start llama-server with a model
llama-server -m model.gguf --host 0.0.0.0 --port 8080

# Or with router mode for model management
llama-server --host 0.0.0.0 --port 8080 --model-store /path/to/models
```

### Ollama

```bash
ollama serve
# Models available at http://localhost:11434
```

## Usage

1. Open HexAI
2. Go to Settings (gear icon)
3. Enter your server URL (e.g., `http://192.168.1.100:8080`)
4. Add API key if required (optional for most local servers)
5. Tap CONNECT
6. Select a model from the dropdown
7. Start chatting!

The app will automatically reconnect to saved servers on restart.

## Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/HexAI.git
cd HexAI

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore)
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

## Architecture

```
com.hexai/
├── MainActivity.kt              # Entry point, navigation
├── data/
│   ├── api/
│   │   ├── OpenAIApiService.kt  # Retrofit + SSE streaming
│   │   └── Models.kt            # DTOs with llama-serve timings
│   ├── preferences/
│   │   └── SettingsDataStore.kt # DataStore persistence
│   └── repository/
│       └── ChatRepository.kt    # Data access layer
├── ui/
│   ├── screens/
│   │   ├── ChatScreen.kt        # Main chat interface
│   │   └── SettingsScreen.kt    # Server config, model settings
│   ├── components/              # Reusable UI components
│   └── theme/                   # Colors, typography, effects
└── viewmodel/
    └── ChatViewModel.kt         # State management
```

## API Compatibility

HexAI works with any OpenAI-compatible chat completions API:

- **llama.cpp / llama-serve** - Full support including `timings` statistics
- **Ollama** - Via OpenAI compatibility layer
- **vLLM** - OpenAI-compatible endpoint
- **LocalAI** - OpenAI-compatible endpoint
- **OpenAI / OpenRouter** - With API key

### llama-serve Specific Features

- Parsing of `timings` object for accurate tokens/second
- `/health` endpoint for connection monitoring
- Model load/unload via `/models/load` and `/models/unload` (router mode)

## License

MIT License
