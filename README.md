# AI Test Generator — IntelliJ IDEA Plugin

[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/31249-ai-test-case-generator.svg)](https://plugins.jetbrains.com/plugin/31249-ai-test-case-generator)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/31249-ai-test-case-generator.svg)](https://plugins.jetbrains.com/plugin/31249-ai-test-case-generator)

An IntelliJ IDEA plugin that automatically generates comprehensive JUnit 5 unit tests for Java methods using the Groq AI API (free, no credit card required).

**[Install from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31249-ai-test-case-generator)**

## What It Does

1. You place your cursor inside any Java method
2. Right-click → **Generate Unit Test with AI**
3. The plugin extracts the method context and sends it to Groq AI
4. A complete test file is generated and written to your `src/test` directory

Generated tests include:
- Happy path tests
- Edge case tests (null inputs, empty collections, boundary values)
- Error case tests
- Mockito mocks for dependencies
- `@DisplayName` annotations with descriptive names
- Arrange-Act-Assert pattern

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java |
| Build System | Gradle (Kotlin DSL) |
| Target IDE | IntelliJ IDEA 2024.1+ |
| AI Backend | Groq API (llama-3.3-70b-versatile) |
| HTTP Client | OkHttp 4.12.0 |
| JSON Parsing | Gson 2.11.0 |
| Testing | JUnit 4 + Mockito 5 + OkHttp MockWebServer |

## Requirements

- IntelliJ IDEA 2024.1 or newer (Community or Ultimate)
- Java 17+
- A free Groq API key (get one at [console.groq.com](https://console.groq.com))

## Installation

### Option 1 — JetBrains Marketplace (recommended)

1. In IntelliJ: Settings → Plugins → Marketplace
2. Search for **"AI Test Case Generator"**
3. Click Install → Restart IDE

Or install directly: [AI Test Case Generator on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31249-ai-test-case-generator)

### Option 2 — Build from source

1. Clone the repository:
   ```bash
   git clone https://github.com/codewithrohan/ai-test-generator.git
   cd ai-test-generator
   ```

2. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   # Windows:
   .\gradlew.bat buildPlugin
   ```

3. Install in IntelliJ:
   - Settings → Plugins → ⚙ → Install Plugin from Disk
   - Select `build/distributions/ai-test-generator-0.1.0.zip`
   - Restart IDE

## Configuration

1. Get a free API key at [console.groq.com](https://console.groq.com) → API Keys → Create
2. In IntelliJ: Settings → Tools → **AI Test Generator**
3. Paste your Groq API key
4. Select model (default: `llama-3.3-70b-versatile`)
5. Click Apply

## Usage

1. Open any Java file with a method
2. Click your cursor inside the method body
3. Right-click → **Generate Unit Test with AI**
4. Wait a few seconds
5. Test file appears in `src/test/java/...`

## Project Structure

```
src/
├── main/java/com/aitestgen/
│   ├── action/          # Right-click menu action
│   ├── client/          # Groq API HTTP client
│   ├── extractor/       # Extracts method context from PSI tree
│   ├── generator/       # Writes generated test files to disk
│   ├── model/           # Data models (MethodContext, GeneratedTestResult)
│   ├── prompt/          # Builds structured prompts for the AI
│   └── settings/        # Plugin settings UI and state
└── test/java/com/aitestgen/
    └── ...              # Unit tests for each component
```

## License

MIT
