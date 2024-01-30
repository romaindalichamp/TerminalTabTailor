package com.terminaltabtailor.activities

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.terminaltabtailor.listeners.TerminalActionListener
import com.terminaltabtailor.listeners.ProjectTreeSelectionListener
import com.terminaltabtailor.managers.TerminalTabNamesManager

class StartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project
                .messageBus
                .connect()
                .subscribe(AnActionListener.TOPIC, TerminalActionListener(project, TerminalTabNamesManager()))

        ApplicationManager
                .getApplication()
                .invokeLater {
                    val projectView = ProjectView.getInstance(project)
                    val projectViewPane = projectView.currentProjectViewPane ?: return@invokeLater

                    projectViewPane
                            .tree
                            .addTreeSelectionListener(ProjectTreeSelectionListener(project, projectView.currentProjectViewPane.tree))

                }
    }

}