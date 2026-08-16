# Spec: SVY-21342b — Migrate OpenAI integration to Responses API

## 1. Goal

Replace the underlying OpenAI Chat Completions implementation (`langchain4j-open-ai`) with the new OpenAI Responses API (`langchain4j-open-ai-official`) for chat models, while keeping the Servoy developer-facing scripting API unchanged. Fall back to Chat Completions when a custom `baseUrl` is set (for OpenAI-compatible providers that don't support the Responses API).

This enables support for GPT-5.6 reasoning models (Sol, Terra, Luna) and their Responses API features (reasoning effort, reasoning summaries, encrypted reasoning for multi-turn tool-calling).

## 2. Background

The Servoy AI Plugin uses LangChain4j as its LLM abstraction layer. Currently, OpenAI chat is powered by:

- **Dependency:** `langchain4j-open-ai` version `1.19.0`
- **Model class:** `OpenAiStreamingChatModel` from `dev.langchain4j.model.openai`
- **Token estimator:** `OpenAiTokenCountEstimator` from `dev.langchain4j.model.openai`

OpenAI's new Responses API (`/v1/responses`) supersedes the Chat Completions API for reasoning-capable models. LangChain4j provides support via:

- **Dependency:** `langchain4j-open-ai-official` version `1.19.0-beta29`
- **Model class:** `OpenAiOfficialResponsesStreamingChatModel` from `dev.langchain4j.model.openaiofficial`

Key benefits of the Responses API:
- ~3% better performance with reasoning models
- Reasoning effort levels (`low`, `medium`, `high`)
- Reasoning summaries in responses
- Encrypted reasoning for multi-turn tool-calling context
- Full tool calling support (same as Chat Completions)

### 2.1. baseUrl compatibility concern

Many Servoy customers use `baseUrl()` to point at OpenAI-compatible providers
(IONOS, vLLM, LiteLLM, Ollama, etc.). These providers implement the Chat
Completions endpoint (`/v1/chat/completions`) but NOT the Responses API
(`/v1/responses`). Calling the Responses API against them would return a 404.

## 3. Design

### 3.1. Dependency changes

- **Keep** `langchain4j-open-ai` — still required by `OpenAiEmbeddingModelBuilder` AND as
  fallback Chat Completions implementation for custom `baseUrl` users.
- **Add** `langchain4j-open-ai-official` version `1.19.0-beta29` as a new dependency.

### 3.2. Dual-mode strategy (Responses API vs Chat Completions)

The decision of which API to use is made **at `build()` time** based on a simple
heuristic:

| Condition | Model used |
|-----------|-----------|
| No `baseUrl` set (default OpenAI endpoint) | `OpenAiOfficialResponsesStreamingChatModel` (Responses API) |
| `baseUrl` starts with `https://api.openai.com` | `OpenAiOfficialResponsesStreamingChatModel` (Responses API) |
| Any other custom `baseUrl` | `OpenAiStreamingChatModel` (Chat Completions, old impl) |
| `useResponsesApi(true)` explicitly set | `OpenAiOfficialResponsesStreamingChatModel` regardless of baseUrl |

**Why not "try first, fallback on failure":**
- The model is chosen at `build()` time, not chat time — it gets wired into `AiServices`
- Streaming errors arrive asynchronously — cannot catch and retry transparently
- Hard to distinguish "endpoint doesn't support Responses API" (404) from real errors (bad key, rate limit)
- Adds first-call latency penalty for all custom endpoint users

### 3.3. OpenAiChatBuilder changes

**Field changes:**
- Remove the eagerly-constructed `OpenAiStreamingChatModel.Builder builder` field
- Add `private String baseUrl` field
- Add `private String apiKey` field (store locally, apply at build time)
- Add `private Double temperature` field (store locally, apply at build time)
- Add `private String reasoningEffort` field
- Add `private Boolean useResponsesApi` field (explicit opt-in override)

**Existing builder methods (unchanged Servoy API):**
- `apiKey(String)` — stores value locally
- `modelName(String)` — stores value locally (already exists)
- `temperature(Double)` — stores value locally
- `baseUrl(String)` — stores value locally

**New optional methods:**
- `reasoningEffort(String)` — accepts `"low"`, `"medium"`, `"high"`; only effective
  when Responses API is used; silently ignored for Chat Completions fallback.
- `useResponsesApi(Boolean)` — explicit override to force Responses API even with a
  custom baseUrl (for providers that do support it).

**`build()` method — dual-mode logic:**
```java
@Override
@JSFunction
public ChatClient build() {
    Pair<AiServices<Assistant>, List<?>> pair = createAssistantBuilder();
    AiServices<Assistant> assistantBuilder = pair.getLeft();

    if (shouldUseResponsesApi()) {
        var modelBuilder = OpenAiOfficialResponsesStreamingChatModel.builder()
            .apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) modelBuilder.baseUrl(baseUrl);
        if (temperature != null) modelBuilder.temperature(temperature);
        if (reasoningEffort != null)
            modelBuilder.reasoningEffort(ReasoningEffort.of(reasoningEffort));
        assistantBuilder.streamingChatModel(modelBuilder.build());
    } else {
        var modelBuilder = OpenAiStreamingChatModel.builder()
            .apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) modelBuilder.baseUrl(baseUrl);
        if (temperature != null) modelBuilder.temperature(temperature);
        assistantBuilder.streamingChatModel(modelBuilder.build());
    }

    if (tokens != null) {
        OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator(modelName);
        assistantBuilder.chatMemory(TokenWindowChatMemory.builder()
            .maxTokens(tokens, estimator).build());
    }

    return new ChatClient(assistantBuilder.build(), access, pair.getRight());
}

private boolean shouldUseResponsesApi() {
    if (Boolean.TRUE.equals(useResponsesApi)) return true;
    if (Boolean.FALSE.equals(useResponsesApi)) return false;
    return baseUrl == null || baseUrl.startsWith("https://api.openai.com");
}
```

### 3.4. AIProvider.createOpenAIClient() changes

This convenience method has no `baseUrl` parameter — it always targets OpenAI
directly. Therefore it should always use the Responses API:

```java
OpenAiOfficialResponsesStreamingChatModel model =
    OpenAiOfficialResponsesStreamingChatModel.builder()
        .apiKey(apiKey).modelName(modelName).build();
```

### 3.5. Token counting

The `OpenAiTokenCountEstimator` from `langchain4j-open-ai` remains on the classpath
(needed for embeddings and for Chat Completions fallback). It works with both
model paths since token counting is model-name-based, not endpoint-based.

### 3.6. Default model name

Keep the current default model name unchanged — the Responses API accepts the same
model identifiers as Chat Completions.

## 4. Implementation plan

### Step 1: Add dependency to pom.xml

Add to `pom.xml`:
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-official</artifactId>
    <version>1.19.0-beta29</version>
</dependency>
```

Keep the existing `langchain4j-open-ai` dependency (needed for embeddings + fallback).

### Step 2: Refactor OpenAiChatBuilder.java

1. Remove the eagerly-constructed `OpenAiStreamingChatModel.Builder builder` field.
2. Add local fields: `apiKey`, `baseUrl`, `temperature`, `reasoningEffort`, `useResponsesApi`.
3. Update `apiKey()`, `baseUrl()`, `temperature()` to store values locally instead of
   forwarding to a builder immediately.
4. Add `reasoningEffort(String)` fluent setter.
5. Add `useResponsesApi(Boolean)` fluent setter.
6. Rewrite `build()` with dual-mode logic (see §3.3).
7. Add `shouldUseResponsesApi()` private method.

### Step 3: Update AIProvider.java

1. Add import for `OpenAiOfficialResponsesStreamingChatModel`.
2. Update `createOpenAIClient()` method to use the new model builder.

### Step 4: Verify compilation

Run `mvnw compile` to ensure everything resolves correctly.

## 5. Acceptance criteria

- [ ] `plugins.ai.createOpenAiChatBuilder().apiKey(k).modelName(m).build()` uses Responses API (no baseUrl set).
- [ ] `plugins.ai.createOpenAiChatBuilder().apiKey(k).modelName(m).baseUrl("https://custom.endpoint.com/v1").build()` uses Chat Completions (fallback).
- [ ] `plugins.ai.createOpenAiChatBuilder().apiKey(k).modelName(m).baseUrl("https://api.openai.com/v1").build()` uses Responses API.
- [ ] `plugins.ai.createOpenAiChatBuilder().apiKey(k).modelName(m).baseUrl("https://custom.endpoint.com/v1").useResponsesApi(true).build()` forces Responses API.
- [ ] Existing scripting API (`apiKey()`, `modelName()`, `temperature()`, `baseUrl()`, `build()`) works identically from the Servoy developer's perspective.
- [ ] `plugins.ai.createOpenAIClient(apiKey, modelName)` uses the Responses API.
- [ ] New `reasoningEffort(String)` method is available on `OpenAiChatBuilder`.
- [ ] Tool calling (custom tools + MCP) continues to work in both modes.
- [ ] Chat memory with token window continues to work in both modes.
- [ ] `addSystemMessage(String)` continues to work.
- [ ] `useBuiltInTools(true)` continues to work.
- [ ] OpenAI embedding model builder is unaffected.
- [ ] No changes to Gemini or Anthropic builders.
- [ ] Plugin compiles and bundles cleanly.

## 6. Out of scope

- Migrating Gemini or Anthropic integrations.
- Migrating the OpenAI embedding model to the official SDK.
- Exposing reasoning summaries to Servoy scripting (future enhancement).
- Exposing encrypted reasoning details to Servoy scripting.
- Streaming token-by-token callbacks to Servoy (already handled by `AiServices`).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does `langchain4j-open-ai-official` provide a `TokenCountEstimator`? If not, reusing `OpenAiTokenCountEstimator` from the old module is acceptable. | Dev | open |
| The `1.19.0-beta29` version is pre-release. Should we pin to this or wait for stable? | Product | open |
| Should `reasoningEffort(String)` validate input values at build time or let the API reject invalid values? | Dev | open |
| Should `useResponsesApi()` be exposed to Servoy developers in the scripting API, or kept as an internal mechanism? | Product | open |
