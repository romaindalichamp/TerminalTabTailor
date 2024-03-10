package com.terminaltabtailor.util

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.actions.ActionId
import com.terminaltabtailor.enums.TabNameSort
import com.terminaltabtailor.listeners.TerminalActionListener
import com.terminaltabtailor.managers.TerminalTabNamesManager
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


class TerminalTabsUtil {
    companion object {
        private val numberRegex = "\\(\\d+\\)".toRegex()

        fun getLastOpenedTab(terminals: ContentManager): Content? {
            return if (terminals.contentCount > 0) {
                terminals.getContent(terminals.contentCount - 1)
            } else {
                null
            }
        }

        fun getNextAvailableTabName(names: List<Content>, prefix: String): Pair<String, String> {
            val usedNumbers = names.asSequence()
                .filter { it.tabName.startsWith(prefix) }
                .map { tab ->
                    numberRegex.find(tab.tabName)
                        ?.value
                        ?.removeSurrounding("(", ")")
                        ?.toInt() ?: 0
                }.toSortedSet()

            var nextNumber = 1
            while (nextNumber in usedNumbers) {
                nextNumber++
            }

            val prefixWithNumber = "$prefix ($nextNumber)"

            return Pair(if (nextNumber == 1) prefix else prefixWithNumber, prefixWithNumber)
        }

        fun ascSort(project: Project) {
            ToolWindowManager
                .getInstance(project)
                .getToolWindow(ActionId.TOOL_WINDOW_ID)
                ?.contentManager
                ?.let { manager ->
                    manager
                        .contents
                        .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
                        .forEach { content ->
                            manager.removeContent(content, false)
                            manager.addContent(content)
                        }
                }
        }

        fun descDateSort(tabs: ContentManager?, dateFormatter: String) {
            tabs?.let {
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
                }.forEach { content ->
                    tabs.removeContent(content, false)
                    tabs.addContent(content)
                }
            }
        }

        fun performManualRenamingAction(terminalContent: Content) {
            terminalContent.let {
                val renameAction =
                    ActionManager
                        .getInstance()
                        .getAction(ActionId.TERMINAL_RENAME_SESSION_ID)

                val actionEvent =
                    AnActionEvent
                        .createFromDataContext(
                            ActionPlaces.UNKNOWN,
                            Presentation(),
                            DataManager.getInstance().getDataContext(it.component)
                        )

                if (renameAction != null && renameAction.templatePresentation.isEnabledAndVisible) {
                    renameAction.actionPerformed(actionEvent)
                }
            }
        }

        fun activateTerminalWindow(project: Project) {
            ToolWindowManager
                .getInstance(project)
                .getToolWindow(ActionId.TOOL_WINDOW_ID)
                ?.activate(null)
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

        fun renameTab(
            project: Project,
            terminalTabNamesManager: TerminalTabNamesManager,
            virtualSelection: TerminalActionListener.VirtualSelection
        ): Content? {
            return terminalTabNamesManager
                .renameTerminalTab(
                    project,
                    virtualSelection.lastSelectedVirtualFile!!,
                    virtualSelection.lastSelectedVirtualFileParent,
                    virtualSelection.lastSelectedVirtualFileParentModule,
                    virtualSelection.lastSelectedVirtualFileParentModuleDirName
                )
        }

        fun selectNewTab(project: Project, newDisplayName: String) {
            val contentManager: ContentManager? =
                ToolWindowManager
                    .getInstance(project)
                    .getToolWindow(ActionId.TOOL_WINDOW_ID)
                    ?.contentManager

            contentManager?.contents?.forEach { content ->
                if (content.displayName == newDisplayName) {
                    contentManager.setSelectedContent(content)
                }
            }
        }


        fun sortTabs(project: Project, settingsService: TerminalTabTailorSettingsService) {
            when (settingsService.state.selectedTabTypeSort) {
                TabNameSort.ASC -> ascSort(project)
                TabNameSort.DESC_DATE -> descDateSort(
                    ToolWindowManager
                        .getInstance(project)
                        .getToolWindow(ActionId.TOOL_WINDOW_ID)
                        ?.contentManager,
                    settingsService.state.dateTemplate
                )
            }
        }
    }
}