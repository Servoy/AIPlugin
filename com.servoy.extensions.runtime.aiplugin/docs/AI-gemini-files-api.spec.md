# Spec: AI Plugin — Gemini Files API for `addFile()` (upload instead of inline base64)

> No Jira key yet. Filename uses the summary slug; rename to `docs/<KEY>-gemini-files-api.spec.md` once a ticket exists.
> This is **part 1 of 2**. Part 2 (`AI-openai-files-api.spec.md`) covers the OpenAI Files API and builds on the shared `FileStore` infrastructure introduced here.

## 1. Goal

When a Servoy solution calls `ChatClient.addFile(...)` / `addBytes(...)`, the plugin currently base64-encodes the file bytes and embeds them inline in the `UserMessage` on every chat call. This spec introduces an optional **provider file-store** capability and implements it for **Google Gemini**: the file is uploaded once to the Gemini Files API, and the returned file reference is used in the chat request instead of inlining base64. This lets Gemini handle file recognition and tokenization natively and cuts request payload size (Gemini requires the Files API above a ~20 MB file / ~100 MB request size anyway). When no file store is available for the active provider, the current inline-base64 behaviour is retained — the Servoy-facing API is unchanged.

This spec also introduces the shared, provider-agnostic `FileStore` abstraction (§3.1–3.3) that a later OpenAI spec reuses.

## 2. Background

### 2.1. Current flow (inline base64)

- `addFile(Object)` / `addFile(Object, String contentType)` / `addBytes(byte[])` / `addBytes(byte[], String)` append the file to a local `List<Pair<Object,String>> files` on `ChatClient` (`ChatClient.java:48`). Nothing is sent yet.
- On `chat()` / `chatSync()`, `getUserMessage()` (`ChatClient.java:221`) maps each pending file through `createContent()` (`ChatClient.java:238`) and combines the results with a `TextContent` into one `UserMessage`.
- `createContent()` inspects the content type and produces a LangChain4j `Content`, always **base64-encoded inline**:

| Content type | LangChain4j type | Encoding |
|---|---|---|
| `text/*` (or unknown) | `TextContent` | plain text inline |
| `image/*` | `ImageContent` | base64 inline |
| `video/*` | `VideoContent` | base64 inline |
| `audio/*` | `AudioContent` | base64 inline |
| `application/pdf` | `PdfFileContent` | base64 inline |

The provider modules then serialise these into their own wire formats — but all still as inline bytes/base64 in that single request.

### 2.2. LangChain4j's `Content` model has no `file_id` variant

The provider-agnostic `Content` model (`ImageContent`, `PdfFileContent`, `VideoContent`, `AudioContent`) used through `AiServices` accepts **only base64 data or a URI** — there is no `file_id` variant. This is true on the latest stable LangChain4j (`1.19.0`, the version already in the pom); upgrading does not change it. A provider Files API can therefore only be used if the provider's own mapper turns some `Content` shape into a file reference. For Gemini this is possible via a URI (§2.3); for OpenAI it is not (covered in the separate OpenAI spec).

### 2.3. Gemini file reference DOES flow through `AiServices` (verified against LC4j 1.19.0 sources)

`dev.langchain4j.model.googleai.GeminiFiles` (present in 1.19.0) exposes:
```java
GeminiFile uploadFile(byte[] fileBytes, String mimeType, String name)   // + Path overload
// GeminiFile.uri() → the fileUri; files are retained ~48h; also getMetadata/listFiles/deleteFile
GeminiFiles.builder().apiKey(...).baseUrl(...optional...).httpClient(...optional...).build();
```

The chat-request mapper `dev.langchain4j.model.googleai.PartsAndContentsMapper.fromContentToGPart` handles each
content type differently (confirmed by the Step 4a spike):
- **`AudioContent` / `VideoContent` / `PdfFileContent` built from a URI**: only the `data:` scheme is special-cased (inline blob); **every other URI — including the raw `https` Gemini file URI — becomes `GeminiFileData(mimeType, uri.toString())`, i.e. a `file_data` part.** This is exactly what we want, so audio/video/pdf reference the uploaded file directly with no download.
- **`ImageContent` built from a URI**: the image branch has an extra `http(s)` case that **downloads and inlines** the URL (`readBytes(url)`). The Gemini file URI is `https` and needs an API-key header to download, so this branch both defeats the file-reference and would fail auth. There is no non-`http` URI form of the Gemini file URI (the mapper sends `uri.toString()` verbatim). **Therefore images cannot use the file store and stay on the inline-base64 path** (`GeminiFileStore.supports("image/*")` returns false).

