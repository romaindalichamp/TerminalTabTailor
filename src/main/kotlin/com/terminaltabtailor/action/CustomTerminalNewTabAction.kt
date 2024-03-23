package com.terminaltabtailor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.terminaltabtailor.manager.TerminalTabNamesManager
import java.util.*

open class CustomTerminalNewTabAction(
    val text: String = ResourceBundle.getBundle("TerminalTabTailorBundle")
        .getString("action.customTerminalNewTab.text"),
) : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        TerminalTabNamesManager.openTabInTerminal(e)
    }

    companion object {
        const val ACTION_ID: String = "Terminal.NewTerminalTabTailorTab"
    }
}