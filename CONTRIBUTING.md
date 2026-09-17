# Contributing to OpenChat AI

Thank you for your interest in improving OpenChat AI! We welcome contributions of all kinds: bug fixes, new features, UI enhancements, documentation improvements, and translations.

## Development Setup

1. Fork the repo and clone your fork locally:
   `ash
   git clone https://github.com/DivineChecker/Open-Chat.git
   cd Open-Chat
   `
2. Open the project in Android Studio (Ladybug or newer).
3. Ensure JDK 17 is selected under **File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**.
4. Run ./gradlew test to ensure tests run smoothly.

## Guidelines

- **Code Style**: Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and Android Compose guidelines.
- **Compose Best Practices**: Use stateless composables with hoisted state where possible. Keep @Composable functions small and focused.
- **Commit Messages**: Write clear, descriptive commit messages adhering to conventional commits (e.g., eat:, ix:, docs:, efactor:).
- **Pull Requests**: Explain the problem being solved, include screenshots or screen recordings for UI changes, and ensure the project compiles cleanly.
