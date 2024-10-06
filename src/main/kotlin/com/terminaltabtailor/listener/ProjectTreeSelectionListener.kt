package com.terminaltabtailor.listener

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.terminaltabtailor.data.ProjectSpecificLastVirtualFile
import com.terminaltabtailor.enum.TabNameOriginEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class ProjectTreeSelectionListener(val project: Project) : TreeSelectionListener {

    override fun valueChanged(e: TreeSelectionEvent?) {
        (e?.source as? JTree).let {
            ApplicationManager.getApplication().invokeLater {
                GlobalScope.launch {
                    withContext(Dispatchers.EDT) {
                        ProjectSpecificLastVirtualFile.setLastVirtualFileForProject(
                            project,
                            CommonDataKeys
                                .VIRTUAL_FILE
                                .getData(DataManager.getInstance().getDataContext(it)),
                            TabNameOriginEnum.PROJECT_TREE_VIEW
                        )
                    }
                }
            }
        }
    }
}