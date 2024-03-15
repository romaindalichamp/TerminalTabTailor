package com.terminaltabtailor.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.terminaltabtailor.settings.TerminalTabTailorConfigurable
import java.util.*

class OpenTerminalSettingsAction :
    AnAction(
        ResourceBundle.getBundle("TerminalTabTailorBundle").getString("action.openTerminalSettingsAction.text"),
        ResourceBundle.getBundle("TerminalTabTailorBundle").getString("action.openTerminalSettingsAction.description"),
        AllIcons.General.Settings
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil
            .getInstance()
            .showSettingsDialog(e.project, TerminalTabTailorConfigurable::class.java)
    }
}