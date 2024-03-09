package com.terminaltabtailor.listeners

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.managers.TerminalTabNamesManager
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.TerminalTabsUtil
import com.terminaltabtailor.util.VirtualFilesUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TerminalActionListener(
    private val project: Project,
    private val terminalTabNamesManager: TerminalTabNamesManager
) : AnActionListener {
    private val settingsService = service<TerminalTabTailorSettingsService>()

    companion object VirtualSelection {
        var lastSelectedVirtualFile: VirtualFile? = null
        var lastSelectedVirtualFileParent: VirtualFile? = null
        var lastSelectedVirtualFileParentModule: Module? = null
        var lastSelectedVirtualFileParentModuleDirName: String? = null
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun afterActionPerformed(
        action: AnAction,
        event: AnActionEvent,
        result: AnActionResult
    ) {
        super.afterActionPerformed(action, event, result)

        if (ActionManager.getInstance().getId(action) == "Terminal.OpenInTerminal") {

            GlobalScope.launch(Dispatchers.EDT) {
                val virtualSelection = VirtualFilesUtil.getVirtualSelection(project, event)

                val terminalTab = TerminalTabsUtil.renameTab(
                    project,
                    terminalTabNamesManager,
                    virtualSelection
                )

                terminalTab?.let {
                    if (settingsService.state.performManualRenaming) {
                        TerminalTabsUtil.performManualRenamingAction(terminalTab)
                    }
                }
            }
        }
    }
}