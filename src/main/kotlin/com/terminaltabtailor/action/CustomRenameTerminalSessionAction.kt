package com.terminaltabtailor.action

import com.intellij.ide.actions.ToolWindowTabRenameActionBase
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import kotlinx.coroutines.*
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.terminal.TerminalBundle
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

/**
 * This file includes software developed by JetBrains ([https://www.jetbrains.com](https://www.jetbrains.com/)) and its contributors
 *
 * Parts of this file are based on "[RenameTerminalSessionAction.kt](https://github.com/JetBrains/intellij-community/blob/499041ac11143873afc214497627952983a94d3a/plugins/terminal/src/org/jetbrains/plugins/terminal/action/RenameTerminalSessionAction.kt)" from the IntelliJ IDEA Community Edition ([GitHub](https://github.com/JetBrains/intellij-community)), distributed under the Apache License 2.0.
 *
 * ## Custom RenameTerminalSessionAction
 *
 * This custom action serves as a substitute for the original "RenameTerminalSessionAction" and retains its core functionality while integrating a coroutine to enhance its capabilities.
 *
 * Specifically, it aims to:
 * - Organize tabs in accordance with the user's selected preferences for this plugin.
 * - Shift focus to the newly activated tab.
 * - Bring the terminal window to the foreground.
 *
 * This enhanced action can be triggered from two distinct sources:
 * - Traditionally, by right-clicking on a terminal tab and selecting "Rename Session" from the menu.
 * - Automatically, through the plugin, if the user has enabled the option to "Prompt the renaming dialogue each time a new terminal tab is opened."
 */
class CustomRenameTerminalSessionAction(
    actionId: String = ActionId.TOOL_WINDOW_ID,
    text: String = TerminalBundle.message(ActionId.RENAME_SESSION_LABEL_ID)
) : ToolWindowTabRenameActionBase(
    actionId,
    text
), DumbAware {
    private val settingsService = service<TerminalTabTailorSettingsService>()


    @OptIn(DelicateCoroutinesApi::class)
    override fun applyContentDisplayName(
        content: Content,
        project: Project,
        @Nls newContentName: String
    ) {
        val contentManager = TerminalTabsUtil.getTerminalToolWindow(project)?.contentManager

        TerminalToolWindowManager.findWidgetByContent(content)?.terminalTitle?.change {
            userDefinedTitle = newContentName
        }
        content.displayName = newContentName
        content.tabName = newContentName

        GlobalScope.launch {
            withContext(Dispatchers.EDT) {
                contentManager?.let {
                    TerminalTabsUtil.sortTabs(it, settingsService)
                    TerminalTabsUtil.selectNewTab(it, newContentName)
                    TerminalTabsUtil.activateTerminalWindow(project)
                }
            }
        }
    }

    companion object {
        const val ACTION_ID: String = "CustomRenameTerminalSessionAction"
    }
}