package com.terminaltabtailor.activities

import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.terminaltabtailor.listeners.TerminalActionListener
import com.terminaltabtailor.listeners.TerminalTabsListener
import com.terminaltabtailor.managers.TerminalTabNamesManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class StartupActivity : ProjectActivity {

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun execute(project: Project) {
        project
            .messageBus
            .connect()
            .subscribe(
                AnActionListener.TOPIC,
                TerminalActionListener(project, TerminalTabNamesManager())
            )
        GlobalScope.launch(Dispatchers.EDT) {
            ToolWindowManager
                .getInstance(project)
                .getToolWindow("Terminal")
                ?.contentManager
                ?.addContentManagerListener(TerminalTabsListener(project))
        }
    }
}