### 2.4. Architectural gap

`addFile()` and `createContent()` live in `ChatClient`, but the knowledge of *which* provider/SDK/endpoint is in use lives in the builder/delegate. `ChatClient` today only holds an opaque `Assistant` (`ChatClient.java:46`). To choose upload-vs-inline per provider, that capability (and the means to perform the upload) must be threaded from the builder/delegate into `ChatClient`.

## 3. Design

### 3.1. Capability abstraction: `FileStore`

Introduce a small internal interface representing "this provider can persist a file and give back a reference the chat request can use":

```java
interface FileStore {
    /** Upload bytes and return a LangChain4j Content that references the uploaded file. */
    Content upload(byte[] bytes, String contentType, String fileName);

    /** Which content types should be uploaded (others fall back to inline). */
    boolean supports(String contentType);
}
```

- Returns a LangChain4j `Content` built from the provider file reference. For Gemini this is a `Content` whose URI lands in `PartsAndContentsMapper`'s `fileData` branch (§2.3).
- The default is `null` (no file store) → inline base64. Only Gemini implements it in this spec.

### 3.2. Threading the capability into `ChatClient`

- Add an optional `FileStore fileStore` field to `ChatClient`, passed via a new constructor parameter (nullable). Keep the existing constructor (or overload) so non-file-store providers are unaffected.
- Each provider delegate decides whether to supply a `FileStore`:
  - `GeminiChatDelegate` → a `GeminiFileStore` backed by `GeminiFiles`.
  - `OpenAiChatDelegate`, `AnthropicChatDelegate`, `BedrockChatDelegate` → `null`.
- The delegate `build(...)` signatures and `ChatClient` construction gain a way to pass the `FileStore` through.

### 3.3. Upload decision in `getUserMessage()`

Modify the file-to-content step so that, per pending file:

```
contentType = getContentType(fileOrBytes, explicitType)
if (fileStore != null && !isText(contentType) && fileStore.supports(contentType)) {
    content = fileStore.upload(getBytes(fileOrBytes), contentType, fileName)   // provider file reference
} else {
    content = createContent(fileOrBytes, contentType)   // existing inline base64
}
```

- **Threshold = 0 bytes**: whenever a file store is available and supports the content type, the Files API is
  always used (no size gate). A configurable threshold can be added later as a builder setting; not in scope now.
- Text content (`text/*`) always stays inline — no benefit from uploading.
- Upload failures fall back to inline base64 with a logged warning, so a Files API outage degrades gracefully rather than breaking chat.

### 3.4. `GeminiFileStore` implementation

- Construct `GeminiFiles` with the delegate's `apiKey` (and `baseUrl` if the delegate uses a custom one).
- `supports(contentType)` returns true for `audio/*`, `video/*`, `application/pdf`; false for `text/*`, `image/*`, and unknown. **`image/*` is deliberately excluded**: LangChain4j's `PartsAndContentsMapper` image branch downloads+inlines any `http(s)` URI (and the Gemini file URI needs an API key to download), so an uploaded image cannot be referenced as a `file_data` part — images therefore stay on the inline-base64 path.
- `upload(bytes, contentType, fileName)`:
  1. `GeminiFile f = geminiFiles.uploadFile(bytes, contentType, fileName)`.
  2. Optionally poll `getMetadata` until `f.isActive()` if the initial response is `PROCESSING` (needed for large media; confirm in spike whether required for the small-file happy path).
  3. Wrap `f.uri()` into the `Content` shape that makes `PartsAndContentsMapper` emit a `fileData` part (Step 4a).
- Wire the store in `GeminiChatDelegate.build(...)` and pass it into the `ChatClient`.

### 3.5. Lifecycle / cleanup

Uploaded Gemini files are retained ~48h and can be deleted via `GeminiFiles.deleteFile(name)`. Decide whether the plugin deletes uploads after a chat, on `ChatClient.close()`, or relies on the 48h retention. Leaning: track uploaded file names on the `ChatClient` and best-effort delete on `close()` (`ChatClient.java:314`), matching the existing closeables pattern. Open question (§7).

