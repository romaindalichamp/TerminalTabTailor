package com.terminaltabtailor.activities

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.terminaltabtailor.actions.ActionId
import com.terminaltabtailor.actions.CustomRenameTerminalSessionAction
import com.terminaltabtailor.actions.CustomRevealFileInTerminalAction

class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val actionManager = ActionManager.getInstance()
        actionManager
            .getAction(ActionId.TERMINAL_RENAME_SESSION_ID)?.let {
                actionManager
                    .replaceAction(
                        ActionId.TERMINAL_RENAME_SESSION_ID,
                        CustomRenameTerminalSessionAction(
                            ActionId.TERMINAL_RENAME_SESSION_ID,
                            "Rename Session"
                        )
                    )
            }

        actionManager
            .getAction(ActionId.OPEN_IN_TERMINAL_ID)?.let {
                actionManager
                    .replaceAction(
                        ActionId.OPEN_IN_TERMINAL_ID,
                        CustomRevealFileInTerminalAction()
                    )
            }

    }
}
