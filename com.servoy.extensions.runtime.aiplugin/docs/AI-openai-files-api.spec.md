# Spec: AI Plugin — OpenAI Files API for `addFile()` (upload instead of inline base64)

> No Jira key yet. Filename uses the summary slug; rename to `docs/<KEY>-openai-files-api.spec.md` once a ticket exists.
> This is **part 2 of 2**. It depends on the shared `FileStore` infrastructure introduced in `AI-gemini-files-api.spec.md` and should be scheduled **after** that spec ships.
> **Status: research / feasibility.** Unlike the Gemini spec, this cannot be implemented purely through `AiServices`; it requires a provider-specific request path. Scope and effort are to be confirmed by the spike in §4.

## 1. Goal

Extend the file-store capability (introduced for Gemini) to **OpenAI**, so that a file added via `ChatClient.addFile(...)` / `addBytes(...)` is uploaded once to the OpenAI Files API (`/v1/files`) and referenced by `file_id` in the chat request, instead of being inlined as base64. Where this is not possible, OpenAI retains the current inline-base64 behaviour. The Servoy-facing API stays unchanged.

## 2. Background

### 2.1. Dependency on the Gemini spec

The shared `FileStore` interface, the nullable `FileStore` field on `ChatClient`, the upload-vs-inline decision in `getUserMessage()`, and the delegate threading are all introduced by `AI-gemini-files-api.spec.md`. This spec only adds an OpenAI implementation of that capability — **but** with the important difference that OpenAI's file reference cannot be expressed as a plain `Content` (§2.3), so more than a `FileStore` implementation is required.

### 2.2. Which OpenAI module is active

The OpenAI delegate wires one of two modules depending on `shouldUseResponsesApi()` (`OpenAiChatBuilder.java:150`):

```java
private boolean shouldUseResponsesApi() {
    if (Boolean.TRUE.equals(useResponsesApi)) return true;
    if (Boolean.FALSE.equals(useResponsesApi)) return false;
    return baseUrl == null || baseUrl.startsWith("https://api.openai.com");
}
```

- `true` → `OpenAiOfficialResponsesStreamingChatModel` (official `com.openai` SDK, Responses API) — the only path that can reach the OpenAI Files API.
- `false` → `OpenAiStreamingChatModel` (custom `langchain4j-open-ai`, Chat Completions fallback) — used for OpenAI-compatible third-party endpoints; **no Files API** and out of scope.

The Files-capable path (official/Responses) is already the default for real OpenAI endpoints, so an OpenAI file store is only relevant when `shouldUseResponsesApi()` is true.

### 2.3. OpenAI file reference does NOT flow through `AiServices` (verified against LC4j 1.19.0 sources)

The underlying official SDK `com.openai:openai-java` (v4.9.0, bundled) **does** provide both pieces:
- Files service: `FileCreateParams` (upload → `file_id`).
- `com.openai.models.responses.ResponseInputFile.Builder` has `fileId(String)`, `fileData(String)`, `fileUrl(String)`, `filename(String)`.

**But** LangChain4j's mapper `OpenAiOfficialResponsesStreamingChatModel` (line ~598) only maps `PdfFileContent` to:
```java
if (pdfFile.url() != null)  pdfInput.fileUrl(url);
else if (pdfFile.base64Data() != null) { pdfInput.filename("document.pdf"); pdfInput.fileData("data:...;base64,..."); }
else throw;
```
It **never** calls `fileId(...)`. The Chat Completions mapper (`InternalOpenAiOfficialHelper.toOpenAiContent`) likewise only emits `file_data` base64. So there is **no way to pass an OpenAI `file_id` through the generic `Content` / `AiServices` path**. Using OpenAI's Files API requires a provider-specific request path that calls the `com.openai` SDK directly, bypassing `AiServices` for file turns.

## 3. Options

Three ways to close the gap, in rough order of increasing invasiveness:

### 3.1. Option A — Upstream contribution to LangChain4j (lowest local complexity)

Add a `file_id` reference to LangChain4j's `Content` model (or teach the OpenAI-official mappers to read a `file_id` carried on `PdfFileContent`/`ImageContent`) and get it merged upstream, then consume the new version.

- **Pro:** keeps the plugin on the clean `AiServices` path; benefits the whole ecosystem; smallest code footprint in this repo.
- **Con:** depends on upstream acceptance and release cadence; out of our control; timing uncertain.

