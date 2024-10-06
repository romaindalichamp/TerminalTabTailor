package com.terminaltabtailor.manager

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.data.VirtualSelection
import com.terminaltabtailor.enum.TabNameTypeEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import com.terminaltabtailor.util.VirtualSelectionUtil
import kotlinx.coroutines.*
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.text.SimpleDateFormat
import java.util.*

class TerminalTabNamesManager {
    companion object {
        private val settingsService by lazy {
            service<TerminalTabTailorSettingsService>()
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun openTabInTerminal(e: AnActionEvent) {
            e.project?.let { project ->
                VirtualSelectionUtil.getSelectedFile(e, project)?.let { selectedFile ->
                    TerminalTabsUtil.getTerminalToolWindow(project)?.contentManager?.let { terminalToolWindowContentManger ->

                        ApplicationManager.getApplication().invokeLater {
                            GlobalScope.launch {
                                if (!reuseExistingTab(
                                        project,
                                        terminalToolWindowContentManger,
                                        selectedFile
                                    )
                                ) {
                                    withContext(Dispatchers.EDT) {
                                        TerminalToolWindowManager.getInstance(project).openTerminalIn(selectedFile)
                                    }
                                    renameNewTab(
                                        project,
                                        terminalToolWindowContentManger,
                                        selectedFile
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        suspend fun reuseExistingTab(
            project: Project,
            terminalToolWindowContentManger: ContentManager,
            selectedFile: VirtualFile
        ): Boolean {
            var tabExists = false

            val virtualSelection: VirtualSelection = VirtualSelectionUtil.getVirtualSelection(project, selectedFile)

            virtualSelection.lastSelectedVirtualFile?.let {
                tabExists = findExistingTab(
                    terminalToolWindowContentManger,
                    project,
                    virtualSelection.lastSelectedVirtualFile!!,
                    virtualSelection.lastSelectedVirtualFileParent,
                    virtualSelection.lastSelectedVirtualFileParentModule,
                    virtualSelection.lastSelectedVirtualFileParentModuleDirName
                )
            }


            return tabExists
        }

        suspend fun renameNewTab(
            project: Project,
            terminalToolWindowContentManger: ContentManager,
            selectedFile: VirtualFile
        ) {

            val virtualSelection: VirtualSelection = VirtualSelectionUtil.getVirtualSelection(project, selectedFile)

            virtualSelection.lastSelectedVirtualFile?.let {

                renameNewTab(
                    terminalToolWindowContentManger,
                    project,
                    virtualSelection.lastSelectedVirtualFile!!,
                    virtualSelection.lastSelectedVirtualFileParent,
                    virtualSelection.lastSelectedVirtualFileParentModule,
                    virtualSelection.lastSelectedVirtualFileParentModuleDirName
                )
            }


        }

        private suspend fun findExistingTab(
            terminalToolWindowContentManger: ContentManager,
            project: Project,
            lastSelectedVirtualFile: VirtualFile,
            lastSelectedVirtualFileParent: VirtualFile?,
            lastSelectedVirtualFileParentModule: Module?,
            lastSelectedVirtualFileParentModuleDirName: String?
        ): Boolean {

            val constructedName: String = constructNewTabName(
                project,
                lastSelectedVirtualFile,
                lastSelectedVirtualFileParent,
                lastSelectedVirtualFileParentModule,
                lastSelectedVirtualFileParentModuleDirName
            )

            val terminalExists: Content? =
                TerminalTabsUtil
                    .alreadyExistingTerminalTab(
                        terminalToolWindowContentManger.contents.toList(), constructedName
                    )

            if (settingsService.state.alreadyExists && terminalExists != null) {
                withContext(Dispatchers.EDT) {
                    TerminalTabsUtil.sortTabs(terminalToolWindowContentManger, settingsService)
                    TerminalTabsUtil.selectNewTab(terminalToolWindowContentManger, constructedName)
                    TerminalTabsUtil.activateTerminalWindow(project)

                    if (settingsService.state.performManualRenaming) {
                        TerminalTabsUtil.performManualRenamingAction(terminalExists)
                    }
                }
                return true
            }
            return false
        }

        private suspend fun renameNewTab(
            terminalToolWindowContentManger: ContentManager,
            project: Project,
            lastSelectedVirtualFile: VirtualFile,
            lastSelectedVirtualFileParent: VirtualFile?,
            lastSelectedVirtualFileParentModule: Module?,
            lastSelectedVirtualFileParentModuleDirName: String?
        ): Content? {

            val constructedName: String = constructNewTabName(
                project,
                lastSelectedVirtualFile,
                lastSelectedVirtualFileParent,
                lastSelectedVirtualFileParentModule,
                lastSelectedVirtualFileParentModuleDirName
            )

            val terminalExists: Content? = TerminalTabsUtil.alreadyExistingTerminalTab(
                terminalToolWindowContentManger.contents.toList(), constructedName
            )

            if (!(settingsService.state.alreadyExists && terminalExists != null)) {
                TerminalTabsUtil.getLastOpenedTab(terminalToolWindowContentManger)
                    ?.let { newTerminalTabContent ->

                        val newDisplayName = TerminalTabsUtil.incrementNumberInName(
                            terminalToolWindowContentManger.contents.toList(), constructedName
                        )
                        newTerminalTabContent.displayName = newDisplayName
                        newTerminalTabContent.tabName = newDisplayName

                        withContext(Dispatchers.EDT) {
                            TerminalTabsUtil.sortTabs(
                                terminalToolWindowContentManger,
                                settingsService
                            )
                            TerminalTabsUtil.selectNewTab(
                                terminalToolWindowContentManger, newTerminalTabContent.displayName
                            )
                            TerminalTabsUtil.activateTerminalWindow(project)

                            if (settingsService.state.performManualRenaming) {
                                TerminalTabsUtil.performManualRenamingAction(newTerminalTabContent)
                            }
                        }
                        return newTerminalTabContent
                    }
            }
            return null
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
                settingsService.state.selectedTabTypeName == TabNameTypeEnum.FIRST_DIR_NAME && lastSelectedVirtualFile.isFile ->
                    lastSelectedVirtualFileParent?.name ?: project.name

                settingsService.state.selectedTabTypeName == TabNameTypeEnum.MODULE_NAME ->
                    lastSelectedVirtualFileParentModule?.name ?: project.name

                settingsService.state.selectedTabTypeName == TabNameTypeEnum.MODULE_DIR_NAME ->
                    lastSelectedVirtualFileParentModuleDirName
                        ?: lastSelectedVirtualFileParentModule?.name
                        ?: project.name

                settingsService.state.selectedTabTypeName == TabNameTypeEnum.PROJECT_NAME -> project.name

                else -> name
            }

            if (settingsService.state.useCurrentDate) {
                name += " <${SimpleDateFormat(settingsService.state.dateTemplate).format(Date())}>"
            }

            return name
        }
    }
}