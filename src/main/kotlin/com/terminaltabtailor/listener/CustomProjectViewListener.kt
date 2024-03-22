package com.terminaltabtailor.listener

import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewListener
import com.intellij.openapi.project.Project

class CustomProjectViewListener(val project: Project) : ProjectViewListener {
    private var alreadyRegistered: Boolean = false
    override fun paneShown(current: AbstractProjectViewPane, previous: AbstractProjectViewPane?) {
        super.paneShown(current, previous)
        if (!alreadyRegistered) {
            current.tree.addTreeSelectionListener(ProjectTreeSelectionListener(project))
            alreadyRegistered = true
        }
    }
}