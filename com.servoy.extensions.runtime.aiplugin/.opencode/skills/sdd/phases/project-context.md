# Project Context — Servoy AI Plugin

This project is the **Servoy AI Plugin** — a standalone Maven project that provides
AI functionality to the Servoy application server via langchain4j.

## Technology stack

| Aspect | Value |
|--------|-------|
| Java version | 21 |
| Build system | Maven (standard, with maven-bundle-plugin for OSGi metadata) |
| Packaging | OSGi bundle (via `maven-bundle-plugin`) |
| Key libraries | langchain4j 1.16.1, PDFBox 3.0.7, Jackson 3.1.4, pgvector |
| Servoy version | 2025.12.0.4122 |
| Version | 1.3.0 |

## Project structure

This is a **single-module Maven project**, not a multi-module Tycho build:

```
com.servoy.extensions.runtime.aiplugin/
├── pom.xml                    (standard Maven with bundle packaging)
├── src/main/java/             (production sources)
├── src/main/resources/        (resources)
├── src/main/assembly/zip.xml  (assembly descriptor for distribution)
└── target/                    (build output)
```

## Dependencies

- Managed in `pom.xml` `<dependencies>` section (standard Maven)
- Servoy dependencies: `servoy_shared`, `servoy_base`, `org.eclipse.dltk.javascript.rhino`
- AI/ML: langchain4j (core, Google Gemini, OpenAI, MCP, web search)
- Document processing: PDFBox
- Data: pgvector, Jackson 3.x
- Format: jtoon (TOON format support)

To add a new dependency, add it to `pom.xml`. The `maven-dependency-plugin` copies
runtime deps to `target/ai/` for distribution (excluding already-provided libs like
Servoy, SLF4J, commons, etc.).

## Build & packaging

- `mvn package` — compiles, copies deps to `target/ai/`, creates distribution zip
- The `maven-bundle-plugin` generates OSGi metadata (`Export-Package: com.servoy.extensions.aiplugin.*`)
- The assembly plugin creates the final distributable zip
- Use `./mvnw` or `mvnw.cmd` wrapper scripts if available

## Code conventions

- Follow existing patterns in neighboring files — consistency over personal preference
- Use try-with-resources for all `Closeable` resources
- Use `volatile` or proper synchronization for shared mutable state
- Use proper logging (check what the project uses — likely SLF4J or Servoy's logging)
- No `System.out.println` — use proper logging
- Prefer existing utility classes from `servoy_shared` and `servoy_base`

## Servoy plugin API

This is a **Servoy runtime plugin** — it implements the Servoy plugin interfaces:
- Classes extend/implement Servoy plugin API from `servoy_shared`/`servoy_base`
- The plugin is loaded by the Servoy application server at runtime
- It exposes scripting API to Servoy solutions

## AGENTS.md

If an `AGENTS.md` exists at the project root, read it at the start of your work —
it contains tool usage policy, workflow requirements, and post-edit checklist.

## Gotchas

- **This is NOT a Tycho/Eclipse RCP project.** Dependencies go in `pom.xml`, not MANIFEST.MF.
  There is no target platform file. Do not use Eclipse PDE tools for dependency management.

- **Bundle packaging:** The `maven-bundle-plugin` handles OSGi metadata generation.
  `Export-Package` is configured in the plugin's `<instructions>` section in pom.xml.

- **Excluded groups in copy-dependencies:** The plugin excludes libs already provided
  by the Servoy runtime (SLF4J, commons, Jackson 2.x, Guava, etc.). If you add a dep
  that overlaps, add its groupId to the exclusion list.

- **Servoy UUID:** Use `com.servoy.j2db.util.UUID`, not `java.util.UUID` — Servoy has
  its own UUID class.

- **Jackson 3.x:** This project uses Jackson 3.x (`tools.jackson.core` groupId), not
  the older `com.fasterxml.jackson`. Import paths are different.
