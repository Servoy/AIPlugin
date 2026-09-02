# Spec: AI Plugin — OpenAI Files API for `addFile()` (upload instead of inline base64)

> No Jira key yet. Filename uses the summary slug; rename to `docs/<KEY>-openai-files-api.spec.md` once a ticket exists.
> This is **part 2 of 2**. It depends on the shared `FileStore` infrastructure introduced in `AI-gemini-files-api.spec.md` and should be scheduled **after** that spec ships.
> **Status: research complete.** The core feasibility questions are resolved (§2.3, §3). A recommended option (D — non-streaming decorator) is selected; a small end-to-end spike (§4) remains before implementation.

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

### 2.3. OpenAI file reference does NOT flow through `AiServices` (verified against LC4j 1.19.0-beta29 + com.openai 4.9.0 sources)

The underlying official SDK `com.openai:openai-java` (v4.9.0, bundled) **does** provide every piece we need, all public:
- `OpenAIClient.files()` → `FileService.create(FileCreateParams)` → `FileObject` (with `.id()`). `FileCreateParams.Builder` accepts `file(byte[])` / `file(InputStream)` / `file(Path)` and `purpose(FilePurpose.USER_DATA)`.
- `OpenAIClient.responses().createStreaming(ResponseCreateParams)` → `StreamResponse<ResponseStreamEvent>` (and a blocking `create(...)` → `Response`).
- `com.openai.models.responses.ResponseInputFile.Builder` has `fileId(String)`, `fileData(String)`, `fileUrl(String)`, `filename(String)`.

**But** LangChain4j's mapper `OpenAiOfficialResponsesStreamingChatModel.createUserMessage` (a `private static` method, line ~583) only maps `PdfFileContent` to:
```java
if (pdfFile.url() != null)  pdfInput.fileUrl(url);
else if (pdfFile.base64Data() != null) { pdfInput.filename("document.pdf"); pdfInput.fileData("data:...;base64,..."); }
else throw;
```
It **never** calls `fileId(...)`. The Chat Completions mapper (`InternalOpenAiOfficialHelper.toOpenAiContent`) likewise only emits `file_data` base64. So there is **no way to pass an OpenAI `file_id` through the generic `Content` / `AiServices` path** without changing LC4j's mapping. Using OpenAI's Files API requires a provider-specific request path (or a modified mapper) that calls the `com.openai` SDK with `ResponseInputFile.fileId(...)`.

### 2.4. Extension-surface findings (why "just subclass their model" does not work)

`OpenAiOfficialResponsesStreamingChatModel` is effectively sealed for our purpose:
- `private` constructor (only reachable via its `Builder`); `client` / `executorService` / `defaultRequestParameters` are `private final`.
- The whole request-building and content-mapping pipeline is `private static`: `createUserMessage`, `toResponseInputItems`, `buildRequestParams` (~95 lines), plus the streaming event pump `ResponsesEventHandler` (~270 lines, an inner class).
- Only `doChat`, `defaultRequestParameters`, `provider`, `supportedCapabilities` are overridable (it `implements StreamingChatModel`).

There is therefore **no narrow override hook** to change only the PDF/file mapping. Anything short of a copy/fork must re-implement the request build + streaming pump for file turns. This is the cost driver behind the options below.

### 2.5. Integration points confirmed available (all public)

- `StreamingChatModel.doChat(ChatRequest, StreamingChatResponseHandler)` is a `default` method on the interface — we can implement `StreamingChatModel` ourselves without touching the sealed class.
- `ChatRequest.messages()` / `.parameters()` / `.toBuilder()` are public — a decorator can inspect and rewrite a request.
- The plugin wires the model via `AiServices.builder(...).streamingChatModel(model)` (see `BaseChatBuilder.createAssistantBuilder`), so any `StreamingChatModel` slots in. The `Assistant` interface only exposes `TokenStream chat(UserMessage)`.
- `client.files().create(...)` and `client.responses().create/createStreaming(...)` are public on `OpenAIClient`.

## 3. Options

Five ways to close the gap. All reuse the shared `FileStore` abstraction from the Gemini spec; they differ in how the `file_id` reaches the wire.

