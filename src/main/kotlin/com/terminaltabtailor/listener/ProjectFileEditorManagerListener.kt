package com.terminaltabtailor.listener

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.terminaltabtailor.data.ProjectSpecificLastVirtualFile
import com.terminaltabtailor.enum.TabNameOriginEnum

open class ProjectFileEditorManagerListener(
    val project: Project
) : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
            event.manager.project,
            event.newFile,
            TabNameOriginEnum.FILE_EDITOR_MANAGER
        )
    }
}