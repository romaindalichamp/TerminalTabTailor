package com.terminaltabtailor.actions

import com.intellij.ide.actions.ToolWindowTabRenameActionBase
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.terminal.TerminalBundle
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory

class CustomRenameTerminalSessionAction : ToolWindowTabRenameActionBase(
    TerminalToolWindowFactory.TOOL_WINDOW_ID,
    TerminalBundle.message(ActionId.RENAME_SESSION_LABEL_ID)
), DumbAware {

    private val settingsService = service<TerminalTabTailorSettingsService>()
    override fun applyContentDisplayName(
        content: Content,
        project: Project,
        @Nls newContentName: String
    ) {
        super.applyContentDisplayName(content, project, newContentName)

        TerminalTabsUtil.sortTabs(project, settingsService)
        TerminalTabsUtil.selectNewTab(project, newContentName)
        TerminalTabsUtil.activateTerminalWindow(project)
    }
}