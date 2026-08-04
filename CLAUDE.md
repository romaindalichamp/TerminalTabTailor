# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Terminal Tab Tailor is an IntelliJ IDEA plugin (Kotlin) that renames terminal tabs after the currently
selected project-tree/editor item, optionally reusing, dating, and sorting them.

## Commands

```bash
./gradlew test                                        # unit tests (JUnit 5)
./gradlew test --tests "*TerminalTabsUtilTest*"       # single test class
./gradlew test --tests "*TerminalTabsUtilTest.test doNotSort sort"   # single test method
./gradlew check                                       # run before opening a PR (per CONTRIBUTING.md)
./gradlew runIde                                      # sandbox IDE with the plugin (also the "Run Plugin" run config)
./gradlew buildPlugin                                 # distributable zip in build/distributions
./gradlew verifyPluginProjectConfiguration            # validates the plugin project configuration
```

`verifyPlugin` also exists, but in the 2.x plugin it runs the IntelliJ Plugin Verifier CLI against named IDE
builds and needs `intellijPlatform { pluginVerification { ides { ... } } }`, which this project does not configure.

`signPlugin`/`publishPlugin` read `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN`
from the environment. There is no CI workflow in the repo — releases are driven locally.

## Toolchain

Pinned in `build.gradle.kts`: IntelliJ Platform Gradle Plugin **2.x** (`org.jetbrains.intellij.platform`),
IC `2025.1`, `sinceBuild` 251, no `untilBuild` (deliberate — avoids re-releasing on every IDE major),
Kotlin 2.1.0, bytecode target 21. Gradle 8.14.5 through the committed wrapper.

Constraints to know before changing any of it:

- **Do not go back to `org.jetbrains.intellij` 1.x.** It cannot read 2025.x distribution layouts and fails with
  `IndexOutOfBoundsException` in `Utils.resolveIdeHomeVariable`.
- Platform plugin 2.10.5 requires **Gradle 8.13+**, and 2.18+ requires Gradle 9. The wrapper is committed so that
  requirement travels with the repo — `.gitignore` deliberately un-ignores `gradle/wrapper/gradle-wrapper.jar`.
- **Bytecode target must stay at 21 or lower**: 2025.1 bundles JBR 21, so anything higher fails to load with
  `UnsupportedClassVersionError`.
- `junit:junit:4.13.2` is a **required** `testRuntimeOnly` dependency even though every test is Jupiter — the
  IntelliJ test framework initialises its `Logger` through JUnit 4 classes. Removing it looks harmless and breaks
  the entire test task with `NoClassDefFoundError: org/junit/runners/model/Statement`.
- Bumping the platform version means re-checking the JetBrains APIs listed under "Platform coupling" below.

## Architecture

Flow of a single "open in terminal" invocation:

```
listeners (record selection)        actions (entry points)
  ProjectFileEditorManagerListener ─┐    CustomRevealFileInTerminalAction  (replaces Terminal.OpenInTerminal)
  CustomProjectViewListener         ├─►  CustomTerminalNewTabAction        ("TTT Button" in the terminal toolbar)
    └─ ProjectTreeSelectionListener ┘                    │
                 │                                       ▼
                 ▼                            TerminalTabNamesManager.openTabInTerminal
   ProjectSpecificLastVirtualFile                        │
   (WeakHashMap<Project, VirtualFile>,          VirtualSelectionUtil ──► VirtualSelection
    one map per TabNameOriginEnum)                       │
                                                         ▼
                                              TerminalTabsUtil  ──► ContentManager of the "Terminal" tool window
```

- **`TerminalTabNamesManager`** is the only orchestrator: decide reuse-vs-create, build the name
  (`constructNewTabName`, driven by `TabNameTypeEnum` + optional ` <date>` suffix), then rename/sort/select/focus.
- **`TerminalTabsUtil`** holds all `ContentManager` manipulation and all pure logic (name numbering, sorting,
  date extraction). It is the layer that unit tests exercise — keep new logic here, not in the manager or actions.
- **`ProjectSpecificLastVirtualFile`** keeps three separate `WeakHashMap`s (editor / project-tree / mixed) and
  picks one based on `TabNameOriginEnum`. `PROJECT_NAME` bypasses the maps and returns `project.guessProjectDir()`.
