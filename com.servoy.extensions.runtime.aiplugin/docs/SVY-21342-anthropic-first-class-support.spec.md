# Spec: SVY-21342 — AI Plugin: Add 1st-class support for Anthropic

## 1. Goal

Add first-class Anthropic (Claude) support to the Servoy AI Plugin, providing
the same builder-pattern API surface that already exists for OpenAI and Gemini.
This gives Servoy developers a native, consistent way to use Anthropic models
for chat without switching to a different SDK or using the OpenAI-compatible
fallback.

## 2. Background

### 2.1 Existing provider pattern

The AI Plugin exposes providers through `AIProvider` (the `plugins.ai` scripting
object). Each provider has:

- A **chat builder** class extending `BaseChatBuilder<T>` (e.g.
  `OpenAiChatBuilder`, `GeminiChatBuilder`) with fluent setters for `apiKey`,
  `modelName`, `temperature`, and a `build()` method returning `ChatClient`.
- A factory method on `AIProvider` (e.g. `createOpenAiChatBuilder()`).
- A convenience **client** factory method (e.g. `createOpenAIClient(apiKey, modelName)`)
  that creates a minimal ChatClient in one call.
- Optionally, an **embedding model builder** for providers that offer embedding
  endpoints.

All chat builders inherit tool support, MCP client support, system messages, and
token-windowed memory from `BaseChatBuilder`.

### 2.2 Langchain4j Anthropic integration

The `langchain4j-anthropic` module (same version line as the rest: 1.18.0)
provides:

- `AnthropicStreamingChatModel` — streaming chat model used with `AiServices`.
- `AnthropicTokenCountEstimator` — token counter usable with
  `TokenWindowChatMemory`.
- Tool calling support in both streaming and non-streaming modes.

Anthropic does **not** offer an embedding model API — there is no
langchain4j-anthropic embedding model class.

### 2.3 Default model

The current default model for Anthropic should be `claude-sonnet-5`
(Claude Sonnet 5), matching the most capable generally-available model at the
time of implementation.

## 3. Design

### 3.1 AnthropicChatBuilder

A new class `com.servoy.extensions.aiplugin.chat.AnthropicChatBuilder` that
follows the exact same pattern as `GeminiChatBuilder`:

```java
@ServoyDocumented
public class AnthropicChatBuilder extends BaseChatBuilder<AnthropicChatBuilder> implements IJavaScriptType
```

**Fields:**
- `private String apiKey`
- `private String modelName = "claude-sonnet-5"`
- `private Double temperature`

**Methods (all `@JSFunction`, fluent):**
- `apiKey(String key)` — sets the Anthropic API key
- `modelName(String modelName)` — sets the model name
- `temperature(Double temperature)` — sets the temperature
- `build()` — builds an `AnthropicStreamingChatModel`, wires it into
  `AiServices`, attaches `TokenWindowChatMemory` (using
  `AnthropicTokenCountEstimator`) when `tokens != null`, returns `ChatClient`

### 3.2 AIProvider factory methods

Two new `@JSFunction` methods on `AIProvider`:

- `createAnthropicChatBuilder()` — returns `new AnthropicChatBuilder(access)`
- `createAnthropicClient(String apiKey, String modelName)` — convenience method
  that creates an `AnthropicStreamingChatModel` and returns a `ChatClient`
  directly (mirrors `createGeminiClient` / `createOpenAIClient`)

### 3.3 Type registration

Add `AnthropicChatBuilder.class` to the array returned by
`AIProvider.getAllReturnedTypes()`.

### 3.4 Maven dependencies

**Update existing dependencies** to the latest langchain4j release (1.19.0):

- `langchain4j.version` property: `1.18.0` → `1.19.0`
- `langchain4j-mcp` version: `1.18.0-beta28` → `1.19.0-beta29`

**Add new dependency** to `pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-anthropic</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

### 3.5 Embedding model (not applicable)

Anthropic does not provide an embedding model API. The ticket mentions
`createAnthropicEmbeddingModelBuilder` and `AnthropicEmbeddingModelBuilder`, but
these cannot be implemented because there is no underlying Anthropic embedding
endpoint. See Open Questions.

## 4. Implementation plan

1. Update `langchain4j.version` property in `pom.xml` from `1.18.0` to `1.19.0`.

2. Update `langchain4j-mcp` version in `pom.xml` from `1.18.0-beta28` to
   `1.19.0-beta29`.

3. Add `langchain4j-anthropic` dependency to `pom.xml` with version
   `${langchain4j.version}`.

4. Create `src/main/java/com/servoy/extensions/aiplugin/chat/AnthropicChatBuilder.java`:
   - Extend `BaseChatBuilder<AnthropicChatBuilder>`, implement `IJavaScriptType`
   - Add `@ServoyDocumented` annotation
   - Implement `apiKey()`, `modelName()`, `temperature()` fluent setters
   - Implement `build()` using `AnthropicStreamingChatModel.builder()` and
     `AnthropicTokenCountEstimator` for memory

5. Add `createAnthropicChatBuilder()` factory method to `AIProvider`.

6. Add `createAnthropicClient(String apiKey, String modelName)` convenience
   method to `AIProvider`.

7. Add `AnthropicChatBuilder.class` to `getAllReturnedTypes()` in `AIProvider`.

8. Verify the build compiles: `mvnw compile`.

## 5. Acceptance criteria

- [ ] `plugins.ai.createAnthropicChatBuilder()` returns an `AnthropicChatBuilder`
- [ ] `AnthropicChatBuilder` supports `.apiKey()`, `.modelName()`, `.temperature()` fluent setters
- [ ] `AnthropicChatBuilder` inherits `.useBuiltInTools()`, `.maxMemoryTokens()`, `.createTool()`, `.createMCPClient()`, `.addSystemMessage()` from `BaseChatBuilder`
- [ ] `.build()` returns a functional `ChatClient` that can chat with Anthropic Claude
- [ ] Token-windowed memory works when `.maxMemoryTokens()` is set (using `AnthropicTokenCountEstimator`)
- [ ] `plugins.ai.createAnthropicClient(apiKey, modelName)` returns a working `ChatClient`
- [ ] Tool calling works through the inherited `BaseChatBuilder` mechanisms
- [ ] Project compiles cleanly with `mvn compile`

## 6. Out of scope

- `AnthropicEmbeddingModelBuilder` — Anthropic does not offer embedding models
- Extended thinking / thinking budget configuration (can be added later)
- Prompt caching configuration (can be added later)
- Batch API support

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| The ticket requests `createAnthropicEmbeddingModelBuilder` and `AnthropicEmbeddingModelBuilder`, but Anthropic has no embedding API. Should these be dropped, or should a stub/error be provided? | Product | open |
| Should `maxTokens` be exposed as a builder method? Anthropic requires it (defaults vary by model). OpenAI/Gemini builders don't expose it currently. | Product | open |