### 3.1. Option A — Upstream contribution to LangChain4j

Add `file_id` support to LC4j's `Content` model (or teach the OpenAI-official mappers to read a `file_id` carried on `PdfFileContent`) and get it merged upstream, then consume the new release.

- **Pro:** cleanest; stays on the `AiServices` path; smallest local footprint; benefits everyone.
- **Con:** depends on upstream acceptance + release cadence; blocked until merged; out of our control.

### 3.2. Option B — Full streaming decorator (native `com.openai` path, streaming)

`OpenAiFilesStreamingChatModel implements StreamingChatModel` wraps the real `OpenAiOfficialResponsesStreamingChatModel`. In `doChat`:
- **No file content** → delegate 1:1 to the wrapped model (zero behavioural risk).
- **File content present** → upload via `client.files().create(...)`, build `ResponseCreateParams` with `ResponseInputFile.fileId(...)`, call `client.responses().createStreaming(...)`, and pump `ResponseStreamEvent`s into the `StreamingChatResponseHandler` ourselves.

- **Pro:** full control; full token streaming; only file turns take the custom path.
- **Con:** must re-implement `buildRequestParams` (~95 lines) **and** the `ResponsesEventHandler` event pump (~270 lines: tool-call assembly, reasoning deltas, token usage, finish-reason mapping). High maintenance/drift risk across LC4j + SDK bumps. This is the biggest-code option.

### 3.3. Option C — Do nothing (keep inline base64 for OpenAI)

- **Pro:** zero cost.
- **Con:** no OpenAI payload/tokenization benefit; interim state only.

### 3.4. Option D — Non-streaming decorator for file turns (RECOMMENDED first version)

Same decorator shape as Option B, but for file turns use the **blocking** `client.responses().create(params)` → `Response` and deliver the result to the handler as a single `onCompleteResponse(...)` (plus reconstructing tool-execution requests from the `Response`, if tools are present). This **avoids re-implementing the ~270-line streaming event pump** entirely — we only replicate the request build (~95 lines) and a small `Response`→`ChatResponse` mapping.

- **Pro:** smallest working native path; no streaming-pump replication; normal turns still delegate 1:1; end-to-end without upstream dependency.
- **Con:** file turns are not token-streamed — the answer arrives in one `onCompleteResponse`. Acceptable because:
  - `chatSync()` already buffers the whole stream before returning; no visible difference there.
  - `chat(...)` async/promise variants still resolve correctly; only the incremental `onPartialResponse` callbacks are absent for file turns.
- **Tools note:** if a file turn also needs multi-round tool calling, the blocking path must run the tool loop itself (call → detect tool calls → execute → re-call). Keep phase-1 scope to a **single** model round for file turns; if tools are requested alongside files, either run one tool round manually or fall back to inline base64. Confirm exact behaviour in the spike (§4).

### 3.5. Option E — Vendored copy of the one class + re-patch per release (skill-driven)

Copy the single upstream file `OpenAiOfficialResponsesStreamingChatModel.java` into our own package (e.g. `com.servoy.extensions.aiplugin.chat.openai`), apply a tiny diff to `createUserMessage` that adds the `fileId(...)` branch (reading the `file_id` from a small marker we place on the `Content`, e.g. a custom URI scheme like `openai-file://<id>` on `PdfFileContent`), and use that class in `OpenAiChatDelegate` instead of the upstream one. All of `buildRequestParams` + the streaming event pump come along **for free**, giving full streaming/tool/reasoning parity.

Maintenance is handled by a repeatable **re-patch workflow**, not a full fork/build pipeline:
1. Store our diff as a patch file in the repo (e.g. `patches/openai-official-fileid.patch`).
2. On each `langchain4j-open-ai-official` bump: extract the new upstream `OpenAiOfficialResponsesStreamingChatModel.java` from the `-sources.jar`, drop it in our package, re-apply the patch, resolve any conflicts, rebuild.
3. Capture this as an **opencode skill** (`langchain4j-openai-repatch`) that automates: locate the sources jar in `~/.m2`, extract the one file, apply the patch, report conflicts.

