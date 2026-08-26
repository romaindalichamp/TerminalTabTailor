package com.terminaltabtailor.util

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.action.ActionId
import com.terminaltabtailor.enum.TabNameSortEnum
import com.terminaltabtailor.enum.TabNameTypeEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettings
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import java.awt.KeyboardFocusManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.SwingUtilities


class TerminalTabsUtil {
    companion object {
        private val numberRegex = "\\(\\d+\\)".toRegex()

        /*
         * The literal `name <date>` shape the DESC_DATE sorter parses back out through
         * extractDateUsingSubstring — changing the delimiters here breaks that sort.
         */
        fun appendDate(
            name: String,
            settings: TerminalTabTailorSettings,
            now: Date = Date(),
        ): String =
            if (settings.useCurrentDate) {
                "$name <${SimpleDateFormat(settings.dateTemplate).format(now)}>"
            } else {
                name
            }

        /**
         * The only Windows shells whose working directory can be read as it changes. Everything else
         * either hides its shell behind a launcher (WSL, ssh, docker), keeps an emulated directory the
         * Win32 process never learns about (Git Bash, MSYS, Cygwin), or — powershell and pwsh — never
         * pushes its location to the OS in the first place.
         *
         * Verified end to end, not assumed: a classic PowerShell tab's diagnostic ("the OS reports
         * '...'") never changed across ~180 poll ticks after a `cd`, and every layer between this
         * plugin and the OS query was decompiled to rule out caching — TerminalWorkingDirectoryManager,
         * ProcessInfoUtil, WinConPtyProcess, all query fresh every call, no cache anywhere. The actual
         * cause is upstream, in PowerShell itself: `Set-Location` never calls Win32's
         * `SetCurrentDirectory()`, because PowerShell's `$PWD` is tracked by its own provider system,
         * entirely separate from the OS-level working directory this whitelist exists to gate reading —
         * see https://github.com/PowerShell/PowerShell/issues/17149 (open, unresolved upstream). Any
         * tool reading a PowerShell process's OS-level cwd hits the same wall; there is no read on our
         * side that would fix it. `cmd.exe`'s `cd` does not have this problem — it has always called
         * `SetCurrentDirectory()` — so it stays whitelisted.
         */
        private val FOLLOWABLE_WINDOWS_SHELLS = setOf("cmd")

        /** On a Unix host the shell is the process itself, and `/proc/<pid>/cwd` follows every `cd`. */
        private val FOLLOWABLE_UNIX_SHELLS = setOf("bash", "sh", "zsh", "fish", "dash", "ksh")

        /*
         * Whether a tab's shell reports a working directory that actually follows it. Naming a tab
         * after anything else would show a folder the shell left long ago, so such a tab is left with
         * the name it was opened under.
         */
        fun canFollowWorkingDirectory(
            shellCommand: List<String>?,
            isWindows: Boolean = SystemInfo.isWindows,
        ): Boolean {
            // Lowercased because Windows spells the very same shell cmd.exe or CMD.EXE, and both
            // removeSuffix and set lookups are case sensitive.
            val name = shellCommand
                ?.firstOrNull()
                ?.lowercase(Locale.getDefault())
                ?.substringAfterLast('\\')
                ?.substringAfterLast('/')
                ?.removeSuffix(".exe")
                ?: return false

            return name in if (isWindows) FOLLOWABLE_WINDOWS_SHELLS else FOLLOWABLE_UNIX_SHELLS
        }

        /*
         * The last `parents + 1` segments of a shell working directory, joined by '/' — so 0 names a
         * tab after the folder alone and 2 after `grandparent/parent/child`. A path with fewer
         * segments than asked for simply yields all it has.
         *
         * The separator is whatever the shell reports, not necessarily the one of the IDE's host OS
         * (a WSL or Docker terminal on Windows reports '/'), so both are split on rather than going
         * through java.nio.file.Path. The result always joins with '/', which reads as a path on
         * every platform and keeps tab names stable whatever the shell.
         */
        fun directoryNameOf(currentDirectory: String, parents: Int = 0): String? {
            if (currentDirectory.isBlank()) return null

            val trimmed = currentDirectory.trim().trimEnd('/', '\\')
            // The path was the filesystem root itself, whose only content is the separator.
            if (trimmed.isEmpty()) return "/"

            val segments = trimmed.split('/', '\\').filter { it.isNotEmpty() }
            // A Windows drive root ("C:\") leaves no segment behind; keep the drive letter.
            if (segments.isEmpty()) return trimmed

            return segments
                .takeLast(parents.coerceAtLeast(0) + 1)
                .joinToString("/")
        }

        /*
         * What a tab following its shell should be called, or null when the naming style is not
         * CURRENT_DIRECTORY_NAME — every other style is static, computed once when the tab opens,
         * and keeps that name for its whole life rather than being renamed into something its style
         * never asked for.
         *
         * "Follow the shell's current folder" is its own naming style now, not a modifier of the
         * other five: FILE_NAME, FIRST_DIR_NAME, MODULE_NAME, MODULE_DIR_NAME and PROJECT_NAME all
         * decline, and CURRENT_DIRECTORY_NAME alone follows, by running directoryNameOf on the
         * shell's current directory — the very computation FIRST_DIR_NAME uses once, at open time
         * (see TerminalTabNamesManager.constructNewTabName). That reuse is what keeps a freshly
         * opened CURRENT_DIRECTORY_NAME tab from being renamed the moment the tracker's first poll
         * runs, and what keeps tab reuse matching: alreadyExistingTerminalTab compares against the
         * exact name constructNewTabName would build for the shell's present directory.
         *
         * Exhaustive on purpose, with no `else` — so a future naming style must be explicitly decided
         * here rather than silently inheriting null (never following) by accident.
         */
        fun followedName(
            nameType: TabNameTypeEnum,
            currentDirectory: String,
            parents: Int = 0,
        ): String? = when (nameType) {
            TabNameTypeEnum.FILE_NAME,
            TabNameTypeEnum.FIRST_DIR_NAME,
            TabNameTypeEnum.MODULE_NAME,
            TabNameTypeEnum.MODULE_DIR_NAME,
            TabNameTypeEnum.PROJECT_NAME -> null

            TabNameTypeEnum.CURRENT_DIRECTORY_NAME -> directoryNameOf(currentDirectory, parents)
        }

        fun getLastOpenedTab(terminals: ContentManager): Content? {
            return if (terminals.contentCount > 0) {
                terminals.getContent(terminals.contentCount - 1)
            } else {
                null
            }
        }

        fun incrementNumberInName(names: List<Content>, prefix: String): String {
            val usedNumbers = names.asSequence()
                .filter { it.displayName.startsWith(prefix) }
                .map { tab ->
                    numberRegex.find(tab.displayName)
                        ?.value
                        ?.removeSurrounding("(", ")")
                        ?.toInt() ?: 1
                }.toSortedSet()
            var nextNumber = 1
            while (nextNumber in usedNumbers) {
                nextNumber++
            }

            val prefixWithNumber = "$prefix ($nextNumber)"

            return if (nextNumber == 1 || usedNumbers.isEmpty()) prefix else prefixWithNumber
        }

        private fun ascSort(manager: ContentManager) {
            applyOrder(
                manager,
                manager.contents.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
            )
        }

        /*
         * Reordering is a remove-then-re-add of every tab, which walks the ContentManager down to zero
         * contents when there is only one tab — and the terminal tool window collapses the moment its
         * last content goes. Callers that follow a sort with activateTerminalWindow never noticed;
         * a tab renaming itself after the shell's working directory does not want to reopen anything.
         *
         * Skipping the no-op case also spares the strip a full rebuild whenever a rename leaves the
         * order untouched.
         */
        private fun applyOrder(manager: ContentManager, sortedContents: List<Content>) {
            if (sortedContents == manager.contents.toList()) return

            sortedContents.forEach { content ->
                manager.removeContent(content, false)
                manager.addContent(content)
            }
        }

        private fun descDateSort(tabs: ContentManager, dateFormatter: String) {
            applyOrder(
                tabs,
                tabs.contents.sortedWith { previous, next ->
                    val previousDisplayName = previous.displayName.lowercase(Locale.getDefault())
                    val nextDisplayName = next.displayName.lowercase(Locale.getDefault())

                    val previousDate = extractDateUsingSubstring(previousDisplayName, dateFormatter)
                    val nextDate = extractDateUsingSubstring(nextDisplayName, dateFormatter)

                    when {
                        previousDate != null && nextDate != null -> {
                            val dateComparison = nextDate.compareTo(previousDate)
                            if (dateComparison != 0) dateComparison else previousDisplayName.compareTo(
                                nextDisplayName
                            )
                        }

                        previousDate != null && nextDate == null -> -1
                        previousDate == null && nextDate != null -> 1
                        else -> previousDisplayName.compareTo(nextDisplayName)
                    }
                }
            )
        }

        /*
         * Deferred to a fresh EDT event: sortTabs, which every caller runs immediately before this,
         * updates the ContentManager's model synchronously through remove-then-re-add, but Swing only
         * *queues* the resulting revalidate and repaint of the tab strip rather than applying it
         * inline. Triggering the rename popup synchronously right after reads the tab's on-screen
         * bounds before they have caught up, so it opens wherever the tab used to sit rather than
         * where sorting just moved it to.
         */
        fun performManualRenamingAction(terminalContent: Content) {
            SwingUtilities.invokeLater {
                val renameAction =
                    ActionManager
                        .getInstance()
                        .getAction(ActionId.TERMINAL_RENAME_SESSION_ID)

                val actionEvent = AnActionEvent.createEvent(
                    DataManager.getInstance().getDataContext(terminalContent.component),
                    Presentation(),
                    ActionPlaces.UNKNOWN,
                    ActionUiKind.POPUP,
                    null)

                if (renameAction != null && renameAction.templatePresentation.isEnabledAndVisible) {
                    ActionUtil.performActionDumbAwareWithCallbacks(renameAction, actionEvent)
                }
            }
        }

        fun activateTerminalWindow(project: Project) {
            getTerminalToolWindow(project)?.activate(null)
        }

        private fun extractDateUsingSubstring(input: String, pattern: String): LocalDate? {
            val startIndex = input.indexOf('<') + 1
            val endIndex = input.indexOf('>')

            return try {
                if (startIndex in 1..<endIndex) {
                    LocalDate.parse(
                        input.substring(startIndex, endIndex),
                        DateTimeFormatter.ofPattern(pattern)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        fun selectNewTab(contentManager: ContentManager, newDisplayName: String) {
            contentManager.contents.forEach { content ->
                if (content.displayName == newDisplayName) {
                    contentManager.setSelectedContent(content)
                }
            }
        }

        /*
         * `isFocused` is a parameter rather than a direct read of the keyboard focus so that the
         * selection rules can be exercised without a running IDE. Production callers omit it.
         */
        fun sortTabs(
            contentManager: ContentManager,
            settingsService: TerminalTabTailorSettingsService,
            isFocused: (Content) -> Boolean = ::holdsKeyboardFocus,
        ) {
            when (settingsService.state.selectedTabTypeSort) {
                // Returns before touching the manager at all, so nothing is selected either.
                TabNameSortEnum.NO_SORT -> return

                TabNameSortEnum.ASC -> keepingSelection(contentManager, isFocused) {
                    ascSort(contentManager)
                }

                TabNameSortEnum.DESC_DATE -> keepingSelection(contentManager, isFocused) {
                    descDateSort(contentManager, settingsService.state.dateTemplate)
                }
            }
        }

        /*
         * Sorting removes and re-adds every Content, and removing the selected one makes the
         * ContentManager fall back to a neighbour — so a sort silently moves the user somewhere else.
         * That matters most when the sort was not asked for: a tab renaming itself after the shell's
         * working directory re-sorts the strip under a user who is typing in it.
         *
         * Re-selecting is not enough on its own: the same cycle takes keyboard focus out of the
         * terminal, and setSelectedContent leaves it out unless asked, which forces the user to click
         * back into the command line to carry on typing. Focus is therefore asked back — but only
         * when the terminal held it to begin with, so a rename happening while the user types in the
         * editor never yanks them into the terminal.
         *
         * Callers that do want a *different* tab selected — opening, reusing, renaming — say so with
         * selectNewTab right after, which overrides this.
         */
        private fun keepingSelection(
            contentManager: ContentManager,
            isFocused: (Content) -> Boolean,
            sort: () -> Unit,
        ) {
            val selectedContent = contentManager.selectedContent
            val hadFocus = selectedContent != null && isFocused(selectedContent)

            sort()

            selectedContent?.let { contentManager.setSelectedContent(it, hadFocus) }
        }

        private fun holdsKeyboardFocus(content: Content): Boolean {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                ?: return false
            val component = content.component ?: return false

            return SwingUtilities.isDescendingFrom(focusOwner, component)
        }

        fun alreadyExistingTerminalTab(
            terminalTabs: List<Content>,
            constructedName: String
        ): Content? {
            return terminalTabs.firstOrNull { it.displayName == constructedName }
        }

        fun getTerminalToolWindow(project: Project): ToolWindow? {
            return ToolWindowManager
                .getInstance(project)
                .getToolWindow(ActionId.TOOL_WINDOW_ID)
        }
    }
}