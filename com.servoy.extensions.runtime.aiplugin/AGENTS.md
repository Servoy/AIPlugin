# Servoy AI Plugin — AGENTS.md

## Project Overview

This is a **Servoy runtime plugin** (`plugins.ai`) that exposes AI capabilities to Servoy Solutions. It provides a scripting API for chat completions, embeddings, vector stores, and tool use (including MCP) — backed by multiple LLM providers.

- **Package**: `com.servoy.extensions.runtime.aiplugin`
- **Packaging**: OSGi bundle (Apache Felix `maven-bundle-plugin`)
- **Java**: 21
- **Build**: Maven (`mvn package` produces the bundle JAR plus provider-specific ZIP assemblies)
- **Test framework**: JUnit 5 + Mockito
- **Primary dependency**: [LangChain4j](https://github.com/langchain4j/langchain4j) `1.19.0`
- **Servoy SDK**: `2026.3.x`

## Supported AI Providers

| Provider | Chat Builder | Chat Delegate | LangChain4j Module |
|-----------|---------------------|----------------------|-------------------------------|
| OpenAI | `OpenAiChatBuilder` | `OpenAiChatDelegate` | `langchain4j-open-ai` / `langchain4j-open-ai-official` |
| Anthropic | `AnthropicChatBuilder` | `AnthropicChatDelegate` | `langchain4j-anthropic` |
| Google Gemini | `GeminiChatBuilder` | `GeminiChatDelegate` | `langchain4j-google-ai-gemini` |
| AWS Bedrock | `BedrockChatBuilder` | `BedrockChatDelegate` | `langchain4j-bedrock` |

## Architecture

### Package Structure

```
com.servoy.extensions.aiplugin
├── AIPlugin.java              — Servoy IClientPlugin entry point
├── AIProvider.java            — Scripting API exposed as plugins.ai (IScriptObject)
├── AiPluginService.java       — Interface for server-side service
├── ProviderLoader.java        — Dynamic provider loading
├── chat/
│   ├── Assistant.java         — LangChain4j AiService interface
│   ├── BaseChatBuilder.java   — Abstract builder with shared config (tools, MCP, memory)
│   ├── *ChatBuilder.java      — Provider-specific builders (fluent API, @JSFunction)
│   ├── *ChatDelegate.java     — Provider-specific model wiring (internal)
│   ├── ChatClient.java        — Wrapper returned to Servoy scripting
│   ├── ChatResponse.java      — Response wrapper for scripting
│   ├── MCPClientBuilder.java  — MCP tool integration builder
│   └── ToolBuilder.java       — Custom tool definition builder
├── embedding/
│   ├── EmbeddingModel.java            — Scripting API for embeddings
│   ├── EmbeddingStore.java            — Scripting API for vector store operations
│   ├── ServoyEmbeddingStore*.java     — Servoy-integrated vector store (PostgreSQL/pgvector)
│   ├── *EmbeddingModelBuilder.java    — Provider-specific embedding model builders
│   ├── *EmbeddingModelDelegate.java   — Provider-specific embedding wiring
│   ├── SearchResult.java              — Embedding search result wrapper
│   ├── MetaDataKey.java               — Metadata column definitions
│   ├── EmbeddingMetaDataColumnAdder.java — Dynamic metadata column support
│   └── pdf/ApachePdfBoxDocumentParser.java — PDF document parsing
├── database/
│   ├── DatabaseHandler.java           — Database operations + DatabasedProduct enum
│   └── postgres/                      — PostgreSQL-specific (pgvector, indexes)
├── server/
│   ├── AiServerPlugin.java            — Servoy IServerPlugin entry point
│   ├── AiPluginServiceImpl.java       — Server-side service implementation
│   ├── ServoyEmbeddingStoreServerImpl.java — Server-side embedding store
│   └── TableModel.java                — Table structure definitions
└── tools/
    └── builtin/ServoyBuiltInTools.java — Built-in Servoy tools for AI agents
```

### Design Pattern

The plugin follows a **Builder → Delegate → Client** pattern per provider:
1. **Builder** (`*ChatBuilder`): Fluent API exposed to Servoy scripting via `@JSFunction` annotations
2. **Delegate** (`*ChatDelegate`): Internal class that wires the LangChain4j model with the configured options
3. **Client** (`ChatClient`): The runtime wrapper returned to the Servoy solution for `chat()` / `chatSync()` calls

All builders extend `BaseChatBuilder` which handles shared concerns: tools, MCP clients, memory tokens, and the `AiServices` setup.

## Build & Test

```bash
# Build (produces bundle JAR + provider ZIP assemblies in target/)
mvn package

# Run tests
mvn test

# Clean build
mvn clean package
```

The build produces:
- Main bundle JAR (OSGi)
- `zip-core` — core dependencies
- `zip-provider-openai` / `zip-provider-anthropic` / `zip-provider-gemini` / `zip-provider-bedrock` — per-provider dependency ZIPs

## Tooling Instructions

### Eclipse MCP Server Tools — PREFERRED

This project is developed inside Eclipse. **Always prefer Eclipse MCP server tools over built-in file tools** for all file operations.

**⚠️ Everything runs through Code Mode.** The Eclipse MCP servers (`eclipse-coder`, `eclipse-ide`, `eclipse-git`, `eclipse-pde`, `eclipse-runner`, `eclipse-context`) and the other MCP tools (`memory`, `time`, `codebase-memory-mcp`) are exposed **only through Code Mode** — there is no direct top-level tool. Call each from inside the **`execute`** tool using bracket notation, e.g. `await tools["eclipse-coder"].replaceString({ ... })` or `await tools["eclipse-ide"].getCompilationErrors({ ... })`. **NEVER** call them as plain tools (`eclipse-coder_replaceString`, `eclipse-ide_getCompilationErrors`, `tools.eclipse_ide...`) — those names do not exist in Code Mode and the call fails with *"No tool named ... is currently available."* When that happens, **do not fall back to the built-in `edit`/`write`** — fix the call by wrapping it in `execute` with the bracket form. If a tool is not shown, find it with `search(...)` inside an `execute` script (synchronous — no `await`). The only tools called directly are the built-in `read`, `grep`, `glob`, and `shell`.

**Every file inside the Eclipse project MUST be edited through `eclipse-coder`**, never the built-in `edit`/`write` (they write behind Eclipse's back and desync the editor, JDT model and undo history). In the table below, each "Use" entry is shorthand for the bracketed Code Mode call, e.g. `tools["eclipse-ide"].readProjectResource(...)`.

| Operation | Use (via `execute`) | Do NOT use |
|---|---|---|
| Read files | `tools["eclipse-ide"].readProjectResource` | built-in `read` tool |
| Write/create files | `tools["eclipse-coder"].createFile` / `replaceFileContent` | built-in `write` tool |
| Edit files | `tools["eclipse-coder"].replaceString` / `applyPatch` / `applyTextEdits` | built-in `edit` tool |
| Search text | `tools["eclipse-ide"].fileSearch` / `fileSearchRegExp` | built-in `grep` |
| Find files | `tools["eclipse-ide"].findFiles` | built-in `glob` |
| Find types | `tools["eclipse-ide"].searchTypes` | — |
| Find methods | `tools["eclipse-ide"].searchMethods` | — |
| Find references | `tools["eclipse-ide"].findReferences` | — |
| Class outline | `tools["eclipse-ide"].getClassOutline` | — |
| Read method source | `tools["eclipse-ide"].getMethodSource` | — |
| Type hierarchy | `tools["eclipse-ide"].getTypeHierarchy` | — |
| Compilation errors | `tools["eclipse-ide"].getCompilationErrors` | — |
| Quick fixes | `tools["eclipse-ide"].executeQuickFix` | — |
| Organize imports | `tools["eclipse-coder"].organizeImports` | — |
| Format code | `tools["eclipse-coder"].formatFile` | — |
| Java refactoring | `tools["eclipse-coder"].refactorRename*` / `refactorMove*` | manual find-replace |
| Git operations | `tools["eclipse-git"].git*` | — |
| Run tests | `tools["eclipse-ide"].runJUnitTests` | — |
| Project layout | `tools["eclipse-ide"].getProjectLayout` | `ls` / `dir` |

### Exceptions — Bash is fine for:

- **Maven commands**: `mvn package`, `mvn test`, `mvn clean`, `mvn versions:display-dependency-updates`, etc.
- **Git operations** not covered by Eclipse MCP (e.g., complex log queries, rebasing)
- **System commands**: checking environment, running scripts

### Codebase Knowledge Graph

The project is indexed in `codebase-memory-mcp`. Use graph tools for structural code discovery before falling back to text search. See the global `AGENTS.md` for full usage instructions.

### Eclipse Project Name

The Eclipse project name is: `com.servoy.extensions.runtime.aiplugin`

Use this as the `projectName` parameter for all Eclipse MCP tool calls.

## After Every Code Change

1. **Check compilation errors**: Run `tools["eclipse-ide"].getCompilationErrors` (via `execute`) after every change to verify the code compiles cleanly.
2. **Fix errors with quick fixes**: Use `tools["eclipse-ide"].executeQuickFix` to resolve compilation errors whenever possible — prefer this over manual edits.
3. **Organize imports**: Run `tools["eclipse-coder"].organizeImports` on every modified Java file to clean up unused imports and sort them correctly.
4. **Use Eclipse refactoring tools for renames**: Never manually find-replace to rename fields, methods, classes, or packages. Always use:
   - `tools["eclipse-coder"].refactorRenameJavaType` — for class/interface/enum renames
   - `tools["eclipse-coder"].refactorRenamePackage` — for package renames
   - `tools["eclipse-coder"].refactorMoveJavaType` — for moving types between packages
   - `tools["eclipse-coder"].refactorExtractTypeToNewFile` — for extracting nested types
   These tools update all references across the entire workspace automatically.
5. **Format code**: Run `tools["eclipse-coder"].formatFile` on modified files to ensure consistent formatting.

## Code Conventions

- All scripting-facing methods are annotated with `@JSFunction`
- Javadoc on public API methods includes `@sample` blocks for Servoy scripting documentation
- Builder methods return `this` for fluent chaining
- Delegate classes are package-private
- Tests use JUnit 5 nested classes to group test scenarios (e.g., `@Nested class Build`, `@Nested class FluentApi`)
- No comments unless they document behavior, intent, or expected outcomes
