---
name: langchain4j-openai-repatch
description: Re-vendor and re-patch OpenAiFilesResponsesStreamingChatModel after a langchain4j-open-ai-official version bump. Triggered by 'repatch openai', 'update langchain4j openai', or when the vendored OpenAI Responses model needs re-syncing with upstream.
---

# LangChain4j OpenAI Responses model — re-vendor + re-patch

The plugin ships a **vendored copy** of LangChain4j's
`dev.langchain4j.model.openaiofficial.OpenAiOfficialResponsesStreamingChatModel`
so it can emit an OpenAI `file_id` for uploaded PDFs (the upstream mapper only
emits `file_url`/`file_data`). The copy lives at:

```
src/main/java/com/servoy/extensions/aiplugin/chat/openai/OpenAiFilesResponsesStreamingChatModel.java
```

It is a **verbatim copy** of upstream EXCEPT three changes, all captured in
`patches/openai-official-fileid.patch`:

1. `package` → `com.servoy.extensions.aiplugin.chat.openai`, and the same-package
   siblings (`OpenAiOfficialResponsesChatRequestParameters`,
   `OpenAiOfficialResponsesChatResponseMetadata`, `OpenAiOfficialTokenUsage`) become
   explicit imports from `dev.langchain4j.model.openaiofficial.*`.
2. Class + `Builder` renamed `OpenAiOfficialResponsesStreamingChatModel` →
   `OpenAiFilesResponsesStreamingChatModel`.
3. The one behavioural change: `createUserMessage`'s `PdfFileContent` branch calls
   `pdfInput.fileId(id)` when `extractOpenAiFileId(...)` finds an `openai-file://<id>`
   marker, plus the new `private static String extractOpenAiFileId(PdfFileContent)`
   helper.

Do this on every `langchain4j-open-ai-official` version bump.

## Steps

1. **Find the new version** in `pom.xml` (property or the
   `langchain4j-open-ai-official` dependency). Call it `<VER>`.

2. **Locate the sources jar** in the local Maven repo:
   ```powershell
   $VER = "<VER>"
   $jar = "$env:USERPROFILE\.m2\repository\dev\langchain4j\langchain4j-open-ai-official\$VER\langchain4j-open-ai-official-$VER-sources.jar"
   Test-Path $jar
   ```
   If missing, run `mvn dependency:sources` (or a build) first to fetch it.

3. **Extract just the one file** to a temp dir:
   ```powershell
   $work = "$env:TEMP\lc4j-oai"
   Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
   $entry = $zip.Entries | Where-Object { $_.FullName -eq 'dev/langchain4j/model/openaiofficial/OpenAiOfficialResponsesStreamingChatModel.java' }
   $dest = "$work\OpenAiOfficialResponsesStreamingChatModel.java"
   New-Item -ItemType Directory -Force -Path $work | Out-Null
   [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
   $zip.Dispose()
   ```

4. **Diff the fresh upstream against the previous upstream** (if you kept the prior
   extract) to see what moved. Focus on `createUserMessage(...)` and its
   `PdfFileContent` branch — that is where the patch applies.

5. **Re-create the vendored file** from the fresh extract:
   - Change the `package` line (change 1) and add the three sibling imports.
   - Rename the class + `Builder` (change 2). A safe way: text-replace
     `OpenAiOfficialResponsesStreamingChatModel` → `OpenAiFilesResponsesStreamingChatModel`
     across the file (only touches the class/ctor/builder-return names).
   - Add the modification note comment at the top:
     `Vendored from LangChain4j <VER> OpenAiOfficialResponsesStreamingChatModel; modified: file_id support in createUserMessage — see patches/openai-official-fileid.patch`.
   - Apply change 3 by hand using `patches/openai-official-fileid.patch` as the guide:
     add the `extractOpenAiFileId` guard to the `PdfFileContent` branch and add the
     helper method right after `createUserMessage`. If the branch was refactored
     upstream and the patch context no longer matches, **report the conflict** and
     re-apply the intent (recognise `openai-file://<id>` → `pdfInput.fileId(id)`).

6. **Re-validate internal signatures.** The class depends on
   `dev.langchain4j.internal.*` (internal-but-public, no stability guarantee). Confirm
   these still exist with the same signatures in the new jar; if any changed, adjust:
   - `InternalStreamingChatResponseHandlerUtils.{onPartialResponse,onPartialThinking,onPartialToolCall,onUnmappedRawEvent,withLoggingExceptions}`
   - `JsonSchemaElementUtils.toMap`, `Utils.{copy,getOrDefault}`, `ValidationUtils.ensureNotNull`
   - `DefaultExecutorProvider`, `ExceptionMapper`, `MappingTrackingStreamingChatResponseHandler`, `ToolSpecificationUtils`
   - `model.openaiofficial.setup.OpenAiOfficialSetup.setupSyncClient(...)`
   Use `javap -cp <jar> <fqcn>` to check.

7. **Re-write the patch file** (`patches/openai-official-fileid.patch`) if change 3's
   surrounding context shifted, so the next re-patch stays accurate.

8. **Post-edit workflow** (per AGENTS.md): `eclipse-coder_organizeImports`,
   `eclipse-coder_formatFile`, `eclipse-ide_getCompilationErrors` on the vendored file
   (fix all Java errors — ignore the unrelated pre-existing `pom.xml` unpack error),
   then `mvn compile` → BUILD SUCCESS.

## Notes

- The Servoy plugin has a single flat classpath (no OSGi split-package), so referencing
  `dev.langchain4j.model.openaiofficial.*` public types from our own package is fine.
- Keep the copy as close to verbatim as possible — the smaller the delta, the cheaper
  the next re-patch.
- `OpenAiChatDelegate` builds the model via the vendored class's own `builder()` and
  passes a shared `OpenAIClient` (so `OpenAiFileStore` uploads/deletes through the same
  client). Do not switch it back to upstream `OpenAiOfficialResponsesStreamingChatModel`.
