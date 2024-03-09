package com.terminaltabtailor.managers

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
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
                .getToolWindow("Terminal")
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
//        TerminalTabsUtil.activateTerminalWindow(project)

        // 1. Initialise the name with the selected VirtualFile name
        var name = lastSelectedVirtualFile.name

        // 2. Change the name with another parent name depending on the option selected
        name = when {
            TabNameType.FIRST_DIR_NAME == settingsService.state.selectedTabTypeName
                    && lastSelectedVirtualFile.isFile
            -> lastSelectedVirtualFileParent?.name ?: project.name

            TabNameType.MODULE_NAME == settingsService.state.selectedTabTypeName
            -> lastSelectedVirtualFileParentModule?.name ?: project.name

            TabNameType.MODULE_DIR_NAME == settingsService.state.selectedTabTypeName
            -> lastSelectedVirtualFileParentModuleDirName
                ?: lastSelectedVirtualFileParentModule?.name
                ?: project.name

            TabNameType.PROJECT_NAME == settingsService.state.selectedTabTypeName
            -> project.name

            else -> name
        }

        // 3. Add the current date if selected
        if (settingsService.state.useCurrentDate) {
            name += " <${SimpleDateFormat(settingsService.state.dateTemplate).format(Date())}>"
        }

        // 4. Add a number to the tab name to make it unique
        val (newDisplayName, newTabName) =
            TerminalTabsUtil.getNextAvailableTabName(terminals.contents.toList(), name);

        newTerminalTab.displayName = newDisplayName
        newTerminalTab.tabName = newTabName

        return newTerminalTab
    }
}