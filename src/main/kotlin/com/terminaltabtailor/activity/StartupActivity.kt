package com.terminaltabtailor.activity

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.terminaltabtailor.action.ActionId
import com.terminaltabtailor.action.CustomRenameTerminalSessionAction
import com.terminaltabtailor.action.CustomRevealFileInTerminalAction
import org.jetbrains.plugins.terminal.TerminalBundle


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
                            TerminalBundle.message(ActionId.RENAME_SESSION_LABEL_ID)
                        )
                    )
            }

        actionManager
            .getAction(ActionId.OPEN_IN_TERMINAL_ID)?.let {
                actionManager
                    .replaceAction(
                        ActionId.OPEN_IN_TERMINAL_ID,
                        CustomRevealFileInTerminalAction(
                            TerminalBundle.message(ActionId.OPEN_IN_TERMINAL_TEXT_ID)
                        )
                    )
            }
    }
}
