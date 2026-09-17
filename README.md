<div align=\"center\">

# ⚡ OpenChat AI

**A native, production-grade Android AI assistant client built with Jetpack Compose & Material 3**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-7C4DFF?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

</div>

---

## 📖 Overview

**OpenChat AI** is a fast, elegant, privacy-first Android client for modern Large Language Models. Built completely in idiomatic Kotlin with Jetpack Compose, it provides a unified interface for connecting to any OpenAI-compatible endpoint (OpenAI, Anthropic Claude via proxy, Google Gemini, DeepSeek, Groq, Mistral, Ollama, OpenRouter, and LocalAI).

Unlike web-wrapper chat clients, OpenChat AI runs 100% natively on Android with hardware-accelerated rendering, real-time SSE streaming, client-side LaTeX math parsing, multi-language syntax highlighting, multimodal file processing, and agentic web-search tool calling.

---

## ✨ Key Features

### 🔌 Universal Multi-Provider Support
- Connect to **OpenAI**, **DeepSeek**, **Groq**, **OpenRouter**, **Together AI**, or **Local Ollama** instances (http://10.0.2.2:11434 / LAN).
- Dynamically fetch available models from provider /v1/models endpoints.
- Fast model switching per conversation or globally.

### ⚡ Real-Time Streaming & Deep Reasoning
- Low-latency Server-Sent Events (SSE) token streaming.
- Collapsible reasoning & thought process display (tailored for **DeepSeek-R1**, **o1**, and reasoning-oriented models).
- Regenerate, edit-and-resend, or prune conversation branches seamlessly.

### 🌐 Agentic Web Search & Tool Calling
- Function calling with live web search integration.
- Assistant automatically searches for up-to-date real-world facts when needed.
- Interactive cited web sources cards with direct web navigation.

### 🧠 Persistent Long-Term Memory
- Cross-conversation memory bank stored on-device.
- Models can autonomously invoke the save_memory tool to remember user preferences, project context, and custom directives.

### 📎 Multimodal Attachments
- **Image Vision**: Attach photos and screenshots for multimodal analysis (automatic base64 image downsampling and optimization).
- **PDF Document Extraction**: Native PDF text extraction using pdfbox-android.
- **Text & Source Code**: Attach code files, markdown docs, and logs directly into the context window.

### 📐 Rich Formatting: LaTeX & Code Syntax Highlighting
- **Mathematical Formulations**: Inline ($...$) and block ($$...) LaTeX equation rendering.
- **Syntax Highlighting**: Real-time token highlighting across Kotlin, Java, Python, JavaScript, TypeScript, Rust, Go, C++, SQL, JSON, YAML, Bash, and XML/HTML.
- One-tap copy for code blocks and individual messages.

### 🎨 Material Design 3 UI/UX
- Dynamic color theming, high-contrast dark mode, and edge-to-edge typography.
- Smooth Compose animations and reactive UI state driven by StateFlow.
- System prompt presets (Coder, Writer, Translator, Concise, Teacher) + customizable personas.

### 🛡️ Local & Private
- **Zero middleman telemetry**: All requests go directly from your device to your configured API endpoints.
- API keys and chat histories are kept strictly on your local device.

---

## 🏗️ Architecture & Tech Stack

`mermaid
graph TD
    UI[Jetpack Compose UI & Material 3]
    VM[AppViewModel - StateFlow]
    REPO[AppRepository]
    STORE[Encrypted / Local Storage]
    KTOR[Ktor HTTP / SSE Client]
    PDF[PDFBox Android Extractor]
    API[(OpenAI-Compatible LLM Providers)]

    UI -->|Events| VM
    VM -->|State| UI
    VM --> REPO
    REPO --> STORE
    REPO --> KTOR
    REPO --> PDF
    KTOR -->|HTTPS / Stream| API
`

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.1+ |
| **UI Framework** | Jetpack Compose (BOM 2026.02.01), Material 3, Navigation Compose |
| **Networking** | Ktor Client Core & Android Engine, Content Negotiation, SSE Streaming |
| **Serialization** | Kotlinx Serialization JSON |
| **Image Loading** | Coil 3 (Compose & OkHttp network engine) |
| **Document Processing** | Apache PDFBox for Android |
| **Dependency Injection** | Koin AndroidX Compose |
| **Target SDK** | Android 15 (compileSdk 36, minSdk 24) |

---

## 📁 Project Structure

`	ext
Open-Chat/
├── .github/
│   └── workflows/
│       └── android.yml            # Automated CI build on GitHub Actions
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/yuvraj/openchatai/
│   │       │   ├── MainActivity.kt
│   │       │   ├── data/
│   │       │   │   ├── attachments/    # Document & image processing
│   │       │   │   ├── model/          # Data entities (Provider, Message, Memory)
│   │       │   │   ├── network/        # Ktor API client & Web Search service
│   │       │   │   └── repository/     # AppRepository & data persistence
│   │       │   └── ui/
│   │       │       ├── AppViewModel.kt # Unified UI state management
│   │       │       ├── components/     # LaTeX, Markdown, SyntaxHighlight, Bubbles
│   │       │       ├── navigation/     # Jetpack Compose Navigation graph
│   │       │       ├── screens/        # Chat, Providers, and Settings screens
│   │       │       └── theme/          # Material 3 Color Schemes & Typography
│   │       └── res/                    # App icons, mipmaps, and theme resources
│   ├── build.gradle.kts
│   └── proguard-rules.pro             # Optimized R8 / ProGuard rules
├── gradle/
│   └── libs.versions.toml             # Centralized Gradle version catalog
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── LICENSE                            # MIT License
└── README.md
`

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Ladybug (2024.2.1+) or newer
- **JDK 17** or **JDK 21** configured as Gradle JDK
- Android Device or Emulator running **Android 7.0 (API 24)** or higher

### Building Locally

1. **Clone the repository**:
   `ash
   git clone https://github.com/DivineChecker/Open-Chat.git
   cd Open-Chat
   `

2. **Open in Android Studio**:
   - Select **File > Open** and choose the Open-Chat project directory.
   - Allow Gradle to sync dependencies.

3. **Run on Device or Emulator**:
   - Connect your Android device or start an emulator.
   - Click **Run 'app'** (Shift + F10).

4. **Build APK via Command Line**:
   `ash
   # On Windows (PowerShell)
   .\gradlew.bat assembleDebug

   # On macOS / Linux
   chmod +x gradlew
   ./gradlew assembleDebug
   `
   The debug APK will be generated at pp/build/outputs/apk/debug/app-debug.apk.

---

## ⚙️ Configuration & First Launch

1. Launch OpenChat AI on your device.
2. Tap the **Providers** tab.
3. Tap **Add Provider** and select a preset or enter a custom endpoint:
   - **OpenAI**: https://api.openai.com/v1
   - **DeepSeek**: https://api.deepseek.com/v1
   - **Groq**: https://api.groq.com/openai/v1
   - **Ollama (Local)**: http://10.0.2.2:11434/v1 (from Android emulator) or your PC LAN IP
4. Paste your API key and tap **Fetch Models** to test the connection.
5. Select your preferred default model and begin chatting!

---

## 🤝 Contributing

Contributions are warmly welcomed! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests.

1. Fork the repository
2. Create a feature branch (git checkout -b feature/amazing-feature)
3. Commit your changes (git commit -m 'Add amazing feature')
4. Push to branch (git push origin feature/amazing-feature)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
