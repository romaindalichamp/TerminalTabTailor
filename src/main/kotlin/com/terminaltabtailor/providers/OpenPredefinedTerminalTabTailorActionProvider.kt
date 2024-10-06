package com.terminaltabtailor.provider

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import com.terminaltabtailor.action.OpenTerminalSettingsAction
import org.jetbrains.plugins.terminal.ui.OpenPredefinedTerminalActionProvider

class OpenPredefinedTerminalTabTailorActionProvider : OpenPredefinedTerminalActionProvider {
    override fun listOpenPredefinedTerminalActions(project: Project): List<AnAction> {
        return listOf(Separator.getInstance(), OpenTerminalSettingsAction())
    }
}
