package com.terminaltabtailor.listeners

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.project.Project
import com.terminaltabtailor.managers.TerminalTabNamesManager

class TerminalActionListener(private val project: Project, private val terminalTabNamesManager: TerminalTabNamesManager) : AnActionListener {
    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        super.afterActionPerformed(action, event, result)

        if (ActionManager.getInstance().getId(action) == "Terminal.OpenInTerminal") {
            terminalTabNamesManager.renameTerminalTab(project)
        }
    }
}