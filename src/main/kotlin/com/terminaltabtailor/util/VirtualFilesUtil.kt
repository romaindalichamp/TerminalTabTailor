package com.terminaltabtailor.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.terminaltabtailor.listeners.TerminalActionListener

class VirtualFilesUtil {
    companion object {
        private fun getVirtualFileFromContext(dataContext: DataContext): VirtualFile? {
            return CommonDataKeys.VIRTUAL_FILE.getData(dataContext)
        }

        /*
         * Could be achieved with: Paths.get(parentModule?.moduleFilePath).parent?.fileName?.toString()
         * But it avoids this warning: 'getModuleFilePath()' is marked unstable with @ApiStatus.Internal
         */
        fun getModuleDirectoryName(module: Module?): String? {
            val contentRoots = module?.let { ModuleRootManager.getInstance(it).contentRoots }
            val moduleDir = contentRoots?.firstOrNull()?.path
            return moduleDir?.substringAfterLast('/')
        }

        fun getVirtualSelection(
            project: Project,
            event: AnActionEvent
        ): TerminalActionListener.VirtualSelection {
            val virtualSelection = TerminalActionListener
            var virtualFile: VirtualFile? = null

            ApplicationManager.getApplication().runReadAction {
                runCatching {
                    virtualFile = getVirtualFileFromContext(event.dataContext)
                }.getOrElse {
                    virtualFile = getRootNodeVirtualFileAsArray(project)
                }
            }

            virtualFile?.let { vFile ->
                virtualSelection.lastSelectedVirtualFile = vFile

                virtualSelection.let {

                    virtualSelection.lastSelectedVirtualFile?.let {

                        virtualSelection.lastSelectedVirtualFileParent =
                            virtualSelection.lastSelectedVirtualFile!!.parent

                        ApplicationManager.getApplication().runReadAction {
                            runCatching {
                                val module = ModuleUtilCore.findModuleForFile(
                                    virtualSelection.lastSelectedVirtualFile!!,
                                    project
                                )
                                virtualSelection.lastSelectedVirtualFileParentModule = module


                                virtualSelection.lastSelectedVirtualFileParentModuleDirName =
                                    getModuleDirectoryName(
                                        virtualSelection.lastSelectedVirtualFileParentModule
                                    )
                            }
                        }
                    }
                }
            }
            return virtualSelection
        }

        private fun getRootNodeVirtualFileAsArray(project: Project): VirtualFile? {
            val basePath: String? = project.basePath
            return basePath?.let {
                LocalFileSystem.getInstance().findFileByPath(it)
            }
        }
    }
}