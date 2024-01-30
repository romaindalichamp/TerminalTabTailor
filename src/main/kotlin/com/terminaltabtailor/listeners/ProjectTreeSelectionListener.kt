package com.terminaltabtailor.listeners

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.util.VirtualFilesUtil
import javax.swing.JTree
import javax.swing.event.TreeSelectionListener

class ProjectTreeSelectionListener(private val project: Project, private val tree: JTree) : TreeSelectionListener {
    companion object {
        var lastSelectedVirtualFileParentModuleDirName: String? = null
        var lastSelectedVirtualFileParentModule: Module? = null
        lateinit var lastSelectedVirtualFile: VirtualFile
        lateinit var lastSelectedVirtualFileParent: VirtualFile
    }
    override fun valueChanged(e: javax.swing.event.TreeSelectionEvent?) {
        VirtualFilesUtil.getVirtualFilesFromContext(
                VirtualFilesUtil.getDataContextFromTree(tree))
                ?.firstOrNull()
                ?.let { virtualFile ->
                    lastSelectedVirtualFile = virtualFile
                    lastSelectedVirtualFileParent = virtualFile.parent
                    lastSelectedVirtualFileParentModule = ModuleUtilCore.findModuleForFile(virtualFile, project)
                    lastSelectedVirtualFileParentModuleDirName = VirtualFilesUtil.getModuleDirectoryName(lastSelectedVirtualFileParentModule)
                }
    }
}