- **Pro:** full parity with upstream streaming/tools/reasoning; only ~10 lines of real change; no dependency on upstream merge; much cheaper than Option B to keep correct because the machinery is copied, not re-derived.
- **Con:** we ship a copy of upstream code (licensing = Apache-2.0, fine; keep the header + note the modification); the copy can silently diverge if a bump changes behaviour we don't notice; the patch may conflict when upstream refactors `createUserMessage`.
- **Package-private dependency check — RESOLVED (spike §4.1 done):** every type the class references is `public`, so **the vendored copy can live in our own package** (e.g. `com.servoy.extensions.aiplugin.chat.openai`) — no split-package, no OSGi problem. Verified against LC4j 1.19.0-beta29 + core 1.19.0:
  - Same-package siblings it uses are all `public`: `OpenAiOfficialResponsesChatRequestParameters`, `OpenAiOfficialResponsesChatResponseMetadata`, `OpenAiOfficialTokenUsage`, and `setup.OpenAiOfficialSetup` (class public, `setupSyncClient(...)` is `public static`).
  - Its LC4j helpers live in `dev.langchain4j.internal.*` and `dev.langchain4j.model.chat.response.*` and are all `public`: `MappingTrackingStreamingChatResponseHandler`, `DefaultExecutorProvider`, `ExceptionMapper`, `InternalStreamingChatResponseHandlerUtils`, `StreamingHandle`, `CompleteToolCall`, `PartialToolCall`.
  - **One residual caveat (not a blocker):** `dev.langchain4j.internal.*` is a public-but-*internal* API with no cross-version stability guarantee — an internal helper's signature can change on a bump, which is exactly why the re-patch skill must re-validate the diff each time.
  - **No OSGi/split-package concern:** this Servoy plugin is a plain Java plugin with a single flat classpath (all dependencies land in `WEB-INF/lib` when deployed, and share one classpath in Servoy Developer). There is no OSGi bundle isolation between our vendored class's package and `dev.langchain4j.model.openaiofficial.*`, so referencing those public types from our own package is unproblematic.

### Recommendation

The §4.1 spike is done and **favourable to Option E**: the vendored class has no package-private dependencies, so it can live in our own package and reuses upstream's full streaming/tool/reasoning machinery for ~10 lines of change. This tilts the recommendation:

