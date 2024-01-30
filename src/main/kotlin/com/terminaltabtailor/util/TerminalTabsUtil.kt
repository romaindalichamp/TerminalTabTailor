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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.swing.SwingUtilities

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
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
            val contentManager = toolWindow?.contentManager

            contentManager?.let { manager ->
                manager
                        .contents
                        .sortedBy { it.displayName }
                        .forEach { content ->
                            manager.removeContent(content, false)
                            manager.addContent(content)
                        }
            }
        }

        fun descDateSort(project: Project, pattern: String) {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
            val contentManager = toolWindow?.contentManager

            contentManager?.let { manager ->
                manager
                        .contents
                        .sortedByDescending { extractDateUsingSubstring(it.displayName, pattern) }
                        .forEach { content ->
                            manager.removeContent(content, false)
                            manager.addContent(content)
                        }
            }
        }

        fun performManualRenamingAction(terminalContent: Content) {
            SwingUtilities.invokeLater {
                terminalContent.let {
                    val renameAction =
                            ActionManager
                                    .getInstance()
                                    .getAction("Terminal.RenameSession")

                    val actionEvent =
                            AnActionEvent
                                    .createFromDataContext(
                                            ActionPlaces.UNKNOWN,
                                            Presentation(),
                                            DataManager.getInstance().getDataContext(it.component))

                    if (renameAction != null && renameAction.templatePresentation.isEnabledAndVisible) {
                        renameAction.actionPerformed(actionEvent)
                    }
                }
            }
        }

        fun activateTerminalWindow(project: Project) {
            ToolWindowManager
                    .getInstance(project)
                    .getToolWindow("Terminal")
                    ?.activate(null)
        }

        private fun extractDateUsingSubstring(input: String, pattern: String): LocalDate? {
            val startIndex = input.indexOf('<') + 1
            val endIndex = input.indexOf('>')
            return if (startIndex > 0 && endIndex > startIndex) {
                LocalDate.parse(input.substring(startIndex, endIndex), DateTimeFormatter.ofPattern(pattern))
            } else {
                null
            }
        }
    }
}