# Spec: SVY-21342d — AI Plugin: Modular Core + Provider Architecture

## 1. Goal

Refactor the Servoy AI Plugin from a monolithic single-ZIP distribution into a
modular architecture where:

- A **lean core plugin** contains the API surface and shared dependencies.
- **Separate provider packages** (OpenAI, Anthropic, Gemini, Bedrock) are
  downloadable artifacts that users install only for the providers they need.
- Users who don't care about size can still use a **full bundle** with
  everything included.

This reduces the base footprint significantly (especially once Bedrock adds
~15–20 MB of AWS SDK JARs), gives users control over what they deploy, and
makes it possible to ship provider updates independently.

## 2. Background

### 2.1 Current architecture

The plugin is a single Maven module (`com.servoy:servoy-aiplugin:1.3.0`,
packaging `bundle`) that compiles all provider code into one JAR (`ai.jar`) and
copies **all** runtime dependencies into `target/ai/`. The assembly descriptor
(`src/main/assembly/zip.xml`) produces a single `servoy-aiplugin.zip`:

```
servoy-aiplugin.zip
??? ai.jar                     (plugin main JAR)
??? ai/
    ??? langchain4j-core-*.jar
    ??? langchain4j-open-ai-*.jar
    ??? langchain4j-anthropic-*.jar
    ??? langchain4j-google-ai-gemini-*.jar
    ??? openai-java-*.jar
    ??? okhttp-*.jar
    ??? google-*-*.jar
    ??? pdfbox-*.jar
    ??? pgvector-*.jar
    ??? ... (~78 JARs total)
```

### 2.2 Provider dependency breakdown

| Provider  | Key JARs                                                                  | Est. Size |
|-----------|---------------------------------------------------------------------------|-----------|
| OpenAI    | langchain4j-open-ai, langchain4j-open-ai-official, openai-java-*, okhttp-*, okio-*, kotlin-* | ~12 MB |
| Anthropic | langchain4j-anthropic                                                     | ~1 MB     |
| Gemini    | langchain4j-google-ai-gemini, google-api-client, google-auth-*, google-http-client-*, google-oauth-*, opencensus-*, grpc-* | ~8 MB |
| Bedrock   | langchain4j-bedrock, software.amazon.awssdk.* (~30–40 JARs)              | ~15–20 MB |
| Shared    | langchain4j-core, langchain4j, langchain4j-mcp, langchain4j-http-client-*, pdfbox-*, pgvector-*, jackson-*, jtokkit, opennlp, jtoon, jsonschema-*, xstream, etc. | ~20 MB |

### 2.3 Factory method pattern

`AIProvider` is the scripting entry point (`plugins.ai`). Factory methods
directly instantiate builders:

```java
@JSFunction
public OpenAiChatBuilder createOpenAiChatBuilder() {
    return new OpenAiChatBuilder(access);
}
```

This direct instantiation is the point where provider availability is checked.

### 2.4 Servoy plugin classloading

Servoy loads all JARs from the `plugins/ai/` directory into the same
classloader. There is no OSGi isolation between provider JARs — they all share
one flat classpath. This means providers can be added/removed by simply adding
or removing JAR files from the directory.

## 3. Design

### 3.1 Module strategy: Single source, multiple assemblies

The project remains a **single Maven module** (no multi-module reactor). All
provider code stays in the same source tree and compiles together. The split is
purely at the **packaging level** using multiple Maven Assembly descriptors that
produce separate ZIP artifacts.

Rationale:
- Avoids the complexity of a multi-module build (shared parent POM, inter-module
  dependencies, IDE project setup).
- All code compiles together, so refactoring across providers is easy.
- The single `ai.jar` still contains all provider classes — what changes is
  which *dependency* JARs are present at runtime.
- Provider classes gracefully handle missing dependencies via `Class.forName()`.

### 3.2 Provider discovery via Class.forName()

When a factory method is called on `AIProvider`, it checks whether the
provider's key class is on the classpath before instantiating the builder:

```java
@JSFunction
public OpenAiChatBuilder createOpenAiChatBuilder() {
    ensureProviderAvailable(
        "dev.langchain4j.model.openai.OpenAiStreamingChatModel",
        "OpenAI",
        "servoy-ai-provider-openai"
    );
    return new OpenAiChatBuilder(access);
}

private void ensureProviderAvailable(String markerClass, String providerName, String artifactName) {
    try {
        Class.forName(markerClass);
    } catch (ClassNotFoundException e) {
        throw new RuntimeException(
            providerName + " provider not installed. " +
            "Download '" + artifactName + "' from the Servoy AI Plugin releases " +
            "and place it in your Servoy plugins/ai/ directory."
        );
    }
}
```

Marker classes per provider:
| Provider  | Marker Class                                                    |
|-----------|-----------------------------------------------------------------|
| OpenAI    | `dev.langchain4j.model.openai.OpenAiStreamingChatModel`        |
| Anthropic | `dev.langchain4j.model.anthropic.AnthropicStreamingChatModel`  |
| Gemini    | `dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel` |
| Bedrock   | `dev.langchain4j.model.bedrock.BedrockStreamingChatModel`      |

### 3.3 Packaging artifacts

The build produces the following ZIP files:

| Artifact                          | Contents                                        |
|-----------------------------------|-------------------------------------------------|
| `servoy-ai-plugin-core.zip`      | `ai.jar` + shared dependency JARs               |
| `servoy-ai-provider-openai.zip`  | OpenAI-specific dependency JARs only            |
| `servoy-ai-provider-anthropic.zip` | Anthropic-specific dependency JARs only       |
| `servoy-ai-provider-gemini.zip`  | Gemini-specific dependency JARs only            |
| `servoy-ai-provider-bedrock.zip` | Bedrock/AWS SDK dependency JARs only            |
| `servoy-aiplugin.zip`            | Full bundle (core + all providers, same as today) |

All ZIPs extract their contents flat into `plugins/ai/` — no nested directories
within the provider ZIPs:

```
plugins/
??? ai/
    ??? ai.jar                          (from core)
    ??? langchain4j-core-1.19.0.jar     (from core)
    ??? pdfbox-3.0.8.jar                (from core)
    ??? ...                             (from core)
    ??? langchain4j-open-ai-1.19.0.jar  (from openai provider)
    ??? openai-java-4.41.0.jar          (from openai provider)
    ??? ...
```

### 3.4 Assembly descriptors

Each provider gets its own assembly descriptor under `src/main/assembly/`:

- `zip.xml` — full bundle (unchanged, backward-compatible)
- `zip-core.xml` — core only: includes `ai.jar` + shared deps, excludes
  provider-specific JARs
- `zip-provider-openai.xml` — OpenAI deps only
- `zip-provider-anthropic.xml` — Anthropic deps only
- `zip-provider-gemini.xml` — Gemini deps only
- `zip-provider-bedrock.xml` — Bedrock deps only

Each provider assembly uses `<dependencySets>` with `<includes>` to pick only
the relevant artifacts. The core assembly uses `<excludes>` to skip all
provider-specific artifacts.

### 3.5 Dependency classification

Dependencies are classified using Maven `<includes>`/`<excludes>` patterns in
each assembly descriptor:

**Core (shared) dependencies:**
```
dev.langchain4j:langchain4j-core
dev.langchain4j:langchain4j
dev.langchain4j:langchain4j-mcp
dev.langchain4j:langchain4j-http-client
dev.langchain4j:langchain4j-http-client-jdk
dev.langchain4j:langchain4j-web-search-engine-google-custom
org.apache.pdfbox:*
com.pgvector:pgvector
tools.jackson:*               (Jackson 3.x)
com.fasterxml.jackson.*:*     (Jackson 2.x transitive)
com.knuddels:jtokkit
org.apache.opennlp:*
dev.toonformat:jtoon
com.github.victools:*         (jsonschema-generator)
io.swagger.core.v3:*
com.thoughtworks.xstream:*
xmlpull:*
com.fasterxml:classmate
```

**OpenAI provider dependencies:**
```
dev.langchain4j:langchain4j-open-ai
dev.langchain4j:langchain4j-open-ai-official
com.openai:*
com.squareup.okhttp3:*
com.squareup.okio:*
org.jetbrains.kotlin:*
org.jetbrains:annotations
```