### 3.2. Option B — Native `com.openai` request path for file turns (self-contained)

When the OpenAI file store is active AND the current chat turn has files, bypass `AiServices` and call the `com.openai` SDK directly:
1. `client.files().create(FileCreateParams ... purpose=user_data)` → `file_id`.
2. Build a Responses request with `ResponseInputFile.builder().fileId(file_id)` + the text.
3. Handle streaming, tool calls, and chat-memory integration ourselves for that turn.

- **Pro:** fully under our control; no upstream dependency.
- **Con:** substantial — re-implements the parts of `AiServices` we currently get for free (streaming callbacks, tool-execution loop, memory). Risk of behavioural drift between the file path and the normal path.

### 3.3. Option C — Do nothing (keep inline base64 for OpenAI)

Accept inline base64 for OpenAI; rely on the Gemini path where payload size matters most.

- **Pro:** zero cost.
- **Con:** no OpenAI payload/tokenization benefit.

**Recommendation:** spike Option A first (check for an existing upstream issue/PR and whether a small mapper change is accepted). Fall back to Option B only if there is a strong customer need and Option A stalls. Option C is the interim state until then.

## 4. Spike (do this before committing to an option)

1. Search LangChain4j issues/PRs for existing `file_id` support work on the OpenAI-official module; gauge willingness/direction.
2. Prototype Option B minimally: upload a file via the `com.openai` client, issue one Responses request with `ResponseInputFile.fileId(...)`, confirm the model reads the file. Measure how much of the `AiServices` behaviour (streaming, tools, memory) must be re-created.
3. From (1) and (2), pick A or B (or defer to C) and write the concrete implementation plan.

## 5. Implementation plan (provisional — finalized after §4)

If **Option A** (upstream lands):
1. Bump the relevant `langchain4j-open-ai-official` version.
2. Add an `OpenAiFileStore` implementing the shared `FileStore` (from the Gemini spec): upload via the SDK, return a `Content` carrying the `file_id` in whatever shape upstream introduces.
3. Wire it in `OpenAiChatDelegate.build(...)` only when `shouldUseResponsesApi()` is true.

If **Option B** (native path):
1. Add an `OpenAiFileStore` that uploads and yields a `file_id` reference object (not a generic `Content`).
2. Add a provider-specific request path in the OpenAI delegate/client used only for turns that carry files, preserving streaming + tools + memory parity.
3. Wire it only when `shouldUseResponsesApi()` is true; custom-`baseUrl` fallback keeps inline base64.

Common to both: verify with `mvn compile` + `getCompilationErrors`; organize imports and format per AGENTS.md.

## 6. Acceptance criteria

- [ ] With OpenAI on the official/Responses path, a supported file added via `addFile()` is uploaded to the OpenAI Files API and referenced by `file_id` (verified via request inspection), not inlined as base64.
- [ ] With OpenAI on the custom-`baseUrl` Chat Completions fallback, files are still sent inline (base64) — unchanged.
- [ ] `text/*` content is always inlined.
- [ ] Upload failures fall back to inline base64 with a logged warning; chat still succeeds.
- [ ] Streaming, tool calling, MCP, chat memory, and system messages behave identically to the non-file path.
- [ ] The shared `FileStore` abstraction from the Gemini spec is reused (no divergent second abstraction).
- [ ] Plugin compiles and bundles cleanly.

## 7. Out of scope

- Gemini file store (delivered in `AI-gemini-files-api.spec.md`).
- Anthropic and Bedrock file stores.
- Migrating the custom `langchain4j-open-ai` fallback to support a file store.
- A configurable upload size threshold and opt-in/opt-out builder flag.

## 8. Open questions

| Question | Owner | Status |
|---|---|---|
| Is there an existing/planned upstream LangChain4j path to pass a `file_id` through the OpenAI-official mappers (Option A)? | Dev | open (spike) |
| If Option B: how much of the `AiServices` streaming/tool/memory behaviour must be re-implemented for file turns, and is the parity risk acceptable? | Dev | open (spike) |
| `purpose` value for `FileCreateParams` (`user_data` vs `assistants`) for Responses API file inputs? | Dev | open (spike) |
| Cleanup policy for uploaded OpenAI files (they persist until deleted, unlike Gemini's 48h retention). | Product | open |
| Which content types does OpenAI accept as `input_file` beyond PDF (images already have their own path)? | Dev | open |
