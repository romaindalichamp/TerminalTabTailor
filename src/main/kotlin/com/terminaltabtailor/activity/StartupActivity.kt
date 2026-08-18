package com.terminaltabtailor.activity

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.terminaltabtailor.action.ActionId
import com.terminaltabtailor.action.CustomRenameTerminalSessionAction
import com.terminaltabtailor.action.CustomRevealFileInTerminalAction
import com.terminaltabtailor.service.CurrentDirectoryTabNameTracker
import org.jetbrains.plugins.terminal.TerminalBundle
import java.util.*


class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // No-op until the user turns "keep the name of the current folder" on; the loop stops with
        // the project, since it runs on the service's own scope.
        CurrentDirectoryTabNameTracker.getInstance(project).start()

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

        /*
         * The platform already registers one "open in terminal" entry per engine, so the plugin takes
         * both over rather than adding its own. They are relabelled because neither creation API
         * follows the "Terminal engine" setting, which makes the choice explicit to the user.
         */
        val bundle = ResourceBundle.getBundle("TerminalTabTailorBundle")

        actionManager
            .getAction(ActionId.OPEN_IN_TERMINAL_ID)?.let {
                actionManager
                    .replaceAction(
                        ActionId.OPEN_IN_TERMINAL_ID,
                        CustomRevealFileInTerminalAction(
                            bundle.getString(ActionId.OPEN_IN_TERMINAL_CLASSIC_TEXT_ID),
                            opensReworkedEngine = false
                        )
                    )
            }

        actionManager
            .getAction(ActionId.OPEN_IN_REWORKED_TERMINAL_ID)?.let {
                actionManager
                    .replaceAction(
                        ActionId.OPEN_IN_REWORKED_TERMINAL_ID,
                        CustomRevealFileInTerminalAction(
                            bundle.getString(ActionId.OPEN_IN_TERMINAL_REWORKED_TEXT_ID),
                            opensReworkedEngine = true
                        )
                    )
            }
    }
}