- **`StartupActivity`** (postStartupActivity) swaps the platform's `Terminal.RenameSession` and
  `Terminal.OpenInTerminal` actions for the plugin's subclasses via `ActionManager.replaceAction`. This is why the
  plugin affects the *stock* "Open in Terminal" everywhere, not just its own menu items.
- **Settings** are an application-level `PersistentStateComponent` (`TerminalTabTailorSettingsService` →
  `TerminalTabTailorSettings.xml`), rendered by `TerminalTabTailorConfigurable` with the Kotlin UI DSL. All user-facing
  strings live in `src/main/resources/TerminalTabTailorBundle.properties`.

## Conventions and pitfalls

- **Package ≠ directory in one place**: `src/main/kotlin/.../providers/OpenPredefinedTerminalTabTailorActionProvider.kt`
  declares `package com.terminaltabtailor.provider` (singular). `plugin.xml` must reference the *package*.
- **Never touch a service at class-init time** — a past bug (commit 4214834). Every `service<...>()` lookup is behind
  `by lazy { }` in a companion object; keep it that way.
- **Every `ContentManager` mutation must run on the EDT** (`withContext(Dispatchers.EDT)`), including reordering.
  Sorting is implemented as remove-then-re-add of each `Content` — the tests assert exactly that call order.
- Coroutines launch on `GlobalScope` from `invokeLater`, so their ambient dispatcher is `Dispatchers.Default`.
  Anything reading or mutating `ContentManager`/`Content` must therefore sit inside `withContext(Dispatchers.EDT)`.
  Getting this wrong was issue #31: it surfaced as an unhandled-exception error report, not a visible failure.
- Date-tagged tab names use the literal form `name <dd-MM-yy>`; `extractDateUsingSubstring` parses between `<` and `>`
  and the sorter depends on that shape. Changing the delimiters breaks `DESC_DATE` sorting.
- Actions that touch the terminal must be `DumbAware`.

## Tests

`src/test/kotlin` uses JUnit 5 + Mockito with plain mocks of platform interfaces (`ContentManager`, `Content`,
`Project`, `Module`, `VirtualFile`). There is no `BasePlatformTestCase` fixture, so anything requiring a live IDE
application cannot be tested here — that constraint drives where logic should live.

**Testability pattern**: a function that needs settings takes them as a parameter with a default —
`TerminalTabsUtil.sortTabs`, `TerminalTabNamesManager.constructNewTabName`,
`ProjectSpecificLastVirtualFile.getLastVirtualFileForProject`. Kotlin evaluates default arguments in the callee, so
a test passing the value explicitly never triggers the global `service<...>()` lookup and the companion's `by lazy`
stays uninitialised. Follow this for any new logic that reads settings; reaching for the global directly makes the
code impossible to unit-test, which is why the naming rules went untested for so long.

**Mockito trap**: never call a helper that stubs its own mock inside `thenReturn(...)` — build the value into a
local first. Nesting one stubbing inside another throws `UnfinishedStubbingException`. Relatedly,
`VirtualSelectionUtilTest` *looks* like it mocks a static (`ModuleRootManager.getInstance`), which Mockito cannot do
without `mockStatic`; it works only because that call delegates to `module.getComponent(...)`, so Mockito captures
the inner call on the mock instead.

Two known quirks are pinned by tests rather than fixed, with the reasoning in the assertion messages: a dated tab
collides with the same undated prefix (with the date option off, `main` becomes `main (2)` when
`main <03-04-24>` exists), and prefix matching is unbounded (`srcx` counts against `src`).

## Releasing

Bump `version` in `build.gradle.kts` and **replace** the `<change-notes>` CDATA block in
`src/main/resources/META-INF/plugin.xml` — the Marketplace shows only the current version's notes, so they are
replaced, not appended. User-visible feature lists exist in three places that must stay in sync: `README.md`, the
`<description>` in `plugin.xml`, and the bundle properties. The README's defaults table is pinned by
`TerminalTabTailorSettingsTest`, so changing a default breaks that test until the table is updated too.

## Platform coupling

Several classes extend or copy JetBrains internals (`ToolWindowTabRenameActionBase`, `RevealFileInTerminalAction`,
`TerminalToolWindowManager`, `OpenPredefinedTerminalActionProvider`, `TerminalBundle` message keys in `ActionId`).
Files derived from IntelliJ Community carry an Apache-2.0 attribution header — preserve it when editing them.
