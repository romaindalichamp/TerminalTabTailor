package com.terminaltabtailor.managers

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.enums.TabNameType
import com.terminaltabtailor.listeners.ProjectTreeSelectionListener
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import java.text.SimpleDateFormat
import java.util.*

class TerminalTabNamesManager {
    private val settingsService = service<TerminalTabTailorSettingsService>()

    fun renameTerminalTab(project: Project) {
        ToolWindowManager
                .getInstance(project)
                .getToolWindow("Terminal")?.contentManager?.let { terminals ->
                    TerminalTabsUtil
                            .getLastOpenedTab(terminals)
                            ?.let { constructNewTabName(project, terminals, it) }
                }
    }

    private fun constructNewTabName(project: Project, terminals: ContentManager, newTerminalTab: Content) {
        var name = ProjectTreeSelectionListener.lastSelectedVirtualFile.name

        name = when {
            TabNameType.FIRST_DIR_NAME == settingsService.state.selectedTabTypeName
                    && ProjectTreeSelectionListener.lastSelectedVirtualFile.isFile
            -> ProjectTreeSelectionListener.lastSelectedVirtualFileParent.name

            TabNameType.MODULE_NAME == settingsService.state.selectedTabTypeName
                    && ProjectTreeSelectionListener.lastSelectedVirtualFileParentModule != null
            -> ProjectTreeSelectionListener.lastSelectedVirtualFileParentModule?.name ?: project.name

            TabNameType.MODULE_DIR_NAME == settingsService.state.selectedTabTypeName
                    && ProjectTreeSelectionListener.lastSelectedVirtualFileParentModuleDirName != null
            -> ProjectTreeSelectionListener.lastSelectedVirtualFileParentModuleDirName ?: project.name

            TabNameType.PROJECT_NAME == settingsService.state.selectedTabTypeName
            -> project.name

            else -> name
        }

        if (settingsService.state.useCurrentDate) {
            name += " <${SimpleDateFormat(settingsService.state.dateTemplate).format(Date())}>"
        }

        val (newDisplayName, newTabName) =
                TerminalTabsUtil.getNextAvailableTabName(terminals.contents.toList(), name);

        newTerminalTab.displayName = newDisplayName
        newTerminalTab.tabName = newTabName

        if (settingsService.state.performManualRenaming) {
            TerminalTabsUtil.performManualRenamingAction(newTerminalTab)
        }

        if (settingsService.state.ascSort) {
            TerminalTabsUtil.ascSort(project)
        }

        if (settingsService.state.descDateSort) {
            TerminalTabsUtil.descDateSort(
                    project,
                    settingsService.state.dateTemplate)
        }

        TerminalTabsUtil.activateTerminalWindow(project)
    }
}