### 3.6. Servoy-facing API

No new required methods. `addFile()` / `addBytes()` are unchanged. A `preferFileStore(boolean)` opt-out on the builder is a possible later enhancement; not required here.

## 4. Implementation plan

1. **Add `FileStore` interface** in `com.servoy.extensions.aiplugin.chat` (`Content upload(byte[], String, String)` + `boolean supports(String)`).
2. **`ChatClient`**: add nullable `FileStore` field + constructor param/overload; update `getUserMessage()` to route each non-text file through `fileStore` when `supports(contentType)` is true, else `createContent()`. Add best-effort cleanup in `close()` per §3.5 decision.
3. **`BaseChatBuilder` / delegates**: thread an optional `FileStore` from each delegate's `build(...)` into the `ChatClient` constructor (default `null` for all non-Gemini providers).
4. **`GeminiFileStore`** in the Gemini package, using `GeminiFiles`:
   - 4a. **Spike**: confirm the exact `Content` construction whose URI lands in `PartsAndContentsMapper`'s `fileData` branch (not the `http`-download branch) for the returned `GeminiFile.uri()`. Verify against a real Gemini call that the request carries a `file_data` part.
   - 4b. Implement `supports()` and `upload()` (build `GeminiFiles` with apiKey/baseUrl, call `uploadFile`, wrap `uri()` into the confirmed `Content`).
   - 4c. Wire it in `GeminiChatDelegate.build(...)`.
5. **OpenAI / Anthropic / Bedrock**: pass `null` (no file store); confirm inline path unchanged.
6. **Verify**: `mvn compile`, then `getCompilationErrors`; organize imports and format modified files per AGENTS.md post-edit workflow.

## 5. Acceptance criteria

- [ ] `addFile()` / `addBytes()` Servoy API signatures and behaviour are unchanged from the developer's perspective.
- [ ] With Gemini configured, a PDF/image added via `addFile()` is uploaded via the Gemini Files API and referenced as a `file_data` part (verified via request inspection/logging), not inlined as base64.
- [ ] When a Gemini file store is available and supports the content type, it is always used (no size threshold).
- [ ] `text/*` content is always inlined regardless of provider.
- [ ] An upload failure falls back to inline base64 with a logged warning; chat still succeeds.
- [ ] With OpenAI, Anthropic and Bedrock, files are still sent inline (base64) — unchanged.
- [ ] Tool calling, MCP, chat memory, and system messages continue to work with Gemini file turns.
- [ ] Uploaded Gemini files are handled per the §3.5 cleanup decision.
- [ ] Plugin compiles and bundles cleanly.

## 6. Out of scope

- **OpenAI Files API** — covered by the separate OpenAI spec (`AI-openai-files-api.spec.md`).
- Anthropic Files API (requires raw HTTP to the beta endpoint; no LC4j support).
- Bedrock file store (no persistent Files API exists).
- A configurable upload size threshold and any opt-in/opt-out builder flag (this spec is always-on for Gemini, threshold 0).
- Exposing upload/delete of provider files as standalone Servoy scripting methods.
- Vector-store / embedding ingestion of documents (separate existing feature).

## 7. Open questions

| Question | Owner | Status |
|---|---|---|
| ~~Does LC4j 1.19.0 expose `GeminiFiles`, and can its `fileUri` be referenced through `AiServices`?~~ **Resolved: Yes** — `GeminiFiles.uploadFile` exists; `PartsAndContentsMapper` emits `fileData` for non-http/non-data URIs (§2.3). Exact `Content` construction confirmed in Step 4a spike. | Dev | resolved |
| ~~Upload-vs-inline size threshold?~~ **Resolved: 0 bytes** (always use file store when available). | Product | resolved |
| Does the small-file happy path need `PROCESSING`→`ACTIVE` polling before the file can be referenced, or only large media? | Dev | open (spike) |
| Cleanup policy: delete uploads after each chat / on `ChatClient.close()`, or rely on Gemini's ~48h retention? | Product | open |
| Should the same uploaded reference be reused across multiple `chat()` calls on one `ChatClient`, or re-uploaded per call? | Dev | open |
