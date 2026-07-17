# Spec: SVY-21117 — Code completion in JS/DLTK on exposed Java classes that use generics doesn't work as expected

## 1. Goal

When Java classes exposed to Servoy scripting use generics (e.g., `BaseChatBuilder<T>`), the generated `servoy-extension.xml` should resolve generic type parameters to their concrete types so that code completion (DLTK) and tooltips in the JS editor display the correct return types and documentation.

Currently, methods inherited from a generic superclass retain their erased/unresolved return type (e.g., `BaseChatBuilder<T>`) instead of the concrete type (e.g., `GeminiChatBuilder`). This makes code completion break because:
1. The undocumented base class is shown as the return type.
2. Tooltips are empty because the type has no entry in the XML.
3. Chained builder patterns lose type information at each generic-returning call.

## 2. Background

### 2.1 Builder pattern with generics

The AI plugin uses a self-referencing generic builder pattern:

```java
public abstract class BaseChatBuilder<T extends BaseChatBuilder<T>> {
    public T useBuiltInTools(boolean b) { return (T)this; }
    public T maxMemoryTokens(Integer tokens) { return (T)this; }
    public MCPClientBuilder<T> createMCPClient() { ... }
    public abstract ChatClient build();
}

public class GeminiChatBuilder extends BaseChatBuilder<GeminiChatBuilder> { ... }
```

In Java, calling `createGeminiChatBuilder().useBuiltInTools(true)` returns `GeminiChatBuilder` (the type parameter `T` is resolved). However, in the generated XML and thus in JS/DLTK, these methods show `BaseChatBuilder<T>` as the return type.

### 2.2 Current XML output (broken)

In `servoy-extension.xml`, under `GeminiChatBuilder`:
```xml
<function name="useBuiltInTools">
  <return type="com.servoy.extensions.aiplugin.chat.BaseChatBuilder&lt;T&gt;" 
          typecode="com.servoy.extensions.aiplugin.chat.BaseChatBuilder&lt;T&gt;"/>
</function>
```

Expected:
```xml
<function name="useBuiltInTools">
  <return type="com.servoy.extensions.aiplugin.chat.GeminiChatBuilder" 
          typecode="com.servoy.extensions.aiplugin.chat.GeminiChatBuilder"/>
</function>
```

Similarly, `MCPClientBuilder.build()` returns `BaseChatBuilder<T>` instead of a resolved type.

### 2.3 Existing generic resolution mechanism

The docgenerator already has partial support for resolving generics:
- `TypeMetaModel.addMembersRecursively()` walks supertypes and interfaces to collect inherited members.
- `TypeMetaModel.applyTypeArguments()` checks if a method's return type matches a type parameter name and substitutes it with the concrete type argument.
- `TypeName.withType(ITypeBinding)` and `MethodMetaModel.withType(ITypeBinding)` create copies with the resolved type.

The resolution works for **interfaces** (type arguments are passed) but fails for **supertypes** (hardcoded `null` is passed).

### 2.4 Root cause in docgenerator

In `TypeMetaModel.addMembersRecursively()` (line 340):
```java
addMembersRecursively(holder.getType(tmm.getSupertype()), null, holder, members);
```

This passes `null` for typeArguments when processing the supertype. When `applyTypeArguments` receives `null`, it falls back to the first type bound of the type parameter (e.g., `BaseChatBuilder` for `T extends BaseChatBuilder<T>`), which is incorrect — it should use the actual type arguments from the supertype declaration (e.g., `GeminiChatBuilder` from `extends BaseChatBuilder<GeminiChatBuilder>`).

### 2.5 MCPClientBuilder problem

`MCPClientBuilder<T extends BaseChatBuilder<T>>` is itself `@ServoyDocumented`. Its `build()` method returns `T`. Since `MCPClientBuilder` is a standalone documented type (not inherited into a concrete subclass), there is no single concrete type to substitute for `T` at XML generation time.

The same problem exists for `ToolBuilder<T>`.

### 2.6 ChatClient.close() empty tooltip

The ticket mentions that `ChatClient.close()` shows an empty tooltip for the result of `build()`. This is a consequence of the same issue: when `build()` returns `BaseChatBuilder<T>`, DLTK can't find that type in the documentation registry (since it's not `@ServoyDocumented`), so it shows nothing.

## 3. Design

### 3.1 Fix supertype type argument resolution in docgenerator

In `TypeMetaModel.addMembersRecursively()`, pass the supertype's type arguments instead of `null`:

```java
// Before:
addMembersRecursively(holder.getType(tmm.getSupertype()), null, holder, members);

// After:
TypeName supertype = tmm.getSupertype();
addMembersRecursively(holder.getType(supertype), 
    supertype != null ? supertype.getTypeArguments() : null, holder, members);
```

This ensures that when `GeminiChatBuilder` collects members from `BaseChatBuilder<GeminiChatBuilder>`, the type parameter `T` is correctly resolved to `GeminiChatBuilder`.