**Anthropic provider dependencies:**
```
dev.langchain4j:langchain4j-anthropic
```

**Gemini provider dependencies:**
```
dev.langchain4j:langchain4j-google-ai-gemini
com.google.api-client:*
com.google.auth:*
com.google.http-client:*
com.google.oauth-client:*
com.google.code.gson:gson
io.opencensus:*
io.grpc:*
com.google.auto.value:*
com.google.errorprone:*
com.google.guava:*           (if not excluded globally)
com.google.j2objc:*
javax.annotation:jsr305
org.apache.httpcomponents:*
```

**Bedrock provider dependencies:**
```
dev.langchain4j:langchain4j-bedrock
software.amazon.awssdk:*
```

### 3.6 Copy-dependencies phase changes

The current `copy-dependencies` execution copies everything to `target/ai/`.
Add additional executions (or use Maven profiles) to copy provider-specific deps
into separate directories:

```
target/ai/              (all deps, used by full bundle)
target/ai-core/         (shared deps only)
target/ai-openai/       (OpenAI deps only)
target/ai-anthropic/    (Anthropic deps only)
target/ai-gemini/       (Gemini deps only)
target/ai-bedrock/      (Bedrock deps only)
```

Each uses `includeGroupIds`/`includeArtifactIds` or
`excludeGroupIds`/`excludeArtifactIds` to select the right subset.

### 3.7 Error messages

When a provider is not installed, the error message must be:
- Visible in the Servoy developer console
- Actionable — tells the user exactly what to download and where to put it

Format:
```
{Provider} provider not installed. Download 'servoy-ai-provider-{provider}'
from the Servoy AI Plugin releases and place the JARs in your Servoy
plugins/ai/ directory.
```

The error is thrown as a `RuntimeException` from the factory method, which
Servoy surfaces in the developer console.

### 3.8 Backward compatibility

- The **full bundle** (`servoy-aiplugin.zip`) continues to be produced and
  remains the **default download** for users who want simplicity.
- The scripting API is **completely unchanged** — same method names, same
  return types, same behavior when all providers are installed.
- Existing users upgrading: replace the old ZIP with the new full bundle. No
  changes needed.
- The modular ZIPs are an **opt-in** for users who want a lean deployment.

### 3.9 GitHub Actions workflow

A new `.github/workflows/build.yml` workflow:

```yaml
name: Build
on:
  push:
    tags: ['v*']
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      - run: ./mvnw package -DskipTests
      - uses: actions/upload-artifact@v4
        with:
          name: servoy-ai-plugin-full
          path: target/servoy-aiplugin.zip
      - uses: actions/upload-artifact@v4
        with:
          name: servoy-ai-plugin-core
          path: target/servoy-ai-plugin-core.zip
      - uses: actions/upload-artifact@v4
        with:
          name: servoy-ai-provider-openai
          path: target/servoy-ai-provider-openai.zip
      - uses: actions/upload-artifact@v4
        with:
          name: servoy-ai-provider-anthropic
          path: target/servoy-ai-provider-anthropic.zip
      - uses: actions/upload-artifact@v4
        with:
          name: servoy-ai-provider-gemini
          path: target/servoy-ai-provider-gemini.zip
      - uses: actions/upload-artifact@v4
        with:
          name: servoy-ai-provider-bedrock
          path: target/servoy-ai-provider-bedrock.zip

  release:
    needs: build
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/download-artifact@v4
      - uses: softprops/action-gh-release@v2
        with:
          files: |
            servoy-ai-plugin-full/servoy-aiplugin.zip
            servoy-ai-plugin-core/servoy-ai-plugin-core.zip
            servoy-ai-provider-openai/servoy-ai-provider-openai.zip
            servoy-ai-provider-anthropic/servoy-ai-provider-anthropic.zip
            servoy-ai-provider-gemini/servoy-ai-provider-gemini.zip
            servoy-ai-provider-bedrock/servoy-ai-provider-bedrock.zip
```

## 4. Implementation plan

1. **Add `ensureProviderAvailable()` helper to `AIProvider`**
   - Private method that does `Class.forName()` check and throws descriptive
     `RuntimeException` on failure.
   - Call it at the top of every `create*ChatBuilder()` and `create*Client()`
     factory method.
   - Call it at the top of `create*EmbeddingModelBuilder()` methods.

