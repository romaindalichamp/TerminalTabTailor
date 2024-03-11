package com.terminaltabtailor.managers

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.terminaltabtailor.actions.ActionId
import com.terminaltabtailor.enums.TabNameType
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TerminalTabNamesManager {
    private val settingsService = service<TerminalTabTailorSettingsService>()

    suspend fun manageNewTab(
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

        val newTerminalTabContent: Content? =
            TerminalTabsUtil.getLastOpenedTab(terminalContentManager)

        newTerminalTabContent?.let {
            var newTerminalTab = it

            val constructedName: String =
                constructNewTabName(
                    project,
                    lastSelectedVirtualFile,
                    lastSelectedVirtualFileParent,
                    lastSelectedVirtualFileParentModule,
                    lastSelectedVirtualFileParentModuleDirName
                )

            var alreadyExistingTerminalTab: Content? = null

            if (settingsService.state.alreadyExists) {
                alreadyExistingTerminalTab =
                    TerminalTabsUtil.alreadyExistingTerminalTab(
                        terminalContentManager.contents.toList(),
                        constructedName
                    )
                if (alreadyExistingTerminalTab != null) {
                    withContext(Dispatchers.EDT) {
                        TerminalTabsUtil.removeJustCreatedTerminalTab(
                            terminalContentManager,
                            newTerminalTab.displayName
                        )
                        newTerminalTab = alreadyExistingTerminalTab
                    }
                }
            }

            if (!settingsService.state.alreadyExists
                || (settingsService.state.alreadyExists && alreadyExistingTerminalTab == null)
            ) {
                val (newDisplayName, newTabName) = TerminalTabsUtil.incrementNumberInName(
                    terminalContentManager.contents.toList(),
                    constructedName
                )
                newTerminalTab.displayName = newDisplayName
                newTerminalTab.tabName = newTabName
            }

            withContext(Dispatchers.EDT) {
                TerminalTabsUtil.sortTabs(project, settingsService)
                TerminalTabsUtil.selectNewTab(project, newTerminalTab.displayName)
                TerminalTabsUtil.activateTerminalWindow(project)

                if (settingsService.state.performManualRenaming) {
                    TerminalTabsUtil.performManualRenamingAction(newTerminalTab)
                }
            }
        }

        return newTerminalTabContent
    }

    private fun constructNewTabName(
        project: Project,
        lastSelectedVirtualFile: VirtualFile,
        lastSelectedVirtualFileParent: VirtualFile?,
        lastSelectedVirtualFileParentModule: Module?,
        lastSelectedVirtualFileParentModuleDirName: String?,
    ): String {
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

        return name
    }
}