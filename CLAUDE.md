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
IDEA `2025.3`, `sinceBuild` 253, no `untilBuild` (deliberate — avoids re-releasing on every IDE major),
Kotlin 2.1.0, bytecode target 21. Gradle 8.14.5 through the committed wrapper.

Constraints to know before changing any of it:

- **Use `intellijIdea(...)`, not `intellijIdeaCommunity(...)`.** IntelliJ IDEA moved to a single unified
  distribution in 2025.3; Community builds no longer exist, so the IC coordinates cannot resolve at all
  (`Could not find idea:ideaIC:2025.3`). This also requires platform plugin 2.10.4 or newer.
- **Do not go back to `org.jetbrains.intellij` 1.x.** It cannot read 2025.x distribution layouts and fails with
  `IndexOutOfBoundsException` in `Utils.resolveIdeHomeVariable`.
- Platform plugin 2.10.5 requires **Gradle 8.13+**, and 2.18+ requires Gradle 9. The wrapper is committed so that
  requirement travels with the repo — `.gitignore` deliberately un-ignores `gradle/wrapper/gradle-wrapper.jar`.
- **Bytecode target must stay at 21 or lower**: the IDE runs on JBR 21, so anything higher fails to load with
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
  (`constructNewTabName`, driven by `TabNameTypeEnum` + optional ` <date>` suffix), then create/sort/select/focus.
  Tabs are created through `TerminalToolWindowTabsManager.createTabBuilder()`, which is the **only** creation API
  that honours the engine chosen in Settings > Tools > Terminal — every older API returns a classic terminal
  whatever the user picked. It is `@ApiStatus.Experimental`, so a platform bump may require adjusting this call.
- **`TerminalTabsUtil`** holds all `ContentManager` manipulation and all pure logic (name numbering, sorting,
  date extraction). It is the layer that unit tests exercise — keep new logic here, not in the manager or actions.
- **`ProjectSpecificLastVirtualFile`** keeps three separate `WeakHashMap`s (editor / project-tree / mixed) and
  picks one based on `TabNameOriginEnum`. `PROJECT_NAME` bypasses the maps and returns `project.guessProjectDir()`.
- **`StartupActivity`** (postStartupActivity) swaps the platform's `Terminal.RenameSession`,
  `Terminal.OpenInTerminal` and `Terminal.OpenInReworkedTerminal` actions for the plugin's subclasses via
  `ActionManager.replaceAction`. This is why the plugin affects the *stock* "Open in Terminal" everywhere, not
  just its own menu items. From 2025.3 the platform registers **one such action per terminal engine**, and the
  plugin takes over both, relabelling them "Terminal (Reworked)" / "Terminal (Classic)".
  `replaceAction` also loses the icon declared in the platform's `plugin.xml`, hence the one set in `init`.
- **Neither creation API follows the "Terminal engine" setting.** `TerminalToolWindowTabsManager` lives in the
  reworked frontend and always creates a reworked tab; `TerminalToolWindowManager.openTerminalIn` always creates
  a classic one. That is why the engine is an explicit menu choice rather than something the plugin infers —
  do not "fix" this by reading `TerminalOptionsProvider`, it changes nothing about what gets created.
- **`CurrentDirectoryTabNameTracker`** (project service, started by `StartupActivity`) backs the "keep the name
  of the current folder" option: it renames tabs as their shell `cd`s around. It **polls** on the service's own
  coroutine scope because neither engine publishes the working directory — `TerminalView.getCurrentDirectory()`
  and `TerminalWidget.getCurrentDirectory()` are plain getters, and the change callbacks
  (`TerminalWorkingDirectoryTracker`, `TerminalSessionModel.terminalState`) sit behind backend/impl classes. Note
  both are *functions*, not Kotlin properties: `view.currentDirectory` does not compile. A tab whose display name
  stops matching what the tracker last wrote is treated as user-renamed and dropped, which is what keeps
  "Rename Session" meaningful while the option is on.
