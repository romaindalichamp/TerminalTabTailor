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
./gradlew verifyPlugin                                # plugin structure/compatibility verification
```

`signPlugin`/`publishPlugin` read `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN`
from the environment. There is no CI workflow in the repo — releases are driven locally.

Platform targets live in `build.gradle.kts`: IC `2024.3.1`, `sinceBuild` 243, no `untilBuild` (deliberate — avoids
re-releasing on every IDE major), Java/Kotlin bytecode target 21. Bumping the platform version usually means
re-checking the JetBrains APIs listed under "Platform coupling" below.

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
- Coroutines currently launch on `GlobalScope` from `invokeLater`. Exceptions escaping these coroutines surface as
  IDE error reports; wrap fallible work rather than letting it reach the default handler.
- Date-tagged tab names use the literal form `name <dd-MM-yy>`; `extractDateUsingSubstring` parses between `<` and `>`
  and the sorter depends on that shape. Changing the delimiters breaks `DESC_DATE` sorting.
- Actions that touch the terminal must be `DumbAware`.

## Tests

`src/test/kotlin` uses JUnit 5 + Mockito with plain mocks of platform interfaces (`ContentManager`, `Content`,
`Project`) — there is no `BasePlatformTestCase` fixture, so anything requiring a live IDE application cannot be
tested here. That constraint is the main reason logic belongs in `TerminalTabsUtil` / `VirtualSelectionUtil`.

## Releasing

Bump `version` in `build.gradle.kts` and update the `<change-notes>` CDATA block in
`src/main/resources/META-INF/plugin.xml`; user-visible feature lists exist in three places that must stay in sync —
`README.md`, the `<description>` in `plugin.xml`, and the bundle properties.

## Platform coupling

Several classes extend or copy JetBrains internals (`ToolWindowTabRenameActionBase`, `RevealFileInTerminalAction`,
`TerminalToolWindowManager`, `OpenPredefinedTerminalActionProvider`, `TerminalBundle` message keys in `ActionId`).
Files derived from IntelliJ Community carry an Apache-2.0 attribution header — preserve it when editing them.
