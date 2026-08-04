package com.terminaltabtailor.manager

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.terminaltabtailor.data.VirtualSelection
import com.terminaltabtailor.enum.TabNameTypeEnum
import com.terminaltabtailor.settings.TerminalTabTailorSettings
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
        fun openTabInTerminal(e: AnActionEvent, openReworkedEngine: Boolean = true) {
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
                                    createNamedTab(
                                        project,
                                        terminalToolWindowContentManger,
                                        selectedFile,
                                        openReworkedEngine
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

        suspend fun createNamedTab(
            project: Project,
            terminalToolWindowContentManger: ContentManager,
            selectedFile: VirtualFile,
            openReworkedEngine: Boolean = true
        ) {

            val virtualSelection: VirtualSelection = VirtualSelectionUtil.getVirtualSelection(project, selectedFile)

            virtualSelection.lastSelectedVirtualFile?.let {

                createNamedTab(
                    terminalToolWindowContentManger,
                    project,
                    virtualSelection.lastSelectedVirtualFile!!,
                    virtualSelection.lastSelectedVirtualFileParent,
                    virtualSelection.lastSelectedVirtualFileParentModule,
                    virtualSelection.lastSelectedVirtualFileParentModuleDirName,
                    openReworkedEngine
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

        private suspend fun createNamedTab(
            terminalToolWindowContentManger: ContentManager,
            project: Project,
            lastSelectedVirtualFile: VirtualFile,
            lastSelectedVirtualFileParent: VirtualFile?,
            lastSelectedVirtualFileParentModule: Module?,
            lastSelectedVirtualFileParentModuleDirName: String?,
            openReworkedEngine: Boolean
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

            if (settingsService.state.alreadyExists && terminalExists != null) {
                return null
            }

            val workingDirectory = workingDirectoryOf(lastSelectedVirtualFile)

            return withContext(Dispatchers.EDT) {
                val newDisplayName = TerminalTabsUtil.incrementNumberInName(
                    terminalToolWindowContentManger.contents.toList(), constructedName
                )

                /*
                 * Neither API follows the "Terminal engine" setting: TerminalToolWindowTabsManager
                 * belongs to the reworked frontend and always builds a reworked tab, while
                 * openTerminalIn always builds a classic one. The caller picks, through a dedicated
                 * menu entry per engine.
                 *
                 * The reworked builder also names the tab up front, which avoids the classic path's
                 * "create, then guess which tab was just opened, then rename it" dance.
                 */
                val newTerminalTabContent = if (openReworkedEngine) {
                    TerminalToolWindowTabsManager.getInstance(project)
                        .createTabBuilder()
                        .workingDirectory(workingDirectory)
                        .tabName(newDisplayName)
                        .requestFocus(true)
                        .createTab()
                        .content
                } else {
                    TerminalToolWindowManager.getInstance(project).openTerminalIn(lastSelectedVirtualFile)
                    TerminalTabsUtil.getLastOpenedTab(terminalToolWindowContentManger)
                } ?: return@withContext null

                /*
                 * Safety net: `userDefinedTitle` is the highest-priority component of
                 * TerminalTitle.buildTitle(), so the terminal cannot replace the name once the shell
                 * reports its own title. Null-safe, as a reworked tab may expose no classic widget.
                 */
                TerminalToolWindowManager
                    .findWidgetByContent(newTerminalTabContent)
                    ?.terminalTitle
                    ?.change { userDefinedTitle = newDisplayName }

                newTerminalTabContent.displayName = newDisplayName
                newTerminalTabContent.tabName = newDisplayName

                TerminalTabsUtil.sortTabs(
                    terminalToolWindowContentManger,
                    settingsService
                )
                TerminalTabsUtil.selectNewTab(
                    terminalToolWindowContentManger, newDisplayName
                )
                TerminalTabsUtil.activateTerminalWindow(project)

                if (settingsService.state.performManualRenaming) {
                    TerminalTabsUtil.performManualRenamingAction(newTerminalTabContent)
                }

                newTerminalTabContent
            }
        }

        /*
         * The tab builder takes a path, whereas the previous openTerminalIn() resolved the directory
         * from the VirtualFile itself.
         */
        private fun workingDirectoryOf(selected: VirtualFile): String? =
            if (selected.isDirectory) selected.path else selected.parent?.path

        /*
         * `settings` and `now` are parameters rather than reads of the global service so that the
         * naming rules can be exercised without a running application. Production callers omit both.
         */
        fun constructNewTabName(
            project: Project,
            lastSelectedVirtualFile: VirtualFile,
            lastSelectedVirtualFileParent: VirtualFile?,
            lastSelectedVirtualFileParentModule: Module?,
            lastSelectedVirtualFileParentModuleDirName: String?,
            settings: TerminalTabTailorSettings = settingsService.state,
            now: Date = Date(),
        ): String {
            var name = lastSelectedVirtualFile.name

            name = when {
                settings.selectedTabTypeName == TabNameTypeEnum.FIRST_DIR_NAME && lastSelectedVirtualFile.isFile ->
                    lastSelectedVirtualFileParent?.name ?: project.name

                settings.selectedTabTypeName == TabNameTypeEnum.MODULE_NAME ->
                    lastSelectedVirtualFileParentModule?.name ?: project.name

                settings.selectedTabTypeName == TabNameTypeEnum.MODULE_DIR_NAME ->
                    lastSelectedVirtualFileParentModuleDirName
                        ?: lastSelectedVirtualFileParentModule?.name
                        ?: project.name

                settings.selectedTabTypeName == TabNameTypeEnum.PROJECT_NAME -> project.name

                else -> name
            }

            if (settings.useCurrentDate) {
                name += " <${SimpleDateFormat(settings.dateTemplate).format(now)}>"
            }

            return name
        }
    }
}