package com.terminaltabtailor.action

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsContexts
import com.terminaltabtailor.manager.TerminalTabNamesManager
import com.terminaltabtailor.settings.TerminalTabTailorSettingsService
import com.terminaltabtailor.util.VirtualSelectionUtil
import org.jetbrains.plugins.terminal.TerminalBundle

/**
 * This file includes software developed by JetBrains ([https://www.jetbrains.com](https://www.jetbrains.com/)) and its contributors
 *
 * Parts of this file are based on "[CustomRevealFileInTerminalAction.kt](https://github.com/JetBrains/intellij-community/blob/bbee37c964da0c0d284cc2b682535b69ad4aeaec/plugins/terminal/src/org/jetbrains/plugins/terminal/action/RevealFileInTerminalAction.java)" from the IntelliJ IDEA Community Edition ([GitHub](https://github.com/JetBrains/intellij-community)), distributed under the Apache License 2.0.
 *
 * ## Custom RenameTerminalSessionAction
 *
 * This custom action serves as a substitute for the original "RevealFileInTerminalAction" and retains its core functionality while integrating a coroutine to enhance its capabilities.
 *
 * Specifically, it aims to:
 * - reuse automatically a terminal tab when they already exists with the same name
 * - rename tabs automatically regarding the settings the user chose in the plugin (filename, parent directory, module name, module directory name, project name)
 *
 * This enhanced action can be triggered from any sources where the traditional action is used.
 */
class CustomRevealFileInTerminalAction(
    @NlsContexts.Label val text: String = TerminalBundle.message(ActionId.OPEN_IN_TERMINAL_TEXT_ID),
) : DumbAwareAction() {
    private val settingsService = service<TerminalTabTailorSettingsService>()

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = text
        e.presentation.isEnabledAndVisible = isAvailable(e)
    }

    private fun isAvailable(e: AnActionEvent): Boolean {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        return project != null && !LightEdit.owns(project) && VirtualSelectionUtil.getSelectedFile(
            e,
            project
        ) != null &&
                (!ActionPlaces.isPopupPlace(e.place) || editor == null || !editor.selectionModel.hasSelection())
    }

    override fun actionPerformed(e: AnActionEvent) {
        TerminalTabNamesManager.openTabInTerminal(e)
    }

}