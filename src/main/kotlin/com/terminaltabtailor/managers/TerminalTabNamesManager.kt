package com.terminaltabtailor.managers

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.actions.ActionId
import com.terminaltabtailor.enums.TabNameType
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import java.text.SimpleDateFormat
import java.util.*

class TerminalTabNamesManager {
    private val settingsService = service<TerminalTabTailorSettingsService>()

    fun renameTerminalTab(
        project: Project,
        lastSelectedVirtualFile: VirtualFile,
        lastSelectedVirtualFileParent: VirtualFile?,
        lastSelectedVirtualFileParentModule: Module?,
        lastSelectedVirtualFileParentModuleDirName: String?
    ): Content? {
        val terminalContentManager =
            ToolWindowManager
                .getInstance(project)
                .getToolWindow(ActionId.TOOL_WINDOW_ID)
                ?.contentManager
                ?: return null

        return constructNewTabName(
            project,
            terminalContentManager,
            TerminalTabsUtil.getLastOpenedTab(terminalContentManager) ?: return null,
            lastSelectedVirtualFile,
            lastSelectedVirtualFileParent,
            lastSelectedVirtualFileParentModule,
            lastSelectedVirtualFileParentModuleDirName
        )
    }

    private fun constructNewTabName(
        project: Project,
        terminals: ContentManager,
        newTerminalTab: Content,
        lastSelectedVirtualFile: VirtualFile,
        lastSelectedVirtualFileParent: VirtualFile?,
        lastSelectedVirtualFileParentModule: Module?,
        lastSelectedVirtualFileParentModuleDirName: String?,
    ): Content {
        var name = lastSelectedVirtualFile.name

        name = when {
            settingsService.state.selectedTabTypeName == TabNameType.FIRST_DIR_NAME
                    && lastSelectedVirtualFile.isFile
            -> lastSelectedVirtualFileParent?.name ?: project.name

            settingsService.state.selectedTabTypeName == TabNameType.MODULE_NAME
            -> lastSelectedVirtualFileParentModule?.name ?: project.name

            settingsService.state.selectedTabTypeName == TabNameType.MODULE_DIR_NAME
            -> lastSelectedVirtualFileParentModuleDirName
                ?: lastSelectedVirtualFileParentModule?.name
                ?: project.name

            settingsService.state.selectedTabTypeName == TabNameType.PROJECT_NAME
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

        return newTerminalTab
    }
}