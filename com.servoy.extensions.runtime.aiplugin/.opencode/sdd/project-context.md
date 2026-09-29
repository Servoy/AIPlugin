# Project Context — Servoy AI Plugin

This project is the **Servoy AI Plugin** — a standalone Maven project (a Servoy runtime
plugin/extension, NOT an Eclipse-OSGi/Tycho plugin) that provides AI functionality to the
Servoy application server via langchain4j.

## SDD variant

This repo uses the **sdd-java-plain** shared skill (plain-Java / standard-Maven). It is a
Servoy runtime extension packaged as a bundle via `maven-bundle-plugin`, but the development
model is standard Maven — dependencies in `pom.xml`, JUnit 5 + Mockito, `mvn` build/test.
It is NOT an Eclipse-plugin/Tycho project (no target platform, no hand-authored MANIFEST).

## Technology stack

| Aspect | Value |
|--------|-------|
| Java version | 21 |
| Build system | Maven (standard, `maven-bundle-plugin` generates OSGi metadata; a Tycho `eclipse-run` goal is used only to run the Servoy doc generator) |
| Packaging | `bundle` (OSGi metadata via `maven-bundle-plugin`) |
| Key libraries | langchain4j 1.19.0 (openai/anthropic/gemini/bedrock/mcp), PDFBox 3.0.8, Jackson 2.22 (`com.fasterxml.jackson`), Netty 4.2, pgvector, jtoon |
| Testing | JUnit 5 (Jupiter) 5.14 + Mockito 5.23 |
| Servoy version | 2026.3.1.4143 |
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
- Servoy dependencies: `servoy_shared`, `servoy_base`, `org.eclipse.dltk.javascript.rhino`, `jabsorb`
- AI/ML: langchain4j (core + openai / openai-official / anthropic / gemini / bedrock / mcp)
- Document processing: PDFBox (+ pdfbox-io)
- Data: pgvector, Jackson 2.22 (`com.fasterxml.jackson`), Netty 4.2 (BOM-managed)
- Format: jtoon (TOON format support)

To add a new dependency, add it to `pom.xml`. The `maven-dependency-plugin` `copy-dependencies`
executions split runtime deps into per-provider output dirs (`target/ai-core`, `ai-openai`,
`ai-anthropic`, `ai-gemini`, `ai-bedrock`), excluding libs already provided by the Servoy
runtime (Servoy, SLF4J, commons, Jackson core, Guava, etc.). If you add a dep that overlaps a
provided lib, add its groupId to the relevant exclude list.

## Build, test & packaging

- `mvn compile` — quick compile check while iterating
- `mvn test` — run the JUnit 5 tests (single class: `mvn test -Dtest=SomeClassTest`)
- `mvn package` — compiles, copies per-provider deps, generates OSGi metadata, builds the
  distribution zips (core + per-provider assemblies)
- The `maven-bundle-plugin` generates OSGi metadata (`Export-Package: com.servoy.extensions.aiplugin.*`)
- Use `./mvnw` / `mvnw.cmd` wrapper scripts if present

## Code conventions

- Follow existing patterns in neighboring files — consistency over personal preference
- Use try-with-resources for all `Closeable` resources
- Use `volatile` or proper synchronization for shared mutable state
- Use proper logging (check what the project uses — likely SLF4J or Servoy's logging)
- No `System.out.println` — use proper logging
- Prefer existing utility classes from `servoy_shared` and `servoy_base`

## Testing

- JUnit 5 (Jupiter) + Mockito, under `src/test/java`, run with `mvn test`.
- Mock collaborators (langchain4j model clients, Servoy services) with Mockito; do not make
  real LLM/network calls in unit tests.
- No OSGi/PDE test runner — this is standard surefire.

## Servoy plugin API

This is a **Servoy runtime plugin** — it implements the Servoy plugin interfaces:
- Classes extend/implement Servoy plugin API from `servoy_shared`/`servoy_base`
- The plugin is loaded by the Servoy application server at runtime
- It exposes scripting API to Servoy solutions (documented via the Servoy doc generator run
  in the `generate-resources` phase — a Tycho `eclipse-run` goal, not the dev model)

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

- **Jackson 2.x:** This project uses Jackson 2.22 (`com.fasterxml.jackson` groupId). Do not
  assume Jackson 3 (`tools.jackson.*`) import paths.

- **Not Tycho for the dev loop:** a `tycho-eclipse-plugin` `eclipse-run` goal exists but only
  runs the Servoy doc generator during `generate-resources`. Day-to-day build/test/compile is
  plain Maven — do not treat this as an eclipse-plugin project.
