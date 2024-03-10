package com.terminaltabtailor.activities

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.terminaltabtailor.actions.ActionId
import com.terminaltabtailor.actions.CustomRenameTerminalSessionAction
import com.terminaltabtailor.listeners.TerminalActionListener
import com.terminaltabtailor.managers.TerminalTabNamesManager

class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.connect().subscribe(
            AnActionListener.TOPIC, TerminalActionListener(project, TerminalTabNamesManager())
        )

        val actionManager = ActionManager.getInstance()
        actionManager.getAction(ActionId.TERMINAL_RENAME_SESSION_ID)?.let {
            actionManager.replaceAction(
                ActionId.TERMINAL_RENAME_SESSION_ID, CustomRenameTerminalSessionAction()
            )
        }
    }
}