- **Option E (vendored class + re-patch skill) is now the preferred target** when full parity matters: it delivers token streaming, the tool loop, reasoning, and token usage exactly as upstream, for a tiny diff, and the only ongoing cost is re-applying a small patch per LC4j bump (automated by a skill).
- **Option D (non-streaming decorator) remains the lowest-risk minimal version** if we want to avoid shipping vendored code at all, or as a stepping stone: it needs no copy of upstream code, only file turns take the custom path, and the single-`onCompleteResponse` tradeoff is masked by `chatSync()`'s buffering.
- **Pursue Option A** (upstream PR) opportunistically in parallel; if it lands, it supersedes D/E and we delete our custom path.
- Option B is **not recommended** — it re-derives the ~270-line event pump by hand (Option E's parity goal at far higher and ongoing cost).

Suggested sequence: implement **D** first for a fast, safe end-to-end win; if token streaming for file turns proves needed, switch to **E** (the spike confirms it's clean); submit **A** upstream whenever we can.

## 4. Spike (before implementation)

1. ~~**Package-private dependency check for Option E**~~ — **DONE (favourable):** every type the upstream class references is `public` (same-package siblings `OpenAiOfficialResponses*`/`OpenAiOfficialTokenUsage`/`setup.OpenAiOfficialSetup`, and the `dev.langchain4j.internal.*` / `model.chat.response.*` helpers). The vendored copy can live in our own package — no split-package, no OSGi issue. Residual: `dev.langchain4j.internal.*` is an internal-but-public API without version-stability guarantees, so the re-patch skill must re-validate on each bump. See §3.5.
2. **Option D end-to-end:** upload a PDF via `client.files().create(purpose=USER_DATA)` → `file_id`; issue one blocking `client.responses().create(ResponseInputFile.fileId(...))`; confirm the model reads the file; map the `Response` to a `ChatResponse` / `AiMessage`.
3. **Tools + files interaction:** determine whether a file turn ever needs a tool round in practice for our users, and whether Option D's single-round limit is acceptable or needs a manual one-round tool loop.
4. Confirm `purpose` (`USER_DATA` vs `ASSISTANTS`) is correct for Responses API `input_file`.

## 5. Implementation plan (Option E — vendored class + patch)

The chosen approach is **Option E**: vendor the one upstream class into our package with a small `fileId(...)` mapping change, so we keep full upstream streaming/tool/reasoning parity. The `FileStore` marks a file with its `file_id`; the vendored model's `createUserMessage` reads that marker and emits `ResponseInputFile.fileId(...)`.

### 5.1. Vendor the upstream class

1. Create `com.servoy.extensions.aiplugin.chat.openai.OpenAiFilesResponsesStreamingChatModel` as a **verbatim copy** of `dev.langchain4j.model.openaiofficial.OpenAiOfficialResponsesStreamingChatModel` (LC4j 1.19.0-beta29), keeping the Apache-2.0 header and adding a short "Vendored from LangChain4j <version>; modified: see §5.2" note.
   - Change only the `package` declaration; leave every referenced type as an import of the original public types (`dev.langchain4j.model.openaiofficial.*`, `.setup.OpenAiOfficialSetup.setupSyncClient`, `dev.langchain4j.internal.*`, `com.openai.*`). Confirmed all-public in §4.1 / §3.5, and the flat Servoy classpath has no package isolation.
2. Store the modification as a patch file in the repo: `patches/openai-official-fileid.patch` (diff of the vendored file against the pristine upstream extract). This is what the re-patch skill re-applies on each bump.

### 5.2. The one behavioural change (the patch)

In the vendored copy's `createUserMessage(...)`, extend the `PdfFileContent` branch so that a `file_id` marker is turned into `ResponseInputFile.builder().fileId(id)` instead of `fileData`/`fileUrl`:

```java
} else if (content instanceof PdfFileContent pdfFileContent) {
    ResponseInputFile.Builder pdfInput = ResponseInputFile.builder();
    String fileId = extractOpenAiFileId(pdfFileContent);   // NEW: recognise our marker
    if (fileId != null) {
        pdfInput.fileId(fileId);                             // NEW: file_id path
    } else if (pdfFileContent.pdfFile().url() != null) {
        pdfInput.fileUrl(pdfFileContent.pdfFile().url().toString());
    } else if (pdfFileContent.pdfFile().base64Data() != null) {
        pdfInput.filename("document.pdf");
        pdfInput.fileData("data:" + pdfFileContent.pdfFile().mimeType() + ";base64," + pdfFileContent.pdfFile().base64Data());
    } else {
        throw new IllegalArgumentException("PDF must have either url or base64Data");
    }
    contentList.add(ResponseInputContent.ofInputFile(pdfInput.build()));
}
```

The marker mechanism: `OpenAiFileStore.upload(...)` returns `PdfFileContent.from(URI.create("openai-file://" + fileId))`. `extractOpenAiFileId` detects the `openai-file` URI scheme and returns the id; anything else returns `null` (unchanged behaviour). Keep the patch minimal so it re-applies cleanly.

### 5.3. `OpenAiFileStore` implementing the shared `FileStore`

- `supports(contentType)`: `application/pdf` (extend to other `input_file` types only once confirmed accepted; images already have their own inline path — decide per spike §4.5).
- `upload(bytes, contentType, fileName)`: `client.files().create(FileCreateParams.builder().file(bytes).purpose(FilePurpose.USER_DATA).build())` → `FileObject.id()`; return `PdfFileContent.from(URI.create("openai-file://" + id))`. Track uploaded ids for cleanup. On any failure return `null` (inline fallback), matching `GeminiFileStore`.

### 5.4. Wire it in `OpenAiChatDelegate.build(...)`

- Only when `shouldUseResponsesApi()` is true: build the vendored `OpenAiFilesResponsesStreamingChatModel` instead of the upstream one (same builder inputs — apiKey, modelName, baseUrl, temperature, reasoningEffort), and pass an `OpenAiFileStore` (sharing the same `OpenAIClient` where practical) into the `ChatClient`.
- When false (custom baseUrl / Chat Completions fallback): unchanged — upstream model, `null` store, inline base64.

### 5.5. Cleanup

Track uploaded `file_id`s on the store; best-effort `client.files().delete(id)` on `ChatClient.close()` (mirrors `GeminiFileStore`). `AutoCloseable`, registered as a closeable by the delegate.

### 5.6. Re-patch skill

Add an opencode skill `langchain4j-openai-repatch` that, on a `langchain4j-open-ai-official` bump:
1. Locates the new `-sources.jar` in `~/.m2`, extracts `OpenAiOfficialResponsesStreamingChatModel.java`.
2. Re-creates the vendored file (package rename) and applies `patches/openai-official-fileid.patch`.
3. Reports any patch conflict and re-validates that referenced `dev.langchain4j.internal.*` signatures still exist.

### 5.7. Verify

`mvn compile` + `getCompilationErrors`; organize imports and format per AGENTS.md.

## 6. Acceptance criteria

- [ ] With OpenAI on the official/Responses path, a supported file added via `addFile()` is uploaded to the OpenAI Files API and referenced by `file_id` (verified via request inspection), not inlined as base64.
- [ ] With OpenAI on the custom-`baseUrl` Chat Completions fallback, files are still sent inline (base64) — unchanged.
- [ ] `text/*` content is always inlined.
- [ ] Upload failures fall back to inline base64 with a logged warning; chat still succeeds.
- [ ] Non-file turns behave identically to today (same streaming, tools, memory, system messages) — the vendored model is a verbatim copy apart from the `file_id` branch.
- [ ] File turns keep full streaming/tool/reasoning parity (vendored upstream machinery), just with the file referenced by `file_id`.
- [ ] The shared `FileStore` abstraction from the Gemini spec is reused (no divergent second abstraction).
- [ ] Uploaded OpenAI files are best-effort deleted on `ChatClient.close()`.
- [ ] The vendored class carries the Apache-2.0 header + a modification note, and the modification is captured in `patches/openai-official-fileid.patch`.
- [ ] A `langchain4j-openai-repatch` skill exists to re-vendor + re-patch on the next LC4j bump.
- [ ] Plugin compiles and bundles cleanly.

## 7. Out of scope

- Gemini file store (delivered in `AI-gemini-files-api.spec.md`).
- Anthropic and Bedrock file stores.
- Migrating the custom `langchain4j-open-ai` fallback to support a file store.
- A configurable upload size threshold and opt-in/opt-out builder flag.
- An upstream LangChain4j PR (Option A) — still worth doing later; if merged it would let us drop the vendored class.

## 8. Open questions

| Question | Owner | Status |
|---|---|---|
| ~~Can an OpenAI `file_id` flow through `AiServices`?~~ **Resolved: No** — LC4j mappers only emit base64; the mapping methods are `private static` (§2.3, §2.4). | Dev | resolved |
| ~~Are the `com.openai` Files + Responses + `ResponseInputFile.fileId` APIs public and usable?~~ **Resolved: Yes** — all public on `OpenAIClient` (§2.3, §2.5). | Dev | resolved |
| ~~Does the upstream `OpenAiOfficialResponsesStreamingChatModel` depend on package-private siblings?~~ **Resolved: No** — all referenced types are `public`; the vendored copy can live in our own package (§3.5, §4.1). Caveat: it leans on `dev.langchain4j.internal.*` (internal API, no stability guarantee) → re-validate per bump. | Dev | resolved |
| ~~Multi-round tool calling on file turns?~~ **Moot for Option E** — the vendored model reuses the upstream tool loop, so tool calling on file turns works exactly as normal turns. | Dev | resolved |
| `purpose` value for `FileCreateParams` (`USER_DATA` vs `ASSISTANTS`) for Responses `input_file`? | Dev | open (spike §4.4) |
| Which content types does OpenAI accept as `input_file` beyond PDF? | Dev | open |
| Cleanup policy for uploaded OpenAI files (they persist until deleted, unlike Gemini's ~48h retention) — delete on `close()` vs rely on manual lifecycle? | Product | open |
| Is there an existing/planned upstream LC4j PR for `file_id` (Option A)? | Dev | open |