2. **Create provider-specific copy-dependencies executions in `pom.xml`**
   - Add execution IDs: `copy-core-deps`, `copy-openai-deps`,
     `copy-anthropic-deps`, `copy-gemini-deps`, `copy-bedrock-deps`.
   - Each writes to its own output directory under `target/`.
   - Uses `includeGroupIds`/`includeArtifactIds` to select the right subset.

3. **Create assembly descriptors**
   - `src/main/assembly/zip-core.xml`
   - `src/main/assembly/zip-provider-openai.xml`
   - `src/main/assembly/zip-provider-anthropic.xml`
   - `src/main/assembly/zip-provider-gemini.xml`
   - `src/main/assembly/zip-provider-bedrock.xml`
   - Each references its corresponding `target/ai-{provider}/` directory.
   - Core descriptor also includes `ai.jar`.

4. **Update `maven-assembly-plugin` configuration**
   - Add all new descriptors to the existing plugin execution.
   - Each produces a ZIP with its own `finalName`.

5. **Create `.github/workflows/build.yml`**
   - Build, test, upload artifacts, create release on tag push.

6. **Update README / documentation**
   - Document the modular installation option.
   - Document the full-bundle (default) option.
   - List which providers are included in which ZIP.

7. **Verify the build**: `mvnw package` produces all expected ZIP files.

8. **Verify runtime behavior**:
   - Install only core ? calling `createOpenAiChatBuilder()` throws clear error.
   - Install core + openai ? `createOpenAiChatBuilder()` works, Anthropic
     throws clear error.
   - Install full bundle ? everything works as before.

## 5. Acceptance criteria

- [ ] `mvnw package` produces 6 ZIP artifacts: full, core, openai, anthropic,
      gemini, bedrock
- [ ] Core ZIP contains `ai.jar` and shared dependencies only (~20 MB)
- [ ] Each provider ZIP contains only provider-specific JARs (no overlap with
      core)
- [ ] Full bundle ZIP contains everything (same as current behavior)
- [ ] When a provider is not installed, calling its factory method throws a
      `RuntimeException` with a clear, actionable message
- [ ] Error message is visible in the Servoy developer console
- [ ] When a provider IS installed (JARs present), all existing functionality
      works identically to before
- [ ] Scripting API is completely unchanged — same method names, return types,
      behavior
- [ ] GitHub Actions workflow builds all artifacts and creates releases on tags
- [ ] `servoy-aiplugin.zip` (full bundle) remains backward-compatible with
      existing installations

## 6. Out of scope

- OSGi-level isolation between providers (the Servoy classloader is flat)
- Maven multi-module reactor conversion (too much complexity for this change)
- Provider auto-discovery via ServiceLoader (unnecessary given direct factory
  methods)
- Version compatibility checks between core and provider ZIPs
- Hot-loading of providers at runtime (requires server restart)
- Download/install automation from within Servoy IDE
- Per-provider versioning (all artifacts share the same version)

## 7. Migration path

| User scenario | Action needed |
|---------------|---------------|
| Existing user, doesn't care about size | Replace old ZIP with new `servoy-aiplugin.zip` (full bundle). No other changes. |
| New user, wants lean deployment | Install `servoy-ai-plugin-core.zip` + only the provider ZIPs they need. |
| Existing user, wants to slim down | Replace old ZIP with core + specific provider ZIPs. |

## 8. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the web-search (Google Custom Search) dependency be in core or in a separate `servoy-ai-provider-google-search.zip`? | Product | open |
| Should the full bundle remain the "default" on the releases page, or should we nudge users toward the modular approach? | Product | open |
| Do we need version compatibility checks between core and provider ZIPs (e.g. core 1.4 with provider 1.3)? | Engineering | open |
| Should the error message include a URL to the GitHub releases page? If so, what is the canonical URL? | Product | open |
| Should embedding model builders (`createOpenAiEmbeddingModelBuilder`, `createGeminiEmbeddingModelBuilder`) also get the `ensureProviderAvailable` check? | Engineering | open |
