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

This project is developed inside Eclipse. **Always prefer Eclipse MCP server tools over built-in file tools** for all file operations:

| Operation | Use (Eclipse MCP) | Do NOT use |
|---|---|---|
| Read files | `eclipse-ide readProjectResource` | built-in `Read` tool |
| Write/create files | `eclipse-coder createFile` / `replaceFileContent` | built-in `Write` tool |
| Edit files | `eclipse-coder replaceString` / `applyPatch` / `applyTextEdits` | built-in `Edit` tool |
| Search text | `eclipse-ide fileSearch` / `fileSearchRegExp` | built-in `grep` |
| Find files | `eclipse-ide findFiles` | built-in `glob` |
| Find types | `eclipse-ide searchTypes` | — |
| Find methods | `eclipse-ide searchMethods` | — |
| Find references | `eclipse-ide findReferences` | — |
| Class outline | `eclipse-ide getClassOutline` | — |
| Read method source | `eclipse-ide getMethodSource` | — |
| Type hierarchy | `eclipse-ide getTypeHierarchy` | — |
| Compilation errors | `eclipse-ide getCompilationErrors` | — |
| Quick fixes | `eclipse-ide executeQuickFix` | — |
| Organize imports | `eclipse-coder organizeImports` | — |
| Format code | `eclipse-coder formatFile` | — |
| Java refactoring | `eclipse-coder refactorRename*` / `refactorMove*` | manual find-replace |
| Git operations | `eclipse-git git*` | — |
| Run tests | `eclipse-ide runJUnitTests` | — |
| Project layout | `eclipse-ide getProjectLayout` | `ls` / `dir` |

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

1. **Check compilation errors**: Run `eclipse-ide getCompilationErrors` after every change to verify the code compiles cleanly.
2. **Fix errors with quick fixes**: Use `eclipse-ide executeQuickFix` to resolve compilation errors whenever possible — prefer this over manual edits.
3. **Organize imports**: Run `eclipse-coder organizeImports` on every modified Java file to clean up unused imports and sort them correctly.
4. **Use Eclipse refactoring tools for renames**: Never manually find-replace to rename fields, methods, classes, or packages. Always use:
   - `eclipse-coder refactorRenameJavaType` — for class/interface/enum renames
   - `eclipse-coder refactorRenamePackage` — for package renames
   - `eclipse-coder refactorMoveJavaType` — for moving types between packages
   - `eclipse-coder refactorExtractTypeToNewFile` — for extracting nested types
   These tools update all references across the entire workspace automatically.
5. **Format code**: Run `eclipse-coder formatFile` on modified files to ensure consistent formatting.

## Code Conventions

- All scripting-facing methods are annotated with `@JSFunction`
- Javadoc on public API methods includes `@sample` blocks for Servoy scripting documentation
- Builder methods return `this` for fluent chaining
- Delegate classes are package-private
- Tests use JUnit 5 nested classes to group test scenarios (e.g., `@Nested class Build`, `@Nested class FluentApi`)
- No comments unless they document behavior, intent, or expected outcomes