### 3.2 Handle MCPClientBuilder and ToolBuilder (standalone generic documented types)

For documented types that are themselves generic (like `MCPClientBuilder<T>` and `ToolBuilder<T>`), the `build()` method's return type cannot be statically resolved to a single concrete type. Two options:

**Option A (recommended): Resolve to type bound in own-class methods**

When generating the XML for a documented generic class's own methods, if the return type is a type variable, resolve it to the type variable's upper bound. For `MCPClientBuilder<T extends BaseChatBuilder<T>>`, `T` would resolve to `BaseChatBuilder`. Since `BaseChatBuilder` is not documented, it can be further mapped (see 3.3).

**Option B: Use `@JSSignature` annotation**

Add `@JSSignature` annotations to `MCPClientBuilder.build()` and `ToolBuilder.build()` to explicitly declare the return type. This is a workaround that doesn't require docgenerator changes but adds maintenance burden.

### 3.3 Ensure undocumented base types fall through to their documented descendants

When a return type resolves to an undocumented class (like `BaseChatBuilder`), the existing `TypeMapper` mechanism in the docgenerator can map it to a documented type. If `tryToMapUndocumentedTypes()` is enabled, the mapper should find a suitable documented subtype.

Alternatively, add `@ServoyDocumented` to `BaseChatBuilder` but configure it so it doesn't appear in user-facing documentation (e.g., using a category that's filtered out from public docs). This way DLTK can at least find the type and show its methods.

### 3.4 Verify ChatClient.close() tooltip

Once the return type of `build()` resolves correctly (to `ChatClient` for the concrete builders, or at least a documented type), the tooltip for `close()` should populate correctly since `ChatClient` is `@ServoyDocumented` and `close()` is fully documented in the XML.

## 4. Implementation plan

1. **Fix `TypeMetaModel.addMembersRecursively()`** in `c:\Users\vosti\git_master\docgenerator-ui\com.servoy.eclipse.docgenerator\src\com\servoy\eclipse\docgenerator\metamodel\TypeMetaModel.java`:
   - Change line 340 from `addMembersRecursively(holder.getType(tmm.getSupertype()), null, holder, members)` to pass `tmm.getSupertype().getTypeArguments()` instead of `null`.

2. **Handle own-class type parameters**: In `applyTypeArguments()`, extend the logic to also resolve type variables that are parameters of the *current* class (not just inherited). When the documented class itself is generic and a method returns one of its type parameters, resolve it to the type bound.

3. **Regenerate `servoy-extension.xml`** for the AI plugin and verify that:
   - `GeminiChatBuilder.useBuiltInTools()` returns `GeminiChatBuilder`
   - `GeminiChatBuilder.maxMemoryTokens()` returns `GeminiChatBuilder`
   - `GeminiChatBuilder.addSystemMessage()` returns `GeminiChatBuilder`
   - `GeminiChatBuilder.createMCPClient()` returns `MCPClientBuilder` (already correct)
   - `MCPClientBuilder.build()` returns a recognizable/documented type

4. **Verify DLTK behavior** in servoy-eclipse:
   - Code completion after `.useBuiltInTools(true).` shows `GeminiChatBuilder` methods
   - Code completion after `.createMCPClient().connectViaSTDIO(...).build().` shows the chat builder methods
   - `ChatClient.close()` tooltip shows the documented description

5. **Test with OpenAiChatBuilder** to ensure the fix works for all concrete builder subclasses.

## 5. Acceptance criteria

- [ ] Methods inherited from `BaseChatBuilder<T>` into `GeminiChatBuilder` show return type `GeminiChatBuilder` in the generated XML.
- [ ] Methods inherited from `BaseChatBuilder<T>` into `OpenAiChatBuilder` show return type `OpenAiChatBuilder` in the generated XML.
- [ ] `MCPClientBuilder.build()` return type resolves to a documented type (not `BaseChatBuilder<T>`).
- [ ] `ToolBuilder.build()` return type resolves to a documented type.
- [ ] Code completion in JS editor correctly chains after calling inherited builder methods.
- [ ] `ChatClient.close()` shows a proper tooltip with description.
- [ ] No regression in other documented types that use generics (e.g., `EmbeddingMetaDataColumnAdder`).

## 6. Out of scope

- SVY-21118: `{Object<String>}` param seen as `Object` — separate generic parameter resolution issue.
- SVY-21119: Overloaded method tooltip matching — separate issue.
- SVY-21120: Parameter name/type mismatch in tooltips — separate issue.
- Resolving generics at the DLTK/Eclipse level (runtime resolution) — this spec only addresses the static XML generation.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `BaseChatBuilder` be made `@ServoyDocumented` as a fallback for MCPClientBuilder.build() return type? | architect | open |
| Is `tryToMapUndocumentedTypes()` enabled for the AI plugin doc generation? | dev | open |
| Should MCPClientBuilder/ToolBuilder be duplicated per concrete builder type instead of being a standalone documented type? | architect | open |