- **The two engines answer `getCurrentDirectory()` from different places**, and it decides which shells can be
  followed. Reworked reads `TerminalSessionModel.terminalState.currentDirectory`, i.e. what the shell pushes
  through shell integration — correct even inside a WSL distro. Classic goes to
  `TerminalWorkingDirectoryManager` → `ProcessInfoUtil` → `WinConPtyProcess.getWorkingDirectory()`, an OS query
  about the spawned pty process: right for PowerShell/cmd, meaningless for `wsl.exe`, whose Windows-side
  directory never follows the shell in the distro. Hence `TerminalTabsUtil.canFollowWorkingDirectory`, a
  **whitelist**: `powershell`/`pwsh`/`cmd` on Windows, `bash`/`zsh`/`sh`/`fish`/`dash`/`ksh` elsewhere. Anything
  else keeps the name it opened with. It is a whitelist rather than a blacklist of launchers because the list of
  things that *don't* work is open-ended (WSL, per-distro launchers, ssh, docker, every MSYS/Cygwin port) while
  what does work is short and verified. Host-dependent, so `isWindows` is a parameter with a
  `SystemInfo.isWindows` default, per the testability pattern below.

  Verified from a sandbox log, not assumed: a Git Bash tab reported its launch directory unchanged for 35
  minutes while reworked PowerShell tabs moved normally. There is **no** public API exposing the shell-integration
  directory of a classic tab: `TerminalWorkingDirectoryManager.getWorkingDirectory(Content)` is package-private,
  every public overload funnels into the OS query, and `TerminalWidget.getSession()` throws
  `UnsupportedOperationException`. **A reworked tab exposes no widget at all**, so shell integration is its only
  source: `resolveFromEngine` must consult it *before* looking for a widget, and must not gate it on the shell
  name (the shell's own report is right whatever the shell). Requiring a widget first regressed every reworked
  tab to "known to neither engine". Report the engine (`findTabByContent != null`) separately from whichever
  source answered, or the diagnostics lie. The backend *does* keep
  a per-session directory with a heuristic fallback for exactly that case
  (`TerminalTabsManager.getTerminalTabs()` → `TerminalSessionTab.workingDirectory`) — but `TerminalTabsManager`
  is Kotlin-`internal`, so it does not compile from a plugin. Don't be fooled by `javap`: `internal` shows up as
  public JVM bytecode. Both sources are chained anyway, never early-returned, since a tab may answer from either.
- **IntelliJ injects no shell integration for Git Bash on Windows**, so nothing can follow such a tab — verified,
  not guessed. `LocalTerminalDirectRunner` always calls `LocalShellIntegrationInjector.injectShellIntegration`,
  which derives the shell name with `PathUtil.getFileName` — `bash.exe` — and matches it through
  `ShellNameUtil.isBash`, whose literals are `bash`/`sh` with **no `.exe` variant** (only `pwsh` has one). So
  `addBashRcFileArgument` never runs. The sandbox log shows it plainly: PowerShell starts as
  `[powershell.exe, …, -File, …\powershell-integration.ps1]` while bash starts as bare
  `[C:\Program Files\Git\bin\bash.exe]`. Dropping the `.exe` from the configured shell path is the likely
  workaround, and normalising it in the plugin's own `TerminalToolWindowTabBuilder.shellCommand(...)` would be
  the code equivalent — deliberately *not* done, as it rewrites what the IDE executes to route around a platform
  bug that a future release may simply fix.
- That OS query **blocks up to 2 seconds** (`CompletableFuture.get(2000, MILLISECONDS)`), so the tracker reads
  directories on `Dispatchers.IO` and hops to the EDT only to write names. Never poll it from the EDT.
- **Predefined shells (the `+` dropdown) already follow the engine setting.** The platform ships two
  `TerminalNewPredefinedSessionAction`s — `org.jetbrains.plugins.terminal.action` (classic) and
  `com.intellij.terminal.frontend.action` (reworked, whose `OpenShellAction` calls
  `TerminalInternalUtilsKt.createTerminalTab`). Don't add predefined-shell entries through
  `openPredefinedTerminalProvider` to "make them reworked": they already are, and the plugin's own provider
  would only duplicate the list. The EP is declared by the terminal plugin but implemented by nobody in it.
- **Settings** are an application-level `PersistentStateComponent` (`TerminalTabTailorSettingsService` →
  `TerminalTabTailorSettings.xml`), rendered by `TerminalTabTailorConfigurable` with the Kotlin UI DSL. All user-facing
  strings live in `src/main/resources/TerminalTabTailorBundle.properties`.

## Conventions and pitfalls

- **Package ≠ directory in one place**: `src/main/kotlin/.../providers/OpenPredefinedTerminalTabTailorActionProvider.kt`
  declares `package com.terminaltabtailor.provider` (singular). `plugin.xml` must reference the *package*.
- **Never touch a service at class-init time** — a past bug (commit 4214834). Every `service<...>()` lookup is behind
  `by lazy { }` in a companion object; keep it that way.
- **Sorting must restore both the selection and the keyboard focus.** The remove-then-re-add cycle hands the
  selection to a neighbour *and* takes focus out of the terminal; `setSelectedContent(content)` alone restores
  neither, leaving the user to click back into the command line. `keepingSelection` therefore calls
  `setSelectedContent(content, hadFocus)` — and `hadFocus` is captured **before** the sort, so a tab renaming
  itself while the user types in the editor never steals focus. `isFocused` is a `sortTabs` parameter defaulting
  to `holdsKeyboardFocus`, per the testability pattern below.
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
