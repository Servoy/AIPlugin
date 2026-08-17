# Spec: SVY-21342c — AI Plugin: Add 1st-class support for Amazon Bedrock

## 1. Goal

Add first-class Amazon Bedrock support to the Servoy AI Plugin, providing the
same builder-pattern API surface that already exists for OpenAI, Gemini, and
Anthropic. This gives Servoy developers a native, consistent way to use
Bedrock-hosted models (Claude, Amazon Nova, Llama, Mistral, etc.) for chat
without switching to a different SDK.

## 2. Background

### 2.1 Existing provider pattern

The AI Plugin exposes providers through `AIProvider` (the `plugins.ai` scripting
object). Each provider has:

- A **chat builder** class extending `BaseChatBuilder<T>` (e.g.
  `OpenAiChatBuilder`, `GeminiChatBuilder`, `AnthropicChatBuilder`) with fluent
  setters and a `build()` method returning `ChatClient`.
- A factory method on `AIProvider` (e.g. `createAnthropicChatBuilder()`).
- A convenience **client** factory method (e.g.
  `createAnthropicClient(apiKey, modelName)`) that creates a minimal ChatClient
  in one call.

All chat builders inherit tool support, MCP client support, system messages, and
token-windowed memory from `BaseChatBuilder`.

### 2.2 Langchain4j Bedrock integration

The `langchain4j-bedrock` module (version `1.19.0`, same as other langchain4j
modules) provides:

- `BedrockStreamingChatModel` — streaming chat model with
  `.builder().region(...).modelId(...).build()`.
- **Auth:** AWS credentials via `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY`
  environment variables, or a `AwsCredentialsProvider`, or a pre-built
  `BedrockRuntimeAsyncClient`.
- **Reasoning:** `enableReasoning(tokenBudget)` via
  `BedrockChatRequestParameters`.
- **Prompt caching:** Supported via `BedrockCachePointPlacement`.
- **Models:** Claude (all versions), Amazon Nova, Llama, Mistral, etc.
- **No dedicated `TokenCountEstimator`** for Bedrock — since it hosts multiple
  model families, there is no single Bedrock token counter. A generic
  `OpenAiTokenCountEstimator` or approximate estimator would be needed.

### 2.3 Key difference: Authentication

Unlike OpenAI/Gemini/Anthropic which use a single API key, Bedrock uses AWS
authentication:

- **Static credentials:** `accessKeyId` + `secretAccessKey` (+ optional
  `sessionToken` for temporary credentials)
- **Region:** required AWS region (e.g. `us-east-1`)
- **Default credentials provider chain:** if no explicit credentials are given,
  the AWS SDK resolves credentials from environment variables, system properties,
  AWS profile, EC2 instance metadata, etc.

### 2.4 Default model

`us.anthropic.claude-sonnet-4-20250514-v1:0` — the most capable
generally-available model on Bedrock at the time of implementation.

## 3. Design

### 3.1 BedrockChatBuilder

A new class `com.servoy.extensions.aiplugin.chat.BedrockChatBuilder` following
the same pattern as the other builders:

```java
@ServoyDocumented
public class BedrockChatBuilder extends BaseChatBuilder<BedrockChatBuilder> implements IJavaScriptType
```

**Fields:**
- `private String region` — AWS region (e.g. `"us-east-1"`)
- `private String modelId = "us.anthropic.claude-sonnet-4-20250514-v1:0"`
- `private String accessKeyId` — AWS access key ID
- `private String secretAccessKey` — AWS secret access key
- `private Double temperature`

**Methods (all `@JSFunction`, fluent):**
- `region(String region)` — sets the AWS region (required)
- `modelId(String modelId)` — sets the Bedrock model ID
- `accessKeyId(String accessKeyId)` — sets the AWS access key ID
- `secretAccessKey(String secretAccessKey)` — sets the AWS secret access key
- `temperature(Double temperature)` — sets the temperature
- `build()` — builds a `BedrockStreamingChatModel`, wires it into `AiServices`,
  attaches `TokenWindowChatMemory` when `tokens != null`, returns `ChatClient`

**Auth strategy in `build()`:**
1. If `accessKeyId` and `secretAccessKey` are both provided, create a
   `StaticCredentialsProvider` with those values.
2. Otherwise, use `DefaultCredentialsProvider.create()` — which resolves from
   environment variables, AWS profiles, EC2 metadata, etc.

**Token counting strategy:**
Since Bedrock hosts multiple model families and has no unified token counter,
use `OpenAiTokenCountEstimator` (jtokkit-based, already on classpath) as an
approximate estimator when `maxMemoryTokens()` is set. This is a reasonable
approximation for most Bedrock models.

### 3.2 AIProvider factory methods

Two new `@JSFunction` methods on `AIProvider`:

- `createBedrockChatBuilder()` — returns `new BedrockChatBuilder(access)`
- `createBedrockClient(String region, String modelId)` — convenience method that
  creates a `BedrockStreamingChatModel` using the default credentials provider
  chain and returns a `ChatClient` directly

### 3.3 Type registration

Add `BedrockChatBuilder.class` to the array returned by
`AIProvider.getAllReturnedTypes()`.

### 3.4 Maven dependency

Add new dependency to `pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-bedrock</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

Also add the AWS SDK BOM or specific AWS dependencies that `langchain4j-bedrock`
pulls transitively. Verify that `software.amazon.awssdk` group is NOT in the
`excludeGroupIds` of the `copy-dependencies` plugin configuration.

### 3.5 Naming: `modelId` vs `modelName`

Bedrock uses "model ID" terminology (e.g.
`us.anthropic.claude-sonnet-4-20250514-v1:0`), not "model name". The builder
method is named `modelId()` to match AWS/Bedrock conventions and avoid confusion
with Anthropic's direct API model names.

### 3.6 Embedding model (not applicable initially)

`langchain4j-bedrock` does include `BedrockEmbeddingModel`, but it requires
additional configuration (model-specific input/output formats). This is deferred
to a follow-up ticket. See Out of Scope.

## 4. Implementation plan

1. Add `langchain4j-bedrock` dependency to `pom.xml` with version
   `${langchain4j.version}`.

2. Verify that `software.amazon.awssdk` is not in `excludeGroupIds` of the
   maven-dependency-plugin copy-dependencies execution. If it is, remove it so
   AWS SDK jars are packaged.

3. Create `src/main/java/com/servoy/extensions/aiplugin/chat/BedrockChatBuilder.java`:
   - Extend `BaseChatBuilder<BedrockChatBuilder>`, implement `IJavaScriptType`
   - Add `@ServoyDocumented` annotation
   - Implement `region()`, `modelId()`, `accessKeyId()`, `secretAccessKey()`,
     `temperature()` fluent setters
   - Implement `build()` using `BedrockStreamingChatModel.builder()` with
     credential resolution logic
   - Use `OpenAiTokenCountEstimator` for approximate token counting when
     `maxMemoryTokens()` is set

4. Add `createBedrockChatBuilder()` factory method to `AIProvider`.

5. Add `createBedrockClient(String region, String modelId)` convenience method
   to `AIProvider`.

6. Add `BedrockChatBuilder.class` to `getAllReturnedTypes()` in `AIProvider`.

7. Verify the build compiles: `mvnw compile`.

## 5. Acceptance criteria

- [ ] `plugins.ai.createBedrockChatBuilder()` returns a `BedrockChatBuilder`
- [ ] `BedrockChatBuilder` supports `.region()`, `.modelId()`, `.accessKeyId()`, `.secretAccessKey()`, `.temperature()` fluent setters
- [ ] `BedrockChatBuilder` inherits `.useBuiltInTools()`, `.maxMemoryTokens()`, `.createTool()`, `.createMCPClient()`, `.addSystemMessage()` from `BaseChatBuilder`
- [ ] `.build()` returns a functional `ChatClient` that can chat with Bedrock-hosted models
- [ ] When `accessKeyId` and `secretAccessKey` are provided, static credentials are used
- [ ] When credentials are not explicitly provided, the default AWS credentials provider chain is used
- [ ] Token-windowed memory works when `.maxMemoryTokens()` is set (using approximate `OpenAiTokenCountEstimator`)
- [ ] `plugins.ai.createBedrockClient(region, modelId)` returns a working `ChatClient` using default credentials
- [ ] Tool calling works through the inherited `BaseChatBuilder` mechanisms
- [ ] Project compiles cleanly with `mvn compile`
- [ ] AWS SDK jars are included in the packaged plugin zip

## 6. Out of scope

- `BedrockEmbeddingModelBuilder` — deferred to a follow-up ticket
- Extended thinking / reasoning budget configuration (can be added later)
- Prompt caching configuration via `BedrockCachePointPlacement` (can be added later)
- Cross-region inference profiles
- Custom `BedrockRuntimeAsyncClient` injection (advanced auth like SSO/role assumption)
- `sessionToken` support for temporary AWS credentials (STS)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should we expose a `profile(String)` method for AWS profile-based auth, or is the default credentials provider chain sufficient? | Product | open |
| Should the convenience method `createBedrockClient` accept optional `accessKeyId`/`secretAccessKey` params, or only support default credentials? | Product | open |
| Is `OpenAiTokenCountEstimator` acceptable as an approximate token counter, or should we skip token-windowed memory for Bedrock entirely until a model-specific estimator is available? | Engineering | open |
| The AWS SDK pulls in many transitive dependencies. Should we shade them or accept the larger plugin zip size? | Engineering | open |
| Should `region` be required in the builder (fail on `build()` if not set), or should it fall back to `AWS_REGION` env var / default region? | Product | open